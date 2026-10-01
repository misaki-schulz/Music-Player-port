"""Check compiled release archives and collect them; never launches Minecraft."""
from __future__ import annotations

import argparse
import hashlib
import io
import json
from pathlib import Path
import shutil
import struct
import zipfile

PORTS = Path(__file__).resolve().parent
ROOT = PORTS.parent
DESTINATION = ROOT / 'dist' / 'port.9'
TARGETS = json.loads((PORTS / 'targets.json').read_text(encoding='utf-8-sig'))
EXPECTED_VERSIONS = ['1.21.2', '1.21.3', '1.21.4', '1.21.5', '1.21.6', '1.21.7', '1.21.8', '1.21.9', '1.21.10', '1.21.11', '26.1', '26.1.1', '26.1.2', '26.2', '26.3']
VARIABLE_ENTRIES = {'fabric.mod.json', 'pack.mcmeta', 'META-INF/MANIFEST.MF'}
BINARY_GROUPS = [
    ('1.21.2-1.21.3', ['1.21.2', '1.21.3']),
    ('1.21.4-1.21.5', ['1.21.4', '1.21.5']),
    ('1.21.6-1.21.8', ['1.21.6', '1.21.7', '1.21.8']),
    ('1.21.9-1.21.10', ['1.21.9', '1.21.10']),
    ('1.21.11', ['1.21.11']),
    ('26.1-26.1.2', ['26.1', '26.1.1', '26.1.2']),
    ('26.2', ['26.2']),
    ('26.3', ['26.3']),
]


def check_classes(archive: zipfile.ZipFile, java: int, label: str) -> int:
    count = 0
    for entry in archive.infolist():
        if not entry.filename.endswith('.class'):
            continue
        parts = entry.filename.split('/')
        if len(parts) > 3 and parts[:2] == ['META-INF', 'versions']:
            if int(parts[2]) > java:
                continue  # The target JVM does not load this multi-release variant.
        with archive.open(entry) as stream:
            magic, minor, major = struct.unpack('>IHH', stream.read(8))
        if magic != 0xCAFEBABE or major > java + 44 or minor == 65535:
            raise ValueError(f'{label}!{entry.filename}: unsupported class version {major}.{minor} for Java {java}')
        count += 1
    return count


