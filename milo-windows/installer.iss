; Inno Setup script: builds milo-setup.exe from the published folder
#define MyApp "milo."
#define MyVer "1.0.0"
[Setup]
AppId={{6B7B3C1E-0F65-4F4B-9B21-4D7A0C0B5E11}
AppName={#MyApp}
AppVersion={#MyVer}
AppPublisher=theLEAF foundation
DefaultDirName={autopf}\milo
DefaultGroupName=milo.
DisableProgramGroupPage=yes
OutputDir=installer
OutputBaseFilename=milo-setup
SetupIconFile=Assets\milo.ico
UninstallDisplayIcon={app}\milo.exe
Compression=lzma2
SolidCompression=yes
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
PrivilegesRequired=lowest
WizardStyle=modern
[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"; Flags: unchecked
[Files]
Source: "publish\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
[Icons]
Name: "{autoprograms}\milo."; Filename: "{app}\milo.exe"
Name: "{autodesktop}\milo."; Filename: "{app}\milo.exe"; Tasks: desktopicon
[Run]
Filename: "{app}\milo.exe"; Description: "Start milo."; Flags: nowait postinstall skipifsilent
