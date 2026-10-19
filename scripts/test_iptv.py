#!/usr/bin/env python3
"""Offline regression checks for playlist metadata enrichment."""
import unittest
import json
import tempfile
import threading
from http.server import BaseHTTPRequestHandler, HTTPServer
from unittest.mock import patch
import importlib.util
from pathlib import Path
spec = importlib.util.spec_from_file_location("prepare_iptv", Path(__file__).with_name("prepare-iptv.py"))
prep = importlib.util.module_from_spec(spec)
spec.loader.exec_module(prep)
from iptv_catalog import chinese_name
from iptv_regions import region_for, REGIONS, UNKNOWN_REGION
from iptv_order import rank_entries


class PlaylistMetadataTests(unittest.TestCase):
    def setUp(self):
        self.previous = prep.METADATA
        prep.METADATA = {
            'AnhuiTV.cn': {'name': 'Anhui TV', 'country': 'CN', 'alt_names': ['安徽卫视']},
            'AnimalPlanet.cz': {'name': 'Animal Planet', 'country': 'CZ', 'alt_names': []},
        }

    def tearDown(self):
        prep.METADATA = self.previous

    def test_native_name_keeps_epg_and_quality(self):
        row = '#EXTINF:-1 tvg-id="AnhuiTV.cn@SD" group-title="Undefined",Anhui TV (720p)'
        result = prep.localize_entry(row)
        self.assertIn('group-title="综合"', result)
        self.assertIn('tvg-id="AnhuiTV.cn@SD"', result)
        self.assertIn('tvg-name="Anhui TV (720p)"', result)
        self.assertTrue(result.endswith(',安徽卫视 (720p)'))
        self.assertEqual(prep.localize_entry(result), result)

    def test_foreign_name_is_appended(self):
        row = '#EXTINF:-1 tvg-id="AnimalPlanet.cz@SD" group-title="Undefined",Animal Planet (720p)'
        result = prep.localize_entry(row)
        self.assertTrue(result.endswith(',Animal Planet（动物星球频道） (720p)'))
        self.assertIn('group-title="纪录片"', result)

    def test_no_invented_alias_or_genre(self):
        row = '#EXTINF:-1 tvg-id="Unknown.xx@SD" group-title="Undefined",Unknown TV'
        result = prep.localize_entry(row)
        self.assertTrue(result.endswith(',Unknown TV'))
        self.assertIn('group-title="其他／待分类"', result)
        self.assertIsNone(chinese_name('Unreviewed.jp', {
            'country': 'JP', 'name': 'NHK General TV', 'alt_names': ['NHK総合テレビジョン']})[0])

    def test_existing_epg_and_quoted_commas(self):
        row = '#EXTINF:-1 tvg-id="AnhuiTV.cn@SD" tvg-name="original EPG" tvg-logo="https://test/a,b.png" group-title="Undefined",Anhui TV'
        result = prep.localize_entry(row)
        self.assertIn('tvg-name="original EPG"', result)
        self.assertIn('tvg-logo="https://test/a,b.png"', result)
        self.assertTrue(result.endswith(',安徽卫视'))

    def test_compound_groups_and_header_requirements(self):
        result = prep.localize_group('#EXTINF:-1 group-title="Animation;Kids",Example')
        self.assertIn('group-title="少儿"', result)
        entries, skipped = prep.compatible('#EXTM3U\n#EXTINF:-1 group-title="News",Good\nhttps://test/live.m3u8\n#EXTINF:-1,Headers\n#EXTVLCOPT:http-referrer=x\nhttps://test/private.m3u8\n')
        self.assertEqual(skipped, 1)
        self.assertEqual(len(entries), 1)
        self.assertEqual(entries[0][1], 'https://test/live.m3u8')


class RegionTests(unittest.TestCase):
    def test_regions_follow_station_country_not_language(self):
        self.assertEqual(region_for('CCTV1.cn', {'country': 'CN'}), '中国大陆')
        self.assertEqual(region_for('Jade.hk', {'country': 'HK'}), '香港')
        self.assertEqual(region_for('VoATVChina.cn', {'country': 'CN'}), '北美洲')
        self.assertEqual(region_for('Unknown.us', {}), UNKNOWN_REGION)
        self.assertEqual(region_for('BBCNews.uk', {'country': 'UK'}), '欧洲')
        codes = [code for _, values in REGIONS for code in values.split()]
        self.assertEqual(len(codes), len(set(codes)))


