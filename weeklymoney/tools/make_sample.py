"""Create only invented demonstration records, never read private notebooks.
Requires cryptography. Public demo passphrase is not a real owner's credential.
"""
import json, os, hashlib
from datetime import date, timedelta
from pathlib import Path
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

OUT = Path(__file__).resolve().parents[1] / 'samples'
OUT.mkdir(exist_ok=True)
PASSWORD = b'Demo-Weekly-2026'
OWNER = {'id': 'demo-owner', 'name': 'மாதிரி உரிமையாளர் / DEMO ONLY'}
AT = '2026-11-15T00:00:00Z'
books = [dict(id='demo-thursday', number='DEMO-THU', day=4, started='2026-08-20', heading='வியாழன் · மாதிரி மட்டும்'),
         dict(id='demo-sunday', number='DEMO-SUN', day=7, started='2026-09-20', heading='ஞாயிறு · விடுமுறை மாதிரி')]
def loan(key, book, customer, name, total, started, sequence=1):
    return dict(id=key, book=book, customerId=customer, place='மாதிரி ஊர்', family='மாதிரி குடும்பம்', name=name,
                total=total*100, started=started, sequence=sequence)
loans = [loan('demo-thu-history', 'demo-thursday', 'demo-customer-a', 'மாதிரி அ', 12500, '2026-08-20'),
         loan('demo-thu-new', 'demo-thursday', 'demo-customer-b', 'மாதிரி ஆ', 5000, '2026-11-05'),
         loan('demo-sun-first', 'demo-sunday', 'demo-customer-c', 'மாதிரி இ', 10000, '2026-09-20'),
         loan('demo-sun-repeat', 'demo-sunday', 'demo-customer-c', 'மாதிரி இ', 2500, '2026-10-25', 2)]
changes = [dict(id='demo-diwali', book='demo-sunday', date='2026-11-08', skipped=True,
                reason='மாதிரி: தீபாவளி விடுமுறை / Demonstration holiday', actor=OWNER['id'], at=AT)]
def schedule(l):
    weekday = next(b['day'] for b in books if b['id'] == l['book'])
    d = date.fromisoformat(l['started']) + timedelta(days=1)
    while d.isoweekday() != weekday: d += timedelta(days=1)
    dates = []
    while len(dates) < 10:
        if not (l['book'] == 'demo-sunday' and d == date(2026,11,8)): dates.append(d.isoformat())
        d += timedelta(days=7)
    return dates
audits = []
for l, amounts in [(loans[0], [1000,1000,None,1500,1000,None,1000,1000,1000,1000]),
                   (loans[1], [500]), (loans[2], [500,None,1000,500,None,1000,500]),
                   (loans[3], [250])]:
    for index, amount in enumerate(amounts):
        p = dict(id=f"{l['id']}-p{index+1}", loan=l['id'], week=index+1, date=schedule(l)[index],
                 amount=None if amount is None else amount*100, revision=1, actor=OWNER['id'], actorName=OWNER['name'], at=AT)
        audits.append(dict(id=f"{p['id']}-event", before=None, after=p))
root = dict(schema=3, kind='FULL_BACKUP', owner=OWNER, books=books, loans=loans,
            audits=audits, archiveHistory=[], loanChanges=[], collectionDays=changes)
plain = json.dumps(root, ensure_ascii=False, separators=(',', ':')).encode()
salt, iv = os.urandom(16), os.urandom(12)
header = b'VVB\x01' + salt + iv
key = hashlib.pbkdf2_hmac('sha256', PASSWORD, salt, 210000, 32)
file = header + AESGCM(key).encrypt(iv, plain, header)
assert AESGCM(key).decrypt(iv, file[32:], header) == plain
(OUT/'WeeklyMoney-DEMO.vvasool').write_bytes(file)
(OUT/'sample-expectations.json').write_text(json.dumps(dict(password=PASSWORD.decode(), loans=[dict(id=l['id'], name=l['name'], dates=schedule(l),
    received=sum(a['after']['amount'] or 0 for a in audits if a['after']['loan']==l['id'])//100,
    remaining=(l['total']-sum(a['after']['amount'] or 0 for a in audits if a['after']['loan']==l['id']))//100) for l in loans]), ensure_ascii=False, indent=2)+'\n')
print(hashlib.sha256(file).hexdigest(), 'WeeklyMoney-DEMO.vvasool')
