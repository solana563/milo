using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace Milo;

public static class Log
{
    static readonly object L = new();
    public static string Dir
    {
        get
        {
            var d = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "milo");
            Directory.CreateDirectory(d);
            return d;
        }
    }
    public static void Add(string s)
    {
        try { lock (L) File.AppendAllText(Path.Combine(Dir, "log.txt"), DateTime.Now.ToString("s") + " " + s + Environment.NewLine); } catch { }
    }
}

/// <summary>A song: from YouTube Music (Path empty, Id = video id) or a file on this PC (Path set).</summary>
public class Track
{
    public string Id { get; set; } = "";
    public string Title { get; set; } = "";
    public string Artist { get; set; } = "";
    public string Album { get; set; } = "";
    public string Thumb { get; set; } = "";
    public int DurSec { get; set; }
    public string Path { get; set; } = "";
    [JsonIgnore] public bool IsLocal => Path.Length > 0;
    [JsonIgnore] public string Key => IsLocal ? "f:" + Path : "y:" + Id;
    [JsonIgnore] public string Dur => DurSec > 0 ? $"{DurSec / 60}:{DurSec % 60:00}" : "";
}

public class Prefs
{
    public List<string> Folders { get; set; } = new();
    public string DownloadDir { get; set; } = "";
    public double Volume { get; set; } = 0.8;
    public bool Shuffle { get; set; }
    public int Repeat { get; set; } // 0 off, 1 all, 2 one
    public bool Guest { get; set; }
    public bool HideLocalOnline { get; set; } = true;
    public List<Track> Liked { get; set; } = new();
    public List<Track> History { get; set; } = new();
    public Dictionary<string, int> Plays { get; set; } = new();
}

/// <summary>Everything the app remembers, in %APPDATA%\milo. Tokens are encrypted with Windows DPAPI.</summary>
public static class Store
{
    static readonly string PrefsFile = System.IO.Path.Combine(Log.Dir, "prefs.json");
    static readonly string SessionFile = System.IO.Path.Combine(Log.Dir, "session.bin");
    public static Prefs P = new();

    public static void Load()
    {
        try { if (File.Exists(PrefsFile)) P = JsonSerializer.Deserialize<Prefs>(File.ReadAllText(PrefsFile)) ?? new(); } catch (Exception e) { Log.Add("prefs: " + e.Message); }
        if (string.IsNullOrEmpty(P.DownloadDir)) P.DownloadDir = System.IO.Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.MyMusic), "milo");
    }

    public static void Save()
    {
        try { File.WriteAllText(PrefsFile, JsonSerializer.Serialize(P)); } catch (Exception e) { Log.Add("save: " + e.Message); }
    }

    public static void SaveSession(string json)
    {
        try { File.WriteAllBytes(SessionFile, ProtectedData.Protect(Encoding.UTF8.GetBytes(json), null, DataProtectionScope.CurrentUser)); } catch (Exception e) { Log.Add("session: " + e.Message); }
    }

    public static string? LoadSession()
    {
        try { return File.Exists(SessionFile) ? Encoding.UTF8.GetString(ProtectedData.Unprotect(File.ReadAllBytes(SessionFile), null, DataProtectionScope.CurrentUser)) : null; } catch { return null; }
    }

    public static void ClearSession() { try { File.Delete(SessionFile); } catch { } }

    public static bool IsLiked(Track t) => P.Liked.Any(x => x.Key == t.Key);
    public static void ToggleLike(Track t)
    {
        var e = P.Liked.FirstOrDefault(x => x.Key == t.Key);
        if (e != null) P.Liked.Remove(e); else P.Liked.Insert(0, t);
        Save();
    }

    public static void Played(Track t)
    {
        P.History.RemoveAll(x => x.Key == t.Key);
        P.History.Insert(0, t);
        if (P.History.Count > 100) P.History.RemoveRange(100, P.History.Count - 100);
        P.Plays[t.Key] = P.Plays.GetValueOrDefault(t.Key) + 1;
        Save();
    }
}
