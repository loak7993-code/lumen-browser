import urllib.parse

AD_HOSTS = {
    "doubleclick.net", "googleadservices.com", "googlesyndication.com",
    "pagead2.googlesyndication.com", "googletagservices.com", "adservice.google.com",
    "ads.google.com", "admob.com", "adsystem.com", "amazon-adsystem.com",
    "aax.amazon-adsystem.com", "aax-us-east.amazon-adsystem.com",
    "adnxs.com", "criteo.com", "criteo.net", "pubmatic.com", "rubiconproject.com",
    "openx.net", "moatads.com", "serving-sys.com", "advertising.com", "2mdn.net",
    "adsrvr.org", "rlcdn.com", "bidswitch.net", "casalemedia.com", "krxd.net",
    "chartbeat.com", "yieldlab.net", "adform.net", "smartadserver.com", "exoclick.com",
    "propellerads.com", "popads.net", "popcash.net", "adcash.com", "adsterra.com",
    "hilltopads.com", "media.net", "taboola.com", "outbrain.com", "revcontent.com",
    "mgid.com", "zedo.com", "adtech.de", "adtech.com", "gumgum.com",
    "quantserve.com", "scorecardresearch.com",
    "adcolony.com", "applovin.com", "chartboost.com", "unityads.unity3d.com",
    "vungle.com", "ironsrc.com", "startapp.com", "inmobi.com", "mopub.com",
    "mintmobile.com", "admob.org", "adsymptotic.com", "adstune.com",
    "admixer.net", "adroll.com", "rollup.io", "adap.tv", "admob.cn",
    "adsymptotic.com", "adbrite.com", "adbmedia.com", "adk2.com",
    "yieldmo.com", "bidsxchange.com", "contextweb.com", "sekindo.com",
    "inneractive.com", "manage.com", "magnifite.com", "media6degrees.com",
    "nrich.ai", "nrichmedia.com", "onead.tf1.fr", "oneadx.com",
    "pixel.advertising.com", "pixfuture.com", "plista.com", "predicta.com",
    "projectwonderful.com", "pulse.yahoo.com", "revjet.com", "rubiconproject.com",
    "sabavision.com", "sara-ad.com", "serving-sys.com", "skai.co.uk",
    "smartadserver.com", "sponsored.com", "stailer.com", "taboola.com",
    "tag.adthin.com", "trafficmanager.com", "tribalfusion.com", "trueadservice.com",
    "undertone.com", "unicast.com", "upravel.com", "vadpay.com",
    "videohub.com", "view.atdmt.com", "viralvideochart.com",
    "adtech.com", "adtech.de", "adtech.net", "adtech.com.au",
    "2mdn.net", "doubleclick.com", "doubleverify.com",
    "adup-tech.com", "adventure.com", "adverity.com", "adhese.com",
    "adikteev.com", "adition.com", "adknife.com", "adleft.com",
    "adlibrium.com", "adlink.net", "adlink.com", "admanager.com",
    "admatic.com", "admedia.com", "admelder.com", "adminisable.com",
    "admixer.net", "admobile.com", "admost.com", "admulti.com",
    "adnami.io", "adnexus.com", "adnium.com", "adonis.com",
    "adoptim.com", "adorika.com", "adotsolution.com", "adotd.com",
    "adpath.com", "adpick.co", "adprediction.com", "adpreserver.com",
    "adprime.com", "adpro.com", "adpurl.com", "adq.com",
    "adreactor.com", "adready.com", "adreadytr.com", "adrive.com",
    "adroll.com", "adrocket.com", "adrove.com", "adsense.com",
    "adservice.com", "adsfactor.com", "adsfair.com", "adside.com",
    "adslot.com", "adsmarket.com", "adsnative.com", "adsonar.com",
    "adspeed.com", "adspring.com", "adsrvmedia.com", "adstogo.com",
    "adstudiocom.com", "adsupply.com", "adswork.com", "adtaily.com",
    "adtech.com", "adtegrity.com", "adtruth.com", "adutopia.com",
    "adv-adserver.com", "adv.com", "advatue.com", "adverserve.com",
    "adversal.com", "advia.com", "adviva.com", "adworldnetwork.com",
    "adxpower.com", "adyard.com", "adyoulike.com", "adzerk.com",
    "dpbolvw.net", "apmebf.com", "kqzyfj.com", "tkqlhj.com",
    "qksz.net", "qksrv.net", "jdoqocy.com", "pjatr.com",
    "pjtra.com", "tqlkg.com", "afcyhf.com", "ltzoa.com",
    "rnsfb.com", "rjzvw.com", "xtrk.com",
}

