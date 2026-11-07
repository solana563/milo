using System.Net;
using Windows.Media;
using Windows.Media.Core;
using Windows.Media.Playback;
using Windows.Storage.Streams;

namespace Milo;

/// <summary>Plays the queue. Local files play directly; YouTube songs play through a tiny loopback proxy that adds the headers YouTube wants.</summary>
public static class Player
{
    public static readonly List<Track> Queue = new();
    public static int Index = -1;
    public static bool Smart;
    public static bool Playing;
    public static string Error = "";
    public static Track? Current => Index >= 0 && Index < Queue.Count ? Queue[Index] : null;
    public static event Action? Changed;
    public static double Position => mp?.PlaybackSession.Position.TotalSeconds ?? 0;
    public static double Duration
    {
        get
        {
            var d = mp?.PlaybackSession.NaturalDuration.TotalSeconds ?? 0;
            return d > 0 && d < 100000 ? d : (Current?.DurSec ?? 0);
        }
    }

    static MediaPlayer? mp;
    static HttpListener? proxy;
    static int port;
    static readonly HttpClient H = new() { Timeout = Timeout.InfiniteTimeSpan };
    static readonly Random Rng = new();
    static int loading;

    public static bool Available => mp != null;
    public const string NoMediaMsg = "Windows can't play audio here: the Media Feature Pack is missing. Settings > System > Optional features > Add a feature > \"Media Feature Pack\", then restart.";

    public static void Init()
    {
        try { StartProxy(); } catch (Exception e) { Log.Add("proxy: " + e.Message); }
        try { InitPlayer(); }
        catch (Exception e)
        {
            Log.Add("player init: " + e);
            mp = null;
            Error = NoMediaMsg;
        }
    }

    static void InitPlayer()
    {
        mp = new MediaPlayer { AudioCategory = MediaPlayerAudioCategory.Media, Volume = Store.P.Volume };
        mp.CommandManager.NextBehavior.EnablingRule = MediaCommandEnablingRule.Always;
        mp.CommandManager.PreviousBehavior.EnablingRule = MediaCommandEnablingRule.Always;
        mp.CommandManager.NextReceived += (_, a) => { a.Handled = true; App.UI.TryEnqueue(() => Next()); };
        mp.CommandManager.PreviousReceived += (_, a) => { a.Handled = true; App.UI.TryEnqueue(() => Prev()); };
        mp.MediaEnded += (_, _) => App.UI.TryEnqueue(() => { if (Store.P.Repeat == 2) Replay(); else Next(true); });
        mp.MediaFailed += (_, e) => App.UI.TryEnqueue(() =>
        {
            Error = "Couldn't play this song (" + e.ErrorMessage + ")";
            Log.Add("media failed: " + e.Error + " " + e.ErrorMessage);
            if (Current is { IsLocal: false } c) YtMusic.Forget(c.Id);
            Playing = false; Changed?.Invoke();
        });
        mp.PlaybackSession.PlaybackStateChanged += (s, _) => App.UI.TryEnqueue(() =>
        {
            Playing = s.PlaybackState == MediaPlaybackState.Playing || s.PlaybackState == MediaPlaybackState.Buffering || s.PlaybackState == MediaPlaybackState.Opening;
            Changed?.Invoke();
        });
    }

    // ---- loopback proxy ----
    static void StartProxy()
    {
        var rnd = new Random();
        for (int i = 0; i < 20 && proxy == null; i++)
        {
            var p = rnd.Next(49200, 49900);
            foreach (var host in new[] { "127.0.0.1", "localhost" })
            {
                try
                {
                    var l = new HttpListener();
                    l.Prefixes.Add($"http://{host}:{p}/");
                    l.Start();
                    proxy = l; port = p;
                    break;
                }
                catch { }
            }
        }
        if (proxy == null) { Log.Add("proxy failed to start"); return; }
        _ = Task.Run(async () =>
        {
            while (proxy.IsListening)
            {
                try { var c = await proxy.GetContextAsync(); _ = Task.Run(() => Serve(c)); }
                catch { }
            }
        });
    }

    static async Task Serve(HttpListenerContext c)
    {
        try
        {
            var id = c.Request.Url!.AbsolutePath.Trim('/').Split('/').Last();
            id = System.IO.Path.GetFileNameWithoutExtension(id);
            var st = await YtMusic.GetStream(id);
            for (int attempt = 0; attempt < 2; attempt++)
            {
                var req = new HttpRequestMessage(HttpMethod.Get, st.Url);
                req.Headers.TryAddWithoutValidation("User-Agent", YtMusic.STREAM_UA);
                var range = c.Request.Headers["Range"];
                req.Headers.TryAddWithoutValidation("Range", string.IsNullOrEmpty(range) ? "bytes=0-" : range);
                using var r = await H.SendAsync(req, HttpCompletionOption.ResponseHeadersRead);
                if ((int)r.StatusCode == 403 && attempt == 0)
                {
                    YtMusic.Forget(id);
                    st = await YtMusic.GetStream(id, true);
                    continue;
                }
                c.Response.StatusCode = (int)r.StatusCode;
                c.Response.ContentType = r.Content.Headers.ContentType?.ToString() ?? "audio/mp4";
                c.Response.AddHeader("Accept-Ranges", "bytes");
                if (r.Content.Headers.ContentRange != null) c.Response.AddHeader("Content-Range", r.Content.Headers.ContentRange.ToString());
                if (r.Content.Headers.ContentLength is long len) c.Response.ContentLength64 = len;
                using var src = await r.Content.ReadAsStreamAsync();
                await src.CopyToAsync(c.Response.OutputStream);
                break;
            }
        }
        catch (Exception e) { if (!(e is HttpListenerException) && !(e is IOException)) Log.Add("proxy: " + e.Message); }
        finally { try { c.Response.Close(); } catch { } }
    }