def inspect(target: dict) -> tuple[Path, dict]:
    version = target['minecraft']
    expected_mod_version = f'2.7.1.351+mc{version}.port.9'
    candidates = []
    output = PORTS / target['group'] / 'build' / version / 'libs'
    for path in output.glob('*.jar'):
        if any(path.name.endswith(suffix) for suffix in ('-sources.jar', '-thin.jar', '-dev.jar', '-dev-shadow.jar', '-shadow.jar')):
            continue
        with zipfile.ZipFile(path) as archive:
            if 'fabric.mod.json' not in archive.namelist():
                continue
            metadata = json.loads(archive.read('fabric.mod.json'))
            if metadata.get('version') == expected_mod_version:
                candidates.append(path)
    if len(candidates) != 1:
        raise ValueError(f'{version}: expected one release JAR in {output}, found {candidates}')
    path = candidates[0]
    with zipfile.ZipFile(path) as archive:
        metadata = json.loads(archive.read('fabric.mod.json'))
        dependencies = metadata['depends']
        if dependencies.get('minecraft') not in (version, '=' + version):
            raise ValueError(f'{version}: Minecraft dependency must target this exact release: {dependencies}')
        if metadata.get('environment') != 'client' or metadata.get('id') != 'musicplayer':
            raise ValueError(f'{version}: wrong mod identity/environment')
        if dependencies.get('java') != f">={target['java']}":
            raise ValueError(f'{version}: wrong Java requirement')
        if dependencies.get('fabricloader') != '>=0.19.5':
            raise ValueError(f'{version}: wrong Fabric Loader baseline')
        if target['fabric_api'] not in str(dependencies.get('fabric-api')):
            raise ValueError(f'{version}: wrong Fabric API requirement')
        names = archive.namelist()
        required = ['LICENSE_Music-Player', 'NOTICE', 'pack.mcmeta', 'info/u_team/music_player/MusicPlayerMod.class', 'info/u_team/music_player/lavaplayer/api/IMusicPlayer.class']
        required += [f'info/u_team/music_player/{name}.class' for name in (
            'util/NaturalOrder', 'util/OrderedTrackLoader',
            'musicplayer/playlist/PlaylistSort', 'gui/playlist/GuiMusicPlaylistSort')]
        for name in required:
            if name not in names:
                raise ValueError(f'{version}: missing {name}')
        if not any(name.startswith('assets/musicplayer/textures/gui/') for name in names):
            raise ValueError(f'{version}: missing GUI textures')
        pack = json.loads(archive.read('pack.mcmeta'))['pack']
        game_jar = Path.home() / '.gradle' / 'caches' / 'fabric-loom' / version / 'minecraft-client.jar'
        if game_jar.exists():
            with zipfile.ZipFile(game_jar) as game_archive:
                game_version = json.loads(game_archive.read('version.json'))
            pack_version = game_version.get('pack_version', {})
            expected_pack = pack_version.get('resource_major', pack_version.get('resource'))
            if expected_pack is not None and pack.get('pack_format') != expected_pack:
                raise ValueError(f'{version}: resource pack format {pack.get("pack_format")} != Minecraft {expected_pack}')
            expected_minor = pack_version.get('resource_minor')
            if expected_minor is not None and (pack.get('min_format') != [expected_pack, expected_minor] or pack.get('max_format') != [expected_pack, expected_minor]):
                raise ValueError(f'{version}: resource pack minor/range does not match Minecraft {expected_pack}.{expected_minor}')
        packed = [name for name in names if name.startswith('dependencies/') and name.endswith('.jar.packed')]
        if not any(name.endswith('/musicplayer-lavaplayer.jar.packed') for name in packed):
            raise ValueError(f'{version}: packed player implementation missing')
        if len(packed) < 5:
            raise ValueError(f'{version}: incomplete packed dependency set')
        classes = check_classes(archive, target['java'], path.name)
        youtube_auth_sha256 = None
        for name in packed:
            with zipfile.ZipFile(io.BytesIO(archive.read(name))) as nested:
                classes += check_classes(nested, target['java'], name)
                if name.endswith('/musicplayer-lavaplayer.jar.packed'):
                    auth_class = 'info/u_team/music_player/lavaplayer/sources/YoutubeAuth.class'
                    compiled = PORTS / target['group'] / 'musicplayer-lavaplayer/build/classes/java/main' / auth_class
                    auth_bytes = nested.read(auth_class)
                    if auth_bytes != compiled.read_bytes() or b'createSource' not in auth_bytes:
                        raise ValueError(f'{version}: packed YouTube source does not preserve the port.8 routing fix')
                    youtube_auth_sha256 = hashlib.sha256(auth_bytes).hexdigest()
        if version.startswith('1.'):
            entrypoint = archive.read('info/u_team/music_player/MusicPlayerMod.class')
            handler = archive.read('info/u_team/music_player/init/MusicPlayerEventHandler.class')
            if b'net/minecraft/client/Minecraft' in entrypoint + handler:
                raise ValueError(f'{version}: unremapped Minecraft class reference in release JAR')
            if b'net/minecraft/class_' not in handler:
                raise ValueError(f'{version}: expected intermediary class references after remapping')
        return path, {
            **target, 'file': path.name, 'mod_version': expected_mod_version,
            'fabric_loader': '0.19.5', 'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
            'bytes': path.stat().st_size, 'checked_classes': classes,
            'packed_dependencies': len(packed), 'minecraft_runtime_tested': False,
            'youtube_auth_class_sha256': youtube_auth_sha256,
        }


