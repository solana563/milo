using Microsoft.UI;
using Microsoft.UI.Windowing;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;

namespace Milo;

public sealed partial class MainWindow : Window
{
    readonly Grid toastHost = new();
    readonly Border toast = new() { Opacity = 0, IsHitTestVisible = false, VerticalAlignment = VerticalAlignment.Bottom, HorizontalAlignment = HorizontalAlignment.Center, Margin = new Thickness(0, 0, 0, 110), Padding = new Thickness(18, 10, 18, 10), CornerRadius = new CornerRadius(20), Background = Ui.Card2 };
    readonly TextBlock toastText = new() { Foreground = Ui.White, FontSize = 14 };

    public MainWindow()
    {
        InitializeComponent();
        Title = "milo.";
        try { SystemBackdrop = new Microsoft.UI.Xaml.Media.MicaBackdrop(); } catch { }
        try
        {
            AppWindow.Resize(new Windows.Graphics.SizeInt32(1280, 820));
            var ico = System.IO.Path.Combine(AppContext.BaseDirectory, "Assets", "milo.ico");
            if (File.Exists(ico)) AppWindow.SetIcon(ico);
        }
        catch { }
        Root.Background = Ui.Bg;
        toast.Child = toastText;
        toastHost.IsHitTestVisible = false;
        toastHost.Children.Add(toast);
        Boot();
    }

    async void Boot()
    {
        try
        {
            Store.Load();
            Library.LoadCache();
            try { Player.Init(); } catch (Exception e) { Log.Add("player: " + e); }
            Root.Children.Add(Ui.Text("milo.", 32, true));
            var restored = await Cloud.Restore();
            if (restored) await Cloud.LoadProfile();
            Root.Children.Clear();
            if (restored && Cloud.Me != null && Cloud.Me.TermsV >= 1) ShowShell();
            else if (Store.P.Guest && !restored) ShowShell();
            else ShowGate(restored);
            Root.Children.Add(toastHost);
            _ = Library.Scan();
        }
        catch (Exception e) { Log.Add("boot: " + e); Root.Children.Add(Ui.Text("Something went wrong starting milo.\n" + e.Message, 14)); }
    }

    public void Show(UIElement page)
    {
        Root.Children.Clear();
        Root.Children.Add(page);
        Root.Children.Add(toastHost);
    }

    public void ShowShell() => Show(new ShellPage());
    public void ShowGate(bool signedIn = false) => Show(new GatePage(signedIn));

    public async void ShowToast(string msg)
    {
        toastText.Text = msg;
        toast.Opacity = 1;
        var my = msg;
        await Task.Delay(2600);
        if (toastText.Text == my) toast.Opacity = 0;
    }
}
