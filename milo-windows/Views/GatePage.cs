using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;

namespace Milo;

/// <summary>Welcome, sign in / create account, claim a username, accept the terms.</summary>
public sealed class GatePage : UserControl
{
    readonly Grid root = new();
    readonly StackPanel box = new() { Spacing = 12, Width = 380, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center };
    readonly TextBlock err = Ui.Text("", 13);
    bool busy;

    public GatePage(bool signedIn)
    {
        err.Foreground = Ui.Brush("FF7A8A");
        err.TextWrapping = TextWrapping.Wrap;
        root.Background = Ui.Bg;
        root.Children.Add(box);
        Content = root;
        if (signedIn) AfterAuth(); else Welcome();
    }

    void Reset(string title, string? sub = null)
    {
        box.Children.Clear();
        var logo = Ui.Text("milo.", 44, true); logo.HorizontalAlignment = HorizontalAlignment.Center; logo.Foreground = Ui.Accent;
        box.Children.Add(logo);
        var t = Ui.Text(title, 20, true); t.HorizontalAlignment = HorizontalAlignment.Center; box.Children.Add(t);
        if (sub != null) { var s = Ui.Text(sub, 13, false, true, 3); s.HorizontalAlignment = HorizontalAlignment.Center; s.TextAlignment = Microsoft.UI.Xaml.TextAlignment.Center; box.Children.Add(s); }
        err.Text = "";
    }

    void Welcome()
    {
        Reset("Your music, everywhere", "Sign in to keep your playlists, likes and friends in sync with the phone app.");
        var g = Ui.Btn("Continue with Google", GoogleClick, true, ""); g.HorizontalAlignment = HorizontalAlignment.Stretch;
        var e = Ui.Btn("Use email", () => EmailForm(false), false, ""); e.HorizontalAlignment = HorizontalAlignment.Stretch;
        var c = Ui.Btn("Continue without an account", () => { Store.P.Guest = true; Store.Save(); App.Win!.ShowShell(); }); c.HorizontalAlignment = HorizontalAlignment.Stretch;
        box.Children.Add(g); box.Children.Add(e); box.Children.Add(c); box.Children.Add(err);
    }

    async void GoogleClick()
    {
        if (busy) return; busy = true;
        err.Text = "";
        box.Children.Add(Ui.Text("Finish signing in in your browser…", 13, false, true));
        var m = await Cloud.GoogleSignIn();
        busy = false;
        if (m != null) { Welcome(); err.Text = m; return; }
        AfterAuth();
    }

    void EmailForm(bool create)
    {
        Reset(create ? "Create your account" : "Sign in");
        var email = Ui.Field("Email");
        var pw = Ui.PwField("Password");
        box.Children.Add(email); box.Children.Add(pw);
        TextBox? user = null, name = null;
        if (create)
        {
            user = Ui.Field("Username (letters, numbers, _ and .)");
            name = Ui.Field("Display name");
            box.Children.Add(user); box.Children.Add(name);
        }
        var go = Ui.Btn(create ? "Create account" : "Sign in", async () =>
        {
            if (busy) return;
            var em = email.Text.Trim(); var p = pw.Password;
            if (em.Length < 5 || !em.Contains('@')) { err.Text = "Enter a valid email."; return; }
            if (p.Length < 6) { err.Text = "Password needs at least 6 characters."; return; }
            busy = true; err.Text = create ? "Creating your account…" : "Signing in…";
            string? m;
            if (create)
            {
                var u = user!.Text.Trim().ToLowerInvariant();
                if (!System.Text.RegularExpressions.Regex.IsMatch(u, @"^[a-z0-9_.]{3,20}$")) { err.Text = "Username must be 3–20 letters, numbers, _ or ."; busy = false; return; }
                if (!await Cloud.UsernameFree(u)) { err.Text = "That username is taken."; busy = false; return; }
                m = await Cloud.SignUp(em, p, u, name!.Text.Trim().Length > 0 ? name.Text.Trim() : u);
                if (m == "confirm") { busy = false; Reset("Check your email", "We sent a confirmation link to " + em + ". Open it, then come back and sign in."); var b = Ui.Btn("Back to sign in", () => EmailForm(false), true); b.HorizontalAlignment = HorizontalAlignment.Stretch; box.Children.Add(b); return; }
            }
            else m = await Cloud.SignIn(em, p);
            busy = false;
            if (m != null) { err.Text = m; return; }
            AfterAuth();
        }, true);
        go.HorizontalAlignment = HorizontalAlignment.Stretch;
        var sw = Ui.Btn(create ? "I already have an account" : "Create an account", () => EmailForm(!create)); sw.HorizontalAlignment = HorizontalAlignment.Stretch;
        var back = Ui.Btn("Back", Welcome); back.HorizontalAlignment = HorizontalAlignment.Stretch;
        box.Children.Add(go); box.Children.Add(sw); box.Children.Add(back); box.Children.Add(err);
    }

    async void AfterAuth()
    {
        Reset("Setting things up…");
        var p = await Cloud.LoadProfile();
        if (p == null) { Claim(); return; }
        if (p.TermsV < 1) { TermsStep(); return; }
        Store.P.Guest = false; Store.Save();
        App.Win!.ShowShell();
    }

    void Claim()
    {
        Reset("Pick a username", "Friends find you by it. You can change your display name later.");
        var user = Ui.Field("Username");
        var name = Ui.Field("Display name");
        box.Children.Add(user); box.Children.Add(name);
        var go = Ui.Btn("Continue", async () =>
        {
            if (busy) return;
            var u = user.Text.Trim().ToLowerInvariant();
            if (!System.Text.RegularExpressions.Regex.IsMatch(u, @"^[a-z0-9_.]{3,20}$")) { err.Text = "Username must be 3–20 letters, numbers, _ or ."; return; }
            busy = true;
            if (!await Cloud.UsernameFree(u)) { err.Text = "That username is taken."; busy = false; return; }
            var m = await Cloud.CreateProfile(u, name.Text.Trim().Length > 0 ? name.Text.Trim() : u);
            busy = false;
            if (m != null) { err.Text = m; return; }
            TermsStep();
        }, true);
        go.HorizontalAlignment = HorizontalAlignment.Stretch;
        box.Children.Add(go); box.Children.Add(err);
    }

    void TermsStep()
    {
        Reset("Terms & privacy");
        var sv = new ScrollViewer { Height = 280, Padding = new Thickness(12), Background = Ui.Card, CornerRadius = new CornerRadius(12) };
        sv.Content = new TextBlock { Text = Terms.Text, TextWrapping = TextWrapping.Wrap, FontSize = 13, Foreground = Ui.White };
        box.Children.Add(sv);
        var ok = Ui.Btn("I agree", async () =>
        {
            await Cloud.AgreeTerms();
            Store.P.Guest = false; Store.Save();
            App.Win!.ShowShell();
        }, true);
        ok.HorizontalAlignment = HorizontalAlignment.Stretch;
        var no = Ui.Btn("Sign out", () => { Cloud.SignOut(); Welcome(); }); no.HorizontalAlignment = HorizontalAlignment.Stretch;
        box.Children.Add(ok); box.Children.Add(no);
    }
}
