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


def java_environment(java_version: int) -> dict[str, str] | None:
    if os.name == 'nt':
        return None
    java_home = Path('/usr/lib/jvm') / f'java-{max(java_version, 21)}-openjdk'
    if not (java_home / 'bin' / 'java').exists():
        raise RuntimeError(f'JDK {max(java_version, 21)} not found at {java_home}')
    environment = os.environ.copy()
    environment['JAVA_HOME'] = str(java_home)
    environment['PATH'] = str(java_home / 'bin') + os.pathsep + environment.get('PATH', '')
    return environment


def menu_entries(targets):
    entries = []
    grouped = {}
    for target in targets:
        key = target.get('package_group') or f'{target["loader"]}/{target["minecraft"]}'
        if key not in grouped:
            grouped[key] = []
            entries.append((key, grouped[key]))
        grouped[key].append(target)
    return entries


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
                                '--no-daemon', '--max-workers=1', '--console=plain'], cwd=project, check=True,
                                env=java_environment(t['java']))
        packaged = set()
        for t in targets:
            package_group = t.get('package_group')
            full_group = package_group and {
                item['minecraft'] for item in targets if item.get('package_group') == package_group
            } == {
                item['minecraft'] for item in TARGETS if item.get('package_group') == package_group
            }
            if full_group:
                if package_group in packaged:
                    continue
                command = [sys.executable, str(PORTS/'package_loaders.py'), '--group', package_group]
                packaged.add(package_group)
            else:
                command = [sys.executable, str(PORTS/'package_loaders.py'), '--loader', t['loader'],
                           '--version', t['minecraft']]
            subprocess.run(command, check=True)

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
        entries = menu_entries(targets)
        for i, (label, entry_targets) in enumerate(entries, 1):
            if len(entry_targets) > 1:
                versions = label.removeprefix(f'{entry_targets[0]["loader"]}-')
                print(f'{i}. {entry_targets[0]["loader"]} / {versions}')
            else:
                print(f'{i}. {entry_targets[0]["loader"]} / {entry_targets[0]["minecraft"]}')
        choice = input('Number, A for all, 0 to exit: ').strip().upper()
        if choice == 'A': build(targets)
        elif choice.isdigit() and 1 <= int(choice) <= len(entries): build(entries[int(choice)-1][1])
    else:
        parser.print_help()

if __name__ == '__main__':
    main()