class OrderingTests(unittest.TestCase):
    def test_chinese_priority_group_boundaries_and_alternatives(self):
        def entry(key, name, group='新闻'):
            return (f'#EXTINF:-1 tvg-id="{key}@HD" group-title="{group}",{name}', 'https://test/' + name)
        rows = [entry('Unknown.us', 'Unlisted'), entry('BBCNews.uk', 'BBC 720'),
                entry('CCTV13.cn', 'CCTV 13'), entry('BBCNews.uk', 'BBC 1080'),
                entry('Channel8.sg', 'Channel 8'), entry('CCTV1.cn', 'CCTV 1', '综合')]
        metadata = {'BBCNews.uk': {'name': 'BBC News', 'country': 'UK'}}
        result = rank_entries(rows, metadata, {'Channel8.sg'})
        self.assertEqual([row[0].rsplit(',', 1)[-1] for row in result],
                         ['CCTV 13', 'Channel 8', 'BBC 720', 'BBC 1080', 'Unlisted', 'CCTV 1'])
        self.assertCountEqual(rows, result)
        self.assertEqual(rank_entries(result, metadata, {'Channel8.sg'}), result)


class ImportMigrationTests(unittest.TestCase):
    def test_import_migrates_favorites_without_launching(self):
        spec = importlib.util.spec_from_file_location('configure_iptv', Path(__file__).with_name('configure-iptv.py'))
        cfg = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(cfg)
        state = {'iptvSourceList': {'value': []}, 'iptvSourceCurrent': {},
                 'iptvChannelFavoriteList': ['Anhui TV', 'Existing favorite'],
                 'videoPlayerUserAgent': 'preserve'}
        headers = []

        class Handler(BaseHTTPRequestHandler):
            def log_message(self, *args):
                pass

            def do_GET(self):
                headers.append(self.headers.get('Content-Type'))
                self.send_response(200)
                self.end_headers()
                self.wfile.write(json.dumps(state).encode())

            def do_POST(self):
                value = json.loads(self.rfile.read(int(self.headers['Content-Length'])))
                state.clear()
                state.update(value)
                self.send_response(200)
                self.end_headers()
                self.wfile.write(b'success')

        server = HTTPServer(('127.0.0.1', 0), Handler)
        threading.Thread(target=server.serve_forever, daemon=True).start()
        calls = []

        def adb(command, text):
            calls.append(command)
            return str(server.server_port) if 'tcp:0' in command else ''

        try:
            with tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                local = root / '.local/iptv'
                local.mkdir(parents=True)
                (local / 'all.m3u').write_text('#EXTM3U\n')
                (local / 'sources.json').write_text(json.dumps({
                    'combined': {'name': 'All', 'file': 'all.m3u'}, 'sources': []}))
                (local / 'name-migrations.json').write_text(json.dumps({'Anhui TV': ['安徽卫视']}))
                with patch.object(cfg, 'ROOT', root), patch.object(cfg.subprocess, 'check_output', side_effect=adb), patch('sys.argv', ['configure-iptv.py', 'test']):
                    cfg.main()
                    cfg.main()
                    current = {'name': 'Existing source', 'url': 'https://example.test/source.m3u', 'isLocal': False}
                    state['iptvSourceCurrent'] = current.copy()
                    with patch('sys.argv', ['configure-iptv.py', 'test', '--keep-current']):
                        cfg.main()
                    self.assertEqual(state['iptvSourceCurrent'], current)
            self.assertEqual(state['iptvChannelFavoriteList'], ['安徽卫视', 'Existing favorite'])
            self.assertEqual(state['videoPlayerUserAgent'], 'preserve')
            self.assertEqual(len(state['iptvSourceList']['value']), 1)
            self.assertTrue(all(value is None for value in headers))
            self.assertFalse(any('start' in command for command in calls))
        finally:
            server.shutdown()
            server.server_close()


if __name__ == '__main__':
    unittest.main()
