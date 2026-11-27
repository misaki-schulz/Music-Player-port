"""Plan and build every supported release group on GitHub Actions."""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import subprocess
import sys

sys.dont_write_bytecode = True
from package_ports import BINARY_GROUPS, DESTINATION, ROOT, TARGETS as FABRIC_TARGETS

PORTS = ROOT / "ports"
NATIVE_TARGETS = json.loads((PORTS / "loader-targets.json").read_text(encoding="utf-8"))


def jobs() -> list[dict]:
    result = [
        {"id": f"fabric-{group}", "loader": "fabric", "package_group": group,
         "targets": [{**target, "loader": "fabric"} for target in FABRIC_TARGETS
                     if target["minecraft"] in versions]}
        for group, versions in BINARY_GROUPS
    ]
    native_groups = {}
    for target in NATIVE_TARGETS:
        key = target.get("package_group") or f'{target["loader"]}-{target["minecraft"]}'
        if key not in native_groups:
            job = {"id": key, "loader": target["loader"],
                   "package_group": target.get("package_group"), "targets": []}
            native_groups[key] = job
            result.append(job)
        native_groups[key]["targets"].append(target)
    expected = [("fabric", t["minecraft"]) for t in FABRIC_TARGETS]
    expected += [(t["loader"], t["minecraft"]) for t in NATIVE_TARGETS]
    actual = [(t["loader"], t["minecraft"]) for job in result for t in job["targets"]]
    if sorted(actual) != sorted(expected) or len(set(actual)) != len(actual):
        raise ValueError("CI matrix must cover every loader/version exactly once")
    return result


def run(command: list[str], cwd: Path, log: Path, environment: dict[str, str]) -> None:
    print(f'Running in {cwd}: {command}', flush=True)
    with log.open("w", encoding="utf-8") as output:
        process = subprocess.Popen(command, cwd=cwd, env=environment, stdout=subprocess.PIPE,
                                   stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace")
        assert process.stdout is not None
        for line in process.stdout:
            print(line, end="", flush=True)
            output.write(line)
        status = process.wait()
    if status:
        raise subprocess.CalledProcessError(status, command)


def build(job: dict) -> None:
    if os.environ.get("GITHUB_ACTIONS") != "true":
        raise RuntimeError("This builder runs only on GitHub Actions; use the local build menu locally")
    homes = {}
    for major in (17, 21, 25):
        value = os.environ.get(f"MUSICPLAYER_JAVA{major}_HOME")
        if not value or not (Path(value) / "bin/java").is_file():
            raise RuntimeError(f"Missing JDK {major}: MUSICPLAYER_JAVA{major}_HOME")
        homes[major] = value
    # setup-java installs outside /usr/lib/jvm. Make all three toolchains visible
    # to every Gradle version instead of relying on Linux distribution paths.
    gradle_home = Path(os.environ.get("GRADLE_USER_HOME", str(Path.home() / ".gradle")))
    gradle_home.mkdir(parents=True, exist_ok=True)
    properties = gradle_home / "gradle.properties"
    existing = properties.read_text(encoding="utf-8") if properties.exists() else ""
    properties.write_text(existing + "\norg.gradle.java.installations.paths="
                          + ",".join(homes.values()) + "\n", encoding="utf-8")
    logs = ROOT / ".cache/ci-logs"
    logs.mkdir(parents=True, exist_ok=True)
    for target in job["targets"]:
        # ForgeGradle 6 uses Gradle 8.12.1, which cannot run on Java 25.
        major = 21 if target["loader"] == "forge" and target["minecraft"] == "1.21.1" else 25
        environment = os.environ.copy()
        environment["JAVA_HOME"] = homes[major]
        environment["PATH"] = str(Path(homes[major]) / "bin") + os.pathsep + environment.get("PATH", "")
        if target.get("build_system") == "forgegradle7":
            environment["_JAVA_OPTIONS"] = "-Xms256m -Xmx3g -XX:ActiveProcessorCount=2"
        version = target["minecraft"]
        run(["sh", "./gradlew", "assemble", f"-PtargetVersion={version}", "--no-daemon",
             "--max-workers=2", "--console=plain",
             "-Dorg.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8"],
            PORTS / target["group"], logs / f"{target['loader']}-{version}.log", environment)
        if target.get("build_system") == "forgegradle7":
            run([sys.executable, "-B", str(PORTS / "check_forge_decompiler.py"), version],
                ROOT, logs / f"decompiler-{version}.log", environment)
    if job["loader"] == "fabric":
        package = ["package_ports.py", "--group", job["package_group"]]
    elif job["package_group"]:
        package = ["package_loaders.py", "--group", job["package_group"]]
    else:
        package = ["package_loaders.py", "--loader", job["loader"],
                   "--version", job["targets"][0]["minecraft"]]
    run([sys.executable, "-B", str(PORTS / package[0]), *package[1:]], ROOT,
        logs / "packaging.log", os.environ.copy())
    if not list(DESTINATION.glob("*.jar")):
        raise RuntimeError("Packaging produced no release JAR")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("matrix", "build"))
    parser.add_argument("--job", choices=[job["id"] for job in jobs()])
    args = parser.parse_args()
    if args.command == "matrix":
        print("matrix=" + json.dumps({"include": [{"id": job["id"]} for job in jobs()]}, separators=(",", ":")))
    elif not args.job:
        parser.error("build requires --job")
    else:
        build(next(job for job in jobs() if job["id"] == args.job))


if __name__ == "__main__":
    main()
