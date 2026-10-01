"""Cross-platform interactive builder and repository-local cleanup for port.10."""
from __future__ import annotations

import argparse
from contextlib import contextmanager
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

sys.dont_write_bytecode = True
from package_ports import BINARY_GROUPS

if os.name == 'nt':
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')

ROOT = Path(__file__).resolve().parent.parent
PORTS = ROOT / 'ports'
TARGETS = json.loads((PORTS / 'targets.json').read_text(encoding='utf-8-sig'))
VERSIONS = [target['minecraft'] for target in TARGETS]
TARGET_BY_VERSION = {target['minecraft']: target for target in TARGETS}
GROUP_BY_NAME = dict(BINARY_GROUPS)
GENERATED_DIR_NAMES = {
    '.gradle', '.cache', '.pytest_cache', '__pycache__',
    'build', 'run', 'runs', 'logs', 'crash-reports',
}


@contextmanager
def exclusive_operation():
    """Prevent two menu instances from building or cleaning at the same time."""
    suffix = hashlib.sha256(str(ROOT).lower().encode('utf-8')).hexdigest()[:16]
    lock_path = Path(tempfile.gettempdir()) / f'music-player-port-{suffix}.lock'
    with lock_path.open('a+b') as lock:
        lock.seek(0, os.SEEK_END)
        if lock.tell() == 0:
            lock.write(b'0')
            lock.flush()
        lock.seek(0)
        if os.name == 'nt':
            import msvcrt
            msvcrt.locking(lock.fileno(), msvcrt.LK_LOCK, 1)
        else:
            import fcntl
            fcntl.flock(lock.fileno(), fcntl.LOCK_EX)
        try:
            yield
        finally:
            lock.seek(0)
            if os.name == 'nt':
                msvcrt.locking(lock.fileno(), msvcrt.LK_UNLCK, 1)
            else:
                fcntl.flock(lock.fileno(), fcntl.LOCK_UN)


def group_for_version(version: str) -> tuple[str, list[str]]:
    for group, versions in BINARY_GROUPS:
        if version in versions:
            return group, versions
    raise ValueError(f'Нет группы для Minecraft {version}')


def compile_version(version: str) -> None:
    target = TARGET_BY_VERSION[version]
    print(f'\n=== Сборка Minecraft {version} ({target["group"]}) ===', flush=True)
    if os.name == 'nt':
        powershell = shutil.which('pwsh') or shutil.which('powershell')
        if powershell is None:
            raise RuntimeError('PowerShell не найден; он нужен для Build-Target.ps1')
        command = [powershell, '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File',
                   str(PORTS / 'Build-Target.ps1'), '-Group', target['group'],
                   '-Version', version]
        subprocess.run(command, cwd=ROOT, check=True)
    else:
        project = PORTS / target['group']
        command = ['sh', str(project / 'gradlew'), 'assemble', f'-PtargetVersion={version}',
                   '--no-daemon', '--max-workers=1',
                   '-Dorg.gradle.jvmargs=-Xmx1200m -Dfile.encoding=UTF-8', '--console=plain']
        subprocess.run(command, cwd=project, check=True)


def build(versions: list[str], group: str | None) -> None:
    with exclusive_operation():
        print('Сборки идут последовательно. Minecraft не запускается.', flush=True)
        for version in versions:
            compile_version(version)
        command = [sys.executable, str(PORTS / 'package_ports.py')]
        if group is not None:
            command.extend(['--group', group])
        print('\n=== Проверка и упаковка JAR ===', flush=True)
        subprocess.run(command, cwd=ROOT, check=True)
        if group is not None:
            print(f'Готово: {ROOT / "dist" / "port.10" / f"music_player-fabric-{group}-2.7.1.351.port.10.jar"}')
        else:
            print(f'Готово: восемь JAR в {ROOT / "dist" / "port.10"}')


