#!/usr/bin/env python3
"""Bounded decode audit of EVERY shipped stream + capped Radio Browser discoveries.
A decodable stream is NOT proof of programme identity or absence of spoken promotions.
Run on CI with FFprobe; do not store audio or publish apps automatically.
"""
import concurrent.futures, datetime, hashlib, ipaddress, json, pathlib, socket, subprocess, sys, urllib.parse, urllib.request

UA = 'RameshRadio/1.30 catalogue-audit (+https://github.com/universalrp2003/RP-Android-Apps)'
PROVIDER_SUFFIX = 'listenon.in'

def reported_provider(url):
    host = (urllib.parse.urlsplit(url).hostname or '').lower().rstrip('.')
    return host.endswith('.listenon.in') and host.startswith('psrlive')

def public_url(url):
    parsed = urllib.parse.urlsplit(url)
    if parsed.scheme not in ('https', 'http') or not parsed.hostname or parsed.username or parsed.password:
        raise ValueError('unsupported or credentialled URL')
    addresses = socket.getaddrinfo(parsed.hostname, parsed.port or (443 if parsed.scheme == 'https' else 80), type=socket.SOCK_STREAM)
    if not addresses or any(not ipaddress.ip_address(x[4][0]).is_global for x in addresses):
        raise ValueError('non-public address')

def discover():
    rows, errors = [], []
    for params in [dict(language='tamil',order='votes',reverse='true',limit='250',hidebroken='true'),
                   dict(language='tamil',order='clicktrend',reverse='true',limit='100',hidebroken='true')]:
        for base in ['https://de1.api.radio-browser.info','https://nl1.api.radio-browser.info','https://de2.api.radio-browser.info']:
            try:
                req=urllib.request.Request(base+'/json/stations/search?'+urllib.parse.urlencode(params),headers={'User-Agent':UA})
                with urllib.request.urlopen(req,timeout=10) as r:
                    data=r.read(2_000_001)
                    if len(data)>2_000_000: raise ValueError('directory response limit')
                batch=json.loads(data)
                if not isinstance(batch,list): raise ValueError('directory did not return a list')
                rows.extend(batch);break
            except Exception as e: errors.append(type(e).__name__)
    return rows, errors

def probe(station):
    url=station['url']; result={'id':station.get('id'), 'name':station['name'], 'url':url,
        'providerReview':reported_provider(url),'contentReviewed':False}
    try:
        public_url(url)
        p=subprocess.run(['ffprobe','-v','error','-rw_timeout','6000000','-user_agent',UA,
             '-protocol_whitelist','http,https,tcp,tls,crypto,httpproxy',
             '-read_intervals','%+2','-show_entries','stream=codec_type,codec_name,sample_rate','-of','json',url],
             capture_output=True,text=True,timeout=18)
        streams=json.loads(p.stdout or '{}').get('streams',[])
        audio=[s for s in streams if s.get('codec_type')=='audio' and s.get('codec_name')]
        result.update(status='audio_reachable' if p.returncode==0 and audio else 'unavailable',audio=audio,
                      detail='audio metadata parsed' if audio and p.returncode==0 else 'probe/decode failed')
    except Exception as e: result.update(status='unavailable',detail=type(e).__name__)
    return result

def main():
    source=pathlib.Path(sys.argv[1]); out=pathlib.Path(sys.argv[2]);out.mkdir(parents=True,exist_ok=True)
    raw=source.read_bytes(); seed=json.loads(raw); old=seed['stations'];existing={s['url'] for s in old}
    candidates,errors=discover();new=[]
    for r in candidates:
        url=(r.get('url_resolved') or r.get('url') or '').strip()
        if not url or url in existing or reported_provider(url):continue
        existing.add(url)
        new.append(dict(id=r.get('stationuuid') or url,name=r.get('name','').strip() or 'Unnamed station',url=url,
            favicon=r.get('favicon',''),homepage=r.get('homepage',''),tags=r.get('tags',''),country=r.get('country',''),
            countryCode=r.get('countrycode',''),state=r.get('state',''),language=r.get('language',''),
            codec=r.get('codec',''),bitrate=r.get('bitrate',0),votes=r.get('votes',0),category='tamil',
            isCustom=False,verified=False,source='directory-audit',order=0))
        if len(new)>=80:break
    with concurrent.futures.ThreadPoolExecutor(max_workers=12) as pool:
        results=list(pool.map(probe,old+new))
    by_url={r['url']:r for r in results}; added=[s for s in new if by_url[s['url']]['status']=='audio_reachable']
    for s in old+added:
        p=by_url[s['url']];s['verified']=p['status']=='audio_reachable' and not p['providerReview']
        s['auditStatus']='provider_review' if p['providerReview'] else p['status']
    seed['stations']=old+added;seed['total']=len(seed['stations']);seed['counts']={}
    for s in seed['stations']:seed['counts'][s['category']]=seed['counts'].get(s['category'],0)+1
    seed['verifiedCount']=sum(s['verified'] for s in seed['stations'])
    now=datetime.datetime.now(datetime.timezone.utc).isoformat();seed['generatedAt']=now
    seed['note']='All shipped endpoints probed at generatedAt. verified means audio metadata reachable from CI, NOT programme/content review or phone/region availability. Reported-provider family is held for content review; existing URLs retained for favourites/history.'
    report=dict(checkedAt=now,sourceSha256=hashlib.sha256(raw).hexdigest(),shippedChecked=len(old),newCandidatesChecked=len(new),
        added=len(added),directoryErrors=errors,results=results,
        limits='Bounded single-location audio-metadata probe, not full listening/content review, regional guarantee, ASR or provider restriction bypass. Initial hosts screened; this is not a complete redirect/HLS/DNS-rebinding sandbox.')
    (out/'stations_seed.json').write_text(json.dumps(seed,ensure_ascii=False,indent=2)+'\n')
    (out/'audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({k:report[k] for k in ('shippedChecked','newCandidatesChecked','added','directoryErrors')}))
    if report['shippedChecked']!=len(old):raise SystemExit('Incomplete shipped coverage')
if __name__=='__main__':main()
