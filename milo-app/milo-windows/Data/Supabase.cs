using System.Diagnostics;
using System.Net;
using System.Net.Http.Headers;
using System.Net.Sockets;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json.Nodes;

namespace Milo;

public class Profile
{
    public string Id = "", Username = "", Name = "";
    public int TermsV;
}

/// <summary>milo. cloud account (same Supabase project as the Android and web apps).</summary>
public static class Cloud
{
    const string URL = "https://lwcypxiwkgphuekygrps.supabase.co";
    const string KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imx3Y3lweGl3a2dwaHVla3lncnBzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQxMjU3NzEsImV4cCI6MjA5OTcwMTc3MX0.qCLKCluVfjkByX9GqRvxcihVl0Qo8eJOhfkVXN-nqAQ";
    public const int LoopPort = 53682;
    public const string LoopUrl = "http://127.0.0.1:53682/cb";
    static readonly HttpClient H = new() { Timeout = TimeSpan.FromSeconds(30) };

    public static string Access = "", Refresh = "", UserId = "", Email = "";
    public static long ExpiresAt;
    public static Profile? Me;
    public static bool SignedIn => Access.Length > 0;

    static async Task<(int code, string body)> Send(string method, string path, string? body, bool anon = false, string? prefer = null)
    {
        var req = new HttpRequestMessage(new HttpMethod(method), URL + path);
        req.Headers.Add("apikey", KEY);
        req.Headers.Authorization = new AuthenticationHeaderValue("Bearer", anon || Access.Length == 0 ? KEY : Access);
        if (prefer != null) req.Headers.Add("Prefer", prefer);
        if (body != null) req.Content = new StringContent(body, Encoding.UTF8, "application/json");
        using var r = await H.SendAsync(req);
        return ((int)r.StatusCode, await r.Content.ReadAsStringAsync());
    }

    static string Err(string body, string fallback)
    {
        try
        {
            var o = JsonNode.Parse(body);
            foreach (var k in new[] { "msg", "error_description", "message", "error" })
            {
                var s = o?[k]?.ToString();
                if (!string.IsNullOrEmpty(s)) return s!;
            }
        }
        catch { }
        return fallback;
    }

    static void TakeSession(JsonNode o)
    {
        Access = o["access_token"]?.ToString() ?? "";
        Refresh = o["refresh_token"]?.ToString() ?? Refresh;
        var exp = (long?)o["expires_in"] ?? 3600;
        ExpiresAt = DateTimeOffset.UtcNow.ToUnixTimeSeconds() + exp;
        UserId = o["user"]?["id"]?.ToString() ?? UserId;
        Email = o["user"]?["email"]?.ToString() ?? Email;
        Store.SaveSession(new JsonObject { ["a"] = Access, ["r"] = Refresh, ["e"] = ExpiresAt, ["u"] = UserId, ["m"] = Email }.ToJsonString());
    }

    /// <summary>Restore a saved session; refresh the token if it has expired.</summary>
    public static async Task<bool> Restore()
    {
        var s = Store.LoadSession();
        if (s == null) return false;
        try
        {
            var o = JsonNode.Parse(s)!;
            Access = o["a"]?.ToString() ?? ""; Refresh = o["r"]?.ToString() ?? "";
            ExpiresAt = (long?)o["e"] ?? 0; UserId = o["u"]?.ToString() ?? ""; Email = o["m"]?.ToString() ?? "";
            if (Access.Length == 0) return false;
            if (ExpiresAt < DateTimeOffset.UtcNow.ToUnixTimeSeconds() + 60 && !await DoRefresh()) return false;
            return true;
        }
        catch (Exception e) { Log.Add("restore: " + e.Message); return false; }
    }

    static async Task<bool> DoRefresh()
    {
        if (Refresh.Length == 0) return false;
        try
        {
            var (c, b) = await Send("POST", "/auth/v1/token?grant_type=refresh_token", new JsonObject { ["refresh_token"] = Refresh }.ToJsonString(), true);
            if (c != 200) { if (c == 400 || c == 401) SignOut(); return false; }
            TakeSession(JsonNode.Parse(b)!);
            return true;
        }
        catch { return false; }
    }

    public static async Task<string?> SignIn(string email, string pw)
    {
        try
        {
            var (c, b) = await Send("POST", "/auth/v1/token?grant_type=password", new JsonObject { ["email"] = email, ["password"] = pw }.ToJsonString(), true);
            if (c != 200) return Err(b, "Could not sign in");
            TakeSession(JsonNode.Parse(b)!);
            return null;
        }
        catch (Exception e) { return "No connection: " + e.Message; }
    }

    /// <summary>Returns null on success (signed in), "confirm" when the email must be confirmed first, or an error message.</summary>
    public static async Task<string?> SignUp(string email, string pw, string username, string name)
    {
        try
        {
            var body = new JsonObject
            {
                ["email"] = email, ["password"] = pw,
                ["data"] = new JsonObject { ["username"] = username, ["name"] = name },
            };
            var (c, b) = await Send("POST", "/auth/v1/signup", body.ToJsonString(), true);
            if (c != 200) return Err(b, "Could not create the account");
            var o = JsonNode.Parse(b)!;
            if (o["access_token"] != null) { TakeSession(o); return null; }
            return "confirm";
        }
        catch (Exception e) { return "No connection: " + e.Message; }
    }

    public static async Task<bool> UsernameFree(string u)
    {
        try
        {
            var (c, b) = await Send("POST", "/rest/v1/rpc/username_free", new JsonObject { ["u"] = u }.ToJsonString(), true);
            return c == 200 && b.Trim() != "false";
        }
        catch { return true; }
    }

    public static async Task<Profile?> LoadProfile()
    {
        try
        {
            var (c, b) = await Send("GET", $"/rest/v1/profiles?id=eq.{UserId}&select=id,username,name,terms_v", null);
            if (c != 200) return null;
            var a = JsonNode.Parse(b)!.AsArray();
            if (a.Count == 0) return null;
            var o = a[0]!;
            Me = new Profile { Id = UserId, Username = o["username"]?.ToString() ?? "", Name = o["name"]?.ToString() ?? "", TermsV = (int?)o["terms_v"] ?? 0 };
            return Me;
        }
        catch { return null; }
    }

    public static async Task<string?> CreateProfile(string username, string name)
    {
        try
        {
            var (c, b) = await Send("POST", "/rest/v1/profiles", new JsonObject { ["id"] = UserId, ["username"] = username, ["name"] = name }.ToJsonString(), false, "return=minimal");
            if (c >= 200 && c < 300) { await LoadProfile(); return null; }
            return Err(b, "Could not save your profile");
        }
        catch (Exception e) { return e.Message; }
    }

    public static async Task AgreeTerms()
    {
        try
        {
            await Send("PATCH", $"/rest/v1/profiles?id=eq.{UserId}", new JsonObject { ["terms_v"] = 1, ["terms_at"] = DateTime.UtcNow.ToString("o") }.ToJsonString(), false, "return=minimal");
            if (Me != null) Me.TermsV = 1;
        }
        catch { }
    }

    public static void SignOut()
    {
        Access = ""; Refresh = ""; UserId = ""; Email = ""; Me = null;
        Store.ClearSession();
    }

    // ---- Google (PKCE through the browser; the app listens on 127.0.0.1:53682) ----
    public static async Task<string?> GoogleSignIn()
    {
        TcpListener? l = null;
        try
        {
            var vb = RandomNumberGenerator.GetBytes(48);
            var verifier = B64(vb);
            var challenge = B64(SHA256.HashData(Encoding.ASCII.GetBytes(verifier)));
            l = new TcpListener(IPAddress.Loopback, LoopPort);
            l.Start();
            var url = $"{URL}/auth/v1/authorize?provider=google&redirect_to={Uri.EscapeDataString(LoopUrl)}&code_challenge={challenge}&code_challenge_method=s256";
            Process.Start(new ProcessStartInfo(url) { UseShellExecute = true });

            using var cts = new CancellationTokenSource(TimeSpan.FromMinutes(3));
            using var cl = await l.AcceptTcpClientAsync(cts.Token);
            using var ns = cl.GetStream();
            var buf = new byte[8192];
            var n = await ns.ReadAsync(buf, cts.Token);
            var head = Encoding.ASCII.GetString(buf, 0, n);
            var line = head.Split("\r\n")[0];
            var q = line.Split(' ').ElementAtOrDefault(1) ?? "";
            var qs = q.Contains('?') ? q[(q.IndexOf('?') + 1)..] : "";
            string code = "", error = "";
            foreach (var kv in qs.Split('&'))
            {
                var p = kv.Split('=', 2);
                if (p.Length != 2) continue;
                if (p[0] == "code") code = p[1];
                if (p[0] == "error_description") error = Uri.UnescapeDataString(p[1].Replace('+', ' '));
            }
            var ok = code.Length > 0;
            var page = "<html><body style='font-family:Segoe UI;background:#0b0b0f;color:#fff;text-align:center;padding-top:20vh'><h1>" +
                       (ok ? "You're signed in" : "Sign-in failed") + "</h1><p>" + (ok ? "You can close this tab and go back to milo." : WebUtility.HtmlEncode(error)) + "</p></body></html>";
            var bytes = Encoding.UTF8.GetBytes(page);
            var resp = Encoding.ASCII.GetBytes($"HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: {bytes.Length}\r\nConnection: close\r\n\r\n");
            await ns.WriteAsync(resp);
            await ns.WriteAsync(bytes);
            await ns.FlushAsync();
            if (!ok) return error.Length > 0 ? error : "Google sign-in was cancelled";

            var (c, b) = await Send("POST", "/auth/v1/token?grant_type=pkce", new JsonObject { ["auth_code"] = code, ["code_verifier"] = verifier }.ToJsonString(), true);
            if (c != 200) return Err(b, "Google sign-in failed");
            TakeSession(JsonNode.Parse(b)!);
            return null;
        }
        catch (OperationCanceledException) { return "Google sign-in timed out"; }
        catch (SocketException) { return "Port 53682 is busy. Close other milo. windows and try again."; }
        catch (Exception e) { Log.Add("google: " + e); return "Google sign-in failed: " + e.Message; }
        finally { try { l?.Stop(); } catch { } }
    }

    static string B64(byte[] b) => Convert.ToBase64String(b).TrimEnd('=').Replace('+', '-').Replace('/', '_');
}