TRACKER_HOSTS = {
    "googletagmanager.com", "google-analytics.com", "analytics.google.com",
    "segment.io", "segment.com", "mixpanel.com", "amplitude.com", "hotjar.com",
    "fullstory.com", "logrocket.com", "mouseflow.com", "luckyorange.com",
    "clarity.ms", "facebook.net", "connect.facebook.net", "nr-data.net",
    "newrelic.com", "intercom.io", "intercomcdn.com", "branch.io",
    "adjust.com", "appsflyer.com", "kochava.com", "bluekai.com", "demdex.net",
    "omtrdc.net", "tealium.com", "ensighten.com", "quantcast.com",
    "comscore.com", "optimizely.com", "crazyegg.com", "kissmetrics.com",
    "heap.io", "snowplowanalytics.com", "fingerprintjs.com",
    "recaptcha.net", "g.doubleclick.net", "xandr.com", "appnexus.com",
    "scorecardresearch.com", "quantserve.com",
    "chartbeat.com", "chartbeat.net", "alexa.com", "quantserve.com",
    "bidswitch.net", "contextweb.com", "openx.net", "pubmatic.com",
    "rubiconproject.com", "casalemedia.com", "criteo.com", "criteo.net",
    "adform.net", "smartadserver.com", "moatads.com", "serving-sys.com",
    "adsrvr.org", "rlcdn.com", "krxd.net",
    "tagmanager.google.com", "www.googletagmanager.com", "www.google-analytics.com",
    "ssl.google-analytics.com", "pixel.facebook.com", "connect.facebook.net",
    "graph.facebook.com", "api.facebook.com", "analytics.tiktok.com",
    "analytics.twitter.com", "analytics.snapchat.com",
    "t.co", "ads.twitter.com", "ads.linkedin.com",
    "px.ads.linkedin.com", "snap.licdn.com",
    "rumcdn.com", "piwik.org", "matomo.org", "matomo.cloud",
    "statcounter.com", "statcdn.com", "clicky.com",
    "woopra.com", "gosquared.com", "logrocket.com",
    "td.doubleclick.net", "ad.doubleclick.net", "stats.g.doubleclick.net",
    "adservice.google.com", "adservice.google.de", "adservice.google.co.uk",
    "fundingchoicesmessages.google.com", "geoip.tech",
    "mmstat.com", "alicdn.com", "amap.com",
    "umeng.com", "umeng.co", "umtrack.com",
    "datafat.com", "dtiads.com", "dynamicyield.com",
    "easysite.com", "edgemesh.com", "egorigin.com",
    "eloqua.com", "email-match.com", "ensighten.com",
    "evidon.com", "evergage.com", "eyeota.com",
    "ezodn.com", "ezoic.net", "ezoic.com",
    "fwmrm.net", "getclicky.com", "getuplift.com", "globaldatagroup.com",
    "growth-revenue.com", "gwallet.com", "hadian.com", "hawk.md",
    "heapanalytics.com", "hubspot.com", "hysteria.com", "ib-rite.com",
    "iesnare.com", "imedia.cz", "impactcdn.com", "in.com",
    "inboundnow.com", "infusionsoft.com", "intentmedia.com",
    "intergi.com", "iperceptions.com", "iristmp.com",
    "itop.com", "ivimeo.com", "ivtrace.com",
    "keywee.com", "kingdom.com", "kirtec.com", "krux.com",
    "krux.com", "kruxcdn.com", "l-idc.com", "lckr.net",
    "leadgen.com", "lentice.com", "linksynergy.com",
    "liftdna.com", "lijit.com", "linicom.co.il",
    "liveperson.net", "liveperson.com", "lkqd.com",
    "loopme.com", "lpsnmedia.com", "lpmtrk.com",
    "ludigames.com", "magnify360.com", "mediabrix.com",
    "mediavoice.com", "megaxus.com", "mercent.com",
    "metanetwork.com", "microad.net", "mixpo.com",
    "ml314.com", "mnet.com", "mopub.com",
    "moxad.net", "mplayer.com", "mparticle.com",
    "mparticle.com", "mybuys.com", "myclock.com",
    "myroitech.com", "naiadsystems.com", "nanigans.com",
    "narrative.com", "neodatagroup.com", "netshelter.com",
    "netshelter.net", "newopen.com", "nextag.com",
    "nexstar.com", "nextperformance.com", "ngtv.com",
    "northpitch.com", "nuggad.net", "nytimes.com",
    "oewa.at", "omtrdc.net", "onead.com.tw",
    "onclickads.net", "opentext.com", "opentracking.com",
    "operamediaworks.com", "opinmind.com", "optomail.com",
    "otclick.com", "otspl.com", "ourstreet.com",
    "parsely.com", "pat-sites.com", "payhit.com",
    "pcash.com", "pepperjam.com", "perfectseer.com",
    "permutive.com", "phgclick.com", "pinterest.com",
    "pladform.com", "plista.com", "pmdigital.com",
    "pmetrics.com", "popserve.com", "popunder.com",
    "powerlinks.com", "ppcb.com", "p-p-g.com",
    "pr-aide.com", "prdock.com", "predictad.com",
    "premiumadvertising.com", "present.com", "primedirect.com",
    "pro-market.net", "proclivity.com", "projectwonderful.com",
    "propellerads.com", "pswec.com", "pubmatic.com",
    "pubnation.com", "pubmatic-cn.com", "pulsepoint.com",
    "punchtab.com", "qnsr.com", "quantcast.com",
    "quantserve.com", "quantserv.com", "questus.com",
    "radiusmarketing.com", "rakutenadvertising.com", "raven.com",
    "reactenc.com", "redtram.com", "reevoo.com",
    "res-x.com", "resy.com", "retailads.net",
    "retargeter.com", "revcontent.com", "revjets.com",
    "revjet.com", "revresponse.com", "rightaction.com",
    "rmcp.net", "roia.biz", "rtk.com",
    "rubiconproject.com", "safehaven.com", "salesviewer.com",
    "sascdn.com", "saymedia.com", "sayyesto.com",
    "scnear.com", "sda.com", "sdsf.com",
    "segfault.com", "sekindo.com", "semantiqo.com",
    "sense.com", "serverbid.com", "servedby.com",
    "shareaholic.com", "sharethrough.com", "shopzilla.com",
    "sidebar.com", "silverpop.com", "simpli.fi", "sitescout.com",
    "skimresources.com", "smartadserver.com", "smit.com",
    "snapgadget.com", "social-twitter.com", "sociomantic.com",
    "sonobi.com", "sortable.com", "spntech.com",
    "spotx.com", "spotxchange.com", "spotx.tv",
    "statcounter.com", "statspy.com", "stillsomething.com",
    "strap.com", "streamads.com", "sumome.com",
    "superads.com", "sxp.com", "sync1.com",
    "taboola.com", "taboola.com", "taboolasyndication.com",
    "tapad.com", "tapgage.com", "tapit.com",
    "teadma.com", "teads.tv", "telemetree.com",
    "telerama.com", "temel.com", "temefee.com",
    "teracent.com", "thehub.com", "thirdpresence.com",
    "thru.com", "tpc.googlesyndication.com", "tpgs.com",
    "tpid.com", "trackalyzer.com", "trackcmp.com",
    "trackers.com", "tracking.com", "tradeadexchange.com",
    "tradedoubler.com", "tradetracker.net", "trafficmanager.com",
    "trafficjunky.com", "trf.com", "tribalfusion.com",
    "triggit.com", "truefit.com", "truelead.com",
    "tubemogul.com", "turn.com", "twitter.com",
    "tynt.com", "ucis.edu.cn", "unifive.com",
    "union.com", "unrulymedia.com", "uptime.com",
    "upscore.com", "ur-distance.com", "userlytics.com",
    "uspeed.com", "verticalscope.com", "viglink.com",
    "viralmedia.com", "visiblemeasures.com", "vmmapi.com",
    "voicefive.com", "vpon.com", "vs.com",
    "web-ads.com", "webtelemetry.com", "weightshift.com",
    "wfads.com", "worldsoft.com", "wtatistics.com",
    "xplosion.de", "yieldads.com", "yieldbuild.com",
    "yieldlab.net", "yieldmo.com", "yieldoptimizer.com",
    "yldbt.com", "yob.cl", "yoc.com", "yottos.com",
    "zanox.com", "zdbb.net", "zdbb.net", "zedo.com",
    "zemanta.com", "zoomerang.com",
}

