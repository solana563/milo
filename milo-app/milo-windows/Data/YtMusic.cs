using System.Globalization;
using System.Text;
using System.Text.Json.Nodes;
using System.Text.RegularExpressions;

namespace Milo;

public class YtStream
{
    public string Url = "", Mime = "";
    public long ExpiresAt;
    public int DurationSec;
}

/// <summary>YouTube Music through its public web API (same approach as the Android app).</summary>
public static class YtMusic
{
    const string BASE = "https://music.youtube.com/youtubei/v1/";
    const string F_SONG = "EgWKAQIIAWoKEAkQBRAKEAMQBA==";
    const string REMIX_VER = "1.20260213.01.00";
    const string REMIX_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0";
    public const string STREAM_UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15";
    static readonly HttpClient H = new() { Timeout = TimeSpan.FromSeconds(25) };
    static string visitor = "";
    static long visitorAt;
    static readonly Dictionary<string, YtStream> streams = new();

    static string Gl() { try { return RegionInfo.CurrentRegion.TwoLetterISORegionName; } catch { return "US"; } }

    static JsonObject Ctx(bool vision)
    {
        var cl = new JsonObject
        {
            ["clientName"] = vision ? "VISIONOS" : "WEB_REMIX",
            ["clientVersion"] = vision ? "0.1" : REMIX_VER,
            ["gl"] = Gl(), ["hl"] = "en",
        };
        if (vision) { cl["osName"] = "visionOS"; cl["osVersion"] = "1.3.21O771"; cl["deviceMake"] = "Apple"; cl["deviceModel"] = "RealityDevice14,1"; }
        if (visitor.Length > 0) cl["visitorData"] = visitor;
        return new JsonObject
        {
            ["client"] = cl,
            ["request"] = new JsonObject { ["internalExperimentFlags"] = new JsonArray(), ["useSsl"] = true },
            ["user"] = new JsonObject { ["lockedSafetyMode"] = false },
        };
    }

    static async Task EnsureVisitor()
    {
        if (visitor.Length > 0 && Environment.TickCount64 - visitorAt < 6 * 3600_000L) return;
        try
        {
            var req = new HttpRequestMessage(HttpMethod.Get, "https://music.youtube.com/sw.js_data");
            req.Headers.TryAddWithoutValidation("User-Agent", REMIX_UA);
            using var r = await H.SendAsync(req);
            var txt = await r.Content.ReadAsStringAsync();
            var i = txt.IndexOf('[');
            if (i < 0) return;
            var arr = JsonNode.Parse(txt[i..])?[0]?[2]?.AsArray();
            if (arr == null) return;
            foreach (var n in arr)
            {
                var s = n?.ToString() ?? "";
                if (s.StartsWith("Cgt") || s.StartsWith("Cgs")) { visitor = s; visitorAt = Environment.TickCount64; return; }
            }
        }
        catch (Exception e) { Log.Add("visitor: " + e.Message); }
    }

    static async Task<JsonNode> Post(string path, JsonObject body, bool vision = false)
    {
        await EnsureVisitor();
        body["context"] = Ctx(vision);
        var req = new HttpRequestMessage(HttpMethod.Post, BASE + path + "?prettyPrint=false");
        req.Headers.TryAddWithoutValidation("X-Goog-Api-Format-Version", "1");
        req.Headers.TryAddWithoutValidation("X-YouTube-Client-Name", vision ? "101" : "67");
        req.Headers.TryAddWithoutValidation("X-YouTube-Client-Version", vision ? "0.1" : REMIX_VER);
        req.Headers.TryAddWithoutValidation("X-Origin", "https://music.youtube.com");
        req.Headers.TryAddWithoutValidation("Referer", "https://music.youtube.com/");
        req.Headers.TryAddWithoutValidation("User-Agent", vision ? STREAM_UA : REMIX_UA);
        req.Headers.TryAddWithoutValidation("Accept-Language", "en-US,en;q=0.9");
        if (visitor.Length > 0) req.Headers.TryAddWithoutValidation("X-Goog-Visitor-Id", visitor);
        req.Content = new StringContent(body.ToJsonString(), Encoding.UTF8, "application/json");
        using var r = await H.SendAsync(req);
        var tx = await r.Content.ReadAsStringAsync();
        if (!r.IsSuccessStatusCode) throw new IOException("YouTube answered " + (int)r.StatusCode);
        return JsonNode.Parse(tx)!;
    }

    // ---- reading the answers ----
    static void FindAll(JsonNode? n, string key, List<JsonNode?> o)
    {
        if (n is JsonObject obj)
        {
            foreach (var kv in obj)
            {
                if (kv.Key == key) o.Add(kv.Value);
                FindAll(kv.Value, key, o);
            }
        }
        else if (n is JsonArray a) foreach (var x in a) FindAll(x, key, o);
    }

    static List<JsonNode?> All(JsonNode? n, string key) { var l = new List<JsonNode?>(); FindAll(n, key, l); return l; }

