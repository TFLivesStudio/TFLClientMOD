#!/usr/bin/env python3
"""Real Linux/X11 client smoke driver. Focuses the native window for Robot input and verifies report status."""
import argparse,ctypes as C,ctypes.util,json,os,pathlib,re,shutil,subprocess,time
p=argparse.ArgumentParser();p.add_argument('--stack',choices=['baseline','lwjgl3','argentum'],default='baseline');p.add_argument('--mods-dir',type=pathlib.Path);p.add_argument('--restart',action='store_true');p.add_argument('--large',action='store_true');p.add_argument('--production',action='store_true');p.add_argument('--unified',action='store_true');p.add_argument('--graphics-mods-dir',type=pathlib.Path);p.add_argument('--profile',action='store_true');a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[2];project=root/'vendor/trs-client/ornithe'
report=project/'build/smoke'/('production' if a.production else a.stack)/('restart.json' if a.restart else 'report.json');report.unlink(missing_ok=True)
cmd=[str(project/'gradlew'),'-p',str(project),'--no-daemon','--max-workers=2',('runProdClient' if a.production else 'runClient'),'-Psmoke',f'-PtestStack={a.stack}']
if a.production:cmd.append('-PdepthNativeProbe')
if a.unified:cmd.append('-PunifiedSmoke')
if a.mods_dir:cmd.append(f'-PruntimeModsDir={a.mods_dir.resolve()}')
if a.graphics_mods_dir:cmd.append(f'-PruntimeGraphicsModsDir={a.graphics_mods_dir.resolve()}')
if a.profile:cmd.append('-PtflAuditProfile')
if a.restart:cmd.append('-Prestart')
if a.large:cmd.append('-PuiSmokeLarge')
x=C.CDLL(ctypes.util.find_library('X11'));x.XOpenDisplay.argtypes=[C.c_char_p];x.XOpenDisplay.restype=C.c_void_p
x.XSetInputFocus.argtypes=[C.c_void_p,C.c_ulong,C.c_int,C.c_ulong];x.XFlush.argtypes=[C.c_void_p];x.XCloseDisplay.argtypes=[C.c_void_p]
d=x.XOpenDisplay(os.environ.get('DISPLAY',':0').encode());assert d,'An X11 display is required'
class XError(C.Structure):
 _fields_=[('type',C.c_int),('display',C.c_void_p),('resourceid',C.c_ulong),('serial',C.c_ulong),('error_code',C.c_ubyte),('request_code',C.c_ubyte),('minor_code',C.c_ubyte)]
errors=[]
@C.CFUNCTYPE(C.c_int,C.c_void_p,C.POINTER(XError))
def handle_x_error(display,error):
 if error.contents.error_code not in (3,8):errors.append(error.contents.error_code)
 return 0
x.XSetErrorHandler.argtypes=[C.c_void_p];x.XSetErrorHandler(C.cast(handle_x_error,C.c_void_p))
def timeout_diagnostics():
 # Preserve native client stacks when the outer watchdog fires, rather than losing the cause.
 tool=shutil.which('jcmd')
 if not tool:return
 try:
  clients=subprocess.run([tool,'-l'],capture_output=True,text=True,timeout=10).stdout
  report.parent.mkdir(parents=True,exist_ok=True)
  for line in clients.splitlines():
   if not any(name in line for name in ('KnotClient','Minecraft','devlaunchinjector')):continue
   pid=line.split()[0]
   stacks=subprocess.run([tool,pid,'Thread.print','-l'],capture_output=True,text=True,timeout=15)
   (report.parent/f'timeout-threads-{pid}.txt').write_text(stacks.stdout+stacks.stderr)
 except (OSError,subprocess.TimeoutExpired) as error:print('Could not collect timeout stacks:',error,flush=True)

proc=subprocess.Popen(cmd);deadline=time.monotonic()+(900 if a.unified else 480)
pulse=report.parent/'probe-pulse.json'
try:pulse.unlink()
except FileNotFoundError:pass
pulse_started=None
last_pulse=0.0
last_op=None
last_op_at=0.0
# Phase 4 legally waits minutes for Diorama tiles on software GL; every other phase must
# name a different operation within a sustained window (well under the 600 s probe budget).
SLOW_BAKE_PHASE=4
OP_STALE_SEC=420
def echo_diagnostics(reason):
 # Keep the evidence even when the step fails: thread dumps and reports are echoed to the job
 # log, because the artifact upload step can itself be skipped after unrelated failures.
 print(f'Smoke diagnostics: {reason}',flush=True)
 if not report.parent.exists():return print('No smoke directory exists yet',flush=True)
 for item in sorted(report.parent.glob('*')):
  if item==report:continue
  print(f'--- {item}',flush=True)
  if item.suffix in ('.txt','.json') and item.is_file():
   print(item.read_text(errors='replace')[:120_000],flush=True)
def current_phase():
 try:return json.loads(pulse.read_text()).get('phase',-1)
 except (OSError,ValueError):return -1
try:
 while proc.poll() is None:
  now=time.monotonic()
  if pulse.exists():
   stamp=pulse.stat().st_mtime
   if stamp!=pulse_started:
    text=pulse.read_text(errors='replace')
    if pulse_started is None:print(f'Probe pulse: {text}',flush=True)
    pulse_started=stamp;last_pulse=now
    try:current=json.loads(text)
    except ValueError:current={}
    if current.get('lastOp')!=last_op:last_op=current.get('lastOp');last_op_at=now
  if pulse_started is not None and now-last_pulse>180:
   timeout_diagnostics();echo_diagnostics(f'inactivity: no probe pulse for {int(now-last_pulse)}s');proc.terminate();raise SystemExit(f'Smoke inactivity timeout: no probe pulse for {int(now-last_pulse)}s; the printed pulse names the blocked probe phase and last operation')
  if pulse_started is not None and current_phase()!=SLOW_BAKE_PHASE and now-last_op_at>OP_STALE_SEC:
   timeout_diagnostics();echo_diagnostics(f'no probe progress: lastOp unchanged for {int(now-last_op_at)}s');proc.terminate();raise SystemExit(f'Smoke probe progress timeout: lastOp unchanged for {int(now-last_op_at)}s during phase {current_phase()}: {last_op}')
  if now>deadline:
   timeout_diagnostics();echo_diagnostics('total deadline reached');proc.terminate();raise SystemExit('Gradle/client smoke timeout')
  tree=subprocess.run(['xwininfo','-root','-tree'],capture_output=True,text=True,timeout=5).stdout
  win=re.search(r'(0x[0-9a-f]+) "(?:(?:TFL Client · )?Minecraft|TFL Client 1\.8\.9)',tree)
  if win:x.XSetInputFocus(d,int(win.group(1),16),2,0);x.XFlush(d)
  time.sleep(.5)
finally:x.XCloseDisplay(d)
assert not errors,f'Unexpected X11 errors: {errors}'
assert proc.returncode==0,f'Gradle exit {proc.returncode}'
r=json.loads(report.read_text());assert r['status']=='passed',{'error':r.get('error'),'failed':[c['name'] for c in r['checks'] if not c['passed']]}
print(f"Verified {a.stack} {'restart' if a.restart else 'smoke'}: {len(r['checks'])} checks")
