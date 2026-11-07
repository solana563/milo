using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;

namespace Milo;

public sealed class HomePage : UserControl
{
    readonly StackPanel col = new() { Padding = new Thickness(28, 24, 28, 24) };

    public HomePage()
    {
        Content = new ScrollViewer { Content = col };
        Build();
        Loaded += (_, _) => LoadOnline();
    }

    static string Greeting()
    {
        var h = DateTime.Now.Hour;
        return h < 12 ? "Good morning" : h < 18 ? "Good afternoon" : "Good evening";
    }

    void Build()
    {
        col.Children.Clear();
        var who = Cloud.Me?.Name is { Length: > 0 } n ? ", " + n.Split(' ')[0] : "";
        col.Children.Add(Ui.Heading(Greeting() + who));

        var quick = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 10 };
        quick.Children.Add(Ui.Btn("Shuffle my music", () =>
        {
            var l = Library.Tracks.Count > 0 ? Library.Tracks : Store.P.Liked;
            if (l.Count == 0) { _ = Ui.Toast("Add a music folder in Library, or like some songs first."); return; }
            Store.P.Shuffle = true; Store.Save();
            Player.PlayList(l.ToList(), new Random().Next(l.Count), false);
        }, true, ""));
        quick.Children.Add(Ui.Btn("Liked songs", () => { if (Store.P.Liked.Count > 0) Player.PlayList(Store.P.Liked.ToList(), 0); else _ = Ui.Toast("No liked songs yet."); }, false, ""));
        col.Children.Add(quick);

        if (Store.P.History.Count > 0)
            col.Children.Add(Ui.Shelf("Recently played", Store.P.History.Take(14).Select(t =>
                Ui.CardOf(t.Title, t.Artist, t.IsLocal ? "" : t.Thumb, () => Player.PlayOne(t)))));

        // local music only shows on Home while offline (or when the person turns the option off)
        if (Library.Tracks.Count > 0 && (!Ui.Online() || !Store.P.HideLocalOnline))
            col.Children.Add(Ui.Shelf("On this PC", Library.Tracks.Take(14).Select((t, i) =>
                Ui.CardOf(t.Title, t.Artist, "", () => Player.PlayList(Library.Tracks, i)))));
    }

    async void LoadOnline()
    {
        if (!Ui.Online()) return;
        try
        {
            // taste: the artists played most
            var top = Store.P.History.GroupBy(h => h.Artist).Where(g => g.Key.Length > 0 && g.Key != "Unknown artist" && g.Key != "YouTube Music")
                .OrderByDescending(g => g.Sum(h => Store.P.Plays.GetValueOrDefault(h.Key, 1))).Take(3).Select(g => g.Key).ToList();
            foreach (var a in top)
            {
                var r = await YtMusic.Search(a);
                if (r.Count == 0) continue;
                col.Children.Add(Ui.Shelf("More like " + a, r.Take(12).Select(t => Ui.CardOf(t.Title, t.Artist, t.Thumb, () => Player.PlayOne(t)))));
            }
            var cc = System.Globalization.RegionInfo.CurrentRegion.TwoLetterISORegionName;
            var charts = await Charts.Top(cc, 20);
            if (charts.Count == 0) charts = await Charts.Top("us", 20);
            if (charts.Count > 0)
                col.Children.Add(Ui.Shelf("Trending right now", charts.Select(c => Ui.CardOf(c.Title, c.Artist, c.Art, () => Ui.PlayByName(c.Title, c.Artist)))));
        }
        catch (Exception e) { Log.Add("home: " + e.Message); }
    }
}
