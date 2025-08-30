# tests/test_parser.py
import types
import sys
import os
import pytest

from codebase_whisperer.chunking.t_sitter import parser as ts_parser


def _fake_tree_sitter_module(monkeypatch):
    """
    Build a fake `tree_sitter` module with Parser and Language classes
    that record calls to set_language.
    """
    fake = types.ModuleType("tree_sitter")

    class _Lang:
        def __init__(self, lib, name=None):
            self.lib = lib
            self.name = name

    class _Parser:
        def __init__(self, lang=None):
            self._lang = None
            if lang is not None:
                self._lang = lang
        def set_language(self, lang):
            self._lang = lang

    fake.Parser = _Parser
    fake.Language = _Lang
    monkeypatch.setitem(sys.modules, "tree_sitter", fake)
    return fake


def _fake_language_pack(monkeypatch):
    """
    Build a fake `tree_sitter_language_pack` with a get_language(lang) function.
    """
    fake = types.ModuleType("tree_sitter_language_pack")

    class _Lang:
        def __init__(self, name):
            self.name = name

    def get_language(name: str):
        return _Lang(name)

    fake.get_language = get_language
    monkeypatch.setitem(sys.modules, "tree_sitter_language_pack", fake)
    return fake


@pytest.fixture(autouse=True)
def reset_env(monkeypatch):
    # make sure tests don't see real env or real file system
    for k in list(os.environ.keys()):
        if k.startswith("TREE_SITTER_"):
            monkeypatch.delenv(k, raising=False)
    # default: path.exists returns False unless overridden in a test
    monkeypatch.setattr(os.path, "exists", lambda p: False)
    yield


def test_env_override_wins(monkeypatch):
    # Arrange: fake modules + env var + path exists
    _fake_tree_sitter_module(monkeypatch)
    monkeypatch.setenv("TREE_SITTER_JAVA_LIB", "/fake/libjava.so")
    monkeypatch.setattr(os.path, "exists", lambda p: p == "/fake/libjava.so")

    # Act
    parser, lang = ts_parser.get_ts_parser("java")

    # Assert
    assert parser is not None and lang is not None
    # our fake Parser stores the language in _lang (set_language called)
    assert getattr(parser, "_lang", None) is lang
    # and the Language we constructed should carry the lib/name we passed
    assert getattr(lang, "lib", None) == "/fake/libjava.so"
    assert getattr(lang, "name", None) == "java"


def test_fallback_to_language_pack_when_no_env(monkeypatch):
    _fake_tree_sitter_module(monkeypatch)
    _fake_language_pack(monkeypatch)

    # Act
    parser, lang = ts_parser.get_ts_parser("java")

    # Assert
    assert parser is not None and lang is not None
    # our fake Parser stores the language
    assert getattr(parser, "_lang", None) is lang
    # language-pack path constructs a _Lang with only `name`
    assert getattr(lang, "name", None) == "java"
    # no .lib attribute in this path
    assert not hasattr(lang, "lib")


def test_returns_none_when_neither_path_available(monkeypatch):
    # Don't provide tree_sitter_language_pack
    # Provide tree_sitter (so import doesn't fail), but no env var and exists=False
    _fake_tree_sitter_module(monkeypatch)
    import types, sys
    dummy = types.ModuleType("tree_sitter_language_pack")
    monkeypatch.setitem(sys.modules, "tree_sitter_language_pack", dummy)

    parser, lang = ts_parser.get_ts_parser("java")
    assert parser is None and lang is None


def test_env_var_key_construction(monkeypatch):
    """
    Ensures TREE_SITTER_<LANG>_LIB naming is uppercased.
    """
    _fake_tree_sitter_module(monkeypatch)
    # supply TS for types, make os.path.exists True only for the constructed key
    path = "/fake/libts.so"
    monkeypatch.setenv("TREE_SITTER_TYPESCRIPT_LIB", path)
    monkeypatch.setattr(os.path, "exists", lambda p: p == path)

    parser, lang = ts_parser.get_ts_parser("typescript")
    assert parser is not None and lang is not None
    assert getattr(lang, "lib", None) == path