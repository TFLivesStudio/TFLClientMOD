#!/usr/bin/env python3
"""Verify all native rows against identical production/fixture trees, across runs.

Allows repeating a failed driver row without repeating successful native rows.
Filtered/skipped rows never count. Does not waive a failed required assertion.
"""
import argparse
import json
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[2]
REPOSITORY = 'TFLivesStudio/TFLClientMOD'
TREES = ('vendor', 'distribution', 'tools/integrations', 'tools/ornithe')
COMBOS = ('1.8.9, baseline', '1.8.9, lwjgl3', '1.8.9, argentum',
          '26.3, baseline', '26.3, sodium', '26.3, iris')


def successful_rows(jobs):
    rows = set()
    for job in jobs:
        if job.get('conclusion') != 'success':
            continue
        for combo in COMBOS:
            prefix = 'Installed ' + combo.split(',')[0] + ' client'
            if (job['name'].startswith('native (') and combo in job['name']
                    and any(s['name'].startswith(prefix) and s.get('conclusion') == 'success'
                            for s in job.get('steps', []))):
                rows.add(combo)
    return rows


def standard_legacy_variant(log):
    # This is the materialized command in the runner's selector step, not a
    # user-facing success claim. Diagnostic nofog must never qualify for delivery.
    variants = re.findall(r"if \[ '([^']+)' = nofog \] && \[ '1\.8\.9' = '1\.8\.9' \]; then echo variant=", log)
    return bool(variants) and all(value == 'standard' for value in variants)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--runs', required=True, help='Comma-separated Actions run IDs')
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    runs = args.runs.split(',')
    if not runs or not all(run.isdigit() for run in runs):
        raise ValueError('Explicit numeric native acceptance run IDs are required')
    def git(*parts):
        return subprocess.check_output(['git', *parts], cwd=ROOT, text=True).strip()
    def api(path):
        return json.loads(subprocess.check_output(['gh', 'api', 'repos/' + REPOSITORY + '/' + path]))
    accepted, common, evidence = set(), False, []
    for run in dict.fromkeys(runs):
        data = api('actions/runs/' + run)
        sha = data['head_sha']
        for tree in TREES:
            if git('rev-parse', sha + ':' + tree) != git('rev-parse', 'HEAD:' + tree):
                raise ValueError(f'Run {run} has different production/fixture content: {tree}')
        jobs = api(f'actions/runs/{run}/jobs?per_page=100')['jobs']
        rows = successful_rows(jobs)
        for job in jobs:
            matching = rows.intersection(successful_rows([job]))
            if any(combo.startswith('1.8.9,') for combo in matching):
                log = subprocess.check_output(['gh', 'api', f'repos/{REPOSITORY}/actions/jobs/{job["id"]}/logs'], text=True)
                if not standard_legacy_variant(log):
                    raise ValueError(f'Run {run}: diagnostic/unknown fog variant cannot certify {matching}')
        accepted.update(rows)
        common |= any(j['name'] == 'common' and j.get('conclusion') == 'success' for j in jobs)
        evidence.append(dict(run=run, sourceCommit=sha, successfulRows=sorted(rows)))
    if not common or accepted != set(COMBOS):
        raise ValueError(f'Incomplete acceptance: common={common}; missing={sorted(set(COMBOS)-accepted)}')
    result = dict(sourceCommit=git('rev-parse', 'HEAD'), verifiedTrees=list(TREES),
                  commonPassed=common, rows=sorted(accepted), evidence=evidence,
                  physicalGpuCertified=False)
    text = json.dumps(result, indent=2) + '\n'
    if args.output:
        args.output.write_text(text)
    print(text)


if __name__ == '__main__':
    main()
