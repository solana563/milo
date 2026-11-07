using System.Security.Cryptography;
using System.Text;

namespace Milo;

/// <summary>Songs from folders on this PC.</summary>
public static class Library
{
    static readonly string[] Ext = { ".mp3", ".flac", ".m4a", ".aac", ".wav", ".ogg", ".opus", ".wma" };
    public static List<Track> Tracks = new();
    public static event Action? Changed;
    static readonly string CacheFile = System.IO.Path.Combine(Log.Dir, "library.json");
    static readonly string ArtDir = System.IO.Path.Combine(Log.Dir, "art");

    public static IEnumerable<string> Roots()
    {
        var l = new List<string>(Store.P.Folders);
        l.Add(Store.P.DownloadDir);
        var def = Environment.GetFolderPath(Environment.SpecialFolder.MyMusic);
        if (Store.P.Folders.Count == 0) l.Add(def);
        return l.Where(x => !string.IsNullOrWhiteSpace(x)).Distinct(StringComparer.OrdinalIgnoreCase).Where(Directory.Exists);
    }

    public static void LoadCache()
    {
        try
        {
            if (File.Exists(CacheFile))
                Tracks = System.Text.Json.JsonSerializer.Deserialize<List<Track>>(File.ReadAllText(CacheFile)) ?? new();
        }
        catch { }
    }

    public static Task Scan() => Task.Run(() =>
    {
        var found = new List<Track>();
        foreach (var root in Roots())
        {
            IEnumerable<string> files;
            try { files = Directory.EnumerateFiles(root, "*.*", new EnumerationOptions { RecurseSubdirectories = true, IgnoreInaccessible = true }); }
            catch { continue; }
            foreach (var f in files)
            {
                if (!Ext.Contains(System.IO.Path.GetExtension(f).ToLowerInvariant())) continue;
                if (found.Any(x => x.Path.Equals(f, StringComparison.OrdinalIgnoreCase))) continue;
                try
                {
                    using var tf = TagLib.File.Create(f);
                    var t = tf.Tag;
                    var title = string.IsNullOrWhiteSpace(t.Title) ? System.IO.Path.GetFileNameWithoutExtension(f) : t.Title;
                    var artist = t.FirstPerformer ?? t.FirstAlbumArtist ?? "Unknown artist";
                    found.Add(new Track
                    {
                        Id = "", Path = f, Title = title, Artist = artist, Album = t.Album ?? "",
                        DurSec = (int)tf.Properties.Duration.TotalSeconds,
                    });
                }
                catch
                {
                    found.Add(new Track { Path = f, Title = System.IO.Path.GetFileNameWithoutExtension(f), Artist = "Unknown artist" });
                }
            }
        }
        found = found.OrderBy(x => x.Artist, StringComparer.OrdinalIgnoreCase).ThenBy(x => x.Title, StringComparer.OrdinalIgnoreCase).ToList();
        Tracks = found;
        try { File.WriteAllText(CacheFile, System.Text.Json.JsonSerializer.Serialize(found)); } catch { }
        App.UI.TryEnqueue(() => Changed?.Invoke());
    });

    /// <summary>Embedded cover of a local file, saved once as a small jpg. Returns "" when there is none.</summary>
    public static string CoverFor(Track t)
    {
        if (!t.IsLocal) return t.Thumb;
        try
        {
            Directory.CreateDirectory(ArtDir);
            var name = Convert.ToHexString(SHA1.HashData(Encoding.UTF8.GetBytes(t.Path))) + ".jpg";
            var p = System.IO.Path.Combine(ArtDir, name);
            if (File.Exists(p)) return p;
            using var tf = TagLib.File.Create(t.Path);
            var pic = tf.Tag.Pictures.FirstOrDefault();
            if (pic == null) return "";
            File.WriteAllBytes(p, pic.Data.Data);
            return p;
        }
        catch { return ""; }
    }
}
