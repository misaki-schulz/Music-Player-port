"""Validate native-loader release JARs without launching Minecraft."""
from __future__ import annotations
import argparse
import hashlib
import io
import json
from pathlib import Path
import shutil
import tomllib
import zipfile
from package_ports import check_classes, check_soundcloud, ROOT, PORTS, DESTINATION

TARGETS = json.loads((PORTS / 'loader-targets.json').read_text(encoding='utf-8'))
FORMATS = json.loads((PORTS / 'loader-pack-formats.json').read_text(encoding='utf-8'))

def inspect(target: dict) -> tuple[Path, dict]:
    mc, loader = target['minecraft'], target['loader']
    version = f'2.7.1.351+mc{mc}.port.10.2'
    path = PORTS / target['group'] / 'build' / mc / 'libs' / f'music_player-{loader}-{mc}-{version}.jar'
    legacy = loader == 'forge' or mc == '1.20.1'
    meta_path = 'META-INF/mods.toml' if legacy else 'META-INF/neoforge.mods.toml'
    with zipfile.ZipFile(path) as z:
        if z.testzip() is not None:
            raise ValueError(f'{path}: corrupt JAR')
        names = z.namelist()
        if 'fabric.mod.json' in names:
            raise ValueError(f'{path}: unexpected Fabric metadata')
        meta = tomllib.loads(z.read(meta_path).decode('utf-8'))
        mod = meta['mods'][0]
        if mod['modId'] != 'musicplayer' or mod['version'] != version:
            raise ValueError(f'{path}: incorrect mod identity/version')
        dependencies = {d['modId']: d for d in meta['dependencies']['musicplayer']}
        if dependencies['minecraft']['versionRange'] != f'[{mc}]':
            raise ValueError(f'{path}: Minecraft must match the exact target')
        loader_id = 'forge' if legacy else 'neoforge'
        if dependencies[loader_id]['side'] != 'CLIENT':
            raise ValueError(f'{path}: loader dependency must be client-only')
        for name in ['LICENSE_Music-Player', 'NOTICE', 'LICENSE-DEPENDENCIES', 'pack.mcmeta',
                     'info/u_team/music_player/MusicPlayerMod.class',
                     'info/u_team/music_player/init/NativeClientEvents.class',
                     'info/u_team/music_player/platform/LoaderPlatform.class',
                     'info/u_team/music_player/lavaplayer/api/IMusicPlayer.class']:
            if name not in names:
                raise ValueError(f'{path}: missing {name}')
        if not any(n.startswith('assets/musicplayer/textures/gui/') for n in names):
            raise ValueError(f'{path}: missing GUI textures')
        pack = json.loads(z.read('pack.mcmeta'))['pack']
        if pack['pack_format'] != FORMATS[mc][0]:
            raise ValueError(f'{path}: incorrect pack format')
        if FORMATS[mc][0] >= 65 and (pack.get('min_format') != FORMATS[mc] or pack.get('max_format') != FORMATS[mc]):
            raise ValueError(f'{path}: incorrect pack format range')
        packed = [n for n in names if n.startswith('dependencies/') and n.endswith('.jar.packed')]
        if len(packed) != 37:
            raise ValueError(f'{path}: expected 37 audio dependencies, got {len(packed)}')
        classes = check_classes(z, target['java'], path.name)
        for name in names:
            if name.endswith('.class'):
                data = z.read(name)
                if b'net/fabricmc/' in data:
                    raise ValueError(f'{path}: Fabric reference in {name}')
                if loader == 'forge' and b'net/neoforged/' in data:
                    raise ValueError(f'{path}: NeoForge reference in Forge class {name}')
        for module, release in [('lavadsp','0.7.8'), ('native-loader','0.0.1')]:
            jar = f'{module}-{release}.jar'
            pinned = ROOT / 'vendor/maven/dev/arbjerg' / module / release / jar
            if z.read(f'dependencies/{jar}.packed') != pinned.read_bytes():
                raise ValueError(f'{path}: changed vendored dependency {jar}')
        auth_sha = None
        for name in packed:
            with zipfile.ZipFile(io.BytesIO(z.read(name))) as nested:
                if nested.testzip() is not None:
                    raise ValueError(f'{path}: corrupt dependency {name}')
                classes += check_classes(nested, target['java'], name)
                if name.endswith('/musicplayer-lavaplayer.jar.packed'):
                    check_soundcloud(nested, target['source_group'])
                    auth = 'info/u_team/music_player/lavaplayer/sources/YoutubeAuth.class'
                    compiled = PORTS / target['source_group'] / 'musicplayer-lavaplayer/build/classes/java/main' / auth
                    data = nested.read(auth)
                    if data != compiled.read_bytes() or b'createSource' not in data:
                        raise ValueError(f'{path}: audio routing regression')
                    auth_sha = hashlib.sha256(data).hexdigest()
        handler = z.read('info/u_team/music_player/init/MusicPlayerEventHandler.class')
        if legacy and target.get('build_system') != 'forgegradle7' and not any(marker in handler for marker in [b'm_', b'f_']):
            raise ValueError(f'{path}: Forge SRG remapping missing')
        if target.get('hud_mixin'):
            config = json.loads(z.read('musicplayer-forge.mixins.json'))
            if not config.get('required') or 'GuiHudMixin' not in config.get('client', []):
                raise ValueError(f'{path}: HUD mixin missing')
            if b'MixinConfigs: musicplayer-forge.mixins.json' not in z.read('META-INF/MANIFEST.MF'):
                raise ValueError(f'{path}: HUD mixin not registered')
    return path, {**target, 'minecraft': [mc], 'mod_version': version, 'file': path.name,
                  'bytes': path.stat().st_size, 'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
                  'checked_classes': classes, 'packed_dependencies': len(packed),
                  'youtube_auth_class_sha256': auth_sha, 'minecraft_runtime_tested': False}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--loader', choices=['forge', 'neoforge'])
    parser.add_argument('--version')
    args = parser.parse_args()
    selected = [t for t in TARGETS if (not args.loader or t['loader']==args.loader)
                and (not args.version or t['minecraft']==args.version)]
    if not selected:
        parser.error('Unsupported loader/Minecraft combination')
    DESTINATION.mkdir(parents=True, exist_ok=True)
    manifest = DESTINATION / 'loader-manifest.json'
    records = json.loads(manifest.read_text(encoding='utf-8')) if manifest.exists() else []
    for target in selected:
        path, record = inspect(target)
        name = f'music_player-{target["loader"]}-{target["minecraft"]}-2.7.1.351.port.10.2.jar'
        shutil.copyfile(path, DESTINATION / name)
        record['file'] = name
        records = [r for r in records if not (r['loader']==target['loader'] and r['minecraft']==record['minecraft'])]
        records.append(record)
        print(f'Validated {name}: {record["checked_classes"]} classes, {record["packed_dependencies"]} dependencies')
    records.sort(key=lambda r: (r['loader'], [t['minecraft'] for t in TARGETS].index(r['minecraft'][0])))
    manifest.write_text(json.dumps(records, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')
    (DESTINATION / 'LOADER-SHA256SUMS').write_text(''.join(f'{r["sha256"]}  {r["file"]}\n' for r in records), encoding='utf-8')

if __name__ == '__main__':
    main()
