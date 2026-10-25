#!/usr/bin/env python3
"""Real Linux/X11 client smoke driver. Focuses the native window for Robot input and verifies report status."""
import argparse,ctypes as C,ctypes.util,json,os,pathlib,re,subprocess,time
p=argparse.ArgumentParser();p.add_argument('--stack',choices=['baseline','lwjgl3','argentum'],default='baseline');p.add_argument('--mods-dir',type=pathlib.Path);p.add_argument('--restart',action='store_true');p.add_argument('--large',action='store_true');a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[2];project=root/'vendor/trs-client/ornithe'
report=project/'build/smoke'/a.stack/('restart.json' if a.restart else 'report.json');report.unlink(missing_ok=True)
cmd=[str(project/'gradlew'),'-p',str(project),'--no-daemon','--max-workers=2','runClient','-Psmoke',f'-PtestStack={a.stack}']
if a.mods_dir:cmd.append(f'-PruntimeModsDir={a.mods_dir.resolve()}')
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
proc=subprocess.Popen(cmd);deadline=time.monotonic()+480
try:
 while proc.poll() is None:
  if time.monotonic()>deadline:proc.terminate();raise SystemExit('Gradle/client smoke timeout')
  tree=subprocess.run(['xwininfo','-root','-tree'],capture_output=True,text=True,timeout=5).stdout
  win=re.search(r'(0x[0-9a-f]+) "(?:TFL Client · )?Minecraft',tree)
  if win:x.XSetInputFocus(d,int(win.group(1),16),2,0);x.XFlush(d)
  time.sleep(.5)
finally:x.XCloseDisplay(d)
assert not errors,f'Unexpected X11 errors: {errors}'
assert proc.returncode==0,f'Gradle exit {proc.returncode}'
r=json.loads(report.read_text());assert r['status']=='passed',r
print(f"Verified {a.stack} {'restart' if a.restart else 'smoke'}: {len(r['checks'])} checks")
