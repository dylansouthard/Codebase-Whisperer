
const fs = require("fs");
const path = require("path");

const rulesPath = path.join(__dirname, "../src/main/resources/rules/update-rules/animal_update_rules.json");
const outputPath = path.join(__dirname, "../src/test/resources/test-data/animal_update_cases.json");

const rules = JSON.parse(fs.readFileSync(rulesPath, "utf8"));

const generateTestCase = (field, isPrimaryCaretaker, originalValue, updateValue, shouldSucceed) => {
    return {
        desc: `Update field: ${field} by ${isPrimaryCaretaker ? "primary caretaker " : "non-primary caretaker"} with original value of ${originalValue}`,
        userIsPrimaryCaretaker: isPrimaryCaretaker,
        originalValues: {[field]: originalValue},
        updates: {
            [field]: updateValue
        },
        shouldSucceed: shouldSucceed,
        throwsError: shouldSucceed ? null : "AUTH_FORBIDDEN"
    };
};

const testCases = [];

Object.entries(rules).forEach(([field, rule]) => {
    testCases.push(generateTestCase(field, false, rule.testValue, rule.testUpdateValue, rule.access === "PUBLIC") )
    testCases.push(generateTestCase(field, true, rule.testValue, rule.testUpdateValue, true ))

    if (rule.access === "CONDITIONAL") {
        testCases.push(generateTestCase(field, false, rule.condition, rule.testUpdateValue, true) )
    }
})

fs.writeFileSync(outputPath, JSON.stringify(testCases, null, 2));
console.log(`✅ Test cases generated at: ${outputPath}`);