// Standard capability authentication over TLS; opaque AES-GCM payloads only. No location logs.
export const TTL = 15 * 60_000;
export const INVITE_TTL = 10 * 60_000;
export const PAIR_TTL = 30 * 86400_000;
export const MAX_SHARE = 8 * 3600_000;
const hex = /^[a-f0-9]{64}$/;
const nonce = /^[a-f0-9]{32}$/;
const b64 = /^[A-Za-z0-9_-]+$/;
export async function digest(secret) {
  return Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode('oor-family-auth-v1:'+secret))),b=>b.toString(16).padStart(2,'0')).join('');
}
const reply=(state,status,body={})=>({state,status,body});
const keys=(o,wanted)=>o && typeof o==='object' && !Array.isArray(o) && Object.keys(o).sort().join(',')===wanted.sort().join(',');
export async function handle(state, {method,action,token,body={}}, now) {
  if (!hex.test(token??'')) return reply(state,403);
  const auth=await digest(token);
  if (method==='POST' && action==='create') {
    if (!keys(body,['inviteHash']) || !hex.test(body.inviteHash)) return reply(state,400);
    if (state) return reply(state,state.hostHash===auth && state.inviteHash===body.inviteHash && !state.revoked ? 200:409);
    return reply({hostHash:auth,inviteHash:body.inviteHash,inviteUntil:now+INVITE_TTL,expires:now+PAIR_TTL,
      confirmed:{a:false,b:false},grants:{a:0,b:0},generation:{a:0,b:0},updates:{},limits:{}},201);
  }
  if (!state) return reply(state,404);
  if (state.revoked || now>=state.expires) return reply({revoked:true,expires:Math.max(now+TTL,state.expires)},410);
  // Physically discard expired ciphertext on every request; alarm also performs this without reads.
  for (const role of ['a','b']) if (state.updates[role]?.expires<=now || state.grants[role]<=now) delete state.updates[role];
  if (method==='POST' && action==='claim') {
    if (!keys(body,['guestHash','confirmation']) || !hex.test(body.guestHash) || !nonce.test(body.confirmation)) return reply(state,400);
    if (now>=state.inviteUntil || auth!==state.inviteHash) return reply(state,403);
    if (body.guestHash===state.hostHash || body.guestHash===state.inviteHash) return reply(state,400);
    if (state.guestHash && (state.guestHash!==body.guestHash || state.confirmation!==body.confirmation)) return reply(state,409);
    state.guestHash=body.guestHash; state.confirmation=body.confirmation;
    return reply(state,200);
  }
  const role=auth===state.hostHash ? 'a' : auth===state.guestHash ? 'b':null;
  if (!role) return reply(state,403);
  const other=role==='a'?'b':'a';
  const minute=Math.floor(now/60_000), bucket=state.limits[role]??{minute,count:0};
  if (bucket.minute!==minute) { bucket.minute=minute;bucket.count=0; }
  state.limits[role]=bucket;
  if (++bucket.count>30) return reply(state,429);
  if (method==='DELETE' && action==='pair') return reply({revoked:true,expires:state.expires},200);
  if (method==='POST' && action==='confirm') {
    if (!state.guestHash || !keys(body,['confirmation']) || body.confirmation!==state.confirmation) return reply(state,409);
    state.confirmed[role]=true; return reply(state,200);
  }
  const approved=state.confirmed.a && state.confirmed.b;
  if (method==='PUT' && action==='consent') {
    if (!keys(body,['until']) || !Number.isSafeInteger(body.until) || (body.until!==0 && (!approved || body.until<=now || body.until>now+MAX_SHARE))) return reply(state,400);
    state.grants[role]=body.until;state.generation[role]++;delete state.updates[role];
    return reply(state,200,{generation:state.generation[role],until:state.grants[role]});
  }
  if (method==='PUT' && action==='town') {
    if (!approved || state.grants[role]<=now) return reply(state,403);
    if (!keys(body,['sampleAt','generation','iv','ciphertext']) || body.generation!==state.generation[role] ||
      !Number.isSafeInteger(body.sampleAt) || Math.abs(now-body.sampleAt)>60_000 ||
      typeof body.iv!=='string' || body.iv.length!==16 || !b64.test(body.iv) ||
      typeof body.ciphertext!=='string' || !b64.test(body.ciphertext) || body.ciphertext.length<24 || body.ciphertext.length>1536) return reply(state,400);
    if (state.updates[role] && body.sampleAt<=state.updates[role].sampleAt) return reply(state,409);
    state.updates[role]={sampleAt:body.sampleAt,expires:Math.min(body.sampleAt+TTL,state.grants[role]),iv:body.iv,ciphertext:body.ciphertext};
    return reply(state,200);
  }
  if (method==='GET' && action==='status') return reply(state,200,{now,expires:state.expires,paired:!!state.guestHash,
    confirmation:state.confirmation??'',confirmed:state.confirmed,ownUntil:state.grants[role],generation:state.generation[role],
    peerUntil:state.grants[other],update:approved ? state.updates[other]??null:null});
  return reply(state,404);
}
export function expire(state,now) {
  if (!state || state.expires<=now) return null;
  if (!state.revoked) for (const role of ['a','b']) if (state.updates[role]?.expires<=now || state.grants[role]<=now) delete state.updates[role];
  return state;
}
export function nextAlarm(state,now) {
  return Math.min(state.expires,...Object.values(state.updates??{}).map(u=>u.expires).filter(t=>t>now));
}
