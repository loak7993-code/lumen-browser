package com.lwbrowser

import android.net.Uri
import java.util.concurrent.atomic.AtomicLong

object AdBlocker {

    private val adHosts = setOf(
        "doubleclick.net", "googleadservices.com", "googlesyndication.com",
        "pagead2.googlesyndication.com", "googletagservices.com", "adservice.google.com",
        "ads.google.com", "admob.com", "adsystem.com", "amazon-adsystem.com",
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
        "quantserve.com", "scorecardresearch.com"
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
        "snowplowanalytics.com", "piwik.org", "matomo.org", "fingerprintjs.com",
        "fpjs.io", "deviceatlas.com", "wurfl.io", "iovation.com", "threatmetrix.com",
        "recaptcha.net", "g.doubleclick.net", "stats.g.doubleclick.net", "snap.licdn.com",
        "px.ads.linkedin.com", "ads.linkedin.com", "ads.pinterest.com", "tags.tiqcdn.com",
        "ct.pinterest.com", "analytics.tiktok.com", "analytics.snapchat.com",
        "sc-static.net", "bat.bing.com", "xandr.com", "appnexus.com"
    )

    private val blockedPaths = listOf(
        "/ads", "/ads/", "/adserver", "/adserver/", "/advert", "/advertis",
        "/banner", "/bannerads", "/popup", "/popunder", "/prebid", "/adsystem",
        "/tracker", "/tracker/", "/tracking", "/tracking/", "/track.gif",
        "/track.js", "/beacon", "/beacon/", "/beacon.gif", "/beacon.js",
        "/analytics", "/analytics.js", "/analytics/", "/gampad", "/gampad/",
        "/doubleclick", "/gtm.js", "/gtm/", "/tagmanager", "/tealium",
        "/scorecard", "/quantserve", "/chartbeat", "/pixel.gif", "/pixel.js",
        "/log.gif", "/log.js", "/__log", "/event.gif", "/event.js", "/collect"
    )

    private val blockedFileExt = setOf(
        "ads.js", "ad.js", "adsense.js", "adsbygoogle.js", "analytics.js",
        "tracker.js", "track.js", "beacon.js", "gtm.js", "tag.js"
    )

    val adsBlocked = AtomicLong(0)
    val trackersBlocked = AtomicLong(0)

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

    enum class BlockResult { Allow, BlockAd, BlockTracker }

    fun stats(): Pair<Long, Long> = adsBlocked.get() to trackersBlocked.get()

    fun resetStats() {
        adsBlocked.set(0)
        trackersBlocked.set(0)
    }
}
