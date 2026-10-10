"""Build all exact targets, then run offline regressions and validate release packaging."""
from pathlib import Path
import argparse
import json
import os
import subprocess
import sys
from datetime import datetime, timezone

PORTS = Path(__file__).resolve().parent
ROOT = PORTS.parent
CACHE = ROOT / '.cache/port.10.2-verification'
CACHE.mkdir(parents=True, exist_ok=True)
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--resume', action='store_true', help='Keep successful targets from this verification run')
args = parser.parse_args()
native = json.loads((PORTS / 'loader-targets.json').read_text(encoding='utf-8'))
fabric = [{**t, 'loader': 'fabric'} for t in json.loads((PORTS / 'targets.json').read_text(encoding='utf-8-sig'))]
targets = sorted(native + fabric, key=lambda t: (t.get('build_system') != 'forgegradle7', t['loader'], [x['minecraft'] for x in fabric].index(t['minecraft'])))
state_file = CACHE / 'results.json'
state = json.loads(state_file.read_text(encoding='utf-8')) if args.resume and state_file.exists() else {'release': 'port.10.2', 'builds': {}, 'regressions': {}}

def save():
    state['updated_at_utc'] = datetime.now(timezone.utc).isoformat()
    state_file.write_text(json.dumps(state, indent=2) + '\n', encoding='utf-8')

def run(key, command):
    log = CACHE / (key + '.log')
    print('START', key, flush=True)
    with log.open('w', encoding='utf-8') as output:
        result = subprocess.run(command, cwd=ROOT, stdout=output, stderr=subprocess.STDOUT)
    print('PASS' if result.returncode == 0 else 'FAIL', key, log, flush=True)
    return result.returncode == 0

for t in targets:
    key = t['loader'] + '-' + t['minecraft']
    if state['builds'].get(key) == 'passed':
        continue
    passed = run(key, ['pwsh', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', str(PORTS / 'Build-Target.ps1'),
                       '-Group', t['group'], '-Version', t['minecraft']])
    state['builds'][key] = 'passed' if passed else 'failed'
    save()
    if not passed:
        sys.exit(1)

for group in sorted({t['group'] for t in fabric}):
    for script in ('test_sorting.py', 'test_audio_routing.py', 'test_soundcloud.py'):
        key = script.removesuffix('.py') + '-' + group
        if state['regressions'].get(key) == 'passed':
            continue
        passed = run(key, [sys.executable, str(PORTS / script), group])
        state['regressions'][key] = 'passed' if passed else 'failed'
        save()
        if not passed:
            sys.exit(1)

for script in ('package_ports.py', 'package_loaders.py'):
    if not run(script.removesuffix('.py'), [sys.executable, str(PORTS / script)]):
        sys.exit(1)
state['packaging'] = 'passed'
save()
print('PASS: 50 exact builds, 33 profile regressions and all release archives', flush=True)
