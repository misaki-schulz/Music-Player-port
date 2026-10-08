from pathlib import Path
import subprocess,zipfile,os
source=Path(__file__).resolve().parent
root=source.parents[1]
here=root/'.cache/client-tests'
here.mkdir(parents=True,exist_ok=True)
import wave
with wave.open(str(here/'fixture.wav'),'wb') as fixture:
 fixture.setnchannels(2);fixture.setsampwidth(2);fixture.setframerate(48000)
 fixture.writeframes(bytes(48000*30*2*2))
(here/'classes').mkdir(exist_ok=True)
java_candidates=[Path(os.environ.get('JAVA_HOME','')),*Path.home().joinpath('.jdks').glob('*')]
if os.name=='nt':java_candidates+=list(Path(os.environ.get('ProgramFiles',r'C:\Program Files')).joinpath('Java').glob('*'))
javac=next((p/'bin'/('javac.exe' if os.name=='nt' else 'javac') for p in java_candidates if (p/'bin'/('javac.exe' if os.name=='nt' else 'javac')).exists()),None)
if javac is None:raise SystemExit('Install a JDK 17+ or set JAVA_HOME')
cache=Path.home()/'.gradle/caches/modules-2/files-2.1/org.ow2.asm'
jars=[next((cache/p/'9.10.1').rglob('*.jar')) for p in ['asm','asm-tree']]
subprocess.run([str(javac),'--release','17','-cp',os.pathsep.join(map(str,jars)),
 '-d',str(here/'classes'),str(source/'GuiScenario.java'),str(source/'PickerFixture.java')],check=True)
with zipfile.ZipFile(here/'client-probe-v2.jar','w',zipfile.ZIP_DEFLATED) as out:
 out.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\nPremain-Class: GuiScenario\n\n')
 for p in (here/'classes').glob('*.class'):
  if p.name.startswith(('GuiScenario','PickerFixture')):out.write(p,p.name)
 for jar in jars:
  with zipfile.ZipFile(jar) as z:
   for name in z.namelist():
    if name.startswith('org/') and not name.endswith('/'):out.writestr(name,z.read(name))
with zipfile.ZipFile(here/'client-probe-v2.jar') as packed,zipfile.ZipFile(here/'client-probe-fabric.jar','w',zipfile.ZIP_DEFLATED) as out:
 for name in packed.namelist():
  if not name.startswith('org/'):out.writestr(name,packed.read(name))
folder=here/'fixture-folder';folder.mkdir(exist_ok=True)
for name in ['track2.wav','track10.wav']:(folder/name).write_bytes((here/'fixture.wav').read_bytes())
print('Built deterministic GUI scenario agent')
