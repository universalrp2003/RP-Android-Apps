#!/usr/bin/env python3
"""Retrieve bounded public player HTML and same-origin scripts for manual research.

Never execute scripts, follow station links, or import their records automatically.
The content is a transient CI artifact, not part of the distributed app.
"""
import html.parser
import json
import pathlib
import sys
import urllib.parse
import urllib.request

BASE='https://fm.biogem.org/'
class Scripts(html.parser.HTMLParser):
    def __init__(self):
        super().__init__(); self.urls=[]
    def handle_starttag(self,tag,attrs):
        a=dict(attrs)
        if tag == 'script' and a.get('src'):
            url=urllib.parse.urljoin(BASE,a['src'])
            if urllib.parse.urlsplit(url).netloc == urllib.parse.urlsplit(BASE).netloc:
                self.urls.append(url)

def read(url):
    req=urllib.request.Request(url,headers={'User-Agent':'RameshRadio/1.0 reference reconciliation'})
    with urllib.request.urlopen(req,timeout=20) as response:
        data=response.read(3_000_001)
        if len(data)>3_000_000:raise ValueError('response too large')
        text = data.decode('utf8', errors='replace')
        if 'Please wait while your request is being verified' in text:
            raise ValueError('verification challenge; no player data retrieved')
        return text

def main(output):
    results={};errors=[]
    try:
        text=read(BASE);results[BASE]=text
        parser=Scripts();parser.feed(text)
        for url in list(dict.fromkeys(parser.urls))[:8]:
            try: results[url]=read(url)
            except Exception as e: errors.append([url,type(e).__name__])
    except Exception as e:errors.append([BASE,type(e).__name__])
    p=pathlib.Path(output);p.mkdir(parents=True,exist_ok=True)
    (p/'player-source.json').write_text(json.dumps({'content':results,'errors':errors},ensure_ascii=False),encoding='utf8')
    print('Retrieved',len(results),'documents; errors',errors)
if __name__=='__main__':main(sys.argv[1])
