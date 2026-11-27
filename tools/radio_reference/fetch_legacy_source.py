#!/usr/bin/env python3
"""Read a publicly indexed historical station list for review, never auto-import.

Only two fixed URLs are requested; returned station endpoints are never contacted here.
Old catalogues may be wrong, copyrighted, or stale; identity and availability need review.
"""
import html
import json
import pathlib
import re
import sys
import urllib.request

URLS = ('https://coolaf.com/run/snippets/gd6mdshxv6', 'http://coolaf.com/run/snippets/gd6mdshxv6')

def main(out):
    errors=[]; records=[]
    for url in URLS:
        try:
            request=urllib.request.Request(url, headers={'User-Agent':'RameshRadio/1.0 reference reconciliation'})
            with urllib.request.urlopen(request,timeout=25) as response:
                data=response.read(5_000_001)
            if len(data)>5_000_000: raise ValueError('response too large')
            text=html.unescape(data.decode('utf8',errors='replace'))
            for group in re.findall(r'\{[^{}]{10,3000}\}',text):
                try:
                    value=json.loads(group)
                    if isinstance(value,dict) and isinstance(value.get('name'),str) and isinstance(value.get('url'),str):
                        records.append({k:value[k] for k in ('name','url','url1','url2','link2','category') if isinstance(value.get(k),str)})
                except (ValueError,TypeError):pass
            if records: break
            errors.append('no parseable records in '+url)
        except Exception as exc:
            errors.append(url+': '+type(exc).__name__)
    out=pathlib.Path(out);out.mkdir(parents=True,exist_ok=True)
    (out/'historical-source.json').write_text(json.dumps({'source':URLS[0], 'records':records, 'errors':errors},ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    print('Historical records:',len(records),'errors:',errors)

if __name__=='__main__':main(sys.argv[1])
