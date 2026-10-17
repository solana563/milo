// milo-push: sends a push when a message or friend request is created.
// Called by Supabase Database Webhooks (see PUSH.md). Deno runtime.
import { createClient } from "npm:@supabase/supabase-js@2";
import webpush from "npm:web-push@3.6.7";
import { SignJWT, importPKCS8 } from "npm:jose@5";

const db = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
const VP = Deno.env.get("VAPID_PUBLIC"), VK = Deno.env.get("VAPID_PRIVATE");
if (VP && VK) webpush.setVapidDetails(Deno.env.get("VAPID_SUBJECT") || "mailto:admin@example.com", VP, VK);
const SA = Deno.env.get("FCM_SERVICE_ACCOUNT") ? JSON.parse(Deno.env.get("FCM_SERVICE_ACCOUNT")!) : null;

let fcmTok: { t: string; exp: number } | null = null;
async function fcmToken() {
  if (!SA) return null;
  if (fcmTok && fcmTok.exp > Date.now() + 60000) return fcmTok.t;
  const key = await importPKCS8(SA.private_key, "RS256"), now = Math.floor(Date.now() / 1000);
  const jwt = await new SignJWT({ scope: "https://www.googleapis.com/auth/firebase.messaging" })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" }).setIssuer(SA.client_email).setSubject(SA.client_email)
    .setAudience("https://oauth2.googleapis.com/token").setIssuedAt(now).setExpirationTime(now + 3500).sign(key);
  const r = await fetch("https://oauth2.googleapis.com/token", { method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion: jwt }) });
  const j = await r.json();
  fcmTok = { t: j.access_token, exp: Date.now() + (j.expires_in || 3500) * 1000 };
  return fcmTok.t;
}

type P = { title: string; body: string; tag: string; act: string; chat?: string };

async function send(uid: string, p: P) {
  const { data: subs } = await db.from("push_subs").select("id,kind,endpoint,keys").eq("user_id", uid);
  await Promise.all((subs || []).map(async (s) => {
    try {
      if (s.kind === "web") {
        if (!VP || !VK) return;
        await webpush.sendNotification({ endpoint: s.endpoint, keys: s.keys }, JSON.stringify(p), { TTL: 86400, urgency: "high" });
      } else {
        const t = await fcmToken(); if (!t) return;
        const r = await fetch(`https://fcm.googleapis.com/v1/projects/${SA.project_id}/messages:send`, {
          method: "POST", headers: { Authorization: "Bearer " + t, "Content-Type": "application/json" },
          body: JSON.stringify({ message: { token: s.endpoint, notification: { title: p.title, body: p.body },
            data: { act: p.act, chat: p.chat || "" },
            android: { priority: "HIGH", ttl: "86400s", notification: { tag: p.tag, channel_id: "milo_messages", icon: "ic_stat_milo" } } } }) });
        if (r.status === 404 || r.status === 400) { const j = await r.json().catch(() => ({})); if (/UNREGISTERED|INVALID_ARGUMENT/.test(JSON.stringify(j))) await db.from("push_subs").delete().eq("id", s.id); }
      }
    } catch (e: any) {
      if (e && (e.statusCode === 404 || e.statusCode === 410)) await db.from("push_subs").delete().eq("id", s.id);
    }
  }));
}

async function who(id: string) {
  const { data } = await db.from("profiles").select("name,username").eq("id", id).maybeSingle();
  return (data && (data.name || data.username)) || "Someone";
}

Deno.serve(async (req) => {
  try {
    const e = await req.json(), r = e.record, o = e.old_record;
    if (e.table === "messages" && e.type === "INSERT") {
      const nm = await who(r.sender);
      const body = r.kind === "song" ? "Sent you a song" : r.kind === "img" ? "Sent you a photo" : String(r.pv || r.body || "").slice(0, 120);
      await send(r.rcpt, { title: nm, body, tag: "milo-" + r.sender, act: "chat", chat: r.sender });
    } else if (e.table === "friendships") {
      if (e.type === "INSERT" && r.status === "pending") {
        await send(r.b, { title: await who(r.a), body: "Sent you a friend request", tag: "milo-fr-" + r.a, act: "friends" });
      } else if (e.type === "UPDATE" && r.status === "accepted" && o && o.status !== "accepted") {
        await send(r.a, { title: await who(r.b), body: "Accepted your friend request", tag: "milo-fr-" + r.b, act: "friends" });
      }
    }
    return new Response("ok");
  } catch (err) { return new Response(String(err), { status: 200 }); }
});
