#!/bin/sh
set -eu
script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if command -v python3 >/dev/null 2>&1 && python3 -c 'import sys' >/dev/null 2>&1; then
    exec python3 "$script_dir/ports/release_cli.py" "$@"
fi
if ! command -v python >/dev/null 2>&1; then
    printf '%s\n' 'Python 3 is required to build Music Player port.8.' >&2
    exit 1
fi
exec python "$script_dir/ports/release_cli.py" "$@"
