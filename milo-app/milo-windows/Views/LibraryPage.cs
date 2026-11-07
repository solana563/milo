using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Windows.Storage.Pickers;

namespace Milo;

public sealed class LibraryPage : UserControl
{
    readonly StackPanel col = new() { Padding = new Thickness(28, 24, 28, 24) };
    readonly StackPanel list = new();
    readonly TextBox filter = new() { PlaceholderText = "Filter…", Padding = new Thickness(12, 8, 12, 8), MinWidth = 220 };
    string tab = "songs";
    readonly Action refresh;

    public LibraryPage()
    {
        Content = new ScrollViewer { Content = col };
        col.Children.Add(Ui.Heading("Library"));
        var bar = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 10, Margin = new Thickness(0, 0, 0, 10) };
        bar.Children.Add(Ui.Btn("Songs", () => { tab = "songs"; Fill(); }));
        bar.Children.Add(Ui.Btn("Liked", () => { tab = "liked"; Fill(); }));
        bar.Children.Add(Ui.Btn("Add folder", AddFolder, true, ""));
        bar.Children.Add(Ui.Btn("Rescan", () => { _ = Library.Scan(); _ = Ui.Toast("Scanning…"); }, false, ""));
        bar.Children.Add(filter);
        col.Children.Add(bar);
        col.Children.Add(list);
        filter.TextChanged += (_, _) => Fill();
        refresh = Fill;
        Loaded += (_, _) => { Library.Changed += refresh; Fill(); };
        Unloaded += (_, _) => Library.Changed -= refresh;
    }

    async void AddFolder()
    {
        try
        {
            var p = new FolderPicker();
            p.FileTypeFilter.Add("*");
            WinRT.Interop.InitializeWithWindow.Initialize(p, WinRT.Interop.WindowNative.GetWindowHandle(App.Win));
            var f = await p.PickSingleFolderAsync();
            if (f == null) return;
            if (!Store.P.Folders.Contains(f.Path)) { Store.P.Folders.Add(f.Path); Store.Save(); }
            _ = Library.Scan();
            _ = Ui.Toast("Added " + f.Path);
        }
        catch (Exception e) { Log.Add("folder: " + e.Message); _ = Ui.Toast("Couldn't open the folder picker"); }
    }

    void Fill()
    {
        list.Children.Clear();
        var src = tab == "liked" ? Store.P.Liked : Library.Tracks;
        var q = filter.Text.Trim();
        var items = (q.Length == 0 ? src : src.Where(t => t.Title.Contains(q, StringComparison.OrdinalIgnoreCase) || t.Artist.Contains(q, StringComparison.OrdinalIgnoreCase)).ToList()).ToList();
        if (items.Count == 0)
        {
            list.Children.Add(Ui.Text(tab == "liked" ? "No liked songs yet. Tap the heart on any song." : "No songs found. Use “Add folder” to pick where your music lives. Folders: " + string.Join(", ", Library.Roots()), 14, false, true, 4));
            return;
        }
        list.Children.Add(Ui.Text(items.Count + " songs", 12, false, true));
        var play = Ui.Btn("Play all", () => Player.PlayList(items, 0), true, ""); play.Margin = new Thickness(0, 8, 0, 8);
        play.HorizontalAlignment = HorizontalAlignment.Left;
        list.Children.Add(play);
        // a ListView-like virtualised feel: build rows in pages so thousands of songs don't freeze the window
        var rows = new StackPanel();
        list.Children.Add(rows);
        int shown = 0;
        void More()
        {
            foreach (var t in items.Skip(shown).Take(150))
            {
                var idx = items.IndexOf(t);
                rows.Children.Add(Ui.TrackRow(t, () => Player.PlayList(items, idx)));
            }
            shown += 150;
        }
        More();
        if (items.Count > shown)
        {
            Button? btn = null;
            btn = Ui.Btn("Show more", () => { More(); if (shown >= items.Count) btn!.Visibility = Visibility.Collapsed; }, false);
            btn.Margin = new Thickness(0, 10, 0, 0);
            list.Children.Add(btn);
        }
    }
}
