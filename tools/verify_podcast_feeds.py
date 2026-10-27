#!/usr/bin/env python3
"""Release-time network smoke test. Never treat a directory listing as a working feed."""
from pathlib import Path
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET

SOURCE = Path(__file__).resolve().parents[1] / "podcast/src/main/java/com/universalrp/podcast/BuiltInShows.kt"
MAX_BYTES = 8 * 1024 * 1024


def feed_urls(source: str) -> list[str]:
    urls = re.findall(r'"(https://[^"\s]+/podcast/rss)"', source)
    if not urls:
        raise ValueError("No built-in feed URLs matched; refusing to verify nothing")
    if len(urls) != len(set(urls)):
        raise ValueError("Duplicate built-in feeds")
    # Any newly added URL must also be verified, not silently excluded by the extractor.
    all_urls = re.findall(r'"(https?://[^"\s]+)"', source)
    if sorted(urls) != sorted(all_urls):
        raise ValueError("Unverified URL format in BuiltInShows.kt")
    return urls


def episodes(raw: bytes) -> tuple[str, list[str]]:
    root = ET.fromstring(raw)
    if root.tag != "rss":
        raise ValueError("Response is not RSS (possibly an HTML challenge page)")
    channel = root.find("channel")
    if channel is None or not channel.findtext("title", "").strip():
        raise ValueError("No channel title")
    urls = [enclosure.attrib.get("url", "") for enclosure in channel.findall("item/enclosure")]
    urls = [url for url in urls if url.startswith("https://")]
    if not urls:
        raise ValueError("No playable HTTPS enclosures")
    return channel.findtext("title", ""), urls


def verify(url: str) -> None:
    request = urllib.request.Request(url, headers={"User-Agent": "RPAndroidPodcastPlayer/1.3", "Accept": "application/rss+xml, application/xml, text/xml"})
    with urllib.request.urlopen(request, timeout=40) as response:
        raw = response.read(MAX_BYTES + 1)
    if len(raw) > MAX_BYTES:
        raise ValueError("Feed exceeds safe size limit")
    title, audio_urls = episodes(raw)
    # Read just a small range of the newest episode: don't download entire audio archives.
    audio = urllib.request.Request(audio_urls[0], headers={"User-Agent": "RPAndroidPodcastPlayer/1.3", "Range": "bytes=0-1023"})
    with urllib.request.urlopen(audio, timeout=40) as response:
        kind = response.headers.get("Content-Type", "").lower()
        first = response.read(1024)
        if not first or "html" in kind or first.lstrip().lower().startswith((b"<!doctype html", b"<html")):
            raise ValueError("Audio enclosure returned an empty body or HTML")
    print(f"PASS: {title}: {len(audio_urls)} audio enclosures; first audio responded ({url})")


def main() -> int:
    try:
        for url in feed_urls(SOURCE.read_text(encoding="utf-8")):
            verify(url)
    except Exception as error:
        print(f"FAIL: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
