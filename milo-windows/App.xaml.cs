using Microsoft.UI.Dispatching;
using Microsoft.UI.Xaml;

namespace Milo;

public partial class App : Application
{
    public static MainWindow? Win;
    public static DispatcherQueue UI = null!;

    public App()
    {
        InitializeComponent();
        UnhandledException += (_, e) => { Log.Add("unhandled: " + e.Exception); e.Handled = true; };
    }

    protected override void OnLaunched(LaunchActivatedEventArgs args)
    {
        UI = DispatcherQueue.GetForCurrentThread();
        Win = new MainWindow();
        Win.Activate();
    }
}