BLOCKED_PATHS = [
    "/ads", "/ads/", "/adserver", "/advert", "/banner", "/popup", "/prebid",
    "/tracker", "/tracking", "/beacon", "/analytics", "/gampad",
    "/doubleclick", "/gtm.js", "/tagmanager", "/scorecard", "/pixel.gif",
    "/log.gif", "/collect", "/adsense", "/adsystem", "/adview",
    "/adclick", "/adcall", "/adrender", "/adserve", "/adspaces",
    "/bouncer", "/sync", "/redirect", "/impression", "/track/",
    "/track?", "/analytics.js", "/ga.js", "/mixpanel", "/segment",
    "/criteo", "/pubmatic", "/rubicon", "/openx", "/bidrequest",
    "/adnxs", "/moat", "/chartbeat", "/hotjar", "/clarity",
    "/pixel", "/ping", "/visit", "/visit/", "/stats", "/metrics",
    "/telemetry", "/sentry", "/amplitude", "/heap", "/mouseflow",
]

ads_blocked = 0
trackers_blocked = 0


def should_block(url, settings):
    global ads_blocked, trackers_blocked
    if not url:
        return False
    parsed = urllib.parse.urlparse(url)
    host = (parsed.hostname or "").lower()
    path = parsed.path or ""

    if not host:
        return False

    if settings.get("block_ads", True):
        for h in AD_HOSTS:
            if host == h or host.endswith("." + h):
                ads_blocked += 1
                return True
        for p in BLOCKED_PATHS:
            if path.startswith(p):
                ads_blocked += 1
                return True

    if settings.get("block_trackers", True):
        for h in TRACKER_HOSTS:
            if host == h or host.endswith("." + h):
                trackers_blocked += 1
                return True

    return False


def stats():
    return ads_blocked, trackers_blocked


def reset():
    global ads_blocked, trackers_blocked
    ads_blocked = 0
    trackers_blocked = 0
