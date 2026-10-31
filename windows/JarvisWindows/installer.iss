[Setup]
AppName=Jarvis
AppVersion=1.1
SetupIconFile=jarvis.ico
UninstallDisplayIcon={app}\Jarvis.exe
DefaultDirName={autopf}\Jarvis
DefaultGroupName=Jarvis
DisableProgramGroupPage=yes
OutputDir=Output
OutputBaseFilename=JarvisSetup
Compression=lzma2
SolidCompression=yes
PrivilegesRequired=lowest
ArchitecturesInstallIn64BitMode=x64compatible
[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"
Name: "startup"; Description: "Start Jarvis when Windows starts"; Flags: unchecked
[Files]
Source: "dist\Jarvis\*"; DestDir: "{app}"; Flags: recursesubdirs ignoreversion
[Icons]
Name: "{autoprograms}\Jarvis"; Filename: "{app}\Jarvis.exe"
Name: "{autodesktop}\Jarvis"; Filename: "{app}\Jarvis.exe"; Tasks: desktopicon
Name: "{userstartup}\Jarvis"; Filename: "{app}\Jarvis.exe"; Tasks: startup
[Run]
Filename: "{app}\Jarvis.exe"; Description: "Launch Jarvis"; Flags: nowait postinstall skipifsilent
