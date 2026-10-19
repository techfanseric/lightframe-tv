"""Conservative display-name and category enrichment; never changes stream identity.

Native aliases: https://iptv-org.github.io/api/channels.json
Channel identities: https://tv.cctv.com/live/, https://www.rthk.hk/tv,
https://www.qhtb.cn/, and broadcaster websites recorded in that database.
Brand aliases are deliberately limited to established Chinese names.
"""
import re

DOMESTIC = {'CN', 'HK', 'MO', 'TW'}
OVERSEAS_IDS = {'VoATVChina.cn'}
HAN = re.compile(r'[\u3400-\u9fff]')

# Disambiguate generic native aliases (e.g. several stations named 新闻综合频道).
NAMES = {
    'AndoTV.cn': '安多卫视', 'HunanTV.cn': '湖南卫视',
    'GuangdongSatelliteTV.cn': '广东卫视', 'GuangzhouTV.cn': '广州综合频道',
    'GuizhouTV.cn': '贵州卫视', 'JiangxiTV.cn': '江西卫视',
    'QinghaiTV.cn': '青海卫视', 'ShandongTV.cn': '山东卫视',
    'ShandongTVLifeChannel.cn': '山东生活频道', 'ShandongTVQiluChannel.cn': '山东齐鲁频道',
    'ShandongTVVarietyChannel.cn': '山东综艺频道', 'ShandongTVSportsChannel.cn': '山东体育频道',
    'ShandongTVAgriculturalScienceChannel.cn': '山东农科频道',
    'ShandongTVChildrensChannel.cn': '山东少儿频道',
    'SichuanSatelliteTV.cn': '四川卫视', 'SichuanTVWomenandChildrenChannel.cn': '四川妇女儿童频道',
    'TianjinTV.cn': '天津卫视', 'LiaoningTV.cn': '辽宁卫视',
    'GoldenEagleCartoon.cn': '金鹰卡通', 'YouManCartoonChannel.cn': '优漫卡通',
    'HarbinMovieChannel.cn': '哈尔滨影视频道', 'HarbinComprehensiveNewsChannel.cn': '哈尔滨新闻综合频道',
    'ChifengComprehensiveNewsChanel.cn': '赤峰新闻综合频道', 'ChuxiongNewsChannel.cn': '楚雄新闻频道',
    'DongguanComprehensiveNewsChannel.cn': '东莞新闻综合频道', 'HebiNewsChannel.cn': '鹤壁新闻综合频道',
    'HezhengTV.cn': '和政电视台', 'DunhuangTV.cn': '敦煌电视台',
    'JiuquanTVNewsComprehensiveChannel.cn': '酒泉新闻综合频道',
    'LanzhouComprehensiveNewsChannel.cn': '兰州新闻综合频道', 'LanzhouCultureTourismChannel.cn': '兰州文旅频道',
    'NanchangNewsGeneralistChannel.cn': '南昌新闻综合频道',
    'NeiMonggolTV2MongolianCultureChannel.cn': '内蒙古蒙语文化频道',
    'NingxiaSatelliteChannel.cn': '宁夏卫视', 'PingxiangTVNewsChannel.cn': '萍乡新闻综合频道',
    'GuangxiVarietyTravelChannel.cn': '广西综艺旅游频道',
    'XianBusinessInformationChannel.cn': '西安商务资讯频道', 'XianSilkRoadChannel.cn': '西安丝路频道',
    'XinjiangTV1.cn': '新疆卫视', 'XinjiangTV2.cn': '新疆维语新闻综合频道',
    'XinjiangTV3.cn': '新疆哈语新闻综合频道', 'XinjiangTV8.cn': '新疆哈语综艺频道',
    'XizangTVChinese.cn': '西藏卫视', 'XizangTVTibetan.cn': '西藏藏语卫视',
    'XuzhouEconomicLifeChannel.cn': '徐州经济生活频道', 'YanbianSatelliteTV.cn': '延边卫视',
    'ZhejiangInternationalChannel.cn': '浙江国际频道',
    'CETV1.cn': '中国教育电视台一套', 'CETV2.cn': '中国教育电视台二套',
    'CCTVPlus1.cn': '央视国际视频通讯社1', 'CCTVPlus2.cn': '央视国际视频通讯社2',
    'TVBRICSChinese.cn': '金砖国家电视台中文频道',
    'CGTN.cn': '中国国际电视台英语频道', 'CGTNArabic.cn': '中国国际电视台阿拉伯语频道',
    'CGTNDocumentary.cn': '中国国际电视台纪录频道', 'CGTNFrench.cn': '中国国际电视台法语频道',
    'CGTNRussian.cn': '中国国际电视台俄语频道', 'CGTNSpanish.cn': '中国国际电视台西班牙语频道',
    'CGTNGlobalBiz.cn': '中国国际电视台环球财经',
    'DaliTV.tw': '大立电视台',
    'CTS.tw': '华视主频', 'DaAi2.tw': '大爱二台',
    'EBCFinancialNews.tw': '东森财经新闻台', 'EBCNews.tw': '东森新闻台',
    'GoodTV.tw': '好消息电视台', 'BeautifulLifeTV.tw': '人间卫视',
    'SETNews.tw': '三立新闻台', 'TVBSNews.tw': 'TVBS新闻台', 'TVBSAsia.tw': 'TVBS亚洲台',
    'FTVTaiwan.tw': '民视台湾台', 'WXTV.tw': '唯心电视台',
    'TaiwanIndigenousTV.tw': '原住民族电视台', 'SupremeMasterTV.tw': '无上师电视台',
    'TaiwanPlusTV.tw': 'TaiwanPlus（台湾国际影音平台）',
    'DiscoveryAsiaTaiwan.tw': '探索亚洲频道（台湾）', 'CanalMacau.mo': '澳视葡文台',
    'HOYTV.hk': 'HOY TV（开电视）',
    # The database assigns this US broadcaster to CN; keep its actual identity.
    'VoATVChina.cn': '美国之音中文电视',
    'DasErste.de': '德国电视一台', 'ZDF.de': '德国电视二台',
    'Rai1.it': '意大利广播电视一台', 'Rai2.it': '意大利广播电视二台',
    'Rai3.it': '意大利广播电视三台',
    'France2.fr': '法国电视二台', 'France3.fr': '法国电视三台',
    'France4.fr': '法国电视四台', 'France5.fr': '法国电视五台',
    'NHKGeneralTV.jp': 'NHK综合频道', 'NHKEducationalTV.jp': 'NHK教育频道',
    'KBS1.kr': '韩国广播公司第一频道', 'KBS2.kr': '韩国广播公司第二频道',
}

