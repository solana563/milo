using System.Net.Http.Headers;

namespace Milo;

public class DownloadItem
{
    public Track T = new();
    public double Progress;
    public string State = "waiting"; // waiting, downloading, done, failed
    public string Error = "";
}

/// <summary>Saves YouTube songs to the downloads folder so they play offline and appear in Library.</summary>
public static class Downloads
{
    public static readonly List<DownloadItem> Items = new();
    public static event Action? Changed;
    static readonly HttpClient H = new() { Timeout = TimeSpan.FromMinutes(10) };
    static bool running;

    static string Safe(string s)
    {
        foreach (var c in System.IO.Path.GetInvalidFileNameChars()) s = s.Replace(c, '_');
        return s.Trim();
    }

    public static bool Has(Track t) => Library.Tracks.Any(x => x.IsLocal && System.IO.Path.GetFileNameWithoutExtension(x.Path).Equals(Safe($"{t.Artist} - {t.Title}"), StringComparison.OrdinalIgnoreCase));

    public static void Enqueue(IEnumerable<Track> ts)
    {
        foreach (var t in ts)
        {
            if (t.IsLocal || Items.Any(i => i.T.Id == t.Id && i.State != "failed") || Has(t)) continue;
            Items.Add(new DownloadItem { T = t });
        }
        Changed?.Invoke();
        if (!running) { running = true; _ = Task.Run(Run); }
    }

    static async Task Run()
    {
        try
        {
            while (true)
            {
                var it = Items.FirstOrDefault(i => i.State == "waiting");
                if (it == null) break;
                it.State = "downloading"; Ping();
                try { await One(it); it.State = "done"; it.Progress = 1; }
                catch (Exception e) { it.State = "failed"; it.Error = e.Message; Log.Add("download: " + e); }
                Ping();
            }
        }
        finally { running = false; }
        await Library.Scan();
    }

    static void Ping() => App.UI.TryEnqueue(() => Changed?.Invoke());

    static async Task One(DownloadItem it)
    {
        var s = await YtMusic.GetStream(it.T.Id);
        var ext = s.Mime.Contains("mp4") ? ".m4a" : s.Mime.Contains("webm") ? ".webm" : ".m4a";
        Directory.CreateDirectory(Store.P.DownloadDir);
        var path = System.IO.Path.Combine(Store.P.DownloadDir, Safe($"{it.T.Artist} - {it.T.Title}") + ext);
        var tmp = path + ".part";
        long total = 0, got = 0;
        const long CH = 2 * 1024 * 1024;
        using (var fs = File.Create(tmp))
        {
            while (true)
            {
                var req = new HttpRequestMessage(HttpMethod.Get, s.Url);
                req.Headers.TryAddWithoutValidation("User-Agent", YtMusic.STREAM_UA);
                req.Headers.Range = new RangeHeaderValue(got, got + CH - 1);
                using var r = await H.SendAsync(req, HttpCompletionOption.ResponseHeadersRead);
                if (!r.IsSuccessStatusCode) throw new IOException("download refused (" + (int)r.StatusCode + ")");
                if (total == 0) total = r.Content.Headers.ContentRange?.Length ?? r.Content.Headers.ContentLength ?? 0;
                var data = await r.Content.ReadAsByteArrayAsync();
                if (data.Length == 0) break;
                await fs.WriteAsync(data);
                got += data.Length;
                if (total > 0) { it.Progress = Math.Min(1.0, (double)got / total); Ping(); }
                if (total > 0 && got >= total) break;
            }
        }
        if (File.Exists(path)) File.Delete(path);
        File.Move(tmp, path);
        try
        {
            using var tf = TagLib.File.Create(path);
            tf.Tag.Title = it.T.Title;
            tf.Tag.Performers = new[] { it.T.Artist };
            tf.Tag.Album = it.T.Album;
            tf.Save();
        }
        catch { }
    }
}
