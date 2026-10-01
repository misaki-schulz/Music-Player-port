"""Test production sorting/playlist code offline with a Minecraft-free manager boundary."""
from pathlib import Path
import os
import shutil
import subprocess
import sys

PORTS = Path(__file__).resolve().parent
ROOT = PORTS.parent
JAVA = Path('common/src/common/java/info/u_team/music_player')
group = sys.argv[1] if len(sys.argv) > 1 else 'root'
profile = ROOT if group == 'root' else PORTS / group
cache = Path.home() / '.gradle/caches/modules-2/files-2.1'
dependencies = []
for artifact in ('com.google.code.gson/gson', 'org.apache.commons/commons-lang3', 'com.google.guava/guava'):
    jars = sorted((cache / artifact).rglob('*.jar'))
    if not jars:
        raise SystemExit(f'Missing cached {artifact}; build a profile first')
    dependencies.append(jars[-1])
cp = os.pathsep.join(map(str, dependencies))
output = PORTS / 'tests/build/sorting' / group
output.mkdir(parents=True, exist_ok=True)
sources = [profile / JAVA / path for path in (
    'util/TitleFilter.java', 'util/NaturalOrder.java', 'util/OrderedTrackLoader.java', 'util/WrappedObject.java',
    'musicplayer/playlist/PlaylistSort.java', 'musicplayer/playlist/Playlist.java',
    'musicplayer/playlist/LoadedTracks.java', 'musicplayer/playlist/Skip.java',
    'musicplayer/playlist/Playlists.java', 'gui/GuiMusicPlayerList.java',
    'gui/playlist/GuiMusicPlaylistList.java')]
api = profile / 'musicplayer-lavaplayer-api/src/main/java/info/u_team/music_player/lavaplayer/api'
sources += list((api / 'audio').glob('IAudioTrack*.java'))
sources += [api / 'search/ITrackSearch.java', api / 'search/ISearchResult.java', api / 'queue/ITrackQueue.java']
sources += list((PORTS / 'tests/sorting-stubs').rglob('*.java'))
sources += list((PORTS / 'tests/sorting-java').rglob('*.java'))
jdk = Path(os.environ['JAVA_HOME']) / 'bin' if os.environ.get('JAVA_HOME') else None
def executable(name):
    suffix = '.exe' if os.name == 'nt' else ''
    return str(jdk / (name + suffix)) if jdk else shutil.which(name)
subprocess.run([executable('javac'), '--release', '21', '-encoding', 'UTF-8', '-cp', cp,
                '-d', str(output), *map(str, sources)], check=True)
subprocess.run([executable('java'), '-cp', str(output) + os.pathsep + cp,
                'info.u_team.music_player.SortingRegression'], check=True)
