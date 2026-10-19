"""Editorial browsing priority, not audience measurement or live popularity.

Chinese channels use an explicit, reviewable order. Chinese-language membership
comes from the upstream zho playlist. Other recognized broadcaster brands follow;
unlisted stations remain available and sort naturally within their group.
"""
import re

CHINESE_PRIORITY = '''CCTV1.cn CCTV13.cn CCTV6.cn CCTV5.cn CCTV5Plus.cn CCTV16.cn
CCTV8.cn CCTV9.cn CCTV2.cn CCTV3.cn CCTV4Asia.cn CCTV4America.cn CCTV4Europe.cn
CCTV10.cn CCTV14.cn CCTV15.cn CCTV7.cn CCTV11.cn CCTV12.cn CCTV17.cn CCTV4K.cn CCTV8K.cn
HunanTV.cn ZhejiangSatelliteTV.cn JiangsuSatelliteTV.cn DragonTV.cn DragonTVInternational.cn
BeijingSatelliteTV.cn GuangdongSatelliteTV.cn ShenzhenSatelliteTV.cn ShandongTV.cn
ShandongSatelliteTV.cn AnhuiTV.cn TianjinTV.cn LiaoningTV.cn SichuanSatelliteTV.cn
HeilongjiangTV.cn HebeiTV.cn JiangxiTV.cn GuizhouTV.cn YunnanSatelliteTV.cn
PhoenixChineseChannel.hk PhoenixInfoNewsChannel.hk Jade.hk RTHKTV31.hk RTHKTV32.hk
HOYTV.hk HOYInfotainment.hk GoldenJade.hk HongKongInternationalBusinessChannel.hk
TVBSNews.tw SETNews.tw EBCNews.tw CTSNews.tw TTV.tw CTS.tw FTV.tw CTiVariety.tw
HakkaTV.tw IndigenousTV.tw TDMOuMun.mo TDMInformation.mo TDMSports.mo
Channel8.sg ChannelU.sg 8TV.my CGTN.cn CGTNDocumentary.cn'''.split()
CHINESE_ORDER = {key: i for i, key in enumerate(CHINESE_PRIORITY)}
OVERSEAS_PRIORITY = '''BBCNews.uk BBCOne.uk BBCTwo.uk BBCEarth.uk CNNInternational.us
CNN.us NHKWorldJapan.jp NHKGeneralTV.jp NationalGeographic.us DiscoveryChannel.us
AnimalPlanet.us BloombergTelevision.us CNBC.us Euronews.fr France24.fr
DWEnglish.de AlJazeeraEnglish.qa SkyNews.uk ABC.us CBS.us NBC.us PBS.us
DasErste.de ZDF.de Rai1.it KBS1.kr KBS2.kr'''.split()
OVERSEAS_ORDER = {key: i for i, key in enumerate(OVERSEAS_PRIORITY)}
OVERSEAS_BRANDS = [
    r'^BBC(?:\b|[0-9])', r'^CNN(?:\b|$)', r'^NHK(?:\b|$)',
    r'^National Geographic(?:\b|$)', r'^Nat Geo(?:\b|$)',
    r'^Discovery(?:\b|$)', r'^Animal Planet(?:\b|$)',
    r'^Bloomberg(?:\b|$)', r'^CNBC(?:\b|$)', r'^Euronews(?:\b|$)',
    r'^France 24(?:\b|$)', r'^(?:DW|Deutsche Welle)(?:\b|$)',
    r'^Al Jazeera(?:\b|$)', r'^Sky News(?:\b|$)',
    r'^(?:ABC|CBS|NBC|PBS|CBC|CTV)(?:\b|$)', r'^(?:Das Erste|ZDF|Rai [123])$',
    r'^(?:KBS|MBC|SBS)(?:\b|$)', r'^(?:TV5MONDE|France [2345])(?:\b|$)',
    r'^(?:ESPN|Eurosport|beIN Sports)(?:\b|[0-9]|$)',
    r'^(?:Cartoon Network|Nickelodeon|Disney Channel)(?:\b|$)',
]


def entry_id(info):
    match = re.search(r'tvg-id="([^"]*)"', info)
    return match.group(1).split('@')[0] if match else ''


def natural(value):
    return tuple((0, int(token)) if token.isdigit() else (1, token)
                 for token in re.split(r'(\d+)', value.casefold()))


def channel_key(info, metadata, chinese_ids):
    key = entry_id(info)
    station = metadata.get(key, {})
    name = station.get('name') or key or info.rsplit(',', 1)[-1]
    if key in CHINESE_ORDER:
        tier, priority = 0, CHINESE_ORDER[key]
    elif key in OVERSEAS_ORDER:
        tier, priority = 1, OVERSEAS_ORDER[key]
    elif any(re.search(pattern, name, re.I) for pattern in OVERSEAS_BRANDS):
        tier = 1
        priority = len(OVERSEAS_ORDER) + next(i for i, pattern in enumerate(OVERSEAS_BRANDS)
                                           if re.search(pattern, name, re.I))
    elif key in chinese_ids:
        tier, priority = 2, 0
    elif station.get('country') in {'CN', 'HK', 'MO', 'TW'}:
        # Domestic English-language brands follow the declared Chinese-language set.
        tier, priority = 3, 0
    else:
        tier, priority = 4, 0
    # Use station identity before stream/profile details so its alternatives stay adjacent.
    return tier, priority, natural(name), key


def rank_entries(entries, metadata, chinese_ids):
    order = {}
    for info, _ in entries:
        match = re.search(r'group-title="([^"]*)"', info)
        group = match.group(1) if match else ''
        order.setdefault(group, len(order))

    def key(row):
        info = row[0]
        match = re.search(r'group-title="([^"]*)"', info)
        group = match.group(1) if match else ''
        return order[group], channel_key(info, metadata, chinese_ids)

    return sorted(entries, key=key)
