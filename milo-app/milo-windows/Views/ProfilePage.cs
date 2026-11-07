using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Windows.Storage.Pickers;

namespace Milo;

public sealed class ProfilePage : UserControl
{
    readonly StackPanel col = new() { Padding = new Thickness(28, 24, 28, 24), Spacing = 6, MaxWidth = 720, HorizontalAlignment = HorizontalAlignment.Left };

    public ProfilePage()
    {
        Content = new ScrollViewer { Content = col };
        Build();
    }

    void Build()
    {
        col.Children.Clear();
        col.Children.Add(Ui.Heading("Profile"));
        if (Cloud.SignedIn)
        {
            col.Children.Add(Ui.Text(Cloud.Me?.Name is { Length: > 0 } n ? n : "Signed in", 20, true));
            col.Children.Add(Ui.Text("@" + (Cloud.Me?.Username ?? "") + "  ·  " + Cloud.Email, 13, false, true));
            var so = Ui.Btn("Sign out", () => { Cloud.SignOut(); Store.P.Guest = false; Store.Save(); App.Win!.ShowGate(); }); so.Margin = new Thickness(0, 10, 0, 0);
            col.Children.Add(so);
        }
        else
        {
            col.Children.Add(Ui.Text("You're using milo. without an account.", 14, false, true));
            var si = Ui.Btn("Sign in", () => App.Win!.ShowGate(), true); si.Margin = new Thickness(0, 10, 0, 0);
            col.Children.Add(si);
        }

        col.Children.Add(Ui.Section("Home"));
        var hide = new ToggleSwitch { Header = "Hide songs from this PC on Home while online", IsOn = Store.P.HideLocalOnline, OnContent = "On", OffContent = "Off" };
        hide.Toggled += (_, _) => { Store.P.HideLocalOnline = hide.IsOn; Store.Save(); };
        col.Children.Add(hide);

        col.Children.Add(Ui.Section("Music folders"));
        foreach (var f in Library.Roots())
        {
            var row = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 8 };
            row.Children.Add(Ui.Text(f, 13, false, true));
            if (Store.P.Folders.Contains(f)) { var ff = f; row.Children.Add(Ui.IconBtn("", () => { Store.P.Folders.Remove(ff); Store.Save(); _ = Library.Scan(); Build(); }, "Remove", 13)); }
            col.Children.Add(row);
        }
        var add = Ui.Btn("Add folder", async () =>
        {
            try
            {
                var p = new FolderPicker(); p.FileTypeFilter.Add("*");
                WinRT.Interop.InitializeWithWindow.Initialize(p, WinRT.Interop.WindowNative.GetWindowHandle(App.Win));
                var f = await p.PickSingleFolderAsync();
                if (f == null) return;
                if (!Store.P.Folders.Contains(f.Path)) Store.P.Folders.Add(f.Path);
                Store.Save(); _ = Library.Scan(); Build();
            }
            catch (Exception e) { Log.Add("pick: " + e.Message); }
        }, false, "");
        add.Margin = new Thickness(0, 6, 0, 0);
        col.Children.Add(add);

        col.Children.Add(Ui.Section("Downloads folder"));
        col.Children.Add(Ui.Text(Store.P.DownloadDir, 13, false, true));
        var chg = Ui.Btn("Change", async () =>
        {
            try
            {
                var p = new FolderPicker(); p.FileTypeFilter.Add("*");
                WinRT.Interop.InitializeWithWindow.Initialize(p, WinRT.Interop.WindowNative.GetWindowHandle(App.Win));
                var f = await p.PickSingleFolderAsync();
                if (f == null) return;
                Store.P.DownloadDir = f.Path; Store.Save(); _ = Library.Scan(); Build();
            }
            catch (Exception e) { Log.Add("pick: " + e.Message); }
        });
        chg.Margin = new Thickness(0, 6, 0, 0);
        col.Children.Add(chg);

        col.Children.Add(Ui.Section("About"));
        col.Children.Add(Ui.Text("milo. for Windows 1.0  ·  theLEAF foundation", 13, false, true));
        col.Children.Add(Ui.Text("Media keys and the Windows volume panel work automatically. Logs: " + Log.Dir, 12, false, true, 2));
        var terms = Ui.Btn("Terms & privacy", async () =>
        {
            var d = new ContentDialog { Title = "Terms & privacy", CloseButtonText = "Close", XamlRoot = XamlRoot, Content = new ScrollViewer { Height = 360, Content = new TextBlock { Text = Terms.Text, TextWrapping = TextWrapping.Wrap } } };
            await d.ShowAsync();
        });
        terms.Margin = new Thickness(0, 8, 0, 0);
        col.Children.Add(terms);
    }
}
