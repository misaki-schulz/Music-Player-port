"""Save a portable summary only after all exact GUI targets have passed."""
from pathlib import Path
import hashlib,json,re
from datetime import datetime,timezone
ROOT=Path(__file__).resolve().parents[2]
CACHE=ROOT/'.cache/client-tests'
records=json.loads((CACHE/'matrix-results.json').read_text(encoding='utf-8'))
targets=[{**t,'loader':'fabric'} for t in json.loads((ROOT/'ports/targets.json').read_text(encoding='utf-8'))]+json.loads((ROOT/'ports/loader-targets.json').read_text(encoding='utf-8'))
expected={(t['loader'],t['minecraft']) for t in targets}
agent=hashlib.sha256((CACHE/'client-probe-v2.jar').read_bytes()).hexdigest()
fabric_agent=hashlib.sha256((CACHE/'client-probe-fabric.jar').read_bytes()).hexdigest()
history=json.loads((Path(__file__).parent/'agent-history.json').read_text(encoding='utf-8')) if (Path(__file__).parent/'agent-history.json').exists() else {}
by_key={(r['loader'],r['minecraft']):r for r in records if r.get('release')=='port.10.2' and r['status']=='passed' and r.get('agent_sha256')==(fabric_agent if r['loader']=='fabric' else agent)}
assert set(by_key)==expected, f'Incomplete GUI matrix: {sorted(expected-set(by_key))}'
output=[]
for target in targets:
 r=by_key[target['loader'],target['minecraft']]
 report=ROOT/r['report'];lines=report.read_text(encoding='utf-8').splitlines()
 assert lines[0]=='PASS' and r['client_exit']==0
 checks=lines[1:-1]
 assert len(checks)>=60, (r['key'],len(checks))
 checks=[re.sub(r'http://127\.0\.0\.1:\d+/fixture.wav','http://127.0.0.1:<fixture-port>/fixture.wav',line.replace(str(CACHE),'${fixtures}').replace(CACHE.as_posix(),'${fixtures}')) for line in checks]
 item={k:r[k] for k in ['loader','minecraft','status','mode','client_exit','seconds','limits','agent_sha256','input_sha256']}
 item['checks']=checks;item['check_count']=len(checks)
 item['screenshot_count']=len(r['screenshots'])
 if 'jar_sha256' in r:
  path=ROOT/'dist/port.10.2'/f"music_player-{r['loader']}-{r['minecraft']}-2.7.1.351.port.10.2.jar"
  assert hashlib.sha256(path.read_bytes()).hexdigest()==r['jar_sha256']
  item['jar_sha256']=r['jar_sha256']
 output.append(item)
