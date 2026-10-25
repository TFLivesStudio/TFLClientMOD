#!/usr/bin/env python3
"""Audit supplied/bundled footage and actual native background ROIs; never infer motion from UI counters alone."""
import argparse,hashlib,json,pathlib,subprocess
from PIL import Image,ImageChops,ImageStat
p=argparse.ArgumentParser();p.add_argument('--output',type=pathlib.Path,required=True);a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[2];media=root/'vendor/trs-client/common/src/main/resources/assets/trsclient/video';results={}
for name,fps in [('menu.mjpeg',24),('menu-balanced.mjpeg',24),('menu-performance.mjpeg',15)]:
 f=media/name;probe=json.loads(subprocess.check_output(['ffprobe','-v','error','-f','mjpeg','-framerate',str(fps),'-count_frames','-show_entries','stream=width,height,nb_read_frames','-of','json',str(f)]))['streams'][0]
 results[name]={'sha256':hashlib.sha256(f.read_bytes()).hexdigest(),'bytes':f.stat().st_size,'playbackFps':fps,'rawMjpegHasTimestamps':False,**probe}
comparisons=[]
for target,folder in [('1.8.9',root/'vendor/trs-client/ornithe/build/smoke/production/screenshots'),('26.3',root/'vendor/trs-client/run/prod-depth-sodium/screenshots')]:
 first=folder/'depth-video-start.png';last=folder/'depth-video-0.png'
 if not(first.exists() and last.exists()):continue
 im1=Image.open(first).convert('RGB');im2=Image.open(last).convert('RGB');assert im1.size==im2.size
 w,h=im1.size;box=(int(w*.63),int(h*.12),int(w*.92),int(h*.43));difference=ImageChops.difference(im1.crop(box),im2.crop(box));mean=sum(ImageStat.Stat(difference).mean)/3
 changed=sum(max(pixel)>3 for pixel in difference.getdata())/(difference.width*difference.height)
 assert mean>.05 and changed>.01,(target,mean,changed)
 comparisons.append({'target':target,'sourceScreenshots':[first.name,last.name],'roi':box,'meanAbsoluteRgbDifference':mean,'changedPixelFractionAbove3':changed,'scope':'same native title mode, background ROI away from menu controls and status badge','passed':True})
a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text(json.dumps({'bundledMedia':results,'nativeBackgroundMotion':comparisons},indent=2)+'\n');print('Verified media and '+str(len(comparisons))+' native background ROIs')
