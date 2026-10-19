#!/usr/bin/env python3
"""Stage the official player and public playlists locally; never contact a TV."""
import datetime
import hashlib
import json
from pathlib import Path
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / '.local/iptv'
APK_URL = 'https://github.com/yaoxieyoulei/mytv-android/releases/download/tv2.2.7/mytv-android-tv-2.2.7-all-sdk21.apk'
SOURCES = [('countries/cn', '中国大陆'), ('countries/hk', '香港'),
           ('countries/tw', '台湾'), ('languages/zho', '中文'), ('index', '全球')]


def download(url):
    with urllib.request.urlopen(url, timeout=60) as response:
        return response.read()


def compatible(text):
    """MyTV expects the URL immediately after EXTINF and cannot apply per-stream headers."""
    result, pending, options, skipped = [], None, False, 0
    for line in text.splitlines():
        line = line.strip()
        if line.startswith('#EXTINF:'):
            pending, options = line, False
        elif line.startswith(('#EXTVLCOPT:', '#KODIPROP:')):
            options = True
        elif line and not line.startswith('#') and pending:
            if options or not line.startswith(('https://', 'http://', 'rtsp://', 'rtmp://')):
                skipped += 1
            else:
                result.append((pending, line))
            pending = None
    return result, skipped


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    apk = OUT / 'MyTV-2.2.7.apk'
    if not apk.exists():
        data = download(APK_URL)
        if not data.startswith(b'PK'):
            raise ValueError('APK is not a ZIP archive')
        apk.write_bytes(data)
    manifest = {'prepared_at': datetime.datetime.now(datetime.timezone.utc).isoformat(),
                'apk': {'file': apk.name, 'url': APK_URL,
                        'sha256': hashlib.sha256(apk.read_bytes()).hexdigest()}, 'sources': []}
    combined, seen = [], set()
    for key, name in SOURCES:
        url = 'https://iptv-org.github.io/iptv/' + key + '.m3u'
        data = download(url)
        text = data.decode('utf-8-sig')
        if not text.startswith('#EXTM3U'):
            raise ValueError('Invalid playlist: ' + url)
        stem = key.replace('/', '-')
        (OUT / (stem + '.m3u')).write_bytes(data)
        entries, skipped = compatible(text)
        output = '#EXTM3U\n' + ''.join(info + '\n' + stream + '\n' for info, stream in entries)
        file = stem + '-compatible.m3u'
        (OUT / file).write_text(output)
        manifest['sources'].append({'name': name, 'url': url, 'file': file,
                                    'entries': len(entries), 'original_entries': text.count('#EXTINF:'),
                                    'unsupported_entries': skipped, 'sha256': hashlib.sha256(data).hexdigest()})
        for info, stream in entries:
            if stream not in seen:
                seen.add(stream)
                combined.append((info, stream))
    file = 'all-compatible.m3u'
    (OUT / file).write_text('#EXTM3U\n' + ''.join(i + '\n' + u + '\n' for i, u in combined))
    manifest['combined'] = {'name': '全部公开频道（中文优先）', 'file': file, 'entries': len(combined)}
    (OUT / 'sources.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
    print(f'Ready: {len(combined)} distinct compatible stream URLs in {OUT}')


if __name__ == '__main__':
    main()
