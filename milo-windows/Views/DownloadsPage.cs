using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;

namespace Milo;

public sealed class DownloadsPage : UserControl
{
    readonly StackPanel col = new() { Padding = new Thickness(28, 24, 28, 24) };
    readonly StackPanel list = new();
    readonly Action refresh;

    public DownloadsPage()
    {
        Content = new ScrollViewer { Content = col };
        col.Children.Add(Ui.Heading("Downloads"));
        col.Children.Add(Ui.Text("Saved to " + Store.P.DownloadDir, 13, false, true, 2));
        var row = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 10, Margin = new Thickness(0, 10, 0, 10) };
        row.Children.Add(Ui.Btn("Open folder", () =>
        {
            try { Directory.CreateDirectory(Store.P.DownloadDir); System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(Store.P.DownloadDir) { UseShellExecute = true }); } catch { }
        }, false, ""));
        row.Children.Add(Ui.Btn("Play all downloads", () =>
        {
            var l = Library.Tracks.Where(t => t.Path.StartsWith(Store.P.DownloadDir, StringComparison.OrdinalIgnoreCase)).ToList();
            if (l.Count > 0) Player.PlayList(l, 0); else _ = Ui.Toast("Nothing downloaded yet.");
        }, true, ""));
        col.Children.Add(row);
        col.Children.Add(list);
        refresh = Fill;
        Loaded += (_, _) => { Downloads.Changed += refresh; Library.Changed += refresh; Fill(); };
        Unloaded += (_, _) => { Downloads.Changed -= refresh; Library.Changed -= refresh; };
    }

    void Fill()
    {
        list.Children.Clear();
        if (Downloads.Items.Count > 0)
        {
            list.Children.Add(Ui.Section("This session"));
            foreach (var it in Downloads.Items.ToList())
            {
                var g = new Grid { Padding = new Thickness(0, 6, 0, 6), ColumnSpacing = 12 };
                g.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
                g.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
                var sp = new StackPanel();
                sp.Children.Add(Ui.Text(it.T.Title + " — " + it.T.Artist, 14, true));
                if (it.State == "downloading") sp.Children.Add(new ProgressBar { Minimum = 0, Maximum = 1, Value = it.Progress, Foreground = Ui.Accent, Margin = new Thickness(0, 4, 0, 0) });
                var st = Ui.Text(it.State == "failed" ? "Failed: " + it.Error : it.State, 12, false, true);
                Grid.SetColumn(st, 1); st.VerticalAlignment = VerticalAlignment.Center;
                g.Children.Add(sp); g.Children.Add(st);
                list.Children.Add(g);
            }
        }
        var have = Library.Tracks.Where(t => t.Path.StartsWith(Store.P.DownloadDir, StringComparison.OrdinalIgnoreCase)).ToList();
        list.Children.Add(Ui.Section("On this PC (" + have.Count + ")"));
        if (have.Count == 0) list.Children.Add(Ui.Text("Tap the download arrow on any song to save it for offline.", 14, false, true));
        for (int i = 0; i < Math.Min(have.Count, 200); i++) { var idx = i; list.Children.Add(Ui.TrackRow(have[i], () => Player.PlayList(have, idx))); }
    }
}
