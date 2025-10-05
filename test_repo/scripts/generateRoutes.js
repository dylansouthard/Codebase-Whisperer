const fs = require('fs')
const path = require('path')

const routesPath = path.join(__dirname, '../src/main/resources/data/routes.json')
const outputPath = path.join(__dirname, '../src/main/java/info/dylansouthard/StraysBookAPI/constants/ApiRoutes.java')

const data = JSON.parse(fs.readFileSync(routesPath, 'utf8'))

const lines = [];
lines.push('package info.dylansouthard.StraysBookAPI.constants;\n');
lines.push('public class ApiRoutes {');

// Helper: Convert key to Java class name (upper snake case)
const toClassName = (key) => key.replace(/[^a-zA-Z0-9]/g, '_').replace(/__+/g, '_').replace(/^_|_$/g, '').toUpperCase();

// Helper: Convert route key to constant name
const toConstantName = (key) => (key === '_base' ? 'BASE' : key.toUpperCase());

const formatPath = (path) => path.replace(/:([a-zA-Z0-9_]+)/g, '{$1}');

// Build nested static classes for each top-level API group
const flattenRoutes = (apiObj) => {
    for (const groupKey in apiObj) {
        const group = apiObj[groupKey];
        // Only consider objects
        if (typeof group !== 'object' || group === null) continue;
        const className = toClassName(groupKey);
        lines.push(`    public static class ${className} {`);
        // Gather all keys in this group that have a _path
        for (const routeKey in group) {
            if (routeKey === '_path') continue; // skip _path at group level
            const node = group[routeKey];

            if (node && (typeof node === 'string' ||　(typeof node === 'object' && node._path))) {
                // Constant name: BASE for "_base", else uppercase
                const constName = toConstantName(routeKey);
                lines.push(`        public static final String ${constName} = "${formatPath(typeof node === 'string' ? node : node._path)}";`);
            }
        }
        lines.push('    }');
    }
};

flattenRoutes(data.api);
lines.push('}');

fs.writeFileSync(outputPath, lines.join('\n'));
console.log(`✅ ApiRoutes.java generated at ${outputPath}`);