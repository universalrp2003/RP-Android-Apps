import { DurableObject } from 'cloudflare:workers';
import { handle,expire,nextAlarm } from './policy.mjs';
const PATH=/^\/v1\/pairs\/([a-f0-9]{32})\/(create|claim|confirm|consent|town|status|pair)$/;
function json(status,body={}) { return new Response(JSON.stringify(body),{status,headers:{'Content-Type':'application/json','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'}}); }
export default {
  async fetch(request,env) {
    const url=new URL(request.url);
    if (url.pathname==='/health' && request.method==='GET') return json(200,{protocol:'oor-family/1',retentionMinutes:15});
    const match=PATH.exec(url.pathname);
    if (!match || url.search || !['GET','POST','PUT','DELETE'].includes(request.method)) return json(404);
    if (!/^Bearer [a-f0-9]{64}$/.test(request.headers.get('Authorization')??'')) return json(403);
    const limiter=match[2]==='create' ? env.CREATE_LIMITER:env.REQUEST_LIMITER;
    if (!(await limiter.limit({key:request.headers.get('CF-Connecting-IP')??'local'})).success) return json(429);
    let body={};
    if (['POST','PUT'].includes(request.method)) {
      if (!/^application\/json(?:;\s*charset=utf-8)?$/i.test(request.headers.get('Content-Type')??'') || Number(request.headers.get('Content-Length')??0)>2048) return json(400);
      const reader=request.body?.getReader();if (!reader) return json(400);
      let length=0,chunks=[];
      let timerId;
      const timer=new Promise((_,reject)=>{timerId=setTimeout(()=>reject(new Error('timeout')),5000);});
      try {
        while(true) {
          const {done,value}=await Promise.race([reader.read(),timer]);if(done) break;
          length+=value.length;if(length>2048) { await reader.cancel();return json(413); }chunks.push(value);
        }
        const bytes=new Uint8Array(length);let offset=0;for(const c of chunks){bytes.set(c,offset);offset+=c.length;}
        body=JSON.parse(new TextDecoder('utf-8',{fatal:true}).decode(bytes));
      } catch { await reader.cancel().catch(()=>{});return json(400); }
      finally { clearTimeout(timerId); }
    }
    return env.PAIRS.getByName(match[1]).fetch(new Request('https://internal/'+match[2],{method:request.method,
      headers:{Authorization:request.headers.get('Authorization'),'Content-Type':'application/json'},
      ...(['POST','PUT'].includes(request.method)?{body:JSON.stringify(body)}:{})}));
  }
};
export class FamilyPair extends DurableObject {
  async fetch(request) {
    const action=new URL(request.url).pathname.slice(1);
    const body=['POST','PUT'].includes(request.method)?await request.json():{};
    return this.ctx.blockConcurrencyWhile(async()=>{
      const now=Date.now();
      const result=await handle(await this.ctx.storage.get('pair'),{method:request.method,action,token:request.headers.get('Authorization')?.slice(7),body},now);
      if(result.state){await this.ctx.storage.put('pair',result.state);await this.ctx.storage.setAlarm(nextAlarm(result.state,now));}
      return json(result.status,result.body);
    });
  }
  async alarm() {
    const now=Date.now(),s=expire(await this.ctx.storage.get('pair'),now);
    if (!s) { await this.ctx.storage.deleteAll();return; }
    await this.ctx.storage.put('pair',s);await this.ctx.storage.setAlarm(nextAlarm(s,now));
  }
}
