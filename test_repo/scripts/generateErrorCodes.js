const fs = require('fs');
const path = require('path');
const {camelCase} = require('./utilities.js')

const outputDirectory = path.join(__dirname, '../src/main/java/info/dylansouthard/StraysBookAPI/errors/');

const errorsJsonPath = path.join(__dirname, '../src/main/resources/data/errors.json');
const codesOutputPath = outputDirectory + 'ErrorCodes.java';
const messagesOutPath = outputDirectory + 'ErrorMessages.java';

const errors = JSON.parse(fs.readFileSync(errorsJsonPath, 'utf8'));

const keys = Object.keys(errors);
let javaPackage = `package info.dylansouthard.StraysBookAPI.errors;\nimport java.util.Map;\nimport java.util.function.Supplier;\n\npublic interface `
let javaCode = javaPackage + `ErrorCodes {\n`;

keys.forEach(key => {
    javaCode += `    String ${key} = "${key}";\n`;
});

javaCode += `}\n`;

fs.writeFileSync(codesOutputPath, javaCode);

console.log('✅ ErrorCodes.java generated successfully!');

//GENERATE MESSAGES


let javaMessage = javaPackage + 'ErrorMessages {\n'

keys.forEach(key => {
    javaMessage += `    String ${key} = "${errors[key]['message']['en']}";\n`;
})

javaMessage += `}\n`;

fs.writeFileSync(messagesOutPath, javaMessage);

console.log('✅ ErrorMessages.java generated successfully!');

const factoryOutPath = outputDirectory + 'ErrorFactory.java';

// Start the factory class
let javaFactory = javaPackage + `ErrorFactory {\n\n`;

javaFactory += `    public static AppException of(String code, String message, int status) {\n`;
javaFactory += `        return new AppException(code, message, status);\n`;
javaFactory += `    }\n\n`;

const errorMapEntries = []
// Generate factory methods for each error code
keys.forEach(key => {
    const msg = errors[key]['message']['en'].replace(/"/g, '\\"'); // Escape quotes
    const status = errors[key]['status'];
    const functionName = `${camelCase(key)}`
    javaFactory += `    public static AppException ${functionName}() {\n`;
    javaFactory += `        return of(ErrorCodes.${key}, ErrorMessages.${key}, ${status});\n`;
    javaFactory += `    }\n\n`;
    errorMapEntries.push(`        Map.entry("${key}", ErrorFactory::${functionName})`);
});
var errorMapper = `    public static final Map<String, Supplier<AppException>> errorMap = Map.ofEntries(\n`
errorMapper += errorMapEntries.join(',\n');
errorMapper += `\n    );\n\n    static AppException getErrorOfType(String code) {\n`
errorMapper += `        return errorMap.getOrDefault(code, ErrorFactory::internalServerError).get();\n`;
errorMapper += `    }\n\n`;

javaFactory += errorMapper
javaFactory += `}\n`;

fs.writeFileSync(factoryOutPath, javaFactory);

console.log('✅ ErrorFactory.java generated successfully!');