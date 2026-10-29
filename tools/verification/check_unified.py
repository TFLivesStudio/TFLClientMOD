#!/usr/bin/env python3
"""Check native execution, keeping actual FPS distinct from a configured target."""
import json,pathlib,sys
report=pathlib.Path(sys.argv[1]);data=json.loads(report.read_text())
assert data['status']=='passed',(report,data.get('error'))
assert data['checks'] and all(row['passed'] for row in data['checks'])
assert data['video']['framesPresented']>80
assert data['video']['mode']=='HIGH'
assert data['video']['failure']==''
print(f"Native verified: {len(data['checks'])} checks; actual video {data['measuredVideoFps']:.2f} FPS (hardware dependent)")