    // ---- queue ----
    public static void PlayList(List<Track> list, int idx, bool smart = false)
    {
        Queue.Clear(); Queue.AddRange(list);
        Smart = smart;
        Start(idx);
    }

    /// <summary>One song on its own; Smart play then adds similar songs after it (YouTube songs only).</summary>
    public static void PlayOne(Track t)
    {
        Queue.Clear(); Queue.Add(t);
        Smart = !t.IsLocal;
        Start(0);
    }

    public static void AddNext(Track t)
    {
        if (Queue.Count == 0) { PlayOne(t); return; }
        Queue.Insert(Math.Min(Index + 1, Queue.Count), t);
        Changed?.Invoke();
    }

    public static void AddEnd(Track t)
    {
        if (Queue.Count == 0) { PlayOne(t); return; }
        Queue.Add(t);
        Changed?.Invoke();
    }

    static void Start(int idx)
    {
        Index = Math.Clamp(idx, 0, Math.Max(0, Queue.Count - 1));
        Load();
    }

    static async void Load()
    {
        var t = Current;
        if (t == null) return;
        if (mp == null) { Error = NoMediaMsg; Changed?.Invoke(); return; }
        var my = ++loading;
        Error = "";
        Changed?.Invoke();
        try
        {
            Uri uri;
            if (t.IsLocal) uri = new Uri(t.Path);
            else
            {
                if (port == 0) throw new IOException("audio helper isn't running");
                await YtMusic.GetStream(t.Id); // fail early with a clear message, and warm the cache
                uri = new Uri($"http://127.0.0.1:{port}/s/{t.Id}.m4a");
            }
            if (my != loading) return;
            var item = new MediaPlaybackItem(MediaSource.CreateFromUri(uri));
            var props = item.GetDisplayProperties();
            props.Type = MediaPlaybackType.Music;
            props.MusicProperties.Title = t.Title;
            props.MusicProperties.Artist = t.Artist;
            props.MusicProperties.AlbumTitle = t.Album;
            var cover = Library.CoverFor(t);
            if (cover.Length > 0)
            {
                try { props.Thumbnail = RandomAccessStreamReference.CreateFromUri(cover.StartsWith("http") ? new Uri(cover) : new Uri(cover)); } catch { }
            }
            item.ApplyDisplayProperties(props);
            mp.Source = item;
            mp.Play();
            Store.Played(t);
            Changed?.Invoke();
            _ = EnsureMore();
        }
        catch (Exception e)
        {
            Error = "Couldn't play “" + t.Title + "”: " + e.Message;
            Log.Add("load: " + e);
            Playing = false;
            Changed?.Invoke();
        }
    }

    /// <summary>Smart play: keep a few similar songs lined up after the current one.</summary>
    static async Task EnsureMore()
    {
        if (!Smart) return;
        var t = Current;
        if (t == null || t.IsLocal || Queue.Count - Index > 3) return;
        var more = await YtMusic.Radio(t.Id);
        App.UI.TryEnqueue(() =>
        {
            foreach (var m in more)
                if (!Queue.Any(q => q.Key == m.Key)) Queue.Add(m);
            Changed?.Invoke();
        });
    }

    static void Replay() { if (mp != null) { mp.PlaybackSession.Position = TimeSpan.Zero; mp.Play(); } }

    public static void Next(bool auto = false)
    {
        if (Queue.Count == 0) return;
        if (Store.P.Shuffle && Queue.Count > 1)
        {
            int n; do n = Rng.Next(Queue.Count); while (n == Index);
            Start(n); return;
        }
        if (Index + 1 < Queue.Count) { Start(Index + 1); return; }
        if (Store.P.Repeat == 1) { Start(0); return; }
        if (auto) { Playing = false; Changed?.Invoke(); }
    }

    public static void Prev()
    {
        if (Queue.Count == 0) return;
        if (Position > 4 || Index == 0) { Seek(0); return; }
        Start(Index - 1);
    }

    public static void Toggle()
    {
        if (mp == null) return;
        if (mp.Source == null) { if (Current != null) Load(); return; }
        if (Playing) mp.Pause(); else mp.Play();
    }

    public static void Seek(double sec) { if (mp != null) mp.PlaybackSession.Position = TimeSpan.FromSeconds(Math.Max(0, sec)); }

    public static void SetVolume(double v)
    {
        Store.P.Volume = Math.Clamp(v, 0, 1);
        if (mp != null) mp.Volume = Store.P.Volume;
        Store.Save();
    }

    public static void Jump(int i) { if (i >= 0 && i < Queue.Count) Start(i); }
}
