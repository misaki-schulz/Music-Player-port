"""Reject incomplete Forge Minecraft sources even if the decompiler exits successfully."""
from pathlib import Path
import os
import sys

version = sys.argv[1]
cache = Path(os.environ.get('GRADLE_USER_HOME', str(Path.home() / '.gradle')))
cache /= 'caches/minecraftforge/forgegradle/mavenizer/caches/forge/.global/mcp'
for config in cache.glob(version + '-*'):
    for log in config.glob('forge/*/decompile/log.txt'):
        errors = [line for line in log.read_text(encoding='utf-8', errors='replace').splitlines()
                  if line.startswith(('ERROR:', 'java.lang.OutOfMemoryError'))]
        if errors:
            raise SystemExit(f'Incomplete Minecraft decompilation: {log}\n' + '\n'.join(errors[:8]))
