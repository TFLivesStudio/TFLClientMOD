#!/usr/bin/env python3
"""Reproducible source evidence, deliberately separate from runtime certification."""
import argparse
import hashlib
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "vendor/trs-client"


def evidence_files(folder):
    return [(p, p.read_text(encoding="utf-8")) for p in sorted(folder.rglob("*.java"))
            if "/trsclient/dev/" not in str(p)]


def references(files, field):
    pattern = re.compile(r"\." + re.escape(field) + r"\b")
    return [{"file": str(p.relative_to(ROOT)), "line": n}
            for p, text in files for n, line in enumerate(text.splitlines(), 1)
            if pattern.search(line)]


def generate(runtime=None):
    inventory_path = ROOT / "distribution/modules.json"
    inventory = json.loads(inventory_path.read_text())
    registry = "\n".join(p.read_text() for p in sorted(
        (BASE / "common/src/main/java/dev/theredstonee/trsclient/core/module").glob("*Modules.java")))
    fields = dict((module_id, field) for field, module_id in re.findall(
        r'(\w+)\s*=\s*registry\.register\(new\s+(?:Hud)?Module\("([^"]+)"', registry))
    fields["miniPlayer"]="miniPlayer"
    files = {target: evidence_files(BASE / target / "src/main/java") for target in ("fabric", "ornithe")}
    common = evidence_files(BASE / "common/src/main/java")
    runtime_rows = {r["id"]: r for r in (runtime or {}).get("modules", [])}
    result = []
    for source in inventory["modules"]:
        module_id = source["id"]
        field = fields[module_id]
        unavailable = module_id in ("trsOnline", "social")
        limits = ["Source references include inactive Stonecutter branches; a reference is not proof that a target hook executes.",
                  "Settings interactions, key input, external mod combinations and restart persistence require runtime verification."]
        availability = "registered; runtime Module.available() must be checked"
        dependencies = ["native target hooks", "world/player for gameplay hooks" ]
        if source["hud"]:
            dependencies += ["HUD renderer and current HUD profile"]
        if unavailable:
            availability = "disabled service: unavailable in offline TFL distribution"
            dependencies += ["TRS backend or TRS Launcher; deliberately disabled"]
        if module_id == "builtinOptimizations":
            availability = "conditional: BundledMods.get().available(); no optimization binaries bundled by default"
            dependencies += ["optional version-compatible optimization mods"]
        fixed = module_id in ("titleScreen", "menuStyle")
        if fixed:
            availability = "fixed client infrastructure; always active, settings only; no normal user toggle"
        for key, note in {
            "oldAnimations": "Fabric 26.3 does not implement the legacy swing-while-using animation.",
            "hitboxes": "Hitbox color is unavailable on Fabric 1.21.9+; do not infer color behavior from toggling.",
            "colors": "Postprocessing is unavailable with the Vulkan renderer.",
            "shieldPosition": "Minecraft 1.8.9 has no vanilla shield; modern shield placement needs a modern target.",
            "emotes": "Six local animations only; no account synchronization or remote player delivery.",
            "ping": "Offline world cannot certify real server latency/jitter behavior.",
            "fastConnect": "Local-world harness cannot certify multiplayer connection behavior.",
            "borderlessFullscreen": "Requires window-manager and real display testing.",
        }.items():
            if module_id == key:
                limits.append(note)
        row = {"id": module_id, "category": source["category"], "hud": source["hud"],
               "defaultEnabled": source["defaultEnabled"], "settings": source["settings"],
               "availability": availability, "dependencies": dependencies,
               "activation": "always active; userToggleable=false" if fixed else "Module.setEnabled(true); runtime key/action hooks remain separate",
               "deactivation": "not applicable: fixed client infrastructure" if fixed else "Module.setEnabled(false); verify camera/input/renderer restoration in target runtime",
               "persistence": "Module.write/read via ModuleRegistry.capture/apply and ConfigStore; module state/settings plus HUD layout where applicable",
               "targetHooks": {"fabric26.3SourceCandidates": references(files["fabric"], field),
                               "ornithe1.8.9SourceCandidates": references(files["ornithe"], field)},
               "sharedHookCandidateFiles": sorted(set(r["file"] for r in references(common, field))),
               "results": {"sourceInspection": "registry entry and source reference candidates inspected",
                           "unitTests": "not executed by this audit; common test suite is independent evidence",
                           "nativeRuntime": runtime_rows.get(module_id, {"status": "not-run"}),
                           "fullyCertified": False},
               "limits": limits}
        result.append(row)
    return {"schemaVersion": 1, "moduleCount": len(result),
            "inventorySha256": hashlib.sha256(inventory_path.read_bytes()).hexdigest(),
            "evidencePolicy": "Source inspection, unit tests and native execution are different evidence. State toggling alone never certifies gameplay.",
            "nativeRun": {k: v for k, v in (runtime or {}).items() if k != "modules"} or {"status": "not-run"},
            "modules": result}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--runtime", type=Path, help="native module-certification JSON to attach as evidence")
    parser.add_argument("--output", type=Path, default=ROOT / "distribution/module-audit.json")
    args = parser.parse_args()
    report = generate(json.loads(args.runtime.read_text()) if args.runtime else None)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print(f"Wrote {report['moduleCount']} module audit rows to {args.output}")


if __name__ == "__main__":
    main()
