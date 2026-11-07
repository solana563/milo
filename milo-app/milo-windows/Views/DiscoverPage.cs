using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Input;

namespace Milo;

public sealed class DiscoverPage : UserControl
{
    readonly StackPanel col = new() { Padding = new Thickness(28, 24, 28, 24) };
    readonly StackPanel results = new();
    readonly TextBox box = new() { PlaceholderText = "Search songs, artists…", Padding = new Thickness(14, 10, 14, 10), FontSize = 15 };
    int token;

    public DiscoverPage()
    {
        Content = new ScrollViewer { Content = col };
        col.Children.Add(Ui.Heading("Discover"));
        box.KeyDown += (_, e) => { if (e.Key == Windows.System.VirtualKey.Enter) Search(); };
        var row = new Grid { ColumnSpacing = 10 };
        row.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
        row.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        row.Children.Add(box);
        var go = Ui.Btn("Search", Search, true, ""); Grid.SetColumn(go, 1); row.Children.Add(go);
        col.Children.Add(row);
        col.Children.Add(results);
        Loaded += (_, _) => Charts_();
    }

    async void Charts_()
    {
        if (!Ui.Online()) { results.Children.Add(Ui.Text("You're offline. Your library still works.", 14, false, true)); return; }
        var cc = System.Globalization.RegionInfo.CurrentRegion.TwoLetterISORegionName;
        var list = await Charts.Top(cc, 30);
        if (list.Count == 0) list = await Charts.Top("us", 30);
        if (box.Text.Length > 0) return;
        results.Children.Clear();
        results.Children.Add(Ui.Section("Top songs in your country"));
        int n = 1;
        foreach (var c in list)
        {
            var cc2 = c;
            var t = new Track { Title = c.Title, Artist = c.Artist, Thumb = c.Art };
            var row = Ui.TrackRow(t, () => Ui.PlayByName(cc2.Title, cc2.Artist), false);
            results.Children.Add(row);
            n++;
        }
    }

    async void Search()
    {
        var q = box.Text.Trim();
        if (q.Length == 0) return;
        var my = ++token;
        results.Children.Clear();
        results.Children.Add(Ui.Text("Searching…", 14, false, true));
        try
        {
            var r = await YtMusic.Search(q);
            if (my != token) return;
            results.Children.Clear();
            if (r.Count == 0) { results.Children.Add(Ui.Text("Nothing found for “" + q + "”.", 14, false, true)); return; }
            var head = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 10, Margin = new Thickness(0, 14, 0, 4) };
            head.Children.Add(Ui.Btn("Download all", () => { Downloads.Enqueue(r); _ = Ui.Toast("Downloading " + r.Count + " songs…"); }, false, ""));
            results.Children.Add(head);
            // a song you pick plays on its own; Smart play then adds similar songs
            foreach (var t in r) { var tt = t; results.Children.Add(Ui.TrackRow(tt, () => Player.PlayOne(tt))); }
        }
        catch (Exception e)
        {
            if (my != token) return;
            results.Children.Clear();
            results.Children.Add(Ui.Text("Search failed: " + e.Message, 14, false, true));
        }
    }
}
