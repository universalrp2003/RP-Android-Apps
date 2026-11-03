// Synthetic two-device integration. Never print capabilities, envelopes or locations.
import assert from 'node:assert/strict';import {randomBytes,webcrypto} from 'node:crypto';
import {digest} from '../src/policy.mjs';
const base=process.argv[2];assert(base && /^(http:\/\/(127\.0\.0\.1|localhost):\d+|https:\/\/oor-family-relay\.[a-z0-9-]+\.workers\.dev)$/.test(base));
const random=n=>randomBytes(n).toString('hex');const id=random(16),a=random(32),b=random(32),invite=random(32),key=randomBytes(32),confirmation=random(16);
async function request(method,action,token,body,expected=200) {
 const r=await fetch(`${base}/v1/pairs/${id}/${action}`,{method,redirect:'error',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json; charset=utf-8'},...(body?{body:JSON.stringify(body)}:{})});
 assert.equal(r.status,expected,`${method} ${action} returned ${r.status}`);return r.json();
}
const health=await (await fetch(base+'/health')).json();assert.equal(health.protocol,'oor-family/1');
let created=false,revoked=false;
try {
 await request('POST','create',a,{inviteHash:await digest(invite)},201);created=true;
 await request('GET','status',invite,undefined,403);
 await request('POST','claim',invite,{guestHash:await digest(b),confirmation});
 await request('POST','confirm',a,{confirmation});
 await request('PUT','consent',a,{until:Date.now()+3600000},400);
 await request('POST','confirm',b,{confirmation});
 const status=await request('GET','status',a);assert.equal(status.ownUntil,0);assert.equal(status.peerUntil,0);assert.equal(status.update,null);
 const grant=await request('PUT','consent',a,{until:Date.now()+3600000});
 const at=Date.now(),iv=randomBytes(12),aad=`oor-family-town/v1|${id}|a|${at}`;
 const aes=await webcrypto.subtle.importKey('raw',key,'AES-GCM',false,['encrypt','decrypt']);
 const plain=JSON.stringify({town:'Synthetic Town',district:'Synthetic District',at});
 const ct=await webcrypto.subtle.encrypt({name:'AES-GCM',iv,additionalData:new TextEncoder().encode(aad)},aes,new TextEncoder().encode(plain));
 const envelope={sampleAt:at,generation:grant.generation,iv:iv.toString('base64url'),ciphertext:Buffer.from(ct).toString('base64url')};
 await request('PUT','town',a,envelope);
 const other=await request('GET','status',b);assert.equal(other.update.expires,at+15*60000);assert(!JSON.stringify(other).includes('Synthetic'));
 const decoded=await webcrypto.subtle.decrypt({name:'AES-GCM',iv,additionalData:new TextEncoder().encode(aad)},aes,Buffer.from(other.update.ciphertext,'base64url'));assert.equal(new TextDecoder().decode(decoded),plain);
 await request('PUT','town',a,envelope,409);await request('PUT','town',b,envelope,403);
 await request('PUT','consent',a,{until:0});assert.equal((await request('GET','status',b)).update,null);
 await request('PUT','town',a,envelope,403);
 await request('DELETE','pair',b);revoked=true;await request('GET','status',a,undefined,410);
 console.log('PASS: synthetic two-device create/accept/both-confirm/default-OFF/one-way encrypted latest town/replay/stop/revoke over real HTTP');
} finally {
 if(created && !revoked) await request('DELETE','pair',a).catch(()=>{});
}
