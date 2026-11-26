"""Exercise real service lifecycle/cipher HTTP handling without Minecraft or YouTube.

Uses libraries from an existing mod JAR; compiles only the service manager and
the regression harness, never invokes Gradle or builds the mod.
"""
from __future__ import annotations

import argparse
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import zipfile

PORTS = Path(__file__).resolve().parent
ROOT = PORTS.parent


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--artifact", type=Path, required=True, help="Existing port.11.1 mod JAR")
    parser.add_argument("--slf4j", type=Path, required=True, help="Existing slf4j-api JAR")
    parser.add_argument("--group", default="1.21.11",
                        choices=["root", "1.20.1", "1.21.1", "1.21.11", "26.1", "26.2", "26.3"])
    args = parser.parse_args()
    if os.name == "nt":
        parser.error("The fake Deno executable fixture currently requires Linux/macOS")
    profile = ROOT if args.group == "root" else PORTS / args.group
    source = profile / "musicplayer-lavaplayer/src/main/java/info/u_team/music_player/lavaplayer/sources/YoutubeServicesAudioSourceManager.java"
    jdk = Path(os.environ["JAVA_HOME"]) / "bin" if os.environ.get("JAVA_HOME") else None
    java = str(jdk / "java") if jdk else shutil.which("java")
    javac = str(jdk / "javac") if jdk else shutil.which("javac")
    if not java or not javac:
        parser.error("A JDK is required")
    with tempfile.TemporaryDirectory(prefix="musicplayer-youtube-test-") as directory:
        work = Path(directory)
        jars = []
        api = work / "api"
        api.mkdir()
        with zipfile.ZipFile(args.artifact) as archive:
            for name in archive.namelist():
                if name.startswith("dependencies/") and name.endswith(".jar.packed"):
                    target = work / Path(name).name.removesuffix(".packed")
                    target.write_bytes(archive.read(name))
                    jars.append(target)
                elif name.startswith("info/u_team/music_player/lavaplayer/api/") and name.endswith(".class"):
                    target = api / name
                    target.parent.mkdir(parents=True, exist_ok=True)
                    target.write_bytes(archive.read(name))
        fixture = work / "fake-deno"
        shutil.copy2(PORTS / "tests/youtube-fixtures/fake-deno.py", fixture)
        fixture.chmod(0o755)
        output = work / "classes"
        output.mkdir()
        cp = os.pathsep.join(map(str, [api, *jars, args.slf4j.resolve()]))
        subprocess.run([javac, "-encoding", "UTF-8", "-cp", cp, "-d", str(output),
                        str(source), str(PORTS / "tests/youtube-java/YoutubeStartupCheck.java")], check=True)
        subprocess.run([java, f"-Dmusicplayer.test.work={work}", "-cp", str(output) + os.pathsep + cp,
                        "info.u_team.music_player.lavaplayer.sources.YoutubeStartupCheck", str(fixture)],
                       check=True, timeout=45)


if __name__ == "__main__":
    main()
