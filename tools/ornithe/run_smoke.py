#!/usr/bin/env python3
"""Real Linux/X11 client smoke driver. Focuses the native window for Robot input and verifies report status."""
import argparse,ctypes as C,ctypes.util,json,os,pathlib,re,shutil,subprocess,time
p=argparse.ArgumentParser();p.add_argument('--stack',choices=['baseline','lwjgl3','argentum'],default='baseline');p.add_argument('--mods-dir',type=pathlib.Path);p.add_argument('--restart',action='store_true');p.add_argument('--large',action='store_true');p.add_argument('--production',action='store_true');p.add_argument('--unified',action='store_true');p.add_argument('--graphics-mods-dir',type=pathlib.Path);p.add_argument('--profile',action='store_true');p.add_argument('--audit5',action='store_true');p.add_argument('--no-fog',action='store_true');p.add_argument('--extra-mods-dir',type=pathlib.Path);a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[2];project=root/'vendor/trs-client/ornithe'
report_dir=project/'build/smoke'/('production' if a.production else a.stack)
if a.audit5:report_dir=report_dir/'audit5'
report=report_dir/('restart.json' if a.restart else 'report.json');report.unlink(missing_ok=True)
cmd=[str(project/'gradlew'),'-p',str(project),'--no-daemon','--max-workers=2',('runProdClient' if a.production else 'runClient'),'-Psmoke',f'-PtestStack={a.stack}']
if a.production:cmd.append('-PdepthNativeProbe')
if a.extra_mods_dir:cmd.append(f'-PdepthRuntimeModsDir={a.extra_mods_dir.resolve()}')
if a.unified:cmd.append('-PunifiedSmoke')
if a.audit5:cmd.append('-Paudit5Smoke')
if getattr(a,'no_fog',False):cmd.append('-PsmokeMapNoFog')
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
def timeout_diagnostics(sequence=0):
 # Preserve native client stacks when the outer watchdog fires, rather than losing the cause.
 tool=shutil.which('jcmd')
 if not tool:return print('Timeout stacks: jcmd unavailable; no thread dumps collected',flush=True)
 try:
  clients=subprocess.run([tool,'-l'],capture_output=True,text=True,timeout=10)
  report.parent.mkdir(parents=True,exist_ok=True)
  found=0
  for line in clients.stdout.splitlines():
   if not any(name in line for name in ('KnotClient','Minecraft','devlaunchinjector')):continue
   found+=1
   print(f'Timeout stacks: capturing {line.strip()}',flush=True)
   pid=line.split()[0]
   try:
    stacks=subprocess.run([tool,pid,'Thread.print','-l'],capture_output=True,text=True,timeout=15)
    output=(stacks.stdout or '')+(stacks.stderr or '')
   except subprocess.TimeoutExpired as expired:
    print(f'Timeout stacks: jcmd {pid} Thread.print timed out; keeping the partial output',flush=True)
    output=f'jcmd {pid} Thread.print abandoned after 15s (process kept for the outer watchdog)\n'
    for field in ('stdout','stderr'):
     chunk=getattr(expired,field,None)
     if isinstance(chunk,bytes):chunk=chunk.decode('utf-8','replace')
     if chunk:output+=chunk
   name=f'timeout-threads-{pid}.txt' if not sequence else f'timeout-threads-{sequence}-{pid}.txt'
   (report.parent/name).write_text(output)
  if not found:print('Timeout stacks: no client process matched; jcmd -l printed:\n'+clients.stdout+clients.stderr,flush=True)
 except (OSError,subprocess.TimeoutExpired) as error:print('Could not collect timeout stacks:',error,flush=True)

proc=subprocess.Popen(cmd);deadline=time.monotonic()+(900 if a.unified else 480)
pulse=report.parent/'probe-pulse.json'
try:pulse.unlink()
except FileNotFoundError:pass
pulse_started=None
last_pulse=0.0
last_op=None
last_op_at=0.0
last_ticks=None
last_ticks_at=0.0
last_tiles=None
last_tiles_at=0.0
tick_stall_reported=False
tile_stall_reported=False
# Phase 4 legally waits minutes for Diorama tiles on software GL; every other phase must
# name a different operation within a sustained window (well under the 600 s probe budget).
SLOW_BAKE_PHASE=4
OP_STALE_SEC=420
# A dead client loop is diagnosable long before the global deadline: the run that stalled in
# run 44 never ticked again after opening the world map, and dumps at ~120 s prove the cause.
TICK_STALL_SEC=120
# Ticking but counters frozen is a different failure; capture evidence without aborting bake.
TILE_STALL_SEC=240
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
    ticks=current.get('ticks')
    if ticks is not None and ticks!=last_ticks:last_ticks=ticks;last_ticks_at=now
    if current.get('baked') is not None:
     tiles=current['baked']+current.get('disk',0)+current.get('uploads',0)
     if tiles!=last_tiles:last_tiles=tiles;last_tiles_at=now
  if pulse_started is not None and last_ticks is not None and now-last_ticks_at>TICK_STALL_SEC and not tick_stall_reported:
   tick_stall_reported=True
   timeout_diagnostics(1);echo_diagnostics(f'tick stall: client has not ticked for {int(now-last_ticks_at)}s during phase {current_phase()}: {last_op}; dumps captured immediately, still waiting on the global deadline')
  if pulse_started is not None and last_ticks is not None and now-last_ticks_at<=TICK_STALL_SEC and current_phase()==SLOW_BAKE_PHASE and last_tiles is not None and now-last_tiles_at>TILE_STALL_SEC and not tile_stall_reported:
   tile_stall_reported=True
   timeout_diagnostics(2);echo_diagnostics(f'tile stall: client ticking but Diorama counters unchanged for {int(now-last_tiles_at)}s: ticks={last_ticks} tiles={last_tiles}; dumps captured, still waiting on the global deadline')
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
# Snapshots land in the smoke directory; echo them even on success so the fixed-pipeline
# state is readable from the job log regardless of artifact storage quota.
for snap in sorted(report.parent.glob('gl-snapshot-*.json')):
 print(f'--- {snap}',flush=True);print(snap.read_text(errors='replace')[:120_000],flush=True)
assert not errors,f'Unexpected X11 errors: {errors}'
assert proc.returncode==0,f'Gradle exit {proc.returncode}'
r=json.loads(report.read_text());assert r['status']=='passed',{'error':r.get('error'),'failed':[c['name'] for c in r['checks'] if not c['passed']]}
print(f"Verified {a.stack} {'restart' if a.restart else 'smoke'}: {len(r['checks'])} checks")
