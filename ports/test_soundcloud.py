"""Offline SoundCloud regression against the actual compiled player and a local HTTP fixture."""
from pathlib import Path
import os
import subprocess
import sys

ports = Path(__file__).resolve().parent
group = sys.argv[1] if len(sys.argv) > 1 else '1.21.2-1.21.3'
profile = ports / group
jdk = Path(os.environ.get('JAVA_HOME', r'C:\Program Files\Java\jdk-25.0.4')) / 'bin'
classpath = [profile / 'musicplayer-lavaplayer/build/classes/java/main',
             profile / 'musicplayer-lavaplayer-api/build/classes/java/main']
classpath += list((profile / 'musicplayer-lavaplayer/build/dependencies').glob('*.jar.packed'))
cache = Path.home() / '.gradle/caches/modules-2/files-2.1/org.slf4j'
for artifact in ('slf4j-api', 'slf4j-simple'):
    classpath += list((cache / artifact / '2.0.17').rglob(f'{artifact}-2.0.17.jar'))
assert all(p.exists() for p in classpath[:2]), 'Build the profile before running this regression'
output = ports / 'tests/build/soundcloud' / group
output.mkdir(parents=True, exist_ok=True)
cp = os.pathsep.join(map(str, classpath))
sources = list((ports / 'tests/soundcloud-java').rglob('*.java'))
subprocess.run([str(jdk / 'javac.exe'), '--release', '17' if group == '1.20.1' else '21',
                '-encoding', 'UTF-8', '-cp', cp, '-d', str(output), *map(str, sources)], check=True)
subprocess.run([str(jdk / 'java.exe'), '-cp', str(output) + os.pathsep + cp,
                'info.u_team.music_player.lavaplayer.sources.SoundCloudRegression'], check=True)
