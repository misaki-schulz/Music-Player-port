"""Build and package Forge/NeoForge ports sequentially."""
from __future__ import annotations
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
from release_cli import exclusive_operation

PORTS = Path(__file__).resolve().parent
TARGETS = json.loads((PORTS / 'loader-targets.json').read_text(encoding='utf-8'))

def build(targets):
    with exclusive_operation():
        for t in targets:
            print(f'Building {t["loader"]} / Minecraft {t["minecraft"]}', flush=True)
            project = PORTS / t['group']
            if os.name == 'nt':
                command = [shutil.which('pwsh') or 'powershell', '-NoProfile', '-ExecutionPolicy', 'Bypass',
                           '-File', str(PORTS/'Build-Target.ps1'), '-Group', t['group'], '-Version', t['minecraft']]
                subprocess.run(command, check=True)
            else:
                subprocess.run(['sh', './gradlew', 'assemble', f'-PtargetVersion={t["minecraft"]}',
                                '--no-daemon', '--max-workers=1', '--console=plain'], cwd=project, check=True)
            subprocess.run([sys.executable, str(PORTS/'package_loaders.py'), '--loader', t['loader'],
                            '--version', t['minecraft']], check=True)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--loader', choices=['forge','neoforge'])
    parser.add_argument('--version')
    parser.add_argument('--all', action='store_true')
    parser.add_argument('--list', action='store_true')
    args = parser.parse_args()
    targets = [t for t in TARGETS if (not args.loader or t['loader']==args.loader)
               and (not args.version or t['minecraft']==args.version)]
    if not targets:
        parser.error('Unsupported loader/Minecraft combination')
    if args.list:
        for t in targets: print(t['loader'], t['minecraft'], t['loader_version'], f'Java {t["java"]}')
    elif args.all or args.version:
        build(targets)
    elif sys.stdin.isatty():
        for i,t in enumerate(targets,1): print(f'{i}. {t["loader"]} / {t["minecraft"]}')
        choice = input('Number, A for all, 0 to exit: ').strip().upper()
        if choice == 'A': build(targets)
        elif choice.isdigit() and 1 <= int(choice) <= len(targets): build([targets[int(choice)-1]])
    else:
        parser.print_help()

if __name__ == '__main__':
    main()
