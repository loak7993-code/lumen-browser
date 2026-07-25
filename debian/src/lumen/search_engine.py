import json
import urllib.parse
import urllib.request
import re
import threading
from html import unescape

DDG_URL = "https://html.duckduckgo.com/html/?q={}"
BING_URL = "https://www.bing.com/search?q={}&count=30&setlang=en"
WIKI_URL = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch={}&format=json&srlimit=8&utf8=1"
BING_IMG_URL = "https://www.bing.com/images/search?q={}&form=HDRSC2&first=1&count=30"
BING_VID_URL = "https://www.bing.com/videos/search?q={}&form=HDRSC3&first=1&count=20"
NEWS_URL = "https://news.google.com/rss/search?q={}&hl=en-US&gl=US&ceid=US:en"
SUGGEST_URL = "https://duckduckgo.com/ac/?q={}&type=list"

UA = "Mozilla/5.0 (X11; Linux x86_64; rv:121.0) Gecko/20100101 Firefox/121.0 Lumen/1.0"


def _fetch(url):
    req = urllib.request.Request(url, headers={
        "User-Agent": UA,
        "Accept": "text/html,application/xhtml+xml,application/json,application/rss+xml",
        "Accept-Language": "en-US,en;q=0.9",
    })
    with urllib.request.urlopen(req, timeout=10) as resp:
        return resp.read().decode("utf-8", errors="replace")


def _strip_tags(html):
    text = re.sub(r"<[^>]*>", "", html)
    return unescape(text).strip()


def _host(url):
    try:
        return urllib.parse.urlparse(url).hostname or url
    except Exception:
        return url


def _decode_ddg_url(href):
    parsed = urllib.parse.urlparse(href)
    params = urllib.parse.parse_qs(parsed.query)
    if "uddg" in params:
        return urllib.parse.unquote(params["uddg"][0])
    return href


def _normalize(url):
    n = url.replace("http://", "https://").replace("https://www.", "https://")
    if n.endswith("/"):
        n = n[:-1]
    return n.split("#")[0].lower()


def _parse_ddg(html):
    results = []
    titles = re.findall(r'<a[^>]*class="[^"]*result__a[^"]*"[^>]*href="([^"]*)"[^>]*>(.*?)</a>', html, re.DOTALL)
    snippets = re.findall(r'<a[^>]*class="[^"]*result__snippet[^"]*"[^>]*>(.*?)</a>', html, re.DOTALL)
    for i in range(min(len(titles), len(snippets))):
        url = _decode_ddg_url(titles[i][0])
        title = _strip_tags(titles[i][1])
        snippet = _strip_tags(snippets[i])
        if title and url.startswith("http"):
            results.append({"title": title, "url": url, "display": _host(url), "snippet": snippet, "source": "ddg"})
    return results


def _parse_bing(html):
    results = []
    for m in re.finditer(r'<li class="b_algo">(.*?)</li>', html, re.DOTALL):
        block = m.group(1)
        link = re.search(r'<a[^>]*href="(https?://[^"]*)"[^>]*>(.*?)</a>', block, re.DOTALL)
        if not link:
            continue
        url = link.group(1)
        title = _strip_tags(link.group(2))
        snip_m = re.search(r"<p[^>]*>(.*?)</p>", block, re.DOTALL)
        snippet = _strip_tags(snip_m.group(1)) if snip_m else ""
        if title and url.startswith("http"):
            results.append({"title": title, "url": url, "display": _host(url), "snippet": snippet, "source": "bing"})
    return results


def _parse_wiki(json_text):
    results = []
    for m in re.finditer(r'\{"title":"([^"]+)","pageid":\d+,"snippet":"(.*?)"\}', json_text, re.DOTALL):
        title = unescape(m.group(1))
        snippet = _strip_tags(unescape(m.group(2)))
        url = "https://en.wikipedia.org/wiki/" + urllib.parse.quote(title.replace(" ", "_"))
        results.append({"title": title, "url": url, "display": "wikipedia.org", "snippet": snippet, "source": "wiki"})
    return results


def _extract_json_field(json_str, field):
    m = re.search(r'"' + field + r'"\s*:\s*"((?:[^"\\]|\\.)*)"', json_str)
    if m:
        return m.group(1).replace("\\/", "/").replace('\\"', '"')
    return None


def _parse_bing_images(html):
    results = []
    for m in re.finditer(r'm="(\{[^"]*\})"', html, re.DOTALL):
        raw = unescape(m.group(1))
        img_url = _extract_json_field(raw, "murl")
        thumb = _extract_json_field(raw, "turl") or ""
        title = _extract_json_field(raw, "t") or ""
        if img_url and img_url.startswith("http"):
            results.append({"title": title, "url": img_url, "display": _host(img_url), "snippet": "", "source": "bing", "thumbnail": thumb})
    return results


def _parse_bing_videos(html):
    results = []
    for m in re.finditer(r'm="(\{[^"]*\})"', html, re.DOTALL):
        raw = unescape(m.group(1))
        vid_url = _extract_json_field(raw, "murl")
        thumb = _extract_json_field(raw, "smturl") or _extract_json_field(raw, "turl") or ""
        title = _extract_json_field(raw, "t") or ""
        if vid_url and vid_url.startswith("http"):
            results.append({"title": title, "url": vid_url, "display": _host(vid_url), "snippet": "", "source": "bing", "thumbnail": thumb})
    return results


