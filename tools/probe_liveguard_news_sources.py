#!/usr/bin/env python3
"""Optional network monitoring, never a release gate or proof of on-phone news delivery.

No credentials, user data or article scraping. Public RSS only; persist counts/status,
not articles. Run outside the repository if you do not want the JSON output here.
"""
import concurrent.futures
import datetime as dt
import email.utils
import json
import os
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

SOURCES = {
    "BBC Tamil": "https://feeds.bbci.co.uk/tamil/rss.xml",
    "News18 Tamil Nadu": "https://tamil.news18.com/commonfeeds/v1/tam/rss/tamil-nadu.xml",
    "News18 India Tamil": "https://tamil.news18.com/commonfeeds/v1/tam/rss/national.xml",
    "Oneindia World Tamil": "https://tamil.oneindia.com/rss/feeds/tamil-international-fb.xml",
    "Oneindia Tamil Technology": "https://tamil.oneindia.com/rss/feeds/tamil-technology-fb.xml",
    "Tamil Gizbot Science": "https://tamil.gizbot.com/rss/feeds/tamil-scitech-fb.xml",
    "The Hindu Science": "https://www.thehindu.com/sci-tech/science/feeder/default.rss",
    "NASA": "https://www.nasa.gov/news-release/feed/",
    "CISA": "https://www.cisa.gov/cybersecurity-advisories/alerts.xml",
    "BleepingComputer": "https://www.bleepingcomputer.com/feed/",
    "Mongabay India": "https://india.mongabay.com/feed/",
    "BBC Science & Environment": "https://feeds.bbci.co.uk/news/science_and_environment/rss.xml",
}


def publication(raw):
    try:
        date = dt.datetime.fromisoformat(raw.replace("Z", "+00:00"))
    except ValueError:
        try:
            date = email.utils.parsedate_to_datetime(raw)
        except (ValueError, TypeError):
            return None
    return date if date.tzinfo else None


def probe(entry):
    name, url = entry
    result = {"source": name, "url": url}
    try:
        request = urllib.request.Request(url, headers={"User-Agent": "LiveGuard/2.78 FeedMonitoring", "Accept": "application/rss+xml, application/atom+xml, text/xml"})
        with urllib.request.urlopen(request, timeout=6) as response:
            raw = response.read(512 * 1024 + 1)
        if len(raw) > 512 * 1024 or b"<!DOCTYPE" in raw.upper() or b"<!ENTITY" in raw.upper():
            return dict(result, status="oversized_or_unsafe")
        root = ET.fromstring(raw)
        entries = [e for e in root.iter() if e.tag.rsplit("}", 1)[-1] in ("item", "entry")][:60]
        now = dt.datetime.now(dt.timezone.utc)
        ist = dt.timezone(dt.timedelta(hours=5, minutes=30))
        dates = []
        for item in entries:
            for child in item:
                if child.tag.rsplit("}", 1)[-1] in ("pubDate", "published", "date"):
                    date = publication((child.text or "").strip())
                    if date:
                        dates.append(date)
                        break
        return dict(result, status="feed" if entries else "no_entries", entries=len(entries), dated=len(dates),
                    today_ist=sum(d <= now and d.astimezone(ist).date() == now.astimezone(ist).date() for d in dates))
    except Exception as error:
        # Do not record redirect URLs, response bodies or query tokens.
        return dict(result, status="unavailable", error=type(error).__name__)


def main():
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        results = list(pool.map(probe, SOURCES.items()))
    report = {"monitoring_only": True, "checked_utc": dt.datetime.now(dt.timezone.utc).isoformat(), "sources": results}
    Path("liveguard-news-source-probe.json").write_text(json.dumps(report, indent=2) + "\n")
    for result in results:
        message = f"{result['source']}: {result['status']}, dated={result.get('dated', 0)}, today_ist={result.get('today_ist', 0)}"
        print(message)
        if os.environ.get("GITHUB_ACTIONS") == "true":
            print(f"::notice title=RSS source monitoring::{message}")


if __name__ == "__main__":
    main()
