# codebase_whisperer/indexing/languages.py

# Exact filename overrides (basename -> lang)
FILENAME_LANGUAGE_MAP = {
    "pom.xml": "xml",
}

# Extension-to-language map (case-insensitive).
# Add more as needed. Keep leading dot.
EXT_LANGUAGE_MAP = {
    # Java / JVM
    ".java": "java",
    ".gradle": "gradle",
    ".groovy": "groovy",
    ".kt": "kotlin",
    ".kts": "kotlin",

    # XML / config
    ".xml": "xml",
    ".properties": "properties",
    ".yml": "yaml",
    ".yaml": "yaml",

    # Web
    ".js": "javascript",
    ".mjs": "javascript",
    ".cjs": "javascript",
    ".jsx": "javascript",
    ".ts": "typescript",
    ".tsx": "typescript",
    ".d.ts": "typescript",

    # Shell / batch / scripts
    ".sh": "bash",
    ".bash": "bash",
    ".zsh": "bash",
    ".ksh": "bash",
    ".ps1": "powershell",
    ".bat": "batch",
    ".cmd": "batch",

    # Misc text
    ".md": "markdown",
    ".txt": "text",
}