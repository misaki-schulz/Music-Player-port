"""Run real Minecraft GUI scenarios sequentially and record every exact loader/version."""
from pathlib import Path
import argparse,gzip,hashlib,json,os,shutil,subprocess,sys,time,threading
from http.server import ThreadingHTTPServer,SimpleHTTPRequestHandler
from functools import partial
ROOT=Path(__file__).resolve().parents[2]
SOURCE=Path(__file__).resolve().parent
HERE=ROOT/'.cache/client-tests'
HERE.mkdir(parents=True,exist_ok=True)
PORTS=ROOT/'ports'
FABRIC=[{**t,'loader':'fabric'} for t in json.loads((PORTS/'targets.json').read_text(encoding='utf-8'))]
NATIVE=json.loads((PORTS/'loader-targets.json').read_text(encoding='utf-8'))
JOBS=NATIVE+FABRIC
parser=argparse.ArgumentParser()
parser.add_argument('--loader',choices=['fabric','forge','neoforge'])
parser.add_argument('--version')
parser.add_argument('--resume',action='store_true')
parser.add_argument('--no-build',action='store_true')
args=parser.parse_args()
RESULTS=HERE/'matrix-results.json'
records=json.loads(RESULTS.read_text(encoding='utf-8')) if RESULTS.exists() else []
history=json.loads((SOURCE/'agent-history.json').read_text(encoding='utf-8')) if (SOURCE/'agent-history.json').exists() else {}
def find_jdk(major):
    candidates=[Path(os.environ.get('JAVA_HOME','')),*Path.home().joinpath('.jdks').glob('*')]
    candidates+=list(Path(os.environ.get('ProgramFiles',r'C:\Program Files')).joinpath('Java').glob('*'))
    for candidate in candidates:
        release=candidate/'release'
        if release.is_file() and ('JAVA_VERSION="'+str(major)+'.') in release.read_text(encoding='utf-8'):return candidate
    raise SystemExit(f'Install JDK {major} or set JAVA_HOME')

if not (HERE/'client-probe-v2.jar').exists():
    raise SystemExit('Run python ports/client-gui/build_agent.py first')
class FixtureHandler(SimpleHTTPRequestHandler):
    def handle(self):
        try:super().handle()
        except (ConnectionResetError,ConnectionAbortedError,BrokenPipeError):pass
    def log_message(self,*args):pass
server=ThreadingHTTPServer(('127.0.0.1',0),partial(FixtureHandler,directory=str(HERE)))
threading.Thread(target=server.serve_forever,daemon=True).start()
(HERE/'http-url.txt').write_text(f'http://127.0.0.1:{server.server_port}/fixture.wav',encoding='utf-8')
try:
    for target in JOBS:
        loader,mc=target['loader'],target['minecraft']
        if args.loader and args.loader!=loader or args.version and args.version!=mc:continue
        key=loader+'-'+mc
        previous=next((r for r in records if r['key']==key),None)
        agent_name='client-probe-fabric.jar' if loader=='fabric' else 'client-probe-v2.jar'
        agent_sha=hashlib.sha256((HERE/agent_name).read_bytes()).hexdigest()
        if args.resume and previous and previous['status']=='passed' and (previous.get('agent_sha256')==agent_sha or key in history.get(previous.get('agent_sha256'),{}).get('compatible_passed_keys',[])):continue
        game=HERE/key
        game.mkdir(exist_ok=True)
        for file in ['gui-result.txt','gui-error.txt','gui.log']:(game/file).unlink(missing_ok=True)
        (game/'config/musicplayer').mkdir(parents=True,exist_ok=True)
        (game/'config/musicplayer.json').write_text('{"internalPlaylists":true}',encoding='utf-8')
        (game/'config/musicplayer/settings.json').write_text('{"volume":0}',encoding='utf-8')
        with gzip.open(game/'config/musicplayer/playlist.json.gz','wt',encoding='utf-8') as f:f.write('{"playlists":[]}')
        (game/'options.txt').write_text('onboardAccessibility:false\nlang:en_us\nguiScale:2\nfullscreen:false\n',encoding='utf-8')
        print('START',key,flush=True)
        if not args.no_build:
            buildlog=HERE/(key+'-build.log')
            with buildlog.open('w',encoding='utf-8') as f:
                build=subprocess.run(['pwsh','-NoProfile','-ExecutionPolicy','Bypass','-File',str(PORTS/'Build-Target.ps1'),
                    '-Group',target['group'],'-Version',mc],cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
            if build.returncode:
                print('BUILD FAILED',key,buildlog,flush=True);sys.exit(1)
            if loader!='fabric':
                with buildlog.open('a',encoding='utf-8') as f:
                    packaged=subprocess.run([sys.executable,str(PORTS/'package_loaders.py'),'--loader',loader,'--version',mc],cwd=ROOT,stdout=f,stderr=subprocess.STDOUT)
                if packaged.returncode:print('PACKAGE FAILED',key,flush=True);sys.exit(1)
        production=loader=='neoforge' and mc!='1.20.1'
        if production:
            (game/'mods').mkdir(exist_ok=True)
            jar=ROOT/'dist/port.10'/f'music_player-{loader}-{mc}-2.7.1.351.port.10.jar'
            shutil.copyfile(jar,game/'mods'/jar.name)
        task='runGuiTest' if loader=='forge' or mc=='1.20.1' and loader=='neoforge' else 'runClient'
        command=[task,f'-PtargetVersion={mc}',
                 f'-PclientTestRoot={game}',f'-PclientTestProduction={str(production).lower()}',
                 '--init-script',str(SOURCE/'client-test.init.gradle'),'--no-daemon','--max-workers=1',
                 '-Dorg.gradle.jvmargs=-Xmx1200m -Dfile.encoding=UTF-8','--console=plain']
        # cmd preserves dotted -P versions on Windows.
        command=['cmd.exe','/d','/c',str(PORTS/target['group']/'gradlew.bat'),*command]
        env=dict(os.environ)
        env['JAVA_HOME']=str(find_jdk(21 if loader=='forge' and mc!='1.20.1' else 25))
        logfile=HERE/(key+'-client.log')
        started=time.time()
        with logfile.open('w',encoding='utf-8') as f:
            process=subprocess.Popen(command,cwd=PORTS/target['group'],env=env,stdout=f,stderr=subprocess.STDOUT)
            try:exitcode=process.wait(timeout=900)
            except subprocess.TimeoutExpired:
                subprocess.run(['taskkill','/PID',str(process.pid),'/T','/F'],capture_output=True)
                exitcode=-1
        report=game/'gui-result.txt'
        error=game/'gui-error.txt'
        status='passed' if exitcode==0 and report.exists() and report.read_text(encoding='utf-8').startswith('PASS\n') and not error.exists() else 'failed'
        record={'key':key,'loader':loader,'minecraft':mc,'status':status,'client_exit':exitcode,
            'mode':'release-jar-in-development-client' if production else 'development-client',
            'seconds':round(time.time()-started,1),'log':str(logfile.relative_to(ROOT)),
            'report':str(report.relative_to(ROOT)) if report.exists() else None,
            'screenshots':[str(p.relative_to(ROOT)) for p in sorted((game/'screenshots').glob('*.png'))],
            'limits':['OS chooser UI not tested; file/folder GUI callbacks use fixture chooser replies','Authenticated network providers not tested']}
        record['agent_sha256']=agent_sha
        if production:record['jar_sha256']=hashlib.sha256(jar.read_bytes()).hexdigest()
        records=[r for r in records if r['key']!=key]+[record]
        RESULTS.write_text(json.dumps(records,indent=2)+'\n',encoding='utf-8')
        print(status.upper(),key,'in',record['seconds'],'seconds',flush=True)
        if status!='passed':print('Inspect',logfile,flush=True);sys.exit(1)
    print('DONE',len(records),'records,',sum(r['status']=='passed' for r in records),'passed',flush=True)
finally:
    server.shutdown();server.server_close()
