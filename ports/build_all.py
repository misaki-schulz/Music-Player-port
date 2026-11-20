"""Build every Fabric, Forge, and NeoForge target and continue after failures."""
from __future__ import annotations

import json
import os
from pathlib import Path
import shlex
import shutil
import subprocess
import sys
from datetime import datetime

from package_ports import BINARY_GROUPS, DESTINATION, ROOT, TARGETS as FABRIC_TARGETS
from release_cli import exclusive_operation

LOADER_TARGETS = json.loads((ROOT / 'ports' / 'loader-targets.json').read_text(encoding='utf-8'))


def command_text(command: list[str]) -> str:
    return shlex.join(command)


def run_step(label: str, command: list[str], cwd: Path, log_dir: Path, env: dict[str, str] | None = None) -> bool:
    print(f'\n=== {label} ===', flush=True)
    process = subprocess.Popen(
        command,
        cwd=cwd,
        env=env,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        encoding='utf-8',
        errors='replace',
    )
    output: list[str] = []
    assert process.stdout is not None
    for line in process.stdout:
        print(line, end='', flush=True)
        output.append(line)
    result = process.wait()
    if result == 0:
        return True


def java_environment(java_version: int) -> dict[str, str] | None:
    if os.name == 'nt':
        return None
    # Current Gradle/Fabric Loom requires Java 21 to run. The target's
    # toolchain still controls the bytecode and Minecraft compatibility.
    java_version = max(java_version, 21)
    java_home = Path('/usr/lib/jvm') / f'java-{java_version}-openjdk'
    if not (java_home / 'bin' / 'java').exists():
        raise RuntimeError(f'JDK {java_version} not found at {java_home}')
    environment = os.environ.copy()
    environment['JAVA_HOME'] = str(java_home)
    environment['PATH'] = str(java_home / 'bin') + os.pathsep + environment.get('PATH', '')
    return environment

    safe_label = ''.join(character if character.isalnum() or character in '._-' else '_' for character in label)
    log_path = log_dir / f'{safe_label}.log'
    log_path.write_text(
        f'Command: {command_text(command)}\nWorking directory: {cwd}\nExit code: {result}\n\n'
        + ''.join(output),
        encoding='utf-8',
    )
    print(f'ОШИБКА: {label}. Лог: {log_path}', file=sys.stderr, flush=True)
    return False


def fabric_command(version: str) -> tuple[list[str], Path, int]:
    target = next(item for item in FABRIC_TARGETS if item['minecraft'] == version)
    project = ROOT / 'ports' / target['group']
    if os.name == 'nt':
        powershell = shutil.which('pwsh') or shutil.which('powershell')
        if powershell is None:
            raise RuntimeError('PowerShell not found')
        return ([powershell, '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File',
                 str(ROOT / 'ports' / 'Build-Target.ps1'), '-Group', target['group'], '-Version', version], ROOT, target['java'])
    return (['sh', str(project / 'gradlew'), 'assemble', f'-PtargetVersion={version}',
             '--no-daemon', '--max-workers=1',
             '-Dorg.gradle.jvmargs=-Xmx1200m -Dfile.encoding=UTF-8', '--console=plain'], project, target['java'])


def loader_build_command(target: dict) -> tuple[list[str], Path, int]:
    project = ROOT / 'ports' / target['group']
    if os.name == 'nt':
        powershell = shutil.which('pwsh') or shutil.which('powershell')
        if powershell is None:
            raise RuntimeError('PowerShell not found')
        return ([powershell, '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File',
                 str(ROOT / 'ports' / 'Build-Target.ps1'), '-Group', target['group'],
                 '-Version', target['minecraft']], ROOT, target['java'])
    return (['sh', str(project / 'gradlew'), 'assemble', f'-PtargetVersion={target["minecraft"]}',
             '--no-daemon', '--max-workers=1', '--console=plain'], project, target['java'])


def package_command(script: str, target: dict | None = None, group: str | None = None) -> list[str]:
    command = [sys.executable, str(ROOT / 'ports' / script)]
    if target is not None:
        command.extend(['--loader', target['loader'], '--version', target['minecraft']])
    if group is not None:
        command.extend(['--group', group])
    return command


def build_everything() -> int:
    timestamp = datetime.now().strftime('%Y%m%d-%H%M%S')
    log_dir = DESTINATION / 'logs' / f'build-all-{timestamp}'
    log_dir.mkdir(parents=True, exist_ok=True)
    failures: list[str] = []

    with exclusive_operation():
        print('Полная сборка: Fabric, Forge и NeoForge. Ошибки не останавливают остальные профили.', flush=True)

        for group, versions in BINARY_GROUPS:
            group_ok = True
            for version in versions:
                try:
                    command, cwd, java_version = fabric_command(version)
                    if not run_step(f'Fabric {version}', command, cwd, log_dir, java_environment(java_version)):
                        group_ok = False
                        failures.append(f'Fabric {version}')
                except Exception as error:
                    group_ok = False
                    failures.append(f'Fabric {version}')
                    (log_dir / f'Fabric_{version}.log').write_text(str(error) + '\n', encoding='utf-8')
            if group_ok:
                if not run_step(f'Упаковка Fabric {group}', package_command('package_ports.py', group=group), ROOT, log_dir):
                    failures.append(f'Упаковка Fabric {group}')
            else:
                print(f'Пропуск упаковки Fabric {group}: один из профилей завершился с ошибкой.', flush=True)

        loader_groups: dict[str, list[dict]] = {}
        loader_built: dict[str, list[dict]] = {}
        for target in LOADER_TARGETS:
            label = f'{target["loader"]} {target["minecraft"]}'
            package_group = target.get('package_group')
            if package_group:
                loader_groups.setdefault(package_group, []).append(target)
            try:
                command, cwd, java_version = loader_build_command(target)
                if not run_step(label, command, cwd, log_dir, java_environment(java_version)):
                    failures.append(label)
                    continue
                if package_group:
                    loader_built.setdefault(package_group, []).append(target)
                elif not run_step(f'Упаковка {label}', package_command('package_loaders.py', target), ROOT, log_dir):
                    failures.append(f'Упаковка {label}')
            except Exception as error:
                failures.append(label)
                safe_label = label.replace('/', '_').replace(' ', '_')
                (log_dir / f'{safe_label}.log').write_text(str(error) + '\n', encoding='utf-8')

        for package_group, targets in loader_groups.items():
            if len(loader_built.get(package_group, [])) != len(targets):
                print(f'Пропуск упаковки {package_group}: один из профилей завершился с ошибкой.', flush=True)
                continue
            if not run_step(f'Упаковка {package_group}', package_command('package_loaders.py', group=package_group), ROOT, log_dir):
                failures.append(f'Упаковка {package_group}')

    summary = log_dir / 'summary.txt'
    if failures:
        summary.write_text('Неудачные шаги:\n' + '\n'.join(f'- {failure}' for failure in failures) + '\n', encoding='utf-8')
        print(f'\nЗавершено с ошибками: {len(failures)}. Логи: {log_dir}', file=sys.stderr, flush=True)
        return 1
    summary.write_text('Все Fabric, Forge и NeoForge сборки завершились успешно.\n', encoding='utf-8')
    print(f'\nВсе сборки завершены успешно. Лог проверки: {summary}', flush=True)
    return 0


if __name__ == '__main__':
    try:
        raise SystemExit(build_everything())
    except (OSError, KeyboardInterrupt) as error:
        print(f'Ошибка запуска полной сборки: {error}', file=sys.stderr)
        raise SystemExit(1)
