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
    urls = re.findall(r'show\("[^"\n]+",\s*"[^"\n]+",\s*"(https://[^"\s]+)"\)', source)
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


def streaming_episodes(response) -> tuple[str, list[str]]:
    # Some real archives exceed eight megabytes. Stream only the first twenty items;
    # do not reject a healthy publisher because its full archive is large.
    root = None
    urls = []
    title = ""
    class BoundedReader:
        count = 0
        def read(self, size=-1):
            data = response.read(min(size if size > 0 else 16384, 16384))
            self.count += len(data)
            if self.count > MAX_BYTES:
                raise ValueError("No useful feed prefix within safe size limit")
            return data
    for event, element in ET.iterparse(BoundedReader(), events=("start", "end")):
        if root is None:
            root = element
            if root.tag != "rss":
                raise ValueError("Response is not RSS")
        if event == "end" and element.tag == "item":
            title = root.findtext("channel/title", "").strip()
            for enclosure in element.findall("enclosure"):
                url = enclosure.attrib.get("url", "")
                if url.startswith("https://"):
                    urls.append(url)
            element.clear()
            if len(urls) >= 20:
                break
    if not title or not urls:
        raise ValueError("Missing channel title or playable HTTPS enclosures")
    return title, urls


def verify(url: str) -> None:
    request = urllib.request.Request(url, headers={"User-Agent": "RPAndroidPodcastPlayer/1.3", "Accept": "application/rss+xml, application/xml, text/xml"})
    with urllib.request.urlopen(request, timeout=40) as response:
        title, audio_urls = streaming_episodes(response)
    # Read just a small range of the newest episode: don't download entire audio archives.
    audio = urllib.request.Request(audio_urls[0], headers={"User-Agent": "RPAndroidPodcastPlayer/1.3", "Range": "bytes=0-1023"})
    with urllib.request.urlopen(audio, timeout=40) as response:
        kind = response.headers.get("Content-Type", "").lower()
        first = response.read(1024)
        if not first or "html" in kind or first.lstrip().lower().startswith((b"<!doctype html", b"<html")):
            raise ValueError("Audio enclosure returned an empty body or HTML")
    print(f"PASS: {title}: {len(audio_urls)} sampled audio enclosures; first audio responded ({url})")


def main() -> int:
    def failure(message):
        print(f"FAIL: {message}", file=sys.stderr)
        escaped = message.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
        print("::error::" + escaped)
    try:
        urls = feed_urls(SOURCE.read_text(encoding="utf-8"))
    except Exception as error:
        failure(str(error))
        return 1
    failed = False
    for url in urls:
        try:
            verify(url)
        except Exception as error:
            failure(f"{url}: {error}")
            failed = True
    return int(failed)


if __name__ == "__main__":
    sys.exit(main())