# Same wording in several provinces must not collapse into indistinguishable names.
for prefix, city in [('Jiangxi', '江西'), ('Jilin', '吉林')]:
    for suffix, title in [('ChildrensChannel', '少儿频道'), ('CityChannel', '都市频道'),
                          ('EconomyLifeChannel', '经济生活频道'), ('MovieChannel', '影视频道'),
                          ('PublicAgricultureChannel', '公共农业频道'),
                          ('LifestyleChannel', '生活频道'), ('RuralChannel', '乡村频道')]:
        NAMES[f'{prefix}{suffix}.cn'] = city + title
for i, title in enumerate(['新闻综合', '生活服务', '影视', '休闲资讯', '都市', '青少旅游'], 1):
    NAMES[f'QTV{i}.cn'] = f'青岛{title}频道'

# Shared aliases only where the broadcaster brand has a widely used Chinese name.
BRANDS = [
    (r'^Animal Planet(?:\b|$)', '动物星球频道'),
    (r'^National Geographic(?:\b|$)', '国家地理频道'),
    (r'^Nat Geo Wild(?:\b|$)', '国家地理野生频道'),
    (r'^Discovery Channel(?:\b|$)', '探索频道'),
    (r'^Discovery Science(?:\b|$)', '探索科学频道'),
    (r'^BBC Earth(?:\b|$)', 'BBC地球频道'),
    (r'^BBC News(?:\b|$)', 'BBC新闻频道'),
    (r'^CNN International(?:\b|$)', '美国有线电视新闻网国际频道'),
    (r'^CNN$', '美国有线电视新闻网'),
    (r'^Bloomberg (?:TV|Television)(?:\b|$)', '彭博电视'),
    (r'^Al Jazeera(?:\b|$)', '半岛电视台'),
    (r'^France 24(?:\b|$)', '法国24新闻台'),
    (r'^(?:DW|Deutsche Welle)(?:\b|$)', '德国之声'),
    (r'^Euronews(?:\b|$)', '欧洲新闻台'),
    (r'^NHK World(?:[ -]Japan)?$', 'NHK日本国际传媒'),
    (r'^Cartoon Network(?:\b|$)', '卡通频道'),
    (r'^Nickelodeon(?:\b|$)', '尼克儿童频道'),
    (r'^Disney Channel(?:\b|$)', '迪士尼频道'),
]

