
const fs = require("fs");
const path = require("path");

const rulesPath = path.join(__dirname, "../src/main/resources/rules/update-rules/user_update_rules.json");
const outputPath = path.join(__dirname, "../src/test/resources/test-data/user_update_cases.json");

const rules = JSON.parse(fs.readFileSync(rulesPath, "utf8"));

const generateTestCase = (field, nullable, shouldSucceed, originalValue, updateValue) => {
    return {
        desc: `Update field: ${field} to ${updateValue}`,
        originalValues: {[field]: originalValue},
        updates: {
            [field]: updateValue
        },
        nullable:nullable,
        shouldSucceed: shouldSucceed,
        throwsError: shouldSucceed ? null : "INVALID_PARAMS"
    };
};

const testCases = [];

Object.entries(rules).forEach(([field, rule]) => {
    testCases.push(generateTestCase(field, rule.nullable, true, rule.testValue, rule.testUpdateValue) )

    if (rule.nullable === false) {
        testCases.push(generateTestCase(field, rule.nullable, false, rule.testValue, null) )
        testCases.push(generateTestCase(field, rule.nullable, false, rule.testValue, "") )
    }
})

fs.writeFileSync(outputPath, JSON.stringify(testCases, null, 2));
console.log(`✅ Test cases generated at: ${outputPath}`);