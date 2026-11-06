#!/usr/bin/env python3
"""Prepare the exact external, compile-only Lucerna API; never embed it in Core."""
import argparse
import hashlib
from pathlib import Path
import shutil
import subprocess
import tempfile

SHA = 'dffa9657cddce76c18bff9221ebe88c0ce79c7b19d36ad25e66b9dc9b38182c1'
ROOT = Path(__file__).resolve().parents[2]

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar',type=Path,help='Use an already downloaded remapped Lucerna 0.1.5-tfl.1 JAR')
    args=parser.parse_args()
    target=ROOT/'vendor/trs-client/ornithe/.gradle/lucerna-api/lucerna-0.1.5-tfl.1.jar'
    if target.is_file() and hashlib.sha256(target.read_bytes()).hexdigest()==SHA:return
    with tempfile.TemporaryDirectory(prefix='tfl-lucerna-api-') as temp:
        source=args.jar
        if source is None:
            subprocess.run(['gh','run','download','42134523929','--repo','jonathanthxd/lucerna',
                            '--name','lucerna','--dir',temp],check=True)
            source=Path(temp)/'lucerna-0.1.5-tfl.1.jar'
        if hashlib.sha256(source.read_bytes()).hexdigest()!=SHA:
            raise ValueError('Lucerna API artifact checksum mismatch; use the pinned remapped artifact')
        target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target)
    print('Prepared external Lucerna API v1, upstream c9a4b8e2; SHA256 verified; compile-only')

if __name__=='__main__':main()
