#!/usr/bin/env python3
"""Stage the official player and public playlists locally; never contact a TV."""
import argparse
import datetime
import hashlib
import json
import re
from pathlib import Path
import urllib.request
from iptv_catalog import (DOMESTIC, OVERSEAS_IDS, OVERRIDE_CATEGORIES, category_hint,
                          chinese_name)
from iptv_regions import region_for, REGION_ORDER
from iptv_order import rank_entries, channel_key

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / '.local/iptv'
APK_URL = 'https://github.com/yaoxieyoulei/mytv-android/releases/download/tv2.2.7/mytv-android-tv-2.2.7-all-sdk21.apk'
SOURCES = [('countries/cn', '中国大陆'), ('countries/hk', '香港'),
           ('countries/tw', '台湾'), ('languages/zho', '中文'), ('index', '全球')]

# Prefer a specific subject over broad labels such as General or Entertainment.
# One primary category avoids turning every tag combination into another group.
CATEGORIES = [
    ('Kids', '少儿'), ('Animation', '动画'), ('Sports', '体育'),
    ('News', '新闻'), ('Movies', '电影'), ('Series', '电视剧'),
    ('Documentary', '纪录片'), ('Music', '音乐'), ('Cooking', '美食'),
    ('Travel', '旅游'), ('Auto', '汽车'), ('Science', '科普'),
    ('Education', '教育'), ('Business', '财经'), ('Weather', '天气'),
    ('Religious', '宗教'), ('Legislative', '议会／政务'), ('Shop', '购物'),
    ('Outdoor', '户外'), ('Relax', '放松／风景'), ('Comedy', '喜剧'),
    ('Culture', '文化'), ('Lifestyle', '生活'), ('Family', '家庭'),
    ('Classic', '经典'), ('Interactive', '互动'),
    ('Entertainment', '娱乐'), ('Public', '公共频道'), ('General', '综合'),
]
GROUP_ATTRIBUTE = re.compile(r'group-title="([^"]*)"')
METADATA = {}
AUDIT = []


def channel_id(info):
    match = re.search(r'tvg-id="([^"]*)"', info)
    return match.group(1).split('@')[0] if match else ''


def split_info(info):
    match = re.fullmatch(r'(#EXTINF:(?:[^",]|"[^"]*")*),(.*)', info)
    if not match:
        raise ValueError('Invalid EXTINF metadata')
    return match.groups()


def localize_group(info):
    match = GROUP_ATTRIBUTE.search(info)
    tags = {tag.strip() for tag in match.group(1).split(';')} if match else set()
    key = channel_id(info)
    hint, _ = category_hint(key, METADATA.get(key, {}))
    if key in OVERRIDE_CATEGORIES or not any(tag in tags or name in tags for tag, name in CATEGORIES):
        if hint:
            tags = {hint.title()}
    category = next((name for tag, name in CATEGORIES if tag in tags or name in tags), '其他／待分类')
    attribute = f'group-title="{category}"'
    if match:
        return info[:match.start()] + attribute + info[match.end():]
    metadata, name = split_info(info)
    return metadata + ' ' + attribute + ',' + name


def localize_entry(info):
    key = channel_id(info)
    metadata = METADATA.get(key, {})
    translated, name_reason = chinese_name(key, metadata)
    old_group = GROUP_ATTRIBUTE.search(info)
    info = localize_group(info)
    header, old_name = split_info(info)
    new_name = old_name
    if translated:
        # Keep quality and availability notes while replacing only the station name.
        suffix = re.search(r'(?:\s+(?:HD|SD))?(?:\s+\(\d{2,4}[pi]\))?(?:\s+\[[^\]]*\])*$', old_name).group()
        base = old_name[:len(old_name) - len(suffix)] if suffix else old_name
        if key not in OVERSEAS_IDS and (metadata.get('country') in DOMESTIC or key.endswith(('.cn', '.hk', '.tw', '.mo'))):
            new_name = translated + suffix
        elif translated not in base:
            new_name = base + '（' + translated + '）' + suffix
        if new_name != old_name and not re.search(r'\btvg-name="', header):
            # MyTV uses tvg-name as its EPG lookup key; preserve the pre-rename key.
            header += ' tvg-name="' + old_name.replace('"', '&quot;') + '"'
    category = GROUP_ATTRIBUTE.search(info).group(1)
    AUDIT.append({'id': key, 'original_name': old_name, 'display_name': new_name,
                  'original_group': old_group.group(1) if old_group else '',
                  'group': category, 'name_basis': name_reason,
                  'category_basis': category_hint(key, metadata)[1],
                  'website': metadata.get('website')})
    return header + ',' + new_name


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
                result.append((localize_entry(pending), line))
            pending = None
    return result, skipped