# Corrections for known channel identities, plus otherwise untagged broadcasters.
CATEGORY_IDS = {
    'general': '''AndoTV.cn AnhuiTV.cn BingtuanSatelliteTV.cn CCTV8K.cn DunhuangTV.cn
GuangdongSatelliteTV.cn HunanTV.cn JiangsuSatelliteTV.cn JiangxiTV.cn QinghaiTV.cn
ShandongTV.cn ShandongTVQiluChannel.cn ShenzhenSatelliteTV.cn SichuanSatelliteTV.cn
TianjinTV.cn XinjiangTV1.cn XinjiangTV2.cn XinjiangTV3.cn XizangTVChinese.cn XizangTVTibetan.cn
YanbianSatelliteTV.cn ZhejiangInternationalChannel.cn ZhejiangSatelliteTV.cn TTV.tw
RTHKTV31.hk RTHKTV32.hk RTHKTV33.hk RTHKTV34.hk GoldenJade.hk
CBCTelevision.ca IciRadioCanadaTele.ca CTV.ca BBCOne.uk BBCTwo.uk BRFernsehen.de
RadioBremenFernsehen.de rbbFernsehen.de SRFernsehen.de SWRFernsehenBadenWurttemberg.de
SWRFernsehenHD.de SWRFernsehenRheinlandPfalz.de ZDF.de RTL.de RTLZwei.de RTLTVI.be
ProSieben.de ProSiebenMaxx.de Rai1.it Rai3.it Rai4.it RIK1.cy RIK2.cy RIKHD.cy RIKSat.cy
RTS1.rs RTS2.rs RTSSvet.rs TVSLO1.si TVSLO2.si SVT1.se SVT2.se YleTV1.fi YleTV2.fi
TVP1.pl TVP2.pl TVPHD.pl TVPPolonia.pl TV3.es TV3CAT.es TVEInternacionalAmerica.es
TVEInternacionalAsiaOceania.es TVEInternacionalEurope.es TRT1.tr TRTAvaz.tr TRTKurdi.tr
TRTTurk.tr UniMas.us Univision.us UnivisionLatinAmerica.mx RedeGlobo.br SABC1.za SABC2.za
TVGloboSaoPaulo.br SBTNacional.br PROTV.ro PROTVInternational.ro Prva.rs BHRT.ba
RTRSTV.ba TVCG1.me TVCGMNE.me TG4.ie S4C.uk ThaiPBS.th VTV1.vn VTV4.vn
VTV5.vn VTV5TayNguyen.vn VTV8.vn VTV9.vn TVBrasil.br TVRIWorld.id SudanTV.sd''',
    'news': '''VoATVChina.cn HOYInfotainment.hk TaiwanPlusTV.tw BBCNewsPashto.uk
BNRNieuwsradio.nl TVPInfo.pl tagesschau24.de WPolscePL.pl TVRepublika.pl TVRain.nl
RealAmericasVoice.us RealitateaPlus.ro RomaniaTV.ro SamaaTV.pk SolovyovLive.ru
TheHillTV.us TeleFormula.mx TVNET.tr UlkeTV.tr YemenTodayTV.ye''',
    'movies': '''CHCHomeTheater.cn La7Cinema.it TalkingPicturesTV.uk TopMoviesPolska.pl
HBO3.pl HBOPlusLatinAmerica.us Golden.mx GoldenEdge.mx GoldenPlus.mx Starz.us
StarzEncoreSpanish.us StopklatkaTV.pl StudioUniversalLatinAmerica.us TCMLatinAmerica.us
TNTLatinAmerica.us TribecaChannel.us ShemarooBollywood.us''',
    'series': '''CCTV8.cn CCTVNostalgiaTheater.cn GloboplayNovelas.br JordanDrama.jo
SerieClub.fr RomanceTV.pl TimelessDiziChannel.pl TheNanny.us TheWalkingDeadenEspanol.us
CSIenespanol.us PlutoTVMelrosePlace.us''',
    'documentary': '''DocumentaryHumanitiesChannel.cn CCTVWorldGeography.cn
AnimalPlanet.cz AnimalPlanet.hu DiscoveryChannel.cz DiscoveryTheaterLatinAmerica.us
InvestigationDiscovery.cz RMCDecouverte.fr PolsatDoku.pl TVPHistoria.pl ZDFinfo.de
PlutoTVDocumentales.us PlutoTVHistoria.us RealWild.uk SamsungWildlife.us
ViasatExplore.ua ViasatExploreClassic.pl ZhivayaPlaneta.ru Zhivayapriroda.ru
ZooTV.ru Zoopark.ru Vmirezhivotnykh.ru StoryTelevision.us TrueHistory.us''',
    'sports': '''beINSports9.qa BigTenNetwork.us SECNetwork.us PolsatSport1.pl
PolsatSport2.pl PolsatSport3.pl ProArena.ro RugbyPassTV.uk TUDN.mx TUDN.us
TNAWrestlingChannel.pl HardKnocksFightingChampionship.pl SevillaFCTV.es TOPBarca.pl
WomensSportsNetwork.us WorldBilliards.pl YesNetwork.us''',
    'kids': '''Caillou.fr CarCityAdventures.pl PlutoTVElReinoInfantil.us Pop.uk RaiGulp.it
RainbowRuby.us RevandRoll.us RTSPoletarac.rs SemillitasTV.us TVRaTimBum.br
TiVi5Monde.fr YuGiOhenespanol.us ZarokTV.tr''',
    'music': '''PoloTV.pl PowerTV.tr PowerTurkTV.tr Radio538.nl RockAntenne.de
RocklandTV.de RFMTV.fr StarsTV.pl SoundcityTV.ng Stereo5TV.it Teleritmo.mx
TarafTV.ro ThaiChaiyo.th IDJTV.rs KpopMix.ar UrbanMix.ar Unserding.de''',
    'religious': '''SupremeMasterTV.tw BibelTV.de BibelTVImpuls.de BoasNovas.br
CalvaryTV.in CanalDiocesano.es EcclesiaTV.gr ExtremaTV.us LabbaykTV.ir
LightChannel.ro PraiseTV.pk PrarthanaBhawanTV.in PrarthanaTV.in RedADvenirTV.bo
ResurrectionTV.gh RodjaTV.id SalvationTV.ng SantvaniChannel.in SarvDharmSangam.in
Sat7Arabic.cy Sat7Turk.cy ScientologyNetwork.us SemerkandTV.tr SenderNeuJerusalem.de
ShekinahTV.in ShraddhaTV.lk ShubhTV.in SikhSpiritualCentreRexdale.ca
SophiaTV.it SophiaTVEspanol.de SunnaTV.ng SurauTV.id SynagogueTV.ng TakbeerTV.uk
TBNArmenia.us TBNNejatTV.ir TBNPacific.us TBNUK.uk TBNUkraine.us TCT.us
TheBuddhist.lk TVCancaoNova.br TVHram.rs TVLux.sk TVNoe.cz TVTrwam.pl
VerbumTV.lk VelayatTV.ir WorldHarvestTV.us''',
    'legislative': '''Riigikogu.ee TBMMTV.tr TVJustica.br TVSenado.br TVSenado.cl
TVSLO3.si TweedeKamerKlompezaal.nl VmesteRF.ru''',
    'education': 'IngenioTV.mx KentuckyEducationalTelevision.us STOUChannel.th TED.us UniNettunoUniversityTV.it VTV7.vn',
    'culture': 'CCTVCultureofQuality.cn TVPKultura.pl TVCultura.br TVCulturas.bo TRT2.tr RTS3.rs YleTeemaFem.fi',
    'entertainment': 'BreadTV.cn ShandongTVVarietyChannel.cn XinjiangTV8.cn TechStorm.sg TVPRozrywka.pl VTV3.vn',
    'lifestyle': 'ShandongTVLifeChannel.cn HGTV.cz HomeTV.pl Homeful.ca CanalPlusDomo.pl PolsatCafe.pl Usadba.ru TLC.cz',
    'cooking': 'FoodNetwork.cz KusKus.ua', 'travel': 'JordanTourism.jo TravelChannel.cz TravelChannel.hu TravelChannel.ru',
    'business': 'Moconomy.pl',
}
OVERRIDE_CATEGORIES = {key: category for category, keys in CATEGORY_IDS.items() for key in keys.split()}
# These channel names explicitly state their subject. No generic "TV" or country fallback.
SUBJECTS = [
    (r'\b(?:news|nieuws|noticias|nachrichten)\b|新闻|新闻资讯', 'news'),
    (r'\b(?:sports?|rugby|wrestling|billiards)\b|体育|台球', 'sports'),
    (r'\b(?:kids|children|childrens|cartoon|animation)\b|少儿|儿童|卡通', 'kids'),
    (r'\b(?:movies?|cinema|cine)\b|电影|影院', 'movies'),
    (r'\b(?:documentary|documentales|wildlife)\b|纪录|纪实', 'documentary'),
    (r'\b(?:music|muzik|musica|jazz)\b|音乐', 'music'),
    (r'\b(?:parliament|senado|senate)\b|议会', 'legislative'),
    (r'\b(?:cooking|food)\b|美食', 'cooking'),
    (r'\b(?:travel|tourism)\b|旅游', 'travel'),
]


def category_hint(channel_id, metadata):
    if channel_id in OVERRIDE_CATEGORIES:
        return OVERRIDE_CATEGORIES[channel_id], 'reviewed-channel-id'
    names = [metadata.get('name', '')] + metadata.get('alt_names', [])
    text = ' '.join(names)
    for pattern, category in SUBJECTS:
        if re.search(pattern, text, re.I):
            return category, 'explicit-subject-in-channel-name'
    return None, None


def chinese_name(channel_id, metadata):
    if channel_id in NAMES:
        return NAMES[channel_id], 'reviewed-channel-id'
    # Chinese-looking Japanese kana or Korean Hanja aliases are not consensus
    # Chinese translations. Accept native aliases in Chinese-language regions only.
    if metadata.get('country') in DOMESTIC | {'SG', 'MY'}:
        for alias in metadata.get('alt_names', []):
            if HAN.search(alias) and not re.search(r'[\u3040-\u30ff\uac00-\ud7af]', alias):
                return alias.strip(), 'upstream-native-alias'
    if metadata.get('country') not in DOMESTIC:
        for pattern, translated in BRANDS:
            if re.search(pattern, metadata.get('name', ''), re.I):
                return translated, 'established-brand-alias'
    return None, None
