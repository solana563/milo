using Microsoft.UI;
using Microsoft.UI.Text;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI.Xaml.Media.Imaging;
using Windows.UI;

namespace Milo;

/// <summary>Small helpers so every screen is built in code with the same look.</summary>
public static class Ui
{
    public static readonly Color AccentC = Color.FromArgb(255, 167, 139, 250);
    public static SolidColorBrush Brush(string hex)
    {
        hex = hex.TrimStart('#');
        byte a = 255; int o = 0;
        if (hex.Length == 8) { a = Convert.ToByte(hex[..2], 16); o = 2; }
        return new SolidColorBrush(Color.FromArgb(a, Convert.ToByte(hex.Substring(o, 2), 16), Convert.ToByte(hex.Substring(o + 2, 2), 16), Convert.ToByte(hex.Substring(o + 4, 2), 16)));
    }
    public static SolidColorBrush Bg => Brush("0B0B0F");
    public static SolidColorBrush Card => Brush("17171E");
    public static SolidColorBrush Card2 => Brush("22222B");
    public static SolidColorBrush Accent => new(AccentC);
    public static SolidColorBrush Dim => Brush("9A9AA8");
    public static SolidColorBrush White => Brush("FFFFFF");

    public static TextBlock Text(string s, double size = 14, bool bold = false, bool dim = false, int lines = 0)
    {
        var t = new TextBlock { Text = s, FontSize = size, Foreground = dim ? Dim : White, TextTrimming = TextTrimming.CharacterEllipsis };
        if (bold) t.FontWeight = FontWeights.SemiBold;
        if (lines > 0) { t.MaxLines = lines; t.TextWrapping = TextWrapping.Wrap; }
        return t;
    }

    public static TextBlock Heading(string s) => new() { Text = s, FontSize = 28, FontWeight = FontWeights.Bold, Foreground = White, Margin = new Thickness(0, 0, 0, 12) };
    public static TextBlock Section(string s) => new() { Text = s, FontSize = 18, FontWeight = FontWeights.SemiBold, Foreground = White, Margin = new Thickness(0, 22, 0, 10) };

    public static FontIcon Icon(string glyph, double size = 16) => new() { Glyph = glyph, FontSize = size, FontFamily = new FontFamily("Segoe MDL2 Assets") };

    public static Button IconBtn(string glyph, Action click, string tip = "", double size = 16, bool accent = false)
    {
        var b = new Button
        {
            Content = Icon(glyph, size), Background = new SolidColorBrush(Colors.Transparent), BorderThickness = new Thickness(0),
            Padding = new Thickness(10), CornerRadius = new CornerRadius(20), Foreground = accent ? Accent : White,
        };
        if (tip.Length > 0) ToolTipService.SetToolTip(b, tip);
        b.Click += (_, _) => click();
        return b;
    }

