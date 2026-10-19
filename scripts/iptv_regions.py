"""A compact browsing geography based on the catalog's station country.

These are navigation buckets, not broadcast availability or language labels.
Transcontinental countries use one documented bucket: Russia in Europe,
Turkey in Middle East/North Africa; Caucasus countries alongside Central Asia.
"""
REGIONS = [
    ('中国大陆', 'CN'), ('香港', 'HK'), ('澳门', 'MO'), ('台湾', 'TW'),
    ('日本', 'JP'), ('韩国', 'KR'),
    ('东南亚', 'BN KH ID LA MY MM PH SG TH TL VN'),
    ('南亚', 'AF BD BT IN MV NP PK LK'),
    ('中亚及高加索', 'AM AZ GE KZ KG TJ TM UZ'),
    ('中东及北非', 'DZ BH EG IR IQ IL JO KW LB LY MA OM PS QA SA SD SY TN TR AE YE EH'),
    ('欧洲', 'AL AD AT BY BE BA BG HR CY CZ DK EE FO FI FR DE GI GR GG VA HU IS IE IM IT JE XK LV LI LT LU MT MD MC ME NL MK NO PL PT RO RU SM RS SK SI ES SJ SE CH UA UK GB AX'),
    ('北美洲', 'US CA GL BM PM'),
    ('拉丁美洲及加勒比', 'AI AG AR AW BS BB BZ BO BQ BR KY CL CO CR CU CW DM DO EC SV FK GF GD GP GT GY HT HN JM MQ MX MS NI PA PY PE PR BL KN LC MF VC SX SR TT TC UY VE VG VI GS'),
    ('撒哈拉以南非洲', 'AO BJ BW BF BI CV CM CF TD KM CG CD CI DJ GQ ER SZ ET GA GM GH GN GW KE LS LR MG MW ML MR MU YT MZ NA NE NG RE RW SH ST SN SC SL SO ZA SS TZ TG UG ZM ZW'),
    ('大洋洲', 'AS AU CK FJ PF GU KI MH FM NR NC NZ NU NF MP PW PG PN WS SB TK TO TV VU WF'),
    ('其他亚洲', 'KP MN'),
]
COUNTRY_REGION = {code: name for name, codes in REGIONS for code in codes.split()}
REGION_ORDER = {name: i for i, (name, _) in enumerate(REGIONS)}
UNKNOWN_REGION = '地区待核实'
REGION_ORDER[UNKNOWN_REGION] = len(REGIONS)
COUNTRY_CORRECTIONS = {'VoATVChina.cn': 'US'}


def region_for(channel_id, metadata):
    country = COUNTRY_CORRECTIONS.get(channel_id, metadata.get('country'))
    return COUNTRY_REGION.get(country, UNKNOWN_REGION)
