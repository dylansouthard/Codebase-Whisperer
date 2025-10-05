const fs = require('fs')
const path = require('path')

const inputDirectory = path.join(__dirname, '../src/main/resources/data/enums/')
const outputDirectory = path.join(__dirname, '../src/main/java/info/dylansouthard/StraysBookAPI/model/enums/')

function generateEnumsfromJSON(inputFileName, outputFileName) {
    const inputFile = path.join(inputDirectory, inputFileName + '.json')
    const outputFile = path.join(outputDirectory, outputFileName + '.java')
    const values = JSON.parse(fs.readFileSync(inputFile, 'utf8'));
    let outputString = `package info.dylansouthard.StraysBookAPI.model.enums;\n\npublic enum ${outputFileName} {\n    `
    outputString += Object.keys(values).join(',\n    ')
    outputString += `\n}`
    fs.writeFileSync(outputFile, outputString, 'utf8');
    console.log(`✅ ${outputFileName}.java generated successfully!`);
}

generateEnumsfromJSON("notification_content_types", "NotificationContentType");
generateEnumsfromJSON("notification_types", "NotificationType");


