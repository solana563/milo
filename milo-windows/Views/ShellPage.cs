using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;
using Windows.System;

namespace Milo;

/// <summary>Navigation rail, the current page, the player bar, and the full-screen Now Playing layer.</summary>
public sealed class ShellPage : UserControl
{
    readonly Grid root = new();
    readonly ContentControl host = new() { HorizontalContentAlignment = HorizontalAlignment.Stretch, VerticalContentAlignment = VerticalAlignment.Stretch };
    readonly StackPanel nav = new() { Spacing = 4, Padding = new Thickness(10, 16, 10, 10), Width = 190 };
    readonly Dictionary<string, Button> navBtns = new();
    readonly PlayerBar bar = new();
    readonly NowPlayingPage np = new();
    string current = "";

    public ShellPage()
    {
        root.Background = Ui.Bg;
        root.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
        root.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        root.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        root.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });

        var logo = Ui.Text("milo.", 28, true); logo.Foreground = Ui.Accent; logo.Margin = new Thickness(12, 0, 0, 14);
        nav.Children.Add(logo);
        AddNav("home", "Home", "");
        AddNav("discover", "Discover", "");
        AddNav("library", "Library", "");
        AddNav("downloads", "Downloads", "");
        AddNav("profile", "Profile", "");
        var navBg = new Border { Background = Ui.Card, Child = nav };
        Grid.SetRow(navBg, 0); Grid.SetColumn(navBg, 0); root.Children.Add(navBg);

        Grid.SetRow(host, 0); Grid.SetColumn(host, 1); root.Children.Add(host);

        bar.Expand = () => { np.Visibility = Visibility.Visible; np.Open(); };
        Grid.SetRow(bar, 1); Grid.SetColumnSpan(bar, 2); root.Children.Add(bar);

        np.Visibility = Visibility.Collapsed;
        np.Close = () => np.Visibility = Visibility.Collapsed;
        Grid.SetColumnSpan(np, 2); root.Children.Add(np);

        Content = root;
        Go("home");
        KeyboardAccelerators.Add(MakeKey(VirtualKey.Space, () => { if (FocusManager.GetFocusedElement(XamlRoot) is not TextBox and not PasswordBox) Player.Toggle(); }));
    }

    static KeyboardAccelerator MakeKey(VirtualKey k, Action a)
    {
        var ka = new KeyboardAccelerator { Key = k };
        ka.Invoked += (_, e) => { a(); e.Handled = true; };
        return ka;
    }

    void AddNav(string id, string label, string glyph)
    {
        var sp = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 14 };
        sp.Children.Add(Ui.Icon(glyph, 16));
        sp.Children.Add(new TextBlock { Text = label, FontSize = 14 });
        var b = new Button { Content = sp, HorizontalAlignment = HorizontalAlignment.Stretch, HorizontalContentAlignment = HorizontalAlignment.Left, Padding = new Thickness(14, 11, 14, 11), CornerRadius = new CornerRadius(10), Background = new SolidColorBrush(Microsoft.UI.Colors.Transparent), BorderThickness = new Thickness(0), Foreground = Ui.White };
        b.Click += (_, _) => Go(id);
        navBtns[id] = b;
        nav.Children.Add(b);
    }

    public void Go(string id)
    {
        current = id;
        foreach (var kv in navBtns)
        {
            kv.Value.Background = kv.Key == id ? Ui.Card2 : new SolidColorBrush(Microsoft.UI.Colors.Transparent);
            kv.Value.Foreground = kv.Key == id ? Ui.Accent : Ui.White;
        }
        UIElement page = id switch
        {
            "discover" => new DiscoverPage(),
            "library" => new LibraryPage(),
            "downloads" => new DownloadsPage(),
            "profile" => new ProfilePage(),
            _ => new HomePage(),
        };
        host.Content = page;
    }
}
