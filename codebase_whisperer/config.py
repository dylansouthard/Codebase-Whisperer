# codebase_whisperer/config.py
from __future__ import annotations
import os
import copy
from typing import Any, Dict, List, Optional, Tuple
from pathlib import Path
import yaml

# Central defaults live here so everyone imports the same thing
DEFAULT_CONFIG: Dict[str, Any] = {
    "repo_root": "./",
    "db_dir": ".codebase_whisperer/lancedb",
    "table": "chunks",
    # Embedding-centric config (primary)
    "embedding": {
        "model": "nomic-embed-text",
        "dim": 768,  # default dimension; can be overridden in config
    },
    # Ollama host + chat settings (keep embed_model for back-compat)
    "ollama": {
        "host": "http://localhost:11434",
        "embed_model": "nomic-embed-text",  # mirrored from embedding.model
        "chat_model": "qwen2.5-coder:14b",
        "chat_context": 8192,
    },
    "indexing": {
        "include_globs": [
            "**/*.java", "**/*.xml", "**/*.properties",
            "**/*.yml", "**/*.yaml", "**/*.gradle", "**/pom.xml"
        ],
        "exclude_globs": [
            "**/target/**", "**/build/**", "**/.git/**",
            "**/.idea/**", "**/.gradle/**", "**/node_modules/**", "**/.m2/**", "**/.txt/**"
        ],
        "max_file_mb": 1.5,
        "max_chunk_chars": 2400,
        "min_chunk_chars": 200,
        "use_tree_sitter_java": True,
        "embed_batch_size": 24,
        "flush_every": 2000,
        "include_hidden": True,
        "follow_symlinks": False,
        "encodings": ["utf-8", "utf-8-sig", "cp932", "shift_jis", "cp1252", "latin-1"],
    },
    "retrieval": {
        "top_k": 12,
        "max_context_chars": 14000,
    },
}

def _read_yaml_or_json(p: Path) -> Dict[str, Any]:
    """JSON is a subset of YAML; yaml.safe_load will parse both."""
    with p.open("r", encoding="utf-8") as f:
        data = yaml.safe_load(f)
    return data or {}

def _merge_dicts(base: Dict[str, Any], override: Dict[str, Any]) -> None:
    """Shallow-recursive merge: override wins; merges nested dicts. Mutates 'base' in place."""
    for k, v in override.items():
        if isinstance(v, dict) and isinstance(base.get(k), dict):
            _merge_dicts(base[k], v)
        else:
            base[k] = v

def _normalize(cfg: Dict[str, Any]) -> None:
    """
    Keep 'embedding.model' and 'ollama.embed_model' in sync.
    Accept user overrides in either location.
    """
    embedding = cfg.setdefault("embedding", {})
    ollama = cfg.setdefault("ollama", {})

    # If user provided embedding.model, mirror to ollama.embed_model
    if "model" in embedding:
        ollama["embed_model"] = embedding["model"]
    # If user only provided ollama.embed_model, mirror back into embedding.model
    elif "embed_model" in ollama:
        embedding["model"] = ollama["embed_model"]

    # Ensure embedding.dim exists (some tests override it)
    if "dim" not in embedding:
        embedding["dim"] = DEFAULT_CONFIG["embedding"]["dim"]

def find_config_path(cli_path: Optional[str]) -> Optional[Path]:
    """
    Precedence:
      1) --config argument
      2) $CODEBASE_WHISPERER_CONFIG
      3) ./config.yaml or ./config.json
      4) ./.codebase_whisperer/config.yaml or .json
      5) ~/.config/codebase-whisperer/config.yaml or .json
    """
    candidates: List[Path] = []
    if cli_path:
        candidates.append(Path(cli_path))

    env_path = os.environ.get("CODEBASE_WHISPERER_CONFIG")
    if env_path:
        candidates.append(Path(env_path))

    cwd = Path.cwd()
    candidates.extend([
        cwd / "config.yaml",
        cwd / "config.json",
        cwd / ".codebase_whisperer" / "config.yaml",
        cwd / ".codebase_whisperer" / "config.json",
        Path.home() / ".config" / "codebase-whisperer" / "config.yaml",
        Path.home() / ".config" / "codebase-whisperer" / "config.json",
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
    cfg = copy.deepcopy(DEFAULT_CONFIG)
    p = find_config_path(cli_path)
    if p:
        file_cfg = _read_yaml_or_json(p)
        _merge_dicts(cfg, file_cfg)
    _normalize(cfg)
    return cfg, p