package com.lwbrowser

import android.net.Uri
import java.util.concurrent.atomic.AtomicLong

object AdBlocker {

    private val adHosts = setOf(
        "2mdn.net", "aax-us-east.amazon-adsystem.com", "aax.amazon-adsystem.com",
        "adadvisor.net", "adap.tv", "adbmedia.com", "adbrite.com", "adbutler.com",
        "adcash.com", "adclix.com", "adcolony.com", "addtot.com", "addynamo.com",
        "adform.net", "adgibbo.com", "adhigh.net", "adk2.com", "adkernel.com",
        "admize.com", "admixer.com", "admixer.co", "admixer.net", "admob.com",
        "admob.org", "admobie.com", "adnxs.com", "adocean.com", "adonize.com",
        "adpath.com", "adreactor.com", "adroll.com", "ads.google.com", "ads.youtube.com",
        "ads.reddit.com", "ads.tiktok.com", "ads.snapchat.com", "ads.pinterest.com",
        "ads.twitter.com", "ads.linkedin.com", "ads.instagram.com", "ads.facebook.com",
        "ads.ebay.com", "ads.craigslist.org", "adsense.com", "adservice.com",
        "adservice.google.com", "adservice.google.de", "adservice.google.co.uk",
        "adsfactor.com", "adsfactor.net", "adsfac.eu", "adsfac.net", "adsfac.us",
        "adspeed.com", "adsrvr.org", "adsterra.com", "adstogo.com", "adsupply.com",
        "adsystem.com", "adtaily.com", "adtech.com", "adtech.com.au", "adtech.de",
        "adtech.net", "adtegrity.com", "adversal.com", "adverserve.com",
        "adview", "adworldnetwork.com", "adyard.com", "adyoulike.com",
        "adzerk.com", "amazon-adsystem.com", "aniview.com", "applovin.com",
        "awempire.com", "buysellads.com", "casalemedia.com", "chartboost.com",
        "clicksor.com", "connexity.com", "contentabc.com", "creativecdn.com",
        "criteo.com", "criteo.net", "digitaldsp.com", "divinity.com",
        "doubleclick.com", "doubleclick.net", "doubleverify.com",
        "dpbolvw.net", "engageya.com", "epom.com", "exoclick.com",
        "freewheel.com", "fundingchoicesmessages.google.com", "fwmrm.net",
        "gemius.pl", "googleadservices.com", "googlesyndication.com",
        "googletagservices.com", "gumgum.com", "hilltopads.com",
        "infolinks.com", "inneractive.com", "inmobi.com", "inskin.com",
        "inskinmedia.com", "intergi.com", "kontera.com", "lockerdome.com",
        "mantisadnetwork.com", "media.net", "mediacom.com", "mediavine.com",
        "mgid.com", "minutemedia.com", "minutemedia-prebid.com",
        "mobileadvertising.com", "mobileads.com", "mopub.com",
        "ntv.io", "outbrain.com", "pagead2.googlesyndication.com",
        "plista.com", "popads.net", "popcash.net", "propellerads.com",
        "pubmatic.com", "quantserve.com", "revcontent.com", "rollup.io",
        "ru4.com", "rubiconproject.com", "scorecardresearch.com",
        "serving-sys.com", "smartadserver.com", "startapp.com",
        "taboola.com", "tsyndicate.com", "tribalfusion.com", "tremorhub.com",
        "undertone.com", "unityads.unity3d.com", "viglink.com", "vibrantmedia.com",
        "vungle.com", "ybrantdigital.com", "yldistrtict.com", "yllix.com",
        "yieldlab.net", "yieldmo.com", "yoc.com", "yoban.com",
        "yumenetwork.com", "ybp.yume.com", "zdbb.net", "zedo.com",
        "zemanta.com", "advertising.com", "2mdn.net", "adserver.yahoo.com",
        "adserver.yadro.com", "advmaker.ru", "advmaker.com",
    )

    private val trackerHosts = setOf(
        "ad.doubleclick.net", "adjust.com", "admitad.com", "ads.linkedin.com",
        "ads.pinterest.com", "amplitude.com", "analytics.example.com",
        "analytics.google.com", "analytics.snapchat.com", "analytics.tiktok.com",
        "analytics.twitter.com", "api.facebook.com", "api.mparticle.com",
        "app.link", "appnexus.com", "apps.facebook.com", "appsflyer.com",
        "bat.bing.com", "bluekai.com", "branch.io", "browser-intake-datadoghq.com",
        "cdn.o2.ingest.sentry.io", "cdn.permutive.com", "clarity.ms",
        "clicktale.net", "comscore.com", "config.gtm-360.com",
        "connect.facebook.net", "conversion.com", "crazyegg.com",
        "ct.pinterest.com", "datafastguru.io", "demdex.net",
        "deviceatlas.com", "dynamicyield.com", "ensighten.com",
        "evergage.com", "everestjs.net", "everesttech.net",
        "evidon.com", "eyeota.com", "fingerprintjs.com", "fls.doubleclick.net",
        "fpjs.io", "fullstory.com", "g.doubleclick.net", "google-analytics.com",
        "googletagmanager.com", "graph.facebook.com", "heap.io",
        "hh-promo.com", "hotjar.com", "intercom.io", "intercomcdn.com",
        "iovation.com", "ip-api.com", "ipify.org", "ipinfo.io",
        "ipredictive.com", "kissmetrics.com", "kochava.com", "logrocket.com",
        "luckyorange.com", "mouseflow.com", "mixpanel.com", "mparticle.com",
        "matomo.org", "matomo.cloud", "mobileapptracking.com", "newrelic.com",
        "new-relic.com", "nr-data.net", "omtrdc.net", "optimizely.com",
        "o1.ingest.sentry.io", "o2.ingest.sentry.io", "parsely.com",
        "permutive.com", "permutive.app", "pixel.facebook.com",
        "piwik.org", "ppctracking.com", "px.ads.linkedin.com",
        "quantcast.com", "quantserve.com", "retargeter.com", "retargeting.com",
        "rfihub.com", "rfihub.net", "rlcdn.com", "rumcdn.com",
        "salesviewer.com", "sb.scorecardresearch.com", "sc-static.net",
        "segment.com", "segment.io", "sentry.io", "sessioncam.com",
        "shareaholic.com", "sharethrough.com", "sitescout.com",
        "skimresources.com", "smartlook.com", "snap.licdn.com",
        "snowplowanalytics.com", "sonobi.com", "sortable.com",
        "spotx.com", "spotxchange.com", "spotx.tv", "ssl.google-analytics.com",
        "statcounter.com", "stats.g.doubleclick.net", "steelhouse.com",
        "steelhousemedia.com", "streamads.com", "sumome.com", "superads.com",
        "sxp.com", "sync1.com", "t.co", "t.datadog.com", "t.datadoghq.com",
        "taboolasyndication.com", "tag.1rx.io", "tagmanager.google.com",
        "tapad.com", "tapgage.com", "tapit.com", "teads.tv", "teadma.com",
        "tealium.com", "tealiumiq.com", "telemetree.com", "temel.com",
        "threatmetrix.com", "tpc.googlesyndication.com", "tpgs.com", "tpid.com",
        "trackcmp.com", "trackers.com", "tracking.com", "tradeadexchange.com",
        "tradedoubler.com", "tradetracker.net", "trafficjunky.com",
        "trafficmanager.com", "trf.com", "tribalfusion.com", "triggit.com",
        "truefit.com", "truelead.com", "tubemogul.com", "turn.com",
        "tynt.com", "unrulymedia.com", "upscore.com", "userlytics.com",
        "userreplay.com", "viglink.com", "viralmedia.com", "visiblemeasures.com",
        "vmmapi.com", "voicefive.com", "vpon.com", "walkme.com",
        "web-ads.com", "webtelemetry.com", "wtatistics.com",
        "www.googletagmanager.com", "www.google-analytics.com",
        "xplosion.de", "yieldads.com", "yieldbuild.com", "yieldoptimizer.com",
        "yldbt.com", "zanox.com", "tags.tiqcdn.com",
    )

    private val blockedPaths = listOf(
        "/ads", "/ads/", "/adserver", "/adserver/", "/advert", "/advertis",
        "/banner", "/bannerads", "/popup", "/popunder", "/prebid", "/adsystem",
        "/tracker", "/tracker/", "/tracking", "/tracking/", "/track.gif",
        "/track.js", "/beacon", "/beacon/", "/beacon.gif", "/beacon.js",
        "/analytics.js", "/analytics/", "/gampad", "/gampad/",
        "/doubleclick", "/gtm.js", "/gtm/", "/tagmanager", "/tealium",
        "/scorecard", "/quantserve", "/chartbeat", "/pixel.gif", "/pixel.js",
        "/log.gif", "/log.js", "/__log", "/event.gif", "/event.js",
        "/adsense", "/adview", "/adclick", "/adcall",
        "/adrender", "/adserve", "/adspaces", "/bouncer",
        "/impression", "/track/", "/track?",
        "/sentry", "/amplitude", "/heap",
        "/mouseflow", "/criteo", "/pubmatic", "/rubicon", "/openx",
        "/bidrequest", "/adnxs", "/moat", "/hotjar", "/clarity",
        "/adsbygoogle",
    )

    private val blockedFileExt = setOf(
        "ads.js", "ad.js", "adsense.js", "adsbygoogle.js", "analytics.js",
        "tracker.js", "track.js", "beacon.js", "gtm.js", "tag.js",
        "gpt.js", "prebid.js", "adserver.js", "popads.js", "popunder.js",
        "fingerprint.js", "fp.js", "chartbeat.js", "hotjar.js",
        "mixpanel.js", "segment.js", "amplitude.js", "heap.js",
    )

    private val allowlistHosts = setOf(
        "startpage.com", "www.startpage.com", "startpage.com.",
        "duckduckgo.com", "www.duckduckgo.com",
        "google.com", "www.google.com",
        "bing.com", "www.bing.com",
        "hcaptcha.com", "www.hcaptcha.com", "newassets.hcaptcha.com",
        "recaptcha.net", "www.recaptcha.net", "recaptcha.google.com",
        "www.gstatic.com", "gstatic.com",
        "challenges.cloudflare.com",
    )

    val adsBlocked = AtomicLong(0)
    val trackersBlocked = AtomicLong(0)
    val cosmeticHidden = AtomicLong(0)

    fun shouldBlock(url: String?): BlockResult {
        if (url == null) return BlockResult.Allow
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return BlockResult.Allow
        val host = uri.host ?: return BlockResult.Allow
        val path = uri.path ?: ""

        // H8: check ad/tracker hosts BEFORE the allowlist so ads.google.com etc.
        // are blocked even though bare "google.com" is allowlisted. The allowlist
        // suffix-match (host.endsWith(".google.com")) was letting every Google
        // ad/tracker subdomain through.
        if (host in adHosts || adHosts.any { host.endsWith(".$it") }) {
            if (Prefs.blockAds) { adsBlocked.incrementAndGet(); return BlockResult.BlockAd }
        }
        if (host in trackerHosts || trackerHosts.any { host.endsWith(".$it") }) {
            if (Prefs.blockTrackers) { trackersBlocked.incrementAndGet(); return BlockResult.BlockTracker }
        }
        // Allowlist only applies once we know it's not an ad/tracker host.
        if (host in allowlistHosts || allowlistHosts.any { host.endsWith(".$it") }) {
            return BlockResult.Allow
        }
        if (Prefs.blockAds && path.isNotEmpty()) {
            // C1: exact-segment match, not bare startsWith, so "/collect" doesn't
            // blank "/collections/all" (Shopify), "/visit" doesn't blank "/visit-us", etc.
            for (p in blockedPaths) {
                val pp = if (p.endsWith("/")) p else "$p/"
                if (path == p || path.startsWith(pp)) {
                    adsBlocked.incrementAndGet()
                    return BlockResult.BlockAd
                }
            }
        }
        if (Prefs.blockTrackers) {
            val seg = path.substringAfterLast('/', "")
            if (seg in blockedFileExt) { trackersBlocked.incrementAndGet(); return BlockResult.BlockTracker }
        }
        return BlockResult.Allow
    }

    fun incrementCosmetic() {
        cosmeticHidden.incrementAndGet()
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
        ".adsbygoogle", ".ad-leaderboard", ".ad-skyscraper",
        ".ad-rectangle", ".ad-square", ".ad-half", ".ad-full", ".ad-wide",
        ".ads-container", ".ads-area", ".ads-box", ".ads-wrap",
        ".adsense", ".adchoices", ".adchoice",
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
                    // H10: do NOT mutate the canvas before toDataURL — the fillRect
                    // corrupted every canvas export (QR codes, signature pads, chart
                    // PNG downloads, meme generators, image croppers). Only spoof the
                    // WebGL vendor/renderer; leave 2D canvas output untouched.
                }catch(e){}
            })();
        """.trimIndent()
    }
}