def generated_paths() -> list[Path]:
    """List only known generated files inside this checkout."""
    found: list[Path] = []
    release_dir = ROOT / 'dist' / 'port.10'
    if release_dir.exists() and not release_dir.is_symlink():
        found.append(release_dir)
    for current, directories, files in os.walk(ROOT, topdown=True, followlinks=False):
        parent = Path(current)
        for name in list(directories):
            child = parent / name
            if child.is_symlink() or (parent == ROOT and name in {'dist', '.git'}):
                directories.remove(name)
            elif name in GENERATED_DIR_NAMES:
                found.append(child)
                directories.remove(name)
        for name in files:
            child = parent / name
            if not child.is_symlink() and (name.endswith(('.log', '.pyc')) or name.startswith('hs_err_pid')):
                found.append(child)
    return sorted(found, key=lambda item: str(item).lower())


def clean(dry_run: bool) -> None:
    with exclusive_operation():
        paths = generated_paths()
        for path in paths:
            absolute = path.absolute()
            resolved = path.resolve()
            if path.is_symlink() or absolute == ROOT or not absolute.is_relative_to(ROOT) or not resolved.is_relative_to(ROOT):
                raise RuntimeError(f'Небезопасный путь очистки: {path}')
        print(f'Найдено {len(paths)} созданных файлов и папок:', flush=True)
        for path in paths:
            print(f'  {path.relative_to(ROOT)}')
        if dry_run:
            print('Пробный запуск: ничего не удалено.')
            return
        for path in paths:
            if path.is_dir():
                shutil.rmtree(path)
            else:
                path.unlink()
        print('Очистка завершена. Исходники и настройки сохранены; dist/port.7 не удаляется, если он есть.')
        print('Общий кеш ~/.gradle не затронут: он находится вне проекта и используется другими сборками.')


def list_groups() -> None:
    print('Один пункт — один итоговый JAR:')
    for index, (group, versions) in enumerate(BINARY_GROUPS, 1):
        print(f'{index}. Minecraft {group} ({", ".join(versions)})')


def interactive() -> None:
    while True:
        print('\nMusic Player port.10')
        list_groups()
        all_choice = str(len(BINARY_GROUPS) + 1)
        print(f'{all_choice} — собрать все восемь JAR')
        print('C — очистить созданные сборки и локальный кеш проекта')
        print('0 — выход')
        choice = input('Выбор: ').strip().upper()
        if choice == '0':
            return
        if choice.isdigit() and 1 <= int(choice) <= len(BINARY_GROUPS):
            group, versions = BINARY_GROUPS[int(choice) - 1]
            build(versions, group)
        elif choice == all_choice:
            build(VERSIONS, None)
        elif choice in {'C', 'С'}:
            clean(False)
        else:
            print(f'Выберите номер от 0 до {all_choice} или C.')


def main() -> None:
    parser = argparse.ArgumentParser(description='Меню сборки Music Player port.10')
    actions = parser.add_mutually_exclusive_group()
    actions.add_argument('--group', choices=list(GROUP_BY_NAME), help='собрать один общий JAR для диапазона Minecraft')
    actions.add_argument('--version', choices=VERSIONS, help=argparse.SUPPRESS)
    actions.add_argument('--all', action='store_true', help='собрать все восемь JAR')
    actions.add_argument('--clean', action='store_true', help='очистить локальные сборки и кеш')
    actions.add_argument('--list', action='store_true', help='показать восемь совместимых диапазонов')
    parser.add_argument('--dry-run', action='store_true', help='показать пути очистки без удаления; только с --clean')
    args = parser.parse_args()
    if args.dry_run and not args.clean:
        parser.error('--dry-run работает только с --clean')
    if args.list:
        list_groups()
    elif args.group:
        build(GROUP_BY_NAME[args.group], args.group)
    elif args.version:
        group, versions = group_for_version(args.version)
        build(versions, group)
    elif args.all:
        build(VERSIONS, None)
    elif args.clean:
        clean(args.dry_run)
    elif sys.stdin.isatty():
        interactive()
    else:
        parser.print_help()


if __name__ == '__main__':
    try:
        main()
    except (RuntimeError, OSError, subprocess.CalledProcessError, KeyboardInterrupt) as error:
        print(f'Ошибка: {error}', file=sys.stderr)
        sys.exit(1)