    public static Button Btn(string text, Action click, bool primary = false, string glyph = "")
    {
        object content = text;
        if (glyph.Length > 0)
        {
            var sp = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 8 };
            sp.Children.Add(Icon(glyph, 14));
            sp.Children.Add(new TextBlock { Text = text });
            content = sp;
        }
        var b = new Button { Content = content, Padding = new Thickness(18, 9, 18, 9), CornerRadius = new CornerRadius(20) };
        if (primary) { b.Background = Accent; b.Foreground = Brush("12081F"); }
        else { b.Background = Card2; b.Foreground = White; }
        b.Click += (_, _) => click();
        return b;
    }

    public static ImageSource? Img(string url, int px = 120)
    {
        if (string.IsNullOrEmpty(url)) return null;
        try
        {
            var bi = new BitmapImage { DecodePixelWidth = px };
            bi.UriSource = url.StartsWith("http") ? new Uri(url) : new Uri(url, UriKind.Absolute);
            return bi;
        }
        catch { return null; }
    }

    public static FrameworkElement Cover(Track t, double size, double radius = 8)
    {
        var url = t.IsLocal ? "" : t.Thumb;
        var g = new Grid { Width = size, Height = size, CornerRadius = new CornerRadius(radius), Background = Card2 };
        g.Children.Add(new FontIcon { Glyph = "", FontSize = size / 3, Foreground = Dim, FontFamily = new FontFamily("Segoe MDL2 Assets"), HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center });
        if (t.IsLocal)
        {
            _ = Task.Run(() => Library.CoverFor(t)).ContinueWith(r =>
            {
                if (r.Result.Length == 0) return;
                App.UI.TryEnqueue(() => g.Children.Add(new Image { Source = Img(r.Result, (int)size * 2), Stretch = Stretch.UniformToFill }));
            });
        }
        else if (url.Length > 0) g.Children.Add(new Image { Source = Img(url, (int)size * 2), Stretch = Stretch.UniformToFill });
        return g;
    }

    public static FrameworkElement TrackRow(Track t, Action play, bool actions = true)
    {
        var g = new Grid { Padding = new Thickness(8, 6, 8, 6), CornerRadius = new CornerRadius(10), Background = new SolidColorBrush(Colors.Transparent) };
        g.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        g.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
        g.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        g.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        var c = Cover(t, 46, 6); Grid.SetColumn(c, 0); g.Children.Add(c);
        var sp = new StackPanel { Margin = new Thickness(12, 0, 12, 0), VerticalAlignment = VerticalAlignment.Center };
        sp.Children.Add(Text(t.Title, 14, true));
        sp.Children.Add(Text(t.Artist, 12, false, true));
        Grid.SetColumn(sp, 1); g.Children.Add(sp);
        var d = Text(t.Dur, 12, false, true); d.VerticalAlignment = VerticalAlignment.Center; d.Margin = new Thickness(0, 0, 6, 0);
        Grid.SetColumn(d, 2); g.Children.Add(d);
        if (actions)
        {
            var bar = new StackPanel { Orientation = Orientation.Horizontal, VerticalAlignment = VerticalAlignment.Center };
            var like = IconBtn(Store.IsLiked(t) ? "" : "", () => { }, "Like", 15, Store.IsLiked(t));
            like.Click += (_, _) => { Store.ToggleLike(t); ((FontIcon)like.Content).Glyph = Store.IsLiked(t) ? "" : ""; like.Foreground = Store.IsLiked(t) ? Accent : White; };
            bar.Children.Add(like);
            bar.Children.Add(IconBtn("", () => Player.AddEnd(t), "Add to queue", 15));
            if (!t.IsLocal) bar.Children.Add(IconBtn("", () => Downloads.Enqueue(new[] { t }), "Download", 15));
            Grid.SetColumn(bar, 3); g.Children.Add(bar);
        }
        g.Tapped += (_, e) => { if (e.OriginalSource is FrameworkElement fe && IsInButton(fe)) return; play(); };
        g.PointerEntered += (_, _) => g.Background = Card;
        g.PointerExited += (_, _) => g.Background = new SolidColorBrush(Colors.Transparent);
        return g;
    }

    static bool IsInButton(DependencyObject? o)
    {
        while (o != null) { if (o is Button) return true; o = VisualTreeHelper.GetParent(o); }
        return false;
    }

    /// <summary>A square-ish card for horizontal shelves.</summary>
    public static FrameworkElement CardOf(string title, string sub, string art, Action click, double size = 150)
    {
        var sp = new StackPanel { Width = size, Spacing = 2, Background = new SolidColorBrush(Colors.Transparent) };
        sp.Children.Add(Cover(new Track { Thumb = art }, size, 12));
        var tt = Text(title, 13, true); tt.Margin = new Thickness(0, 6, 0, 0); sp.Children.Add(tt);
        sp.Children.Add(Text(sub, 12, false, true));
        sp.Tapped += (_, _) => click();
        return sp;
    }

    public static FrameworkElement Shelf(string title, IEnumerable<FrameworkElement> cards)
    {
        var root = new StackPanel();
        root.Children.Add(Section(title));
        var row = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 14 };
        foreach (var c in cards) row.Children.Add(c);
        root.Children.Add(new ScrollViewer { HorizontalScrollBarVisibility = ScrollBarVisibility.Auto, HorizontalScrollMode = ScrollMode.Enabled, VerticalScrollMode = ScrollMode.Disabled, VerticalScrollBarVisibility = ScrollBarVisibility.Disabled, Content = row });
        return root;
    }

    public static TextBox Field(string placeholder, bool password = false) => new() { PlaceholderText = placeholder, MinWidth = 280, Padding = new Thickness(12, 10, 12, 10) };

    public static PasswordBox PwField(string placeholder) => new() { PlaceholderText = placeholder, MinWidth = 280, Padding = new Thickness(12, 10, 12, 10) };

    public static async Task Toast(string msg)
    {
        var w = App.Win;
        if (w == null) return;
        w.ShowToast(msg);
        await Task.CompletedTask;
    }

    public static bool Online() { try { return System.Net.NetworkInformation.NetworkInterface.GetIsNetworkAvailable(); } catch { return true; } }

    /// <summary>Play a chart entry: find it on YouTube Music and play the first match.</summary>
    public static async void PlayByName(string title, string artist)
    {
        try
        {
            var r = await YtMusic.Search(title + " " + artist);
            var hit = r.FirstOrDefault(x => x.Title.Contains(title, StringComparison.OrdinalIgnoreCase)) ?? r.FirstOrDefault();
            if (hit == null) { await Toast("Couldn't find “" + title + "”"); return; }
            Player.PlayOne(hit);
        }
        catch (Exception e) { await Toast("Search failed: " + e.Message); }
    }
}
