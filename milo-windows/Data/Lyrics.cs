using System.Text.Json.Nodes;
using System.Text.RegularExpressions;

namespace Milo;

public record LyricLine(double T, string Text);

/// <summary>Synced lyrics from LRCLIB.</summary>
public static class Lyrics
{
    static readonly HttpClient H = CreateClient();
    static HttpClient CreateClient()
    {
        var c = new HttpClient { Timeout = TimeSpan.FromSeconds(15) };
        c.DefaultRequestHeaders.TryAddWithoutValidation("User-Agent", "milo-windows/1.0");
        return c;
    }

    static string Clean(string s) => Regex.Replace(s, @"\s*[\(\[].*?[\)\]]", "").Trim();

    public static async Task<(List<LyricLine> synced, string plain)> Fetch(Track t)
    {
        try
        {
            var q = $"track_name={Uri.EscapeDataString(Clean(t.Title))}&artist_name={Uri.EscapeDataString(t.Artist)}" + (t.DurSec > 0 ? $"&duration={t.DurSec}" : "");
            JsonNode? o = null;
            try { o = JsonNode.Parse(await H.GetStringAsync("https://lrclib.net/api/get?" + q)); } catch { }
            if (o == null || (o["syncedLyrics"] == null && o["plainLyrics"] == null))
            {
                var a = JsonNode.Parse(await H.GetStringAsync("https://lrclib.net/api/search?q=" + Uri.EscapeDataString(Clean(t.Title) + " " + t.Artist)))?.AsArray();
                o = a?.FirstOrDefault(x => !string.IsNullOrEmpty(x?["syncedLyrics"]?.ToString())) ?? a?.FirstOrDefault();
            }
            var sy = o?["syncedLyrics"]?.ToString() ?? "";
            var pl = o?["plainLyrics"]?.ToString() ?? "";
            return (Parse(sy), pl);
        }
        catch (Exception e) { Log.Add("lyrics: " + e.Message); return (new(), ""); }
    }

    static List<LyricLine> Parse(string s)
    {
        var o = new List<LyricLine>();
        foreach (var line in s.Split('\n'))
        {
            var m = Regex.Match(line, @"^\[(\d+):(\d+(?:\.\d+)?)\]\s*(.*)$");
            if (!m.Success) continue;
            var t = int.Parse(m.Groups[1].Value) * 60 + double.Parse(m.Groups[2].Value, System.Globalization.CultureInfo.InvariantCulture);
            o.Add(new LyricLine(t, m.Groups[3].Value.Trim()));
        }
        return o;
    }
}