    static string RunsText(JsonNode? runs)
    {
        var sb = new StringBuilder();
        if (runs is JsonArray a) foreach (var r in a) sb.Append(r?["text"]?.ToString() ?? "");
        return sb.ToString();
    }

    static string ThumbOf(JsonNode? n)
    {
        foreach (var t in All(n, "thumbnails"))
            if (t is JsonArray a && a.Count > 0) return a[a.Count - 1]?["url"]?.ToString() ?? "";
        return "";
    }

    /// <summary>Ask for a bigger picture than the 60–226 px the API hands out.</summary>
    public static string Art(string url, int px = 544)
    {
        if (string.IsNullOrEmpty(url)) return "";
        var m = Regex.Match(url, @"=w\d+-h\d+");
        if (m.Success) return url[..m.Index] + $"=w{px}-h{px}-l90-rj";
        m = Regex.Match(url, @"=s\d+");
        if (m.Success) return url[..m.Index] + $"=s{px}";
        return url;
    }

    static int DurSec(string s)
    {
        var p = s.Trim().Split(':');
        int v = 0;
        foreach (var x in p) { if (!int.TryParse(x, out var i)) return 0; v = v * 60 + i; }
        return v;
    }

    static readonly Regex DUR = new(@"^\d+(:\d{2}){1,2}$");
    static readonly Regex TYPES = new(@"^(song|video|album|single|ep|playlist|artist|podcast|episode|profile|station)$", RegexOptions.IgnoreCase);

    class Grp { public string Text = "", Artist = "", Album = ""; }

    static List<Grp> Groups(JsonNode? runs)
    {
        var o = new List<Grp>();
        var cur = new Grp();
        if (runs is JsonArray a)
            foreach (var r in a)
            {
                var t = r?["text"]?.ToString() ?? "";
                if (t.Trim() == "•") { o.Add(cur); cur = new Grp(); continue; }
                cur.Text += t;
                var id = r?["navigationEndpoint"]?["browseEndpoint"]?["browseId"]?.ToString() ?? "";
                if (id.StartsWith("UC") && cur.Artist.Length == 0) cur.Artist = id;
                if (id.StartsWith("MPRE")) cur.Album = id;
            }
        o.Add(cur);
        foreach (var g in o) g.Text = g.Text.Trim();
        return o;
    }

    static Track? SongOf(JsonNode n)
    {
        var cols = n["flexColumns"] as JsonArray;
        JsonNode? RunsAt(int i) => cols != null && i < cols.Count ? cols[i]?["musicResponsiveListItemFlexColumnRenderer"]?["text"]?["runs"] : null;
        var vid = n["playlistItemData"]?["videoId"]?.ToString() ?? "";
        if (vid.Length == 0) vid = n["navigationEndpoint"]?["watchEndpoint"]?["videoId"]?.ToString() ?? "";
        if (vid.Length == 0) vid = All(n["overlay"], "videoId").Select(x => x?.ToString() ?? "").FirstOrDefault(x => x.Length > 0) ?? "";
        var title = RunsText(RunsAt(0));
        if (vid.Length == 0 || title.Length == 0) return null;
        var g = Groups(RunsAt(1));
        if (g.Count > 1 && g[0].Artist.Length == 0 && (TYPES.IsMatch(g[0].Text) || g.Skip(1).Any(x => x.Artist.Length > 0) || (g.Count == 2 && DUR.IsMatch(g[1].Text))))
            g = g.Skip(1).ToList();
        var dur = g.LastOrDefault(x => DUR.IsMatch(x.Text))?.Text ?? "";
        if (dur.Length == 0 && n["fixedColumns"] is JsonArray fc)
            foreach (var c in fc)
            {
                var t = RunsText(c?["musicResponsiveListItemFixedColumnRenderer"]?["text"]?["runs"]);
                if (DUR.IsMatch(t)) { dur = t; break; }
            }
        var artist = g.FirstOrDefault()?.Text ?? "";
        if (DUR.IsMatch(artist)) artist = "";
        var album = g.Skip(1).FirstOrDefault(x => x.Album.Length > 0)?.Text ?? "";
        return new Track { Id = vid, Title = title, Artist = artist.Length > 0 ? artist : "YouTube Music", Album = album, DurSec = DurSec(dur), Thumb = Art(ThumbOf(n)) };
    }

    static List<Track> Songs(JsonNode root)
    {
        var o = new List<Track>();
        var seen = new HashSet<string>();
        foreach (var n in All(root, "musicResponsiveListItemRenderer"))
        {
            if (n == null) continue;
            var t = SongOf(n);
            if (t != null && seen.Add(t.Id)) o.Add(t);
        }
        return o;
    }

    public static async Task<List<Track>> Search(string q)
    {
        var j = await Post("search", new JsonObject { ["query"] = q, ["params"] = F_SONG });
        return Songs(j);
    }

