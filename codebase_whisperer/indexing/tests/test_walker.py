# tests/test_walker.py
from pathlib import Path

from ..walker import iter_files


def _touch(p: Path, content: str = "x"):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(content, encoding="utf-8")


def test_basic_includes_and_excludes(tmp_path: Path):
    # tree:
    #   src/main/Foo.java
    #   target/gen/Bar.java
    #   pom.xml
    _touch(tmp_path / "src/main/Foo.java")
    _touch(tmp_path / "target/gen/Bar.java")
    _touch(tmp_path / "pom.xml")

    includes = ["**/*.java", "pom.xml"]
    excludes = ["**/target/**"]

    got = sorted(
        str(Path(p).relative_to(tmp_path))
        for p in iter_files(tmp_path, includes, excludes, max_file_mb=5)
    )
    # target/** should be pruned; Foo.java and pom.xml should remain
    assert got == ["pom.xml", "src/main/Foo.java"]


def test_directory_include_glob_admits_children(tmp_path: Path):
    # tree:
    #   src/main/java/A.java
    #   src/test/java/ATest.java
    _touch(tmp_path / "src/main/java/A.java")
    _touch(tmp_path / "src/test/java/ATest.java")

    includes = ["src/main/**"]  # include dir subtree only
    excludes = []

    got = sorted(
        str(Path(p).relative_to(tmp_path))
        for p in iter_files(tmp_path, includes, excludes, max_file_mb=5)
    )
    assert got == ["src/main/java/A.java"]


def test_hidden_files_and_dirs(tmp_path: Path):
    # tree:
    #   .git/config
    #   app/.secret
    #   app/app.java
    _touch(tmp_path / ".git/config")
    _touch(tmp_path / "app/.secret")
    _touch(tmp_path / "app/app.java")

    includes = ["**/*"]
    excludes = ["**/.git/**"]

    # include_hidden=False should skip dotfiles and dot-dirs
    got_hidden_off = sorted(
        str(Path(p).relative_to(tmp_path))
        for p in iter_files(tmp_path, includes, excludes, max_file_mb=5, include_hidden=False)
    )
    assert got_hidden_off == ["app/app.java"]

    # include_hidden=True (default) should include app/.secret as well
    got_hidden_on = sorted(
        str(Path(p).relative_to(tmp_path))
        for p in iter_files(tmp_path, includes, excludes, max_file_mb=5, include_hidden=True)
    )
    assert got_hidden_on == ["app/.secret", "app/app.java"]


def test_size_gating(tmp_path: Path):
    small = tmp_path / "small.txt"
    big = tmp_path / "big.txt"
    _touch(small, "x" * 10)
    _touch(big, "y" * (2 * 1024 * 1024))  # ~2MB

    includes = ["**/*.txt"]
    excludes = []
    # max_file_mb=1 should drop big
    got = sorted(
        str(Path(p).relative_to(tmp_path))
        for p in iter_files(tmp_path, includes, excludes, max_file_mb=1)
    )
    assert got == ["small.txt"]


def test_symlink_behavior(tmp_path: Path):
    # make a real file and a symlink to it
    real = tmp_path / "real.txt"
    _touch(real, "hello")

    link = tmp_path / "link.txt"
    try:
        link.symlink_to(real)  # may require permissions on Windows
        symlinks_supported = True
    except (NotImplementedError, OSError):
        symlinks_supported = False

    includes = ["**/*.txt"]
    excludes = []

    got_no_follow = sorted(
        str(Path(p).relative_to(tmp_path))
        for p in iter_files(tmp_path, includes, excludes, max_file_mb=5, follow_symlinks=False)
    )
    # If symlinks are supported, the link should be skipped; else it's absent anyway.
    if symlinks_supported:
        assert got_no_follow == ["real.txt"]
    else:
        assert got_no_follow == ["real.txt"]

    got_follow = sorted(
        str(Path(p).relative_to(tmp_path))
        for p in iter_files(tmp_path, includes, excludes, max_file_mb=5, follow_symlinks=True)
    )
    if symlinks_supported:
        assert got_follow == ["link.txt", "real.txt"]
    else:
        assert got_follow == ["real.txt"]
