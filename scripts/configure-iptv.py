#!/usr/bin/env python3
"""Import staged sources over MyTV's API. Launch only with an explicit --launch."""
import argparse
import http.client
import json
import os
from pathlib import Path
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'top.yogiczy.mytv.tv'
REMOTE = '/sdcard/Android/data/' + PACKAGE + '/files/lightframe'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('serial')
    parser.add_argument('--launch', action='store_true', help='Open IPTV on the TV to run its configuration API')
    parser.add_argument('--keep-current', action='store_true', help='Import sources without changing the currently selected source')
    args = parser.parse_args()
    manifest = json.loads((ROOT / '.local/iptv/sources.json').read_text())
    adb = [os.environ.get('ADB', 'adb'), '-P', os.environ.get('ADB_SERVER_PORT', '5037'), '-s', args.serial]

    def run(*params):
        return subprocess.check_output(adb + list(params), text=True).strip()

    # App-specific external files persist across cache clearing and need no broad storage grant.
    run('shell', 'mkdir', '-p', REMOTE)
    sources = [manifest['combined']] + manifest['sources'] + manifest.get('views', [])
    for item in sources:
        run('push', str(ROOT / '.local/iptv' / item['file']), REMOTE + '/' + item['file'])
    port = run('forward', 'tcp:0', 'tcp:10481')

    def request(path, payload=None):
        body = None if payload is None else json.dumps(payload, ensure_ascii=False).encode()
        connection = http.client.HTTPConnection('127.0.0.1', int(port), timeout=5)
        try:
            # AndroidAsync treats Content-Type on a bodyless GET as a JSON body.
            connection.request('GET' if body is None else 'POST', path, body=body,
                               headers={} if body is None else {'Content-Type': 'application/json'})
            response = connection.getresponse()
            data = response.read()
            if response.status != 200:
                raise RuntimeError(f'MyTV API returned HTTP {response.status}')
            return data
        finally:
            connection.close()

    try:
        if args.launch:
            component = run('shell', 'cmd', 'package', 'resolve-activity', '--brief', PACKAGE).splitlines()[-1]
            if '/' not in component:
                raise RuntimeError('IPTV launch activity not found')
            run('shell', 'am', 'start', '-n', component)
        for attempt in range(20 if args.launch else 1):
            try:
                configs = json.loads(request('/api/configs'))
                break
            except (OSError, ValueError):
                if not args.launch or attempt == 19:
                    raise RuntimeError('MyTV configuration API is unavailable. Open IPTV yourself, or rerun with --launch.')
                time.sleep(1)
        existing = configs['iptvSourceList']['value']
        incoming = [{'name': 'Lightframe · ' + item['name'],
                     'url': REMOTE + '/' + item['file'], 'isLocal': True} for item in sources]
        incoming_urls = {item['url'] for item in incoming}
        configs['iptvSourceList']['value'] = [item for item in existing if item['url'] not in incoming_urls] + incoming
        if not args.keep_current:
            configs['iptvSourceCurrent'] = incoming[0]
        expected_current = configs['iptvSourceCurrent']
        migrations_file = ROOT / '.local/iptv/name-migrations.json'
        if migrations_file.exists():
            migrations = json.loads(migrations_file.read_text())
            # MyTV stores favorites by display name, not tvg-id.
            configs['iptvChannelFavoriteList'] = list(dict.fromkeys(
                target for name in configs['iptvChannelFavoriteList']
                for target in migrations.get(name, [name])))
        request('/api/configs', configs)
        actual = json.loads(request('/api/configs'))
        if actual['iptvSourceCurrent'] != expected_current or not incoming_urls.issubset(
                {item['url'] for item in actual['iptvSourceList']['value']}):
            raise RuntimeError('Source configuration verification failed')
        print(f'Imported {len(incoming)} sources; selected {expected_current["name"]}. Playback is not verified.')
    finally:
        run('forward', '--remove', 'tcp:' + port)


if __name__ == '__main__':
    main()