    /// <summary>Songs similar to this one (YouTube's "up next" radio). Used for Smart play.</summary>
    public static async Task<List<Track>> Radio(string videoId)
    {
        try
        {
            var j = await Post("next", new JsonObject { ["videoId"] = videoId, ["playlistId"] = "RDAMVM" + videoId, ["isAudioOnly"] = true });
            var o = new List<Track>();
            foreach (var n in All(j, "playlistPanelVideoRenderer"))
            {
                if (n == null) continue;
                var vid = n["videoId"]?.ToString() ?? "";
                if (vid.Length == 0 || vid == videoId) continue;
                var by = RunsText(n["longBylineText"]?["runs"]).Split(" • ");
                o.Add(new Track { Id = vid, Title = RunsText(n["title"]?["runs"]), Artist = by.FirstOrDefault() ?? "", DurSec = DurSec(RunsText(n["lengthText"]?["runs"])), Thumb = Art(ThumbOf(n)) });
            }
            return o;
        }
        catch (Exception e) { Log.Add("radio: " + e.Message); return new(); }
    }

    // ---- direct audio streams ----
    static JsonNode? Pick(JsonNode sd)
    {
        List<JsonNode> List(string key) =>
            sd[key] is JsonArray a
                ? a.Where(f => f != null && f["width"] == null && f["url"] != null && f["signatureCipher"] == null && f["cipher"] == null).Select(f => f!).ToList()
                : new();
        var l = List("adaptiveFormats");
        if (l.Count == 0) l = List("formats");
        if (l.Count == 0) return null;
        var orig = l.Where(f => { var t = f["audioTrack"]; return t == null || (!((bool?)t["isAutoDubbed"] ?? false) && ((bool?)t["audioIsDefault"] ?? false)); }).ToList();
        if (orig.Count > 0) l = orig;
        // Windows plays AAC (MP4) everywhere; Opus needs an extra codec pack, so prefer AAC.
        long Score(JsonNode f)
        {
            var m = f["mimeType"]?.ToString() ?? "";
            long codec = m.Contains("mp4a") ? 2 : m.Contains("opus") ? 0 : 1;
            long q = (f["audioQuality"]?.ToString()) switch { "AUDIO_QUALITY_HIGH" => 3, "AUDIO_QUALITY_MEDIUM" => 2, "AUDIO_QUALITY_LOW" => 1, _ => 0 };
            long br = Math.Min((long?)f["bitrate"] ?? 0, 9_999_999_999L);
            return codec * 1_000_000_000_000L + q * 10_000_000_000L + br;
        }
        return l.OrderByDescending(Score).First();
    }

    public static async Task<YtStream> GetStream(string videoId, bool force = false)
    {
        lock (streams)
            if (!force && streams.TryGetValue(videoId, out var c) && c.ExpiresAt > DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() + 90_000) return c;
        var j = await Post("player", new JsonObject { ["videoId"] = videoId, ["contentCheckOk"] = true, ["racyCheckOk"] = true }, true);
        var st = j["playabilityStatus"]?["status"]?.ToString() ?? "";
        if (st.Length > 0 && st != "OK") throw new IOException(j["playabilityStatus"]?["reason"]?.ToString() ?? st);
        var sd = j["streamingData"] ?? throw new IOException("no stream data");
        var f = Pick(sd) ?? throw new IOException("no direct audio stream");
        long secs = long.TryParse(sd["expiresInSeconds"]?.ToString(), out var e) ? e : 18000;
        var s = new YtStream
        {
            Url = f["url"]!.ToString(), Mime = f["mimeType"]?.ToString() ?? "",
            ExpiresAt = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() + secs * 1000,
            DurationSec = int.TryParse(j["videoDetails"]?["lengthSeconds"]?.ToString(), out var d) ? d : 0,
        };
        lock (streams) streams[videoId] = s;
        return s;
    }

    public static void Forget(string videoId) { lock (streams) streams.Remove(videoId); }
}

public record Chart(string Title, string Artist, string Art);

public static class Charts
{
    static readonly HttpClient H = new() { Timeout = TimeSpan.FromSeconds(20) };

    /// <summary>Apple Music "most played" songs for a country (two letters).</summary>
    public static async Task<List<Chart>> Top(string cc = "us", int n = 25)
    {
        var o = new List<Chart>();
        try
        {
            var s = await H.GetStringAsync($"https://rss.applemarketingtools.com/api/v2/{cc.ToLower()}/music/most-played/{n}/songs.json");
            var arr = JsonNode.Parse(s)?["feed"]?["results"]?.AsArray();
            if (arr != null)
                foreach (var r in arr)
                    o.Add(new Chart(r?["name"]?.ToString() ?? "", r?["artistName"]?.ToString() ?? "", (r?["artworkUrl100"]?.ToString() ?? "").Replace("100x100", "400x400")));
        }
        catch (Exception e) { Log.Add("charts: " + e.Message); }
        return o;
    }
}