source={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in Path(__file__).parent.glob('*.java')}
summary={'tested_at_utc':datetime.now(timezone.utc).isoformat(),'release':'port.10.2','scope':'GUI buttons and WAV track loading in actual Minecraft clients','agent_sha256':agent,'fabric_agent_sha256':fabric_agent,'agent_source_sha256':source,'prior_agent_history':history,'results':output}
p=Path(__file__).parent/'last-results.json'
p.write_text(json.dumps(summary,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
print('Saved',len(output),'passed GUI results;',sum(r['check_count'] for r in output),'checks')

versions=list(dict.fromkeys(t['minecraft'] for t in targets))
lookup={(r['loader'],r['minecraft']):r for r in output}
text=['# GUI-РїСЂРѕРІРµСЂРєР° port.10.2', '', f'Р’СЃРµ {len(expected)} СЃРѕС‡РµС‚Р°РЅРёР№ РїСЂРѕС€Р»Рё СЃС†РµРЅР°СЂРёР№ РІ РЅР°СЃС‚РѕСЏС‰РµРј РєР»РёРµРЅС‚Рµ Minecraft.', '', 'РџСЂРѕРІРµСЂРµРЅС‹ GUI-РєРЅРѕРїРєРё, РїРµСЂРµС…РѕРґС‹ РјРµР¶РґСѓ РѕРєРЅР°РјРё, РїР»РµР№Р»РёСЃС‚С‹, РїРµСЂРµРјРµС‰РµРЅРёРµ/СѓРґР°Р»РµРЅРёРµ СЃС‚СЂРѕРє, РїРѕРёСЃРє РІ СЃРїРёСЃРєР°С…, WAV РїРѕ РїСѓС‚Рё Рё HTTP, РёРјРїРѕСЂС‚ С„Р°Р№Р»Р°/РїР°РїРєРё, СѓРїСЂР°РІР»РµРЅРёРµ С‚СЂРµРєРѕРј, СЃРѕСЂС‚РёСЂРѕРІРєР°, РЅР°СЃС‚СЂРѕР№РєРё Рё СЌРєРІР°Р»Р°Р№Р·РµСЂ.', '', '| Minecraft | Fabric | Forge | NeoForge |', '|---|---|---|---|']
for mc in versions:
    cells=[]
    for loader in ['fabric','forge','neoforge']:
        r=lookup.get((loader,mc))
        cells.append(('PASS ('+str(r['check_count'])+')') if r else 'вЂ”')
    text.append('| '+mc+' | '+' | '.join(cells)+' |')
text+=['', 'PASS (С‡РёСЃР»Рѕ) вЂ” РєРѕР»РёС‡РµСЃС‚РІРѕ РїСЂРѕРІРµСЂРѕРє РІ СЃРѕРѕС‚РІРµС‚СЃС‚РІСѓСЋС‰РµРј Р·Р°РїСѓСЃРєРµ.', '', '16 СЃРѕРІСЂРµРјРµРЅРЅС‹С… NeoForge Р·Р°РїСѓСЃРєР°СЋС‚ РіРѕС‚РѕРІС‹Р№ release JAR СЃ РІР»РѕР¶РµРЅРЅС‹РјРё Р°СѓРґРёРѕР±РёР±Р»РёРѕС‚РµРєР°РјРё РІ dev-РєР»РёРµРЅС‚Рµ. Fabric, Forge Рё NeoForge 1.20.1 Р·Р°РїСѓСЃРєР°СЋС‚ СЃРєРѕРјРїРёР»РёСЂРѕРІР°РЅРЅС‹Рµ РёСЃС…РѕРґРЅРёРєРё dev-РєР»РёРµРЅС‚Р°. Release-Р°СЂС…РёРІС‹ РґРѕРїРѕР»РЅРёС‚РµР»СЊРЅРѕ РїСЂРѕРІРµСЂСЏСЋС‚СЃСЏ СѓРїР°РєРѕРІС‰РёРєР°РјРё РїСЂРѕРµРєС‚Р°.', '', '**Р“СЂР°РЅРёС†С‹ РїСЂРѕРІРµСЂРєРё:** РѕС‚РІРµС‚С‹ СЃРёСЃС‚РµРјРЅРѕРіРѕ chooser РїРѕРґСЃС‚Р°РІР»СЏСЋС‚СЃСЏ С‚РµСЃС‚РѕРј; РµРіРѕ Windows-РёРЅС‚РµСЂС„РµР№СЃ РЅРµ РїСЂРѕРІРµСЂСЏРµС‚СЃСЏ. РђРІС‚РѕСЂРёР·Р°С†РёСЏ Рё РїРѕРёСЃРє РІ СЃС‚РѕСЂРѕРЅРЅРёС… РјСѓР·С‹РєР°Р»СЊРЅС‹С… СЃРµСЂРІРёСЃР°С…, РєР°С‡РµСЃС‚РІРѕ Р·РІСѓРєР° Рё РёРіСЂРѕРІРѕР№ РјРёСЂ РЅРµ РїСЂРѕРІРµСЂСЏСЋС‚СЃСЏ.', '', 'РџРѕРґСЂРѕР±РЅС‹Рµ СЂРµР·СѓР»СЊС‚Р°С‚С‹ Рё SHA-256: [last-results.json](last-results.json). РљРѕРјР°РЅРґС‹ РїРѕРІС‚РѕСЂРЅРѕРіРѕ Р·Р°РїСѓСЃРєР°: [README.md](README.md).', '', 'Р’СЂРµРјСЏ СЃРѕС…СЂР°РЅРµРЅРёСЏ РѕС‚С‡С‘С‚Р° (UTC): '+summary['tested_at_utc']+'.', '']
(Path(__file__).parent/'last-results.md').write_text('\n'.join(text),encoding='utf-8')
