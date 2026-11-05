#!/usr/bin/env python3
"""Validate the installable Ornithe artifact, including Gen2 references and archive boundaries."""
import hashlib,io,json,pathlib,struct,sys,zipfile
p=pathlib.Path(sys.argv[1])
with zipfile.ZipFile(p) as z:
    assert z.testzip() is None,'Corrupt compressed entry in installable JAR'
    names=set(z.namelist());meta=json.loads(z.read('fabric.mod.json'))
    assert meta['id']=='tflcore' and meta['name']=='TFL Client'
    assert meta['environment']=='client' and meta['license']=='GPL-3.0-only'
    assert meta['depends']['minecraft']=='1.8.9' and meta['depends']['java']=='>=25'
    assert meta['entrypoints']['init']==['dev.theredstonee.trsclient.ornithe.OrnitheClient']
    assert 'LICENSE_trsclient' in names and 'META-INF/TFL-NOTICE.txt' in names
    assert 'assets/trsclient/font/nunito-regular.ttf' in names
    assert 'dev/theredstonee/trsclient/ornithe/capability/CapabilityRegistry.class' in names
    assert not any(n.endswith('/PocMenu.class') or n.endswith('/PocHost.class') for n in names)
    assert 'assets/minecraft/shaders/program/trsclient_color.fsh' in names
    assert 'circuits/index.json' in names
    assert len(json.loads(z.read('circuits/index.json'))['circuits']) >= 3
    nested={n for n in names if n.endswith('.jar')}
    bridge='assets/trsclient/e4mc/e4mc-bridge.jar'
    assert bridge in nested,'World Sharing transport bridge missing'
    assert all(n==bridge or n.startswith('META-INF/jars/fastutil-8.5.15') for n in nested),nested
    with zipfile.ZipFile(io.BytesIO(z.read(bridge))) as b:
        assert 'META-INF/LICENSE-e4mc' in b.namelist()
        assert all(n.startswith('dev/theredstonee/trsclient/e4mcbridge/QuiclimeTunnel') for n in b.namelist() if n.endswith('.class')),'Unexpected code in isolated sharing bridge'
    assert 'META-INF/licenses/diorama-MIT.txt' in names
    assert not any(n.endswith('/MenuFontMixin.class') or n.endswith('/ClientMenuFontMixin.class') for n in names)
    assert 'dev/diorama/bake/TileBaker.class' in names
    assert not any('/ornithe/dev/' in n or '/forge/' in n or n=='mcmod.info' for n in names)
    classes=[z.read(n) for n in names if n.endswith('.class')]
    assert all(struct.unpack('>H',b[6:8])[0]==69 for b in classes)
    adapters=[z.read(n) for n in names if '/ornithe/' in n and n.endswith('.class')]
    assert any(b'net/minecraft/unmapped/' in b for b in adapters),'Expected Calamus Gen2 runtime references'
    assert not any(b'net/minecraftforge/' in b for b in adapters)
print(json.dumps({'artifact':p.name,'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'classes':len(classes),'validated':True},indent=2))
