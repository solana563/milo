/* Discord Rich Presence over Discord's local IPC (a named pipe on Windows). No library needed.
 * It only works while the Discord desktop app is running; if it isn't, nothing happens. */
const net = require("net");
const crypto = require("crypto");

let sock = null, clientId = "", ready = false, buf = Buffer.alloc(0), pending = null, retry = null;

function frame(op, obj) {
  const data = Buffer.from(JSON.stringify(obj));
  const h = Buffer.alloc(8); h.writeInt32LE(op, 0); h.writeInt32LE(data.length, 4);
  return Buffer.concat([h, data]);
}

function tryPipe(i, done) {
  if (i > 9) return done(null);
  const p = process.platform === "win32" ? `\\\\?\\pipe\\discord-ipc-${i}` : `${process.env.XDG_RUNTIME_DIR || process.env.TMPDIR || "/tmp"}/discord-ipc-${i}`;
  const s = net.connect(p);
  s.once("connect", () => done(s));
  s.once("error", () => tryPipe(i + 1, done));
}

function connect() {
  if (sock || !clientId) return;
  tryPipe(0, (s) => {
    if (!s) { retry = setTimeout(connect, 20000); return; }
    sock = s; buf = Buffer.alloc(0); ready = false;
    s.on("data", (d) => {
      buf = Buffer.concat([buf, d]);
      while (buf.length >= 8) {
        const op = buf.readInt32LE(0), len = buf.readInt32LE(4);
        if (buf.length < 8 + len) break;
        let msg = {}; try { msg = JSON.parse(buf.slice(8, 8 + len).toString()); } catch {}
        buf = buf.slice(8 + len);
        if (op === 1 && msg.evt === "READY") { ready = true; if (pending) send(pending); }
        if (op === 2) { close(); }
        if (op === 3) s.write(frame(4, msg)); // ping -> pong
      }
    });
    const gone = () => { if (sock === s) { sock = null; ready = false; if (clientId) retry = setTimeout(connect, 20000); } };
    s.on("close", gone); s.on("error", gone);
    s.write(frame(0, { v: 1, client_id: clientId }));
  });
}

function send(activity) {
  if (!sock || !ready) return;
  try {
    sock.write(frame(1, { cmd: "SET_ACTIVITY", args: { pid: process.pid, activity: activity || null }, nonce: crypto.randomUUID() }));
  } catch {}
}

function close() {
  clearTimeout(retry); retry = null;
  if (sock) { try { send(null); sock.end(); } catch {} sock = null; }
  ready = false;
}

/** cfg: { on, id }. info: { title, artist, album, playing, pos, dur, img } or null to clear. */
function update(cfg, info) {
  if (!cfg || !cfg.on || !/^\d{15,25}$/.test(String(cfg.id || ""))) { clientId = ""; pending = null; close(); return; }
  if (clientId !== String(cfg.id)) { close(); clientId = String(cfg.id); }
  if (!info || !info.title) { pending = null; send(null); connect(); return; }
  const a = {
    type: 2, // "Listening to"
    details: String(info.title).slice(0, 120),
    state: String(info.artist || "milo.").slice(0, 120),
    assets: {},
  };
  if (info.img && /^https:\/\//.test(info.img)) a.assets.large_image = info.img;
  if (info.album) a.assets.large_text = String(info.album).slice(0, 120);
  if (info.playing && info.dur > 0) {
    const now = Date.now(), start = Math.round(now - info.pos * 1000);
    a.timestamps = { start, end: Math.round(start + info.dur * 1000) };
  }
  if (!info.playing) a.state = ("Paused · " + a.state).slice(0, 120);
  pending = a;
  if (ready) send(a); else connect();
}

module.exports = { update, close };
