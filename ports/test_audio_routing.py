"""Offline regression against a compiled profile; does not launch Minecraft or use OAuth/network."""
from pathlib import Path
import os
import subprocess
import sys

ports = Path(__file__).resolve().parent
group = sys.argv[1] if len(sys.argv) > 1 else '1.21.2-1.21.3'
profile = ports / group
jdk = Path(os.environ.get('JAVA_HOME', r'C:\Program Files\Java\jdk-25.0.4')) / 'bin'
cache = Path.home() / '.gradle/caches/modules-2/files-2.1/org.slf4j'
classpath = [profile / 'musicplayer-lavaplayer/build/classes/java/main',
             profile / 'musicplayer-lavaplayer-api/build/classes/java/main']
classpath += list((profile / 'musicplayer-lavaplayer/build/dependencies').glob('*.jar.packed'))
for artifact in ('slf4j-api', 'slf4j-simple'):
    classpath += list((cache / artifact / '2.0.17').rglob(f'{artifact}-2.0.17.jar'))
assert all(p.exists() for p in classpath[:2]), 'Compile the profile before running the regression'
output = ports / 'tests/build' / group
output.mkdir(parents=True, exist_ok=True)
sources = list((ports / 'tests/java').rglob('*.java'))
cp = os.pathsep.join(str(p) for p in classpath)
subprocess.run([str(jdk / 'javac.exe'), '--release', '21', '-encoding', 'UTF-8', '-cp', cp,
                '-d', str(output), *map(str, sources)], check=True)
subprocess.run([str(jdk / 'java.exe'), '-cp', str(output) + os.pathsep + cp,
                'info.u_team.music_player.lavaplayer.sources.SourceRoutingRegression'], check=True)
