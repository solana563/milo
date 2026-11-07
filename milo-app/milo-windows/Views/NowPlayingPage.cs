using Microsoft.UI.Dispatching;
using Microsoft.UI.Text;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;

namespace Milo;

/// <summary>Big cover, synced lyrics and the queue. The player bar below stays in charge of the controls.</summary>
public sealed class NowPlayingPage : UserControl
{
    public Action? Close;
    readonly Grid g = new() { Background = Ui.Bg, Padding = new Thickness(32, 20, 32, 12) };
    readonly Border artBox = new() { HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center };
    readonly TextBlock title = Ui.Text("", 26, true);
    readonly TextBlock artist = Ui.Text("", 16, false, true);
    readonly ScrollViewer lyricScroll = new() { VerticalScrollBarVisibility = ScrollBarVisibility.Hidden };
    readonly StackPanel lyricCol = new() { Spacing = 14, Padding = new Thickness(0, 120, 0, 240) };
    readonly StackPanel queueCol = new() { Spacing = 2 };
    readonly ScrollViewer queueScroll = new() { Visibility = Visibility.Collapsed };
    readonly DispatcherTimer timer = new() { Interval = TimeSpan.FromMilliseconds(300) };
    List<LyricLine> lines = new();
    readonly List<TextBlock> lineViews = new();
    int active = -1;
    string key = "";
    bool showQueue;

    public NowPlayingPage()
    {
        g.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
        g.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1.2, GridUnitType.Star) });
        g.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        g.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });

        var top = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 8 };
        top.Children.Add(Ui.IconBtn("", () => Close?.Invoke(), "Back", 16));
        top.Children.Add(Ui.Btn("Lyrics", () => SetQueue(false)));
        top.Children.Add(Ui.Btn("Up next", () => SetQueue(true)));
        Grid.SetColumnSpan(top, 2); g.Children.Add(top);

        var left = new StackPanel { VerticalAlignment = VerticalAlignment.Center, HorizontalAlignment = HorizontalAlignment.Center, Spacing = 6, MaxWidth = 420 };
        left.Children.Add(artBox);
        title.Margin = new Thickness(0, 14, 0, 0); title.TextAlignment = TextAlignment.Center; title.HorizontalAlignment = HorizontalAlignment.Center;
        artist.HorizontalAlignment = HorizontalAlignment.Center;
        left.Children.Add(title); left.Children.Add(artist);
        Grid.SetRow(left, 1); g.Children.Add(left);

        lyricScroll.Content = lyricCol;
        queueScroll.Content = queueCol;
        var right = new Grid();
        right.Children.Add(lyricScroll); right.Children.Add(queueScroll);
        Grid.SetRow(right, 1); Grid.SetColumn(right, 1); right.Margin = new Thickness(24, 0, 0, 0);
        g.Children.Add(right);
        Content = g;

        timer.Tick += (_, _) => Tick();
        Unloaded += (_, _) => timer.Stop();
        Player.Changed += OnChanged;
    }

    public void Open() { timer.Start(); OnChanged(); }

    void SetQueue(bool q)
    {
        showQueue = q;
        queueScroll.Visibility = q ? Visibility.Visible : Visibility.Collapsed;
        lyricScroll.Visibility = q ? Visibility.Collapsed : Visibility.Visible;
        if (q) FillQueue();
    }

    void FillQueue()
    {
        queueCol.Children.Clear();
        for (int i = 0; i < Player.Queue.Count; i++)
        {
            var idx = i;
            var row = Ui.TrackRow(Player.Queue[i], () => Player.Jump(idx), false);
            if (i == Player.Index) ((Grid)row).Background = Ui.Card2;
            queueCol.Children.Add(row);
        }
    }

    void OnChanged()
    {
        var t = Player.Current;
        if (showQueue) FillQueue();
        if (t == null) { title.Text = "Nothing playing"; artist.Text = ""; return; }
        if (t.Key == key) return;
        key = t.Key;
        title.Text = t.Title; artist.Text = t.Artist;
        var size = 340;
        artBox.Child = Ui.Cover(t, size, 18);
        lines = new(); lineViews.Clear(); active = -1;
        lyricCol.Children.Clear();
        lyricCol.Children.Add(Ui.Text("Looking for lyrics…", 18, false, true));
        _ = LoadLyrics(t);
    }

    async Task LoadLyrics(Track t)
    {
        var (synced, plain) = await Lyrics.Fetch(t);
        if (Player.Current?.Key != t.Key) return;
        lyricCol.Children.Clear();
        lines = synced;
        if (synced.Count > 0)
        {
            foreach (var l in synced)
            {
                var tb = new TextBlock { Text = l.Text.Length > 0 ? l.Text : "♪", FontSize = 26, FontWeight = FontWeights.Bold, Foreground = Ui.Brush("5A5A68"), TextWrapping = TextWrapping.Wrap };
                var ll = l;
                tb.Tapped += (_, _) => Player.Seek(ll.T);
                lineViews.Add(tb); lyricCol.Children.Add(tb);
            }
        }
        else if (plain.Length > 0)
            lyricCol.Children.Add(new TextBlock { Text = plain, FontSize = 18, Foreground = Ui.White, TextWrapping = TextWrapping.Wrap, LineHeight = 30 });
        else
            lyricCol.Children.Add(Ui.Text("No lyrics found for this song.", 18, false, true));
    }

    void Tick()
    {
        if (lines.Count == 0 || Visibility != Visibility.Visible) return;
        var p = Player.Position + 0.25;
        int a = -1;
        for (int i = 0; i < lines.Count; i++) { if (lines[i].T <= p) a = i; else break; }
        if (a == active) return;
        if (active >= 0 && active < lineViews.Count) lineViews[active].Foreground = Ui.Brush("5A5A68");
        active = a;
        if (a >= 0 && a < lineViews.Count)
        {
            lineViews[a].Foreground = Ui.White;
            try { lineViews[a].StartBringIntoView(new BringIntoViewOptions { VerticalAlignmentRatio = 0.35, AnimationDesired = true }); } catch { }
        }
    }
}