def main():
    global METADATA
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--offline', action='store_true',
                        help='Regenerate from existing local snapshots without downloading')
    args = parser.parse_args()
    OUT.mkdir(parents=True, exist_ok=True)
    previous_names = {}
    previous = OUT / 'all-compatible.m3u'
    if previous.exists():
        pending = None
        for line in previous.read_text().splitlines():
            if line.startswith('#EXTINF:'):
                pending = split_info(line)[1]
            elif line and not line.startswith('#') and pending is not None:
                previous_names[line] = pending
                pending = None
    apk = OUT / 'MyTV-2.2.7.apk'
    if not apk.exists():
        if args.offline:
            raise FileNotFoundError(apk)
        data = download(APK_URL)
        if not data.startswith(b'PK'):
            raise ValueError('APK is not a ZIP archive')
        apk.write_bytes(data)
    catalog_file = OUT / 'channel-metadata.json'
    if not args.offline:
        catalog_file.write_bytes(download('https://iptv-org.github.io/api/channels.json'))
    if catalog_file.exists():
        METADATA = {channel['id']: channel for channel in json.loads(catalog_file.read_text())}
    AUDIT.clear()
    chinese_file = OUT / 'languages-zho.m3u'
    if not args.offline:
        chinese_file.write_bytes(download('https://iptv-org.github.io/iptv/languages/zho.m3u'))
    chinese_ids = {channel_id(line) for line in chinese_file.read_text().splitlines()
                   if line.startswith('#EXTINF:')} if chinese_file.exists() else set()
    manifest = {'prepared_at': datetime.datetime.now(datetime.timezone.utc).isoformat(),
                'grouping': 'zh-reviewed-v2',
                'ordering': 'editorial-chinese-first-v1',
                'channel_metadata_sha256': hashlib.sha256(catalog_file.read_bytes()).hexdigest() if catalog_file.exists() else None,
                'apk': {'file': apk.name, 'url': APK_URL,
                        'sha256': hashlib.sha256(apk.read_bytes()).hexdigest()}, 'sources': []}
    combined, seen = [], set()
    for key, name in SOURCES:
        url = 'https://iptv-org.github.io/iptv/' + key + '.m3u'
        stem = key.replace('/', '-')
        snapshot = OUT / (stem + '.m3u')
        data = snapshot.read_bytes() if args.offline or key == 'languages/zho' else download(url)
        text = data.decode('utf-8-sig')
        if not text.startswith('#EXTM3U'):
            raise ValueError('Invalid playlist: ' + url)
        if not args.offline:
            snapshot.write_bytes(data)
        entries, skipped = compatible(text)
        entries = rank_entries(entries, METADATA, chinese_ids)
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
    combined = rank_entries(combined, METADATA, chinese_ids)
    (OUT / file).write_text('#EXTM3U\n' + ''.join(i + '\n' + u + '\n' for i, u in combined))
    manifest['combined'] = {'name': '全部公开频道（中文优先）', 'file': file, 'entries': len(combined)}
    regional, region_counts = [], {}
    for info, stream in combined:
        key = channel_id(info)
        region = region_for(key, METADATA.get(key, {}))
        regional.append((region, GROUP_ATTRIBUTE.sub(f'group-title="{region}"', info), stream))
        region_counts[region] = region_counts.get(region, 0) + 1
    regional.sort(key=lambda row: (REGION_ORDER[row[0]], channel_key(row[1], METADATA, chinese_ids)))
    file = 'all-regions.m3u'
    (OUT / file).write_text('#EXTM3U\n' + ''.join(info + '\n' + stream + '\n' for _, info, stream in regional))
    manifest['views'] = [{'name': '全部公开频道（按地区）', 'file': file,
                          'entries': len(regional), 'groups': region_counts}]
    (OUT / 'sources.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
    (OUT / 'channel-audit.json').write_text(json.dumps(AUDIT, ensure_ascii=False, indent=2) + '\n')
    migrations = {}
    for row in AUDIT:
        if row['display_name'] != row['original_name']:
            migrations.setdefault(row['original_name'], set()).add(row['display_name'])
    for info, stream in combined:
        name = split_info(info)[1]
        if stream in previous_names and previous_names[stream] != name:
            migrations.setdefault(previous_names[stream], set()).add(name)
    old_migrations = OUT / 'name-migrations.json'
    if old_migrations.exists():
        # Retain historic aliases so repeated preparation cannot lose a pending migration.
        for name, values in json.loads(old_migrations.read_text()).items():
            targets = {target for value in values for target in migrations.get(value, {value})}
            migrations.setdefault(name, set()).update(targets)
    (OUT / 'name-migrations.json').write_text(json.dumps(
        {name: sorted(values) for name, values in migrations.items()}, ensure_ascii=False, indent=2) + '\n')
    print(f'Ready: {len(combined)} distinct compatible stream URLs in {OUT}')


if __name__ == '__main__':
    main()
