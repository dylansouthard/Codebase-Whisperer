# codebase_expert/config.py
from __future__ import annotations
import os
import copy
from typing import Any, Dict, List, Optional, Tuple
from pathlib import Path
import yaml

# Central defaults live here so everyone imports the same thing
DEFAULT_CONFIG: Dict[str, Any] = {
    "repo_root": "./",
    "db_dir": ".codebase_whisperer/lancedb", #TODO db not set up yet
    "table": "chunks",
    "ollama": {
        "host": "http://localhost:11434",
        "embed_model": "nomic-embed-text",
        "chat_model": "llama3:8b",
        "chat_context": 8192,
    },
    "indexing": {
        "include_globs": [
            "**/*.java", "**/*.xml", "**/*.properties",
            "**/*.yml", "**/*.yaml", "**/*.gradle", "**/pom.xml"
        ],
        "exclude_globs": [
            "**/target/**", "**/build/**", "**/.git/**",
            "**/.idea/**", "**/.gradle/**", "**/node_modules/**", "**/.m2/**"
        ],
        "max_file_mb": 1.5,
        "max_chunk_chars": 2400,
        "min_chunk_chars": 200,
        "use_tree_sitter_java": True,
        "embed_batch_size": 24,
        "flush_every": 2000,
        "encodings": ["utf-8", "utf-8-sig", "cp932", "shift_jis", "cp1252", "latin-1"],
    },
    "retrieval": {
        "top_k": 12,
        "max_context_chars": 14000,
    },
}

def _read_yaml_or_json(p: Path) -> Dict[str, Any]:
    """
    JSON is a subset of YAML; yaml.safe_load will parse both.
    """

    with p.open("r", encoding="utf-8") as f:
        data = yaml.safe_load(f)
    return data or {}

def _merge_dicts(base: Dict[str, Any], override: Dict[str, Any]) -> None:
    """
    Shallow-recursive merge: override wins; merges nested dicts.
    Mutates 'base' in place.
    """
    for k, v in override.items():
        if isinstance(v, dict) and isinstance(base.get(k), dict):
            _merge_dicts(base[k], v)
        else:
            base[k] = v

def find_config_path(cli_path: Optional[str]) -> Optional[Path]:
    """
    Precedence:
      1) --config argument
      2) $CODEEXPERT_CONFIG
      3) ./config.yaml or ./config.json
      4) ./.codeexpert/config.yaml or .json
      5) ~/.config/codebase-expert/config.yaml or .json
    """
    candidates: List[Path] = []
    if cli_path:
        candidates.append(Path(cli_path))

    env_path = os.environ.get("CODEEXPERT_CONFIG")
    if env_path:
        candidates.append(Path(env_path))

    cwd = Path.cwd()
    candidates.extend([
        cwd / "config.yaml",
        cwd / "config.json",
        cwd / ".codeexpert" / "config.yaml",
        cwd / ".codeexpert" / "config.json",
        Path.home() / ".config" / "codebase-expert" / "config.yaml",
        Path.home() / ".config" / "codebase-expert" / "config.json",
    ])

    for p in candidates:
        if p.is_file():
            return p
    return None

def load_config(cli_path: Optional[str]) -> Tuple[Dict[str, Any], Optional[Path]]:
    """
    Returns (config_dict, loaded_from_path_or_None).
    If no file found, returns a deep-copied DEFAULT_CONFIG and None.
    """
    cfg = copy.deepcopy(DEFAULT_CONFIG)  # cheap deep copy
    p = find_config_path(cli_path)
    if p:
        file_cfg = _read_yaml_or_json(p)
        _merge_dicts(cfg, file_cfg)
    return cfg, p
