#!/usr/bin/env python3
"""Run a separately packaged, Core-free Minecraft guest against a loopback relay fixture.

Prepare the Loom guest command before starting the host. The host probe writes an
invitation, waits for this guest's real join, stops sharing and verifies disconnect.
This driver never starts a public relay, changes authentication or modifies OS DNS.
"""
import argparse
import json
import os
from pathlib import Path
import subprocess
import time


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--fixture', type=Path, required=True)
    parser.add_argument('--launch', type=Path, required=True)
    parser.add_argument('--timeout', type=int, default=180)
    args = parser.parse_args()
    fixture = args.fixture.resolve()
    fixture.mkdir(parents=True, exist_ok=True)
    launch = json.loads(args.launch.read_text())
    run = Path(launch['workingDirectory'])
    run.mkdir(parents=True, exist_ok=True)
    lock = fixture / 'guest-driver.lock'
    # Exclusive fixture ownership prevents stale reports/concurrent log writers.
    with lock.open('x') as handle:
        handle.write(str(os.getpid()))
    guest = None
    try:
        for name in ('guest-ready.json', 'guest-result.json'):
            (fixture / name).unlink(missing_ok=True)
            (run / name).unlink(missing_ok=True)
        deadline = time.monotonic() + args.timeout
        while not (fixture / 'invitation.json').exists():
            if time.monotonic() >= deadline:
                raise TimeoutError('Host did not publish a local invitation')
            time.sleep(.1)
        address = json.loads((fixture / 'invitation.json').read_text())['address']
        if not address.endswith('.share.test'):
            raise ValueError('This acceptance fixture only supports local .share.test leases')
        (fixture / 'hosts').write_text(f'127.0.0.1 localhost {address}\n::1 localhost\n')
        command = [arg.replace('placeholder.share.test', address) for arg in launch['command']]
        with (fixture / 'guest.log').open('w') as output:
            guest = subprocess.Popen(command, cwd=run, stdout=output, stderr=subprocess.STDOUT)
            while time.monotonic() < deadline:
                for name in ('guest-ready.json', 'guest-result.json'):
                    source, target = run / name, fixture / name
                    if source.exists() and not target.exists():
                        target.write_bytes(source.read_bytes())
                result = fixture / 'guest-result.json'
                if result.exists():
                    data = json.loads(result.read_text())
                    if data['status'] != 'passed' or data['coreInstalled'] is not False:
                        raise AssertionError(data)
                    print(json.dumps(data, indent=2))
                    return
                if guest.poll() is not None:
                    raise RuntimeError('Minecraft guest exited without a result; inspect guest.log')
                time.sleep(.1)
            raise TimeoutError('Native guest did not complete join and revocation')
    finally:
        if guest is not None and guest.poll() is None:
            guest.terminate()
            try:
                guest.wait(timeout=10)
            except subprocess.TimeoutExpired:
                guest.kill()
                guest.wait()
        lock.unlink(missing_ok=True)


if __name__ == '__main__':
    main()
