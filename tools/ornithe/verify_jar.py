#!/usr/bin/env python3
"""Validate the installable Ornithe artifact, including Gen2 references and archive boundaries."""
import hashlib,json,pathlib,struct,sys,zipfile
p=pathlib.Path(sys.argv[1])
with zipfile.ZipFile(p) as z:
    names=set(z.namelist());meta=json.loads(z.read('fabric.mod.json'))
    assert meta['id']=='tflcore' and meta['name']=='TFL Client'
    assert meta['environment']=='client' and meta['license']=='GPL-3.0-only'
    assert meta['depends']['minecraft']=='1.8.9' and meta['depends']['java']=='>=25'
    assert meta['entrypoints']['init']==['dev.theredstonee.trsclient.ornithe.OrnitheClient']
    assert 'LICENSE_trsclient' in names and 'META-INF/TFL-NOTICE.txt' in names
    assert 'assets/trsclient/font/nunito-regular.ttf' in names
    assert not any(n.endswith('.jar') or '/ornithe/dev/' in n or '/forge/' in n or n=='mcmod.info' for n in names)
    classes=[z.read(n) for n in names if n.endswith('.class')]
    assert all(struct.unpack('>H',b[6:8])[0]==69 for b in classes)
    adapters=[z.read(n) for n in names if '/ornithe/' in n and n.endswith('.class')]
    assert any(b'net/minecraft/unmapped/' in b for b in adapters),'Expected Calamus Gen2 runtime references'
    assert not any(b'net/minecraftforge/' in b for b in adapters)
print(json.dumps({'artifact':p.name,'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'classes':len(classes),'validated':True},indent=2))