def _parse_news(xml):
    results = []
    for m in re.finditer(r"<item>.*?<title>(.*?)</title>.*?<link>(.*?)</link>.*?<pubDate>(.*?)</pubDate>.*?<source[^>]*>(.*?)</source>.*?</item>", xml, re.DOTALL):
        title = _strip_tags(m.group(1))
        url = m.group(2).strip()
        date = m.group(3).strip()
        source = _strip_tags(m.group(4)) or "news"
        if title and url:
            results.append({"title": title, "url": url, "display": source, "snippet": date, "source": "news"})
    return results


def _parse_related(html):
    related = []
    for m in re.finditer(r'<a[^>]*class="[^"]*related-search[^"]*"[^>]*>(.*?)</a>', html, re.DOTALL):
        text = _strip_tags(m.group(1))
        if text:
            related.append(text)
    return related


def _parse_suggestions(raw):
    items = []
    for m in re.finditer(r'"([^"]+)"', raw):
        v = m.group(1)
        if v and v != "[" and v != "]" and len(v) < 100:
            items.append(v)
    seen = set()
    unique = []
    for i in items:
        if i not in seen:
            seen.add(i)
            unique.append(i)
    return unique[:8]


def _aggregate(all_results):
    by_url = {}
    for r in all_results:
        key = _normalize(r["url"])
        if key not in by_url:
            by_url[key] = dict(r, url=key, score=0.0)
        else:
            by_url[key]["score"] += 0.5
            if not by_url[key].get("snippet") and r.get("snippet"):
                by_url[key]["snippet"] = r["snippet"]
    for r in by_url.values():
        sw = {"ddg": 1.0, "bing": 0.9, "wiki": 0.7, "news": 0.6}.get(r["source"], 0.5)
        r["score"] += sw
        if "wikipedia.org" in r["url"]:
            r["score"] += 0.3
        if r["url"].startswith("https://"):
            r["score"] += 0.1
        if r.get("snippet"):
            r["score"] += 0.1
    return sorted(by_url.values(), key=lambda x: x["score"], reverse=True)


def _favicon(url):
    return "https://icons.duckduckgo.com/ip3/" + _host(url) + ".ico"


def search_web(query, callback):
    def worker():
        try:
            ddg, bing, wiki, related = [], [], [], []
            t1 = threading.Thread(target=lambda: (ddg.extend(_parse_ddg(_fetch(DDG_URL.format(urllib.parse.quote(query))))), related.extend(_parse_related(_fetch(DDG_URL.format(urllib.parse.quote(query)))))))
            t2 = threading.Thread(target=lambda: bing.extend(_parse_bing(_fetch(BING_URL.format(urllib.parse.quote(query))))))
            t3 = threading.Thread(target=lambda: wiki.extend(_parse_wiki(_fetch(WIKI_URL.format(urllib.parse.quote(query))))))
            t1.start(); t2.start(); t3.start()
            t1.join(10); t2.join(10); t3.join(10)
            all_r = ddg + bing + wiki
            ranked = _aggregate(all_r)
            for r in ranked:
                r["favicon"] = _favicon(r["url"])
            callback({"query": query, "results": ranked, "related": related, "error": None})
        except Exception as e:
            callback({"query": query, "results": [], "related": [], "error": str(e)})
    threading.Thread(target=worker, daemon=True).start()


def search_images(query, callback):
    def worker():
        try:
            html = _fetch(BING_IMG_URL.format(urllib.parse.quote(query)))
            results = _parse_bing_images(html)
            callback({"query": query, "results": results, "related": [], "error": None})
        except Exception as e:
            callback({"query": query, "results": [], "related": [], "error": str(e)})
    threading.Thread(target=worker, daemon=True).start()


def search_videos(query, callback):
    def worker():
        try:
            html = _fetch(BING_VID_URL.format(urllib.parse.quote(query)))
            results = _parse_bing_videos(html)
            callback({"query": query, "results": results, "related": [], "error": None})
        except Exception as e:
            callback({"query": query, "results": [], "related": [], "error": str(e)})
    threading.Thread(target=worker, daemon=True).start()


def search_news(query, callback):
    def worker():
        try:
            xml = _fetch(NEWS_URL.format(urllib.parse.quote(query)))
            results = _parse_news(xml)
            callback({"query": query, "results": results, "related": [], "error": None})
        except Exception as e:
            callback({"query": query, "results": [], "related": [], "error": str(e)})
    threading.Thread(target=worker, daemon=True).start()


def get_suggestions(query, callback):
    def worker():
        try:
            raw = _fetch(SUGGEST_URL.format(urllib.parse.quote(query)))
            items = _parse_suggestions(raw)
            callback({"query": query, "suggestions": items})
        except Exception:
            callback({"query": query, "suggestions": []})
    threading.Thread(target=worker, daemon=True).start()
