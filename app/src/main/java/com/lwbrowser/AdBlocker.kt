package com.lwbrowser

import android.net.Uri
import java.util.concurrent.atomic.AtomicLong

object AdBlocker {

    private val adHosts = setOf(
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
        "contentads.com", "mgid.com", "adsterra.com", "yllix.com", "adhigh.net",
        "admixer.net", "adspeed.com", "adkernel.com", "epom.com", "plista.com",
        "adstargets.com", "yldistrtict.com", "zedo.com", "adtech.de", "adtech.com",
        "intergi.com", "gumgum.com", "engageya.com", "ntv.io", "lockerdome.com",
        "mantisadnetwork.com", "connexity.com", "skimresources.com", "viglink.com",
        "linksynergy.com", "awempire.com", "traffichaus.com", "exosrv.com", "tsyndicate.com",
        "quantserve.com", "scorecardresearch.com",
        "adcolony.com", "applovin.com", "chartboost.com", "unityads.unity3d.com",
        "vungle.com", "ironsrc.com", "startapp.com", "inmobi.com", "mopub.com",
        "admob.org", "adsymptotic.com", "adstune.com", "adbrite.com", "adbmedia.com",
        "adk2.com", "yieldmo.com", "bidsxchange.com", "contextweb.com", "sekindo.com",
        "inneractive.com", "undertone.com", "mediavine.com", "adzerk.com",
        "buysellads.com", "dpbolvw.net", "apmebf.com", "kqzyfj.com", "tkqlhj.com",
        "qksz.net", "qksrv.net", "jdoqocy.com", "pjatr.com", "pjtra.com",
        "tqlkg.com", "afcyhf.com", "ltzoa.com", "rnsfb.com", "rjzvw.com",
        "xtrk.com", "adroll.com", "rollup.io", "adap.tv", "tribalfusion.com",
        "clicksor.com", "infolinks.com", "kontera.com", "vibrantmedia.com",
        "intellitxt.com", "echo-topic.com", "mirago.com", "adbutler.com",
        "hi-ads.com", "admanage.com", "addynamo.com", "admobie.com", "admize.com",
        "adocean.com", "adonize.com", "adoptim.com", "adpath.com", "adreactor.com",
        "adsfactor.com", "adsfactor.net", "adstogo.com", "adsupply.com", "adtaily.com",
        "adtegrity.com", "adultfriendfinder.com", "adversal.com", "adverserve.com",
        "adworldnetwork.com", "adxpower.com", "adyard.com", "adyoulike.com",
        "agkn.com", "adgibbo.com", "udmserve.net", "undertone.com", "ybrantdigital.com",
        "yoc.com", "yoban.com", "yoc.com", "zedo.com", "zdbb.net", "zemanta.com",
        "adadvisor.net", "adbrite.com", "adclix.com", "addtot.com",
        "mediacom.com", "freewheel.com", "fwmrm.net", "tremorhub.com",
        "aniview.com", "contentabc.com", "digitaldsp.com", "divinity.com",
        "yumenetwork.com", "ybp.yume.com", "adserver.yahoo.com", "adserver.yadro.com",
        "ru4.com", "advmaker.ru", "advmaker.com", "adsense.com",
        "adservice.com", "adsfactor.com", "tiscali.com", "virgilio.it",
        "adsfac.eu", "adsfac.net", "adsfac.us", "adsrvr.org",
        "adtech.de", "adtech.com", "adtech.net", "adtech.com.au",
        "2mdn.net", "doubleclick.com", "doubleverify.com", "adtech.com",
        "creativecdn.com", "gemius.pl", "inskin.com", "inskinmedia.com",
        "admixer.net", "admixer.com", "admixer.co", "minutemedia.com",
        "minutemedia-prebid.com", "mobileadvertising.com", "mobileads.com",
        "ads.youtube.com", "ads.reddit.com", "ads.tiktok.com", "ads.snapchat.com",
        "ads.pinterest.com", "ads.twitter.com", "ads.linkedin.com",
        "ads.instagram.com", "ads.facebook.com", "ads.ebay.com", "ads.craigslist.org",
        "adservice.google.com", "adservice.google.de", "adservice.google.co.uk",
        "fundingchoicesmessages.google.com", "googlesyndication.com",
    )

    private val trackerHosts = setOf(
        "googletagmanager.com", "google-analytics.com", "ssl.google-analytics.com",
        "analytics.google.com", "segment.io", "segment.com", "mixpanel.com", "amplitude.com",
        "hotjar.com", "fullstory.com", "logrocket.com", "mouseflow.com", "luckyorange.com",
        "clarity.ms", "bat.bing.com", "facebook.net", "connect.facebook.net",
        "sb.scorecardresearch.com", "nr-data.net", "new-relic.com", "newrelic.com",
        "rumcdn.com", "intercom.io", "intercomcdn.com", "app.link", "branch.io",
        "adjust.com", "appsflyer.com", "kochava.com", "tune.com", "mobileapptracking.com",
        "mathtag.com", "bluekai.com", "demdex.net", "omtrdc.net", "everestjs.net",
        "everesttech.net", "datafastguru.io", "hh-promo.com", "ipify.org", "ipinfo.io",
        "ip-api.com", "userlytics.com", "clicktale.net", "smartlook.com", "userreplay.com",
        "tealium.com", "tealiumiq.com", "ensighten.com", "sessioncam.com", "quantcast.com",
        "quantserve.com", "comscore.com", "optimizely.com", "crazyegg.com", "kissmetrics.com",
        "chartbeat.com", "parsely.com", "parsnip.com", "walkme.com", "heap.io",
        "snowplowanalytics.com", "piwik.org", "matomo.org", "matomo.cloud",
        "fingerprintjs.com", "fpjs.io", "deviceatlas.com", "wurfl.io",
        "iovation.com", "threatmetrix.com", "recaptcha.net", "g.doubleclick.net",
        "stats.g.doubleclick.net", "snap.licdn.com", "px.ads.linkedin.com",
        "ads.linkedin.com", "ads.pinterest.com", "tags.tiqcdn.com", "ct.pinterest.com",
        "analytics.tiktok.com", "analytics.snapchat.com", "sc-static.net",
        "bat.bing.com", "xandr.com", "appnexus.com",
        "pixel.facebook.com", "connect.facebook.net", "graph.facebook.com",
        "api.facebook.com", "analytics.twitter.com", "t.co",
        "px.ads.linkedin.com", "snap.licdn.com", "stats.g.doubleclick.net",
        "ad.doubleclick.net", "fls.doubleclick.net", "stats.example.com",
        "t.datadog.com", "t.datadoghq.com", "browser-intake-datadoghq.com",
        "rumcdn.com", "sentry.io", "o1.ingest.sentry.io", "o2.ingest.sentry.io",
        "cdn.o2.ingest.sentry.io", "config.gtm-360.com", "www.googletagmanager.com",
        "www.google-analytics.com", "tagmanager.google.com",
        "tracking.gamingforgood.net", "collector.example.com",
        "analytics.example.com", "trackcmp.com", "trackers.com", "tracking.com",
        "ppctracking.com", "conversion.com", "retargeting.com",
        "dynamicyield.com", "dynamicyield.com", "evergage.com", "permutive.com",
        "permutive.app", "cdn.permutive.com", "mparticle.com", "api.mparticle.com",
        "evidon.com", "ensighten.com", "eyeota.com", "admitad.com", "tradetracker.net",
        "tradedoubler.com", "tradeadexchange.com", "trafficjunky.com",
        "effirst.com", "ipredictive.com", "mandiant.com", "media01.com",
        "tag.1rx.io", "tagsrvr.com", "rfihub.com", "rfihub.net",
        "retargeter.com", "revcontent.com", "revjet.com", "revjets.com",
        "revresponse.com", "rightaction.com", "rlcdn.com", "rmcp.net",
        "roia.biz", "rubiconproject.com", "safehaven.com", "salesviewer.com",
        "sascdn.com", "saymedia.com", "sekindo.com", "semantiqo.com",
        "serverbid.com", "servedby.com", "shareaholic.com", "sharethrough.com",
        "shopzilla.com", "simpli.fi", "sitescout.com", "skimresources.com",
        "smartadserver.com", "smtrm.net", "sonobi.com", "sortable.com",
        "spotx.com", "spotxchange.com", "spotx.tv", "statcounter.com",
        "steelhouse.com", "steelhousemedia.com", "streamads.com", "sumome.com",
        "superads.com", "sxp.com", "sync1.com", "taboola.com",
        "taboolasyndication.com", "tapad.com", "tapgage.com", "tapit.com",
        "teadma.com", "teads.tv", "telemetree.com", "telerama.com",
        "temel.com", "tpc.googlesyndication.com", "tpgs.com", "tpid.com",
        "trackalyzer.com", "trackcmp.com", "trackers.com", "tracking.com",
        "tradeadexchange.com", "tradedoubler.com", "tradetracker.net",
        "trafficmanager.com", "trafficjunky.com", "trf.com", "tribalfusion.com",
        "triggit.com", "truefit.com", "truelead.com", "tubemogul.com",
        "turn.com", "tynt.com", "unrulymedia.com", "upscore.com",
        "userlytics.com", "viglink.com", "viralmedia.com", "visiblemeasures.com",
        "vmmapi.com", "voicefive.com", "vpon.com", "web-ads.com",
        "webtelemetry.com", "wtatistics.com", "xplosion.de", "yieldads.com",
        "yieldbuild.com", "yieldlab.net", "yieldmo.com", "yieldoptimizer.com",
        "yldbt.com", "zanox.com", "zdbb.net", "zedo.com",
    )

    private val blockedPaths = listOf(
        "/ads", "/ads/", "/adserver", "/adserver/", "/advert", "/advertis",
        "/banner", "/bannerads", "/popup", "/popunder", "/prebid", "/adsystem",
        "/tracker", "/tracker/", "/tracking", "/tracking/", "/track.gif",
        "/track.js", "/beacon", "/beacon/", "/beacon.gif", "/beacon.js",
        "/analytics", "/analytics.js", "/analytics/", "/gampad", "/gampad/",
        "/doubleclick", "/gtm.js", "/gtm/", "/tagmanager", "/tealium",
        "/scorecard", "/quantserve", "/chartbeat", "/pixel.gif", "/pixel.js",
        "/log.gif", "/log.js", "/__log", "/event.gif", "/event.js", "/collect",
        "/adsense", "/adsystem", "/adview", "/adclick", "/adcall",
        "/adrender", "/adserve", "/adspaces", "/bouncer", "/sync",
        "/redirect", "/impression", "/track/", "/track?", "/stats",
        "/metrics", "/telemetry", "/sentry", "/amplitude", "/heap",
        "/mouseflow", "/criteo", "/pubmatic", "/rubicon", "/openx",
        "/bidrequest", "/adnxs", "/moat", "/hotjar", "/clarity",
        "/pixel", "/ping", "/visit", "/visit/", "/adsbygoogle",
    )

    private val blockedFileExt = setOf(
        "ads.js", "ad.js", "adsense.js", "adsbygoogle.js", "analytics.js",
        "tracker.js", "track.js", "beacon.js", "gtm.js", "tag.js",
        "gpt.js", "prebid.js", "adserver.js", "popads.js", "popunder.js",
        "fingerprint.js", "fp.js", "chartbeat.js", "hotjar.js",
        "mixpanel.js", "segment.js", "amplitude.js", "heap.js",
    )

    private val thirdPartyAdDomains = setOf(
        "admob.com", "admob.org", "adsense.com", "doubleclick.com",
        "doubleclick.net", "googleadservices.com", "googlesyndication.com",
        "googletagservices.com", "adnxs.com", "criteo.com", "pubmatic.com",
        "rubiconproject.com", "openx.net", "casalemedia.com", "smartadserver.com",
        "adform.net", "yieldlab.net", "exoclick.com", "propellerads.com",
        "popads.net", "adcash.com", "adsterra.com", "media.net",
        "taboola.com", "outbrain.com", "revcontent.com", "mgid.com",
        "contentads.com", "zedo.com", "adtech.de", "gumgum.com",
    )

    val adsBlocked = AtomicLong(0)
    val trackersBlocked = AtomicLong(0)
    val cosmeticHidden = AtomicLong(0)

    fun shouldBlock(url: String?): BlockResult {
        if (url == null) return BlockResult.Allow
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return BlockResult.Allow
        val host = uri.host ?: return BlockResult.Allow
        val path = uri.path ?: ""

        if (host in adHosts || adHosts.any { host.endsWith(".$it") }) {
            if (Prefs.blockAds) { adsBlocked.incrementAndGet(); return BlockResult.BlockAd }
        }
        if (host in trackerHosts || trackerHosts.any { host.endsWith(".$it") }) {
            if (Prefs.blockTrackers) { trackersBlocked.incrementAndGet(); return BlockResult.BlockTracker }
        }
        if (Prefs.blockAds && path.isNotEmpty()) {
            for (p in blockedPaths) {
                if (path.startsWith(p)) { adsBlocked.incrementAndGet(); return BlockResult.BlockAd }
            }
        }
        if (Prefs.blockTrackers) {
            val seg = path.substringAfterLast('/', "")
            if (seg in blockedFileExt) { trackersBlocked.incrementAndGet(); return BlockResult.BlockTracker }
        }
        return BlockResult.Allow
    }

    fun shouldUpgradeToHttps(url: String?): Boolean {
        if (url == null) return false
        if (!url.startsWith("http://")) return false
        return Prefs.blockAds
    }

    enum class BlockResult { Allow, BlockAd, BlockTracker }

    fun stats(): Triple<Long, Long, Long> = Triple(adsBlocked.get(), trackersBlocked.get(), cosmeticHidden.get())

    fun resetStats() {
        adsBlocked.set(0)
        trackersBlocked.set(0)
        cosmeticHidden.set(0)
    }
}

object CosmeticFilters {
    val cssSelectors = listOf(
        ".ad", ".ads", ".ad-container", ".ad-banner", ".ad-block", ".ad-box",
        ".ad-content", ".ad-holder", ".ad-item", ".ad-label", ".ad-placeholder",
        ".ad-section", ".ad-slot", ".ad-space", ".ad-unit", ".ad-wrapper",
        ".ad-zone", ".ad160", ".ad300", ".ad728", ".ad90", ".ad970",
        ".adsbygoogle", ".ad-banner", ".ad-leaderboard", ".ad-skyscraper",
        ".ad-rectangle", ".ad-square", ".ad-half", ".ad-full", ".ad-wide",
        ".ads-container", ".ads-area", ".ads-box", ".ads-wrap",
        ".adsense", ".adsbygoogle", ".adchoices", ".adchoice",
        ".banner-ads", ".banner-ad", ".banner-ads-container",
        ".bottom-ad", ".bottom-ads", ".bottom-banner",
        ".top-ad", ".top-ads", ".top-banner",
        ".side-ad", ".side-ads", ".sidebar-ad", ".sidebar-ads",
        ".header-ad", ".header-ads", ".header-banner",
        ".footer-ad", ".footer-ads", ".footer-banner",
        ".in-content-ad", ".in-content-ads",
        ".native-ad", ".native-ads",
        ".sponsored", ".sponsored-content", ".sponsored-ad",
        ".sponsored-link", ".sponsored-section",
        ".promo", ".promo-box", ".promo-banner",
        ".outbrain", ".taboola", ".taboola-container", ".taboola-widget",
        ".recommendations", ".recommendations-widget",
        ".doubleclick", ".dfp-ad", ".dfp-ad-slot", ".dfp-ad-unit",
        ".gpt-ad", ".gpt-ad-slot", ".google-ad", ".google-ads",
        "#ad", "#ads", "#ad-container", "#ad-banner", "#ad-block",
        "#ad-box", "#ad-content", "#ad-holder", "#ad-slot",
        "#ad-space", "#ad-wrapper", "#ad-zone",
        "#adsbygoogle", "#adsense", "#adchoices",
        "#banner-ad", "#banner-ads", "#bottom-ad", "#top-ad",
        "#side-ad", "#sidebar-ad", "#header-ad", "#footer-ad",
        "#sponsored", "#promo", "#outbrain", "#taboola",
        "#dfp-ad", "#google-ad", "#google-ads",
        "div[class*='advert']", "div[class*='ad-']", "div[class*='-ad']",
        "div[class*='banner-ad']", "div[class*='sponsored']",
        "div[id*='advert']", "div[id*='ad-']", "div[id*='-ad']",
        "div[id*='banner-ad']", "div[id*='sponsored']",
        "iframe[src*='doubleclick']", "iframe[src*='adservice']",
        "iframe[src*='googlesyndication']", "iframe[src*='amazon-adsystem']",
        "iframe[src*='adnxs']", "iframe[src*='taboola']",
        "iframe[src*='outbrain']", "iframe[src*='adsterra']",
        "ins.adsbygoogle", "ins.ads", "ins.ad",
        "script[src*='doubleclick']", "script[src*='adservice']",
        "script[src*='googlesyndication']", "script[src*='googleadservices']",
        "script[src*='googletagmanager']", "script[src*='google-analytics']",
        "script[src*='connect.facebook.net']", "script[src*='analytics']",
        "script[src*='adnxs']", "script[src*='criteo']",
        "script[src*='pubmatic']", "script[src*='rubiconproject']",
        "script[src*='casalemedia']", "script[src*='smartadserver']",
        "script[src*='taboola']", "script[src*='outbrain']",
        "script[src*='hotjar']", "script[src*='mixpanel']",
        "script[src*='segment.io']", "script[src*='amplitude']",
        "script[src*='chartbeat']", "script[src*='quantserve']",
        "script[src*='scorecardresearch']", "script[src*='clarity.ms']",
        "[class*='ad-banner']", "[class*='ad-container']",
        "[class*='ad-slot']", "[class*='ad-unit']",
        "[class*='ad-wrapper']", "[class*='ad-zone']",
        "[class*='ads-container']", "[class*='ads-area']",
        "[class*='sponsored']", "[class*='promo-ad']",
        "[id*='ad-banner']", "[id*='ad-container']",
        "[id*='ad-slot']", "[id*='ad-unit']",
        "[id*='ad-wrapper']", "[id*='ad-zone']",
        "[id*='ads-container']", "[id*='ads-area']",
        "[id*='sponsored']", "[id*='promo-ad']",
    )

    fun cssHideRules(): String {
        return cssSelectors.joinToString(",") + "{display:none!important;visibility:hidden!important;height:0!important;width:0!important;opacity:0!important;}"
    }

    fun cosmeticJs(): String {
        val selectors = cssSelectors.map { it.replace("\"", "\\\"") }
        return """
            (function(){
                var s=document.createElement('style');
                s.id='__lumen_cosmetic';
                s.textContent=${"\"\"\""}${cssHideRules()}${"\"\"\""};
                document.head.appendChild(s);
                var observer=new MutationObserver(function(){
                    document.querySelectorAll('${cssSelectors.joinToString(",")}').forEach(function(el){
                        if(getComputedStyle(el).display!=='none'){
                            el.style.display='none';
                        }
                    });
                });
                observer.observe(document.body||document.documentElement,{childList:true,subtree:true});
            })();
        """.trimIndent()
    }
}

object AntiFingerprint {
    fun js(): String {
        return """
            (function(){
                if(window.__lumen_antifp)return;
                window.__lumen_antifp=true;
                try{
                    var origGetParameter=CanvasRenderingContext2D.prototype.getParameter;
                    CanvasRenderingContext2D.prototype.getParameter=function(p){
                        if(p==='UNMASKED_VENDOR_WEBGL')return 'Google Inc.';
                        if(p==='UNMASKED_RENDERER_WEBGL')return 'ANGLE (Intel)';
                        return origGetParameter.call(this,p);
                    };
                    var origToDataURL=HTMLCanvasElement.prototype.toDataURL;
                    HTMLCanvasElement.prototype.toDataURL=function(){
                        var ctx=this.getContext('2d');
                        if(ctx){
                            ctx.fillStyle='rgba(0,0,0,0.01)';
                            ctx.fillRect(0,0,1,1);
                        }
                        return origToDataURL.apply(this,arguments);
                    };
                    try{Object.defineProperty(navigator,'hardwareConcurrency',{get:function(){return 4;}});}catch(e){}
                    try{Object.defineProperty(navigator,'deviceMemory',{get:function(){return 4;}});}catch(e){}
                    try{Object.defineProperty(navigator,'platform',{get:function(){return 'Linux x86_64';}});}catch(e){}
                    try{Object.defineProperty(navigator,'languages',{get:function(){return ['en-US','en'];}});}catch(e){}
                    try{
                        var plugins=navigator.plugins;
                        Object.defineProperty(navigator,'plugins',{get:function(){return [];}});
                    }catch(e){}
                    try{
                        var origGetBattery=navigator.getBattery;
                        if(origGetBattery){
                            navigator.getBattery=function(){return Promise.resolve({charging:true,chargingTime:0,dischargingTime:Infinity,level:1});};
                        }
                    }catch(e){}
                }catch(e){}
            })();
        """.trimIndent()
    }
}