def build_group(group: str, members: list[tuple[Path, dict]]) -> dict:
    """Combine only binaries whose executable and asset payloads are identical."""
    versions = [record['minecraft'] for _, record in members]
    if len({record['java'] for _, record in members}) != 1:
        raise ValueError(f'{group}: mixed Java requirements')
    archives = [zipfile.ZipFile(path) for path, _ in members]
    try:
        names = set(archives[0].namelist())
        for archive in archives[1:]:
            if set(archive.namelist()) != names:
                raise ValueError(f'{group}: JAR entry lists differ')
            for name in names - VARIABLE_ENTRIES:
                if archive.read(name) != archives[0].read(name):
                    raise ValueError(f'{group}: executable or asset differs: {name}')
        metadata = json.loads(archives[0].read('fabric.mod.json'))
        metadata['version'] = f'2.7.1.351+mc{group}.port.9'
        metadata['depends']['minecraft'] = versions if len(versions) > 1 else versions[0]
        # Fabric API releases for 1.21.x select their own Minecraft versions.
        # The 26.1.x client entrypoint checks its per-patch minimum explicitly.
        metadata['depends']['fabric-api'] = f">={members[0][1]['fabric_api']}"
        pack = json.loads(archives[0].read('pack.mcmeta'))
        pack_formats = [json.loads(archive.read('pack.mcmeta'))['pack'] for archive in archives]
        if len(versions) > 1:
            major_formats = [item['pack_format'] for item in pack_formats]
            if max(major_formats) < 65:
                pack['pack']['pack_format'] = min(major_formats)
                pack['pack']['supported_formats'] = [min(major_formats), max(major_formats)]
                pack['pack']['min_format'] = min((item['min_format'] for item in pack_formats))
                pack['pack']['max_format'] = max((item['max_format'] for item in pack_formats))
            else:
                if min(major_formats) < 65:
                    raise ValueError(f'{group}: pack format crosses the 1.21.9 metadata boundary')
                pack['pack']['min_format'] = min((item['min_format'] for item in pack_formats))
                pack['pack']['max_format'] = max((item['max_format'] for item in pack_formats))
                pack['pack'].pop('supported_formats', None)
        name = f'music_player-fabric-{group}-2.7.1.351.port.9.jar'
        output = DESTINATION / name
        with zipfile.ZipFile(output, 'w') as combined:
            for info in archives[0].infolist():
                if info.filename == 'fabric.mod.json':
                    data = json.dumps(metadata, indent=2, ensure_ascii=False).encode('utf-8') + b'\n'
                elif info.filename == 'pack.mcmeta':
                    data = json.dumps(pack, indent=2, ensure_ascii=False).encode('utf-8') + b'\n'
                elif info.filename == 'META-INF/MANIFEST.MF' and len(versions) > 1:
                    data = b''.join(line for line in archives[0].read(info.filename).splitlines(keepends=True)
                                    if not line.startswith(b'Fabric-Minecraft-Version: '))
                else:
                    data = archives[0].read(info.filename)
                combined.writestr(info, data)
        with zipfile.ZipFile(output) as combined:
            if combined.testzip() is not None:
                raise ValueError(f'{group}: corrupt grouped JAR')
            if json.loads(combined.read('fabric.mod.json'))['depends']['minecraft'] != (versions if len(versions) > 1 else versions[0]):
                raise ValueError(f'{group}: wrong Minecraft compatibility metadata')
            if json.loads(combined.read('pack.mcmeta')) != pack:
                raise ValueError(f'{group}: wrong resource pack metadata')
        return {
            'group': group, 'minecraft': versions, 'java': members[0][1]['java'],
            'fabric_loader': members[0][1]['fabric_loader'],
            'fabric_api_minimum': members[0][1]['fabric_api'],
            'file': name, 'bytes': output.stat().st_size,
            'sha256': hashlib.sha256(output.read_bytes()).hexdigest(),
            'minecraft_runtime_tested': False,
        }
    finally:
        for archive in archives:
            archive.close()


