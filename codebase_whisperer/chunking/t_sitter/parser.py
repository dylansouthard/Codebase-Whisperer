from __future__ import annotations
import os
from typing import Any, Optional, Tuple

def get_ts_parser(lang_name: str) -> Tuple[Optional[Any], Optional[Any]]:
    """
    Return a (parser, language) tuple for the requested language using:
      1) tree_sitter_language_pack (prebuilt grammars)
      2) TREE_SITTER_<LANG>_LIB env var pointing to a compiled .so/.dylib
    On failure, returns (None, None).
    """
    try:
        from tree_sitter import Language, Parser
        env_key = f"TREE_SITTER_{lang_name.upper()}_LIB"
        lib = os.environ.get(env_key)
        if lib and os.path.exists(lib):
            lang = Language(lib, lang_name)
            parser = Parser(lang)
            return parser, lang
    except Exception:
        pass

    try:
        from tree_sitter import Parser
        from tree_sitter_language_pack import get_language
        lang = get_language(lang_name)
        parser = Parser(lang)
        return parser, lang
    except Exception:
        pass

    return None, None