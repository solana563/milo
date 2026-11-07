using Microsoft.UI.Dispatching;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Controls.Primitives;

namespace Milo;

public sealed class PlayerBar : UserControl
{
    public Action? Expand;
    readonly Grid g = new() { Height = 92, Padding = new Thickness(14, 8, 14, 8), Background = Ui.Card };
    readonly Border art = new() { Width = 56, Height = 56 };
    readonly TextBlock title = Ui.Text("Nothing playing", 14, true);
    readonly TextBlock artist = Ui.Text("", 12, false, true);
    readonly Slider seek = new() { Minimum = 0, Maximum = 1, MinWidth = 200, VerticalAlignment = VerticalAlignment.Center, Height = 32 };
    readonly TextBlock pos = Ui.Text("0:00", 12, false, true);
    readonly TextBlock dur = Ui.Text("0:00", 12, false, true);
    readonly Button play, like, shuffle, repeat;
    readonly DispatcherTimer timer = new() { Interval = TimeSpan.FromMilliseconds(500) };
    bool updating, dragging;
    string shownKey = "";

    static string Fmt(double s) { var t = TimeSpan.FromSeconds(Math.Max(0, s)); return $"{(int)t.TotalMinutes}:{t.Seconds:00}"; }

    public PlayerBar()
    {
        g.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
        g.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(2, GridUnitType.Star) });
        g.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });

        var left = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 12, VerticalAlignment = VerticalAlignment.Center };
        left.Children.Add(art);
        var tx = new StackPanel { VerticalAlignment = VerticalAlignment.Center, MaxWidth = 240 };
        tx.Children.Add(title); tx.Children.Add(artist);
        left.Children.Add(tx);
        left.Tapped += (_, _) => Expand?.Invoke();
        Grid.SetColumn(left, 0); g.Children.Add(left);

        shuffle = Ui.IconBtn("", () => { Store.P.Shuffle = !Store.P.Shuffle; Store.Save(); Refresh(); }, "Shuffle", 15);
        var prev = Ui.IconBtn("", Player.Prev, "Previous", 18);
        play = Ui.IconBtn("", Player.Toggle, "Play / pause", 22);
        var next = Ui.IconBtn("", () => Player.Next(), "Next", 18);
        repeat = Ui.IconBtn("", () => { Store.P.Repeat = (Store.P.Repeat + 1) % 3; Store.Save(); Refresh(); }, "Repeat", 15);
        var ctl = new StackPanel { Orientation = Orientation.Horizontal, HorizontalAlignment = HorizontalAlignment.Center, Spacing = 4 };
        foreach (var b in new[] { shuffle, prev, play, next, repeat }) ctl.Children.Add(b);
        var sr = new Grid { Margin = new Thickness(0, -4, 0, 0) };
        sr.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        sr.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
        sr.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        pos.VerticalAlignment = VerticalAlignment.Center; dur.VerticalAlignment = VerticalAlignment.Center;
        Grid.SetColumn(seek, 1); seek.Margin = new Thickness(10, 0, 10, 0);
        Grid.SetColumn(dur, 2);
        sr.Children.Add(pos); sr.Children.Add(seek); sr.Children.Add(dur);
        var mid = new StackPanel { VerticalAlignment = VerticalAlignment.Center };
        mid.Children.Add(ctl); mid.Children.Add(sr);
        Grid.SetColumn(mid, 1); g.Children.Add(mid);

        like = Ui.IconBtn("", () => { if (Player.Current != null) { Store.ToggleLike(Player.Current); Refresh(); } }, "Like", 16);
        var vol = new Slider { Minimum = 0, Maximum = 1, StepFrequency = 0.01, Width = 110, Value = Store.P.Volume, VerticalAlignment = VerticalAlignment.Center };
        vol.ValueChanged += (_, e) => Player.SetVolume(e.NewValue);
        var q = Ui.IconBtn("", () => Expand?.Invoke(), "Now playing", 16);
        var right = new StackPanel { Orientation = Orientation.Horizontal, HorizontalAlignment = HorizontalAlignment.Right, VerticalAlignment = VerticalAlignment.Center, Spacing = 6 };
        right.Children.Add(like); right.Children.Add(Ui.Icon("", 14)); right.Children.Add(vol); right.Children.Add(q);
        Grid.SetColumn(right, 2); g.Children.Add(right);

        seek.ValueChanged += (_, e) => { if (!updating && Player.Duration > 0) Player.Seek(e.NewValue * Player.Duration); };
        seek.AddHandler(UIElement.PointerPressedEvent, new Microsoft.UI.Xaml.Input.PointerEventHandler((_, _) => dragging = true), true);
        seek.AddHandler(UIElement.PointerReleasedEvent, new Microsoft.UI.Xaml.Input.PointerEventHandler((_, _) => dragging = false), true);
        seek.AddHandler(UIElement.PointerCaptureLostEvent, new Microsoft.UI.Xaml.Input.PointerEventHandler((_, _) => dragging = false), true);
        Content = g;
        Player.Changed += Refresh;
        timer.Tick += (_, _) => Tick();
        Loaded += (_, _) => { timer.Start(); Refresh(); };
        Unloaded += (_, _) => { timer.Stop(); Player.Changed -= Refresh; };
    }

    void Tick()
    {
        var d = Player.Duration;
        if (dragging) { pos.Text = Fmt(seek.Value * d); return; }
        updating = true;
        seek.Value = d > 0 ? Math.Min(1, Player.Position / d) : 0;
        seek.Maximum = 1; seek.StepFrequency = 0.001;
        updating = false;
        pos.Text = Fmt(Player.Position);
        dur.Text = Fmt(d);
    }

    void Refresh()
    {
        var t = Player.Current;
        title.Text = t?.Title ?? "Nothing playing";
        artist.Text = Player.Error.Length > 0 ? Player.Error : (t?.Artist ?? "");
        ((FontIcon)play.Content).Glyph = Player.Playing ? "" : "";
        shuffle.Foreground = Store.P.Shuffle ? Ui.Accent : Ui.White;
        repeat.Foreground = Store.P.Repeat > 0 ? Ui.Accent : Ui.White;
        ((FontIcon)repeat.Content).Glyph = Store.P.Repeat == 2 ? "" : "";
        var liked = t != null && Store.IsLiked(t);
        ((FontIcon)like.Content).Glyph = liked ? "" : "";
        like.Foreground = liked ? Ui.Accent : Ui.White;
        var key = t?.Key ?? "";
        if (key != shownKey) { shownKey = key; art.Child = t == null ? null : Ui.Cover(t, 56, 8); }
    }
}