def merge_records(path: Path, updates: list[dict], key: str, order: list[str]) -> list[dict]:
    existing = []
    updated_keys = {record[key] for record in updates}
    if path.is_file():
        existing = json.loads(path.read_text(encoding='utf-8'))
        for record in existing:
            if record[key] in updated_keys:
                continue
            jar = path.parent / record['file']
            if not jar.is_file() or hashlib.sha256(jar.read_bytes()).hexdigest() != record['sha256']:
                raise ValueError(f'Stale release manifest entry: {jar}')
    merged = {record[key]: record for record in existing}
    merged.update({record[key]: record for record in updates})
    return [merged[item] for item in order if item in merged]


def write_manifest(directory: Path, records: list[dict]) -> None:
    (directory / 'manifest.json').write_text(json.dumps(records, indent=2) + '\n', encoding='utf-8')
    (directory / 'SHA256SUMS.txt').write_text(
        ''.join(f"{record['sha256']}  {record['file']}\n" for record in records), encoding='utf-8')


def main() -> None:
    parser = argparse.ArgumentParser(description='Verify and package port.9 release JARs')
    parser.add_argument('--group', choices=[group for group, _ in BINARY_GROUPS],
                        help='Package one binary group after compiling all its Minecraft versions')
    args = parser.parse_args()
    if [target['minecraft'] for target in TARGETS] != EXPECTED_VERSIONS:
        raise ValueError('Target matrix must contain all 15 releases, in order, without extras or omissions')
    if [version for _, versions in BINARY_GROUPS for version in versions] != EXPECTED_VERSIONS:
        raise ValueError('Binary groups must cover all 15 releases in order')
    groups_to_package = [(group, versions) for group, versions in BINARY_GROUPS
                         if args.group is None or group == args.group]
    selected_versions = {version for _, versions in groups_to_package for version in versions}
    checked = []
    for target in TARGETS:
        if target['minecraft'] not in selected_versions:
            continue
        item = inspect(target)
        checked.append(item)
        print(f"Verified {target['minecraft']}: {item[1]['checked_classes']} classes, {item[1]['packed_dependencies']} packed dependencies", flush=True)
    DESTINATION.mkdir(parents=True, exist_ok=True)
    exact_destination = DESTINATION / 'exact'
    exact_destination.mkdir(exist_ok=True)
    for path, record in checked:
        shutil.copy2(path, exact_destination / record['file'])
    records = [record for _, record in checked]
    by_version = {item[1]['minecraft']: item for item in checked}
    grouped = [build_group(group, [by_version[version] for version in versions])
               for group, versions in groups_to_package]
    if [version for record in grouped for version in record['minecraft']] != [
            version for _, versions in groups_to_package for version in versions]:
        raise ValueError('Grouped builds omit or reorder a Minecraft release')
    grouped_names = {record['file'] for record in grouped}
    previous_exact_names = {record['file'] for _, record in checked}
    if args.group is None:
        for obsolete in DESTINATION.glob('music_player-fabric-*-2.7.1.351.port.9.jar'):
            if obsolete.name not in grouped_names:
                obsolete.unlink()
    for name in previous_exact_names - grouped_names:
        obsolete = DESTINATION / name
        if obsolete.is_file():
            obsolete.unlink()
    if args.group is not None:
        records = merge_records(exact_destination / 'manifest.json', records, 'minecraft', EXPECTED_VERSIONS)
        grouped = merge_records(DESTINATION / 'manifest.json', grouped, 'group',
                                [group for group, _ in BINARY_GROUPS])
    write_manifest(exact_destination, records)
    write_manifest(DESTINATION, grouped)
    print(f'Collected {len(grouped)} grouped port.9 JARs for {len(records)} Minecraft releases into {DESTINATION}')


if __name__ == '__main__':
    main()
