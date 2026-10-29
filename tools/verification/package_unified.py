#!/usr/bin/env python3
"""Assemble one verified target. No test probe or external optimizer binary is distributed."""
import hashlib,json,pathlib,shutil,subprocess,sys,zipfile
root=pathlib.Path(__file__).resolve().parents[2]
source=pathlib.Path(sys.argv[1]);out=pathlib.Path(sys.argv[2]);target=sys.argv[3];out.mkdir(parents=True,exist_ok=True)
prefix='tflcore-1.8.9-' if target=='1.8.9' else 'tflcore-fabric-26.3-'
jars=[p for p in source.rglob(prefix+'*.jar') if not any(k in p.name for k in ['sources','probe','dev'])]
assert len(jars)==1,[(str(p)) for p in jars]
jar=jars[0]
with zipfile.ZipFile(jar) as z:
 meta=json.loads(z.read('fabric.mod.json'));names=z.namelist()
 assert meta['depends']['minecraft']==target
 assert 'META-INF/licenses/diorama-MIT.txt' in names
 assert 'dev/diorama/bake/TileBaker.class' in names
 assert not any('/ornithe/dev/' in n or '/trsclient/dev/' in n for n in names)
 assert all('assets/trsclient/video/'+name in names for name in ['menu.mjpeg','menu-balanced.mjpeg','menu-performance.mjpeg'])
shutil.copy2(jar,out/jar.name)
# Separate native report checks from compilation and source metadata.
reportname='report.json' if target=='1.8.9' else 'unified-smoke.json'
reports=[p for p in source.rglob(reportname) if 'production' in str(p) or target=='26.3']
assert reports,'No production native report'
for report in reports:
 r=json.loads(report.read_text());assert r['status']=='passed',(report,r.get('error'));assert r['checks'] and all(c['passed'] for c in r['checks'])
copy_files=['docs/UNIFIED_CLIENT.md','tools/ornithe/dependencies.json' if target=='1.8.9' else 'docs/evidence/depth-polish/optimizer-dependencies-263.json']
for rel in copy_files:shutil.copy2(root/rel,out/pathlib.Path(rel).name)
with zipfile.ZipFile(out/('native-evidence-'+target+'.zip'),'w',zipfile.ZIP_DEFLATED) as z:
 for p in source.rglob('*'):
  if p.is_file() and p.suffix in ['.json','.png','.xml'] and 'build/libs' not in p.as_posix():z.write(p,p.relative_to(source))
subprocess.run(['git','archive','--format=zip','--output='+str((out/'complete-source.zip').resolve()),'HEAD'],cwd=root,check=True)
loader='Ornithe Gen2 1.8.9 + OSL 0.22.2 (Fabric Loader 0.19.5)' if target=='1.8.9' else 'Fabric Loader 0.19.5 + Fabric API 0.161.0+26.3'
readme=f'''TFL Client {meta['version']} — candidata de pruebas para Minecraft {target}

Instala Java 25 y {loader}. Copia únicamente {jar.name} a mods/.
Usa una instancia de pruebas y copia tus ajustes si deseas probar la migración. Retira versiones anteriores de TFL/TRS de esa instancia.
Los JAR de 1.8.9 y 26.3 van en instancias distintas. Este JAR 1.8.9 usa Ornithe Gen2. Forge y NeoForge se han retirado.
Argentum/Legacy LWJGL3 y Sodium son dependencias opcionales separadas: enlaces y hashes exactos en el JSON incluido. No se incluyen sus binarios.
Paper Doll · Mini Player se configura en HUD y se mueve desde el HUD Editor. Diorama: M abre el mapa, arrastrar mueve la vista y la rueda hace zoom. En 1.8.9: C cambia la capa; clic derecho crea un waypoint; clic en un waypoint lo edita. En 26.3 están disponibles los controles nativos de Diorama.
El fondo permite Calidad, Equilibrado, Rendimiento, Estático y Desactivado. La etiqueta inferior muestra el estado real; no altera tu preferencia guardada.

Lee UNIFIED_CLIENT.md para las pruebas, métricas y limitaciones. Las ejecuciones en llvmpipe no certifican rendimiento de GPU física. Iris no pudo validarse en este backend.
Para clips MP4, instala FFmpeg y ffprobe en PATH; no se descargan ni se activa un servicio externo. Las exportaciones son copias.
La evidencia nativa se incluye por separado; complete-source.zip corresponde al commit de esta publicación.
'''
(out/'INSTALACION.txt').write_text(readme)
manifest={'version':meta['version'],'minecraft':target,'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip(),'nativeReports':[str(p.relative_to(source)) for p in reports],'experimental':True,'physicalGpuCertified':False}
(out/'build.json').write_text(json.dumps(manifest,indent=2)+'\n')
files=sorted(p for p in out.iterdir() if p.is_file() and p.name!='SHA256SUMS.txt')
(out/'SHA256SUMS.txt').write_text(''.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name+'\n' for p in files))
print('Packaged verified '+target+': '+jar.name)
