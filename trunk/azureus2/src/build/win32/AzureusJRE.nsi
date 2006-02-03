;NSIS Modern User Interface version 1.69
;Header Bitmap Example Script
;Written by Joost Verburg

!define AppName "Azureus"
!define AZ_VERSION "2.3.0.6"        ;Define your own software version here
!define SWT_VERSION "3139"          ;SWT lib version
!define AZPLUGINS_VERSION "1.8.6"   ;azplugins plugin version
!define AZUPDATER_VERSION "1.8.2"   ;azupdater plugin version
!define RATING_VERSION "1.2"        ;torrent rating plugin version

!define JRE_VERSION "1.5.0"
!define JRE_BUNDLE_NAME "jre-1_5_0_06-windows-i586-p.exe"

!define AZUREUS_URL "http://www.getazureus.com/azureus/Azureus2.3.0.6.zip"
!define JRE_URL "http://www.getazureus.com/java/jre-1_5_0_06-windows-i586-p.exe"

Var InstallJRE
Var JREPath

!include "MUI.nsh"
!include "Sections.nsh"
;--------------------------------
;Configuration

  ;General
  Name "Azureus ${AZ_VERSION}"
  OutFile "Azureus_${AZ_VERSION}_Win32.setup.exe"

  ;Folder selection page
  InstallDir "$PROGRAMFILES\Azureus"
  
  ;Remember install folder
  InstallDirRegKey HKCU "Software\Azureus" ""

;--------------------------------
;Modern UI Configuration

  !define MUI_HEADERIMAGE
  !define MUI_ABORTWARNING

;--------------------------------
;Pages

  !insertmacro MUI_PAGE_LICENSE "License.txt"
  ; This page checks for JRE. It displays a dialog based on JRE.ini if it needs to install JRE
  ; Otherwise you won't see it.
  ;Page custom CheckInstalledJRE
 
  ; Define headers for the 'Java installation successfully' page
  !define MUI_INSTFILESPAGE_FINISHHEADER_TEXT "Java installation complete"
  !define MUI_PAGE_HEADER_TEXT "Installing Java runtime"
  !define MUI_PAGE_HEADER_SUBTEXT "Please wait while we install the Java runtime"
  !define MUI_INSTFILESPAGE_FINISHHEADER_SUBTEXT "Java runtime installed successfully."
  
  


  !insertmacro MUI_PAGE_COMPONENTS
  !insertmacro MUI_PAGE_DIRECTORY
  !insertmacro MUI_PAGE_INSTFILES

  !define MUI_FINISHPAGE_RUN "$INSTDIR\Azureus.exe"
  !define MUI_FINISHPAGE_RUN_TEXT "Run Azureus"
  !insertmacro MUI_PAGE_FINISH  

  !insertmacro MUI_UNPAGE_CONFIRM
  !insertmacro MUI_UNPAGE_INSTFILES
  
;--------------------------------
;Languages
 
  !insertmacro MUI_LANGUAGE "English"
  
;--------------------------------
;Language Strings

  ;Description
  LangString DESC_SecCopyUI ${LANG_ENGLISH} "Required Azureus program files."

  LangString DESC_SecExtention ${LANG_ENGLISH} "Register the .torrent file extension."

  LangString DESC_SecIcons ${LANG_ENGLISH} "Add Start Menu icons."

  LangString DESC_SecJRE ${LANG_ENGLISH} "Java ${JRE_VERSION} JRE (Required)"

  LangString TEXT_JRE_TITLE ${LANG_ENGLISH} "JRE ${JRE_VERSION} Installation"

  LangString TEXT_JRE_SUBTITLE ${LANG_ENGLISH} "JRE Title"

;--------------------------------
;Data
  
  LicenseData "License.txt"

;--------------------------------
;Reserve Files
 
  ;Only useful for BZIP2 compression
 
 
  ReserveFile "jre.ini"
  !insertmacro MUI_RESERVEFILE_INSTALLOPTIONS
  
;--------------------------------
;Installer Sections

Section "JAVA ${JRE_VERSION}" jre
  Push $0
  Push $1
 
  ;MessageBox MB_OK "Inside JRE Section"
  Strcmp $InstallJRE "yes" InstallJREA JREPathStorage
  DetailPrint "Starting the JRE installation"


InstallJREA:

  DetailPrint "Downloading the JRE setup"
    NSISdl::download /TIMEOUT=30000 "${JRE_URL}" "$TEMP\jre_setup.exe"
    Pop $0 ;Get the return value
    StrCmp $0 "success" InstallJRE 0
    StrCmp $0 "cancel" 0 +3
    Push "Download cancelled."
    Goto ExitInstallJRE
    Push "Unkown error during download."
    Goto ExitInstallJRE

InstallJRE:
;  File /oname=$TEMP\jre_setup.exe ${JRE_BUNDLE_NAME}

  ;MessageBox MB_OK "Installing JRE"
  DetailPrint "Launching JRE setup"
  ExecWait "$TEMP\jre_setup.exe /S" $0
  DetailPrint "Setup finished"
  Delete "$TEMP\jre_setup.exe"
  StrCmp $0 "0" InstallVerif 0
  Push "The JRE setup has been abnormally interrupted."
  Goto ExitInstallJRE
 
InstallVerif:
  DetailPrint "Checking the JRE Setup's outcome"
;  MessageBox MB_OK "Checking JRE outcome"
  Push "${JRE_VERSION}"
  Call DetectJRE  
  Pop $0    ; DetectJRE's return value
  StrCmp $0 "0" ExitInstallJRE 0
  StrCmp $0 "-1" ExitInstallJRE 0
  Goto JavaExeVerif
  Push "The JRE setup failed"
  Goto ExitInstallJRE
 
JavaExeVerif:
  IfFileExists $0 JREPathStorage 0
  Push "The following file : $0, cannot be found."
  Goto ExitInstallJRE
  
JREPathStorage:
;  MessageBox MB_OK "Path Storage"
  !insertmacro MUI_INSTALLOPTIONS_WRITE "jre.ini" "UserDefinedSection" "JREPath" $1
  StrCpy $JREPath $0
  Goto End
  
ExitInstallJRE:
  Pop $1
  MessageBox MB_OK "The setup is about to be interrupted for the following reason : $1"
  Pop $1  ; Restore $1
  Pop $0  ; Restore $0
  Abort
End:
  Pop $1  ; Restore $1
  Pop $0  ; Restore $0
SectionEnd

Section "Azureus Core Files" SecCopyUI

  NSISdl::download /TIMEOUT=30000 "${AZUREUS_URL}" "$TEMP\Azureus2.zip"
  CreateDirectory "$TEMP\Azureus2Inst\"
  

  SetOverwrite on
  
  SetOutPath "$INSTDIR"

  ZipDLL::extractall "$TEMP\Azureus2.zip" "$INSTDIR"
  Delete "$TEMP\Azureus2.zip"  
  
  ;Store install folder
  WriteRegStr HKCU "Software\Azureus" "" $INSTDIR
  
  WriteRegStr HKLM SOFTWARE\Azureus "" $INSTDIR
  WriteRegExpandStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Azureus" "UninstallString" "$INSTDIR\Uninstall.exe"
  WriteRegExpandStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Azureus" "InstallLocation" "$INSTDIR"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Azureus" "DisplayName" "Azureus"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Azureus" "DisplayIcon" "$INSTDIR\Azureus.exe,0"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Azureus" "DisplayVersion" "${AZ_VERSION}"
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Azureus" "NoModify" "1"
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Azureus" "NoRepair" "1"
  
  ;Create uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"

SectionEnd

Section "Azureus Shell Extension" SecExtention

	WriteRegStr HKCR ".torrent" "" "BitTorrent"
	WriteRegStr HKCR "BitTorrent" "" "Bittorrent File"
	WriteRegStr HKCR "BitTorrent\shell" "" "open"
	WriteRegStr HKCR "BitTorrent\DefaultIcon" "" $INSTDIR\Azureus.exe,1
  WriteRegStr HKCR "BitTorrent\shell\open\command" "" '"$INSTDIR\Azureus.exe" "%1"'
  WriteRegStr HKCR "BitTorrent\Content Type" "" "application/x-bittorrent"
  	
SectionEnd


Section "Start Menu Shortcuts" SecIcons

  SetOutPath $INSTDIR
  CreateDirectory $SMPROGRAMS\Azureus
  CreateShortCut "$SMPROGRAMS\Azureus\Azureus.lnk" "$INSTDIR\Azureus.exe" ""
  CreateShortCut "$SMPROGRAMS\Azureus\Uninstall Azureus.lnk" "$INSTDIR\Uninstall.exe" ""

SectionEnd


;Display the Finish header
;Insert this macro after the sections if you are not using a finish page

;!insertmacro MUI_SECTIONS_FINISHHEADER

;--------------------------------
;Descriptions

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
!insertmacro MUI_DESCRIPTION_TEXT ${SecCopyUI} $(DESC_SecCopyUI)
!insertmacro MUI_DESCRIPTION_TEXT ${SecExtention} $(DESC_SecExtention)
!insertmacro MUI_DESCRIPTION_TEXT ${SecIcons} $(DESC_SecIcons)
!insertmacro MUI_DESCRIPTION_TEXT ${jre} $(DESC_SecJRE)
!insertmacro MUI_FUNCTION_DESCRIPTION_END
 
;--------------------------------
;Installer Functions
 
Function .onInit
 
  ;Extract InstallOptions INI Files
  !insertmacro MUI_INSTALLOPTIONS_EXTRACT "jre.ini"
  Call SetupSections
  Call CheckInstalledJRE
 
FunctionEnd
 
Function CheckInstalledJRE
  ;MessageBox MB_OK "Checking Installed JRE Version"
  Push "${JRE_VERSION}"
  Call DetectJRE
  ;Messagebox MB_OK "Done checking JRE version"
  Exch $0 ; Get return value from stack
  StrCmp $0 "0" NoFound
  StrCmp $0 "-1" FoundOld
  Goto JREAlreadyInstalled
  
FoundOld:
  ;MessageBox MB_OK "Old JRE found"
  !insertmacro MUI_INSTALLOPTIONS_WRITE "jre.ini" "Field 1" "Text" "${AppName} requires a more recent version of the Java Runtime Environment than the one found on your computer. The installation of JRE ${JRE_VERSION} will start."
  !insertmacro MUI_HEADER_TEXT "$(TEXT_JRE_TITLE)" "$(TEXT_JRE_SUBTITLE)"
  !insertmacro MUI_INSTALLOPTIONS_DISPLAY_RETURN "jre.ini"
  Goto MustInstallJRE
 
NoFound:
  ;MessageBox MB_OK "JRE not found"
  !insertmacro MUI_INSTALLOPTIONS_WRITE "jre.ini" "Field 1" "Text" "No Java Runtime Environment could be found on your computer. The installation of JRE v${JRE_VERSION} will start."
  !insertmacro MUI_HEADER_TEXT "$(TEXT_JRE_TITLE)" "$(TEXT_JRE_SUBTITLE)"
  !insertmacro MUI_INSTALLOPTIONS_DISPLAY_RETURN "jre.ini"
  Goto MustInstallJRE
 
MustInstallJRE:
  Exch $0 ; $0 now has the installoptions page return value
  ; Do something with return value here
  Pop $0  ; Restore $0
  StrCpy $InstallJRE "yes"
  Call SetupJRESection
  Return
  
JREAlreadyInstalled:
;  MessageBox MB_OK "No download: ${TEMP2}"
  ;MessageBox MB_OK "JRE already installed"
  StrCpy $InstallJRE "no"
  !insertmacro MUI_INSTALLOPTIONS_WRITE "jre.ini" "UserDefinedSection" "JREPath" $JREPATH
  Pop $0    ; Restore $0
  Return
 
FunctionEnd
 
; Returns: 0 - JRE not found. -1 - JRE found but too old. Otherwise - Path to JAVA EXE
 
; DetectJRE. Version requested is on the stack.
; Returns (on stack)  "0" on failure (java too old or not installed), otherwise path to java interpreter
; Stack value will be overwritten!
 
Function DetectJRE
  Exch $0 ; Get version requested  
    ; Now the previous value of $0 is on the stack, and the asked for version of JDK is in $0
  Push $1 ; $1 = Java version string (ie 1.5.0)
  Push $2 ; $2 = Javahome
  Push $3 ; $3 and $4 are used for checking the major/minor version of java
  Push $4
  ;MessageBox MB_OK "Detecting JRE"
  ReadRegStr $1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ;MessageBox MB_OK "Read : $1"
  StrCmp $1 "" DetectTry2
  ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$1" "JavaHome"
  ;MessageBox MB_OK "Read 3: $2"
  StrCmp $2 "" DetectTry2
  Goto GetJRE
 
DetectTry2:
  ReadRegStr $1 HKLM "SOFTWARE\JavaSoft\Java Development Kit" "CurrentVersion"
  ;MessageBox MB_OK "Detect Read : $1"
  StrCmp $1 "" NoFound
  ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$1" "JavaHome"
  ;MessageBox MB_OK "Detect Read 3: $2"
  StrCmp $2 "" NoFound
 
GetJRE:
; $0 = version requested. $1 = version found. $2 = javaHome
  ;MessageBox MB_OK "Getting JRE"
  IfFileExists "$2\bin\java.exe" 0 NoFound
  StrCpy $3 $0 1      ; Get major version. Example: $1 = 1.5.0, now $3 = 1
  StrCpy $4 $1 1      ; $3 = major version requested, $4 = major version found
  ;MessageBox MB_OK "Want $3 , found $4"
  IntCmp $4 $3 0 FoundOld FoundNew
  StrCpy $3 $0 1 2
  StrCpy $4 $1 1 2      ; Same as above. $3 is minor version requested, $4 is minor version installed
 ; MessageBox MB_OK "Want $3 , found $4" 
  IntCmp $4 $3 FoundNew FoundOld FoundNew
 
NoFound:
  ;MessageBox MB_OK "JRE not found"
  Push "0"
  Goto DetectJREEnd
 
FoundOld:
  ;MessageBox MB_OK "JRE too old: $3 is older than $4"
;  Push ${TEMP2}
  Push "-1"
  Goto DetectJREEnd  
FoundNew:
  ;MessageBox MB_OK "JRE is new: $3 is newer than $4"
 
  Push "$2\bin\java.exe"
;  Push "OK"
;  Return
   Goto DetectJREEnd
DetectJREEnd:
  ; Top of stack is return value, then r4,r3,r2,r1
  Exch  ; => r4,rv,r3,r2,r1,r0
  Pop $4  ; => rv,r3,r2,r1r,r0
  Exch  ; => r3,rv,r2,r1,r0
  Pop $3  ; => rv,r2,r1,r0
  Exch  ; => r2,rv,r1,r0
  Pop $2  ; => rv,r1,r0
  Exch  ; => r1,rv,r0
  Pop $1  ; => rv,r0
  Exch  ; => r0,rv
  Pop $0  ; => rv 
FunctionEnd
 
Function SetupSections

;1: Selected 8 : Bold , 16 : Read Only => 25 (selected), 24 (unselected)

  SectionSetFlags ${jre} 24
  SectionSetFlags ${SecCopyUI} 25
  !insertmacro SelectSection ${SecIcons}
  !insertmacro SelectSection ${SecExtention}
  
FunctionEnd


Function SetupJRESection
  SectionSetFlags jre 25
FunctionEnd
 
;--------------------------------


;Uninstaller Section

Section "Uninstall"    
  
 IfFileExists $INSTDIR\Azureus.exe skip_confirmation
    MessageBox MB_YESNO "It does not appear that Azureus is installed in the directory '$INSTDIR'.$\r$\nContinue anyway (not recommended)" IDYES skip_confirmation
    Abort "Uninstall aborted by user"
  skip_confirmation:
  ReadRegStr $1 HKCR ".torrent" ""
  StrCmp $1 "BitTorrent" 0 NoOwn ; only do this if we own it
    ReadRegStr $1 HKCR ".nsi" "backup_val"
    StrCmp $1 "" 0 RestoreBackup ; if backup == "" then delete the whole key
      DeleteRegKey HKCR ".nsi"
    Goto NoOwn
    RestoreBackup:
      WriteRegStr HKCR ".nsi" "" $1
      DeleteRegValue HKCR ".nsi" "backup_val"
  NoOwn:

  DeleteRegKey HKCR "BitTorrent"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Azureus"
  DeleteRegKey HKLM SOFTWARE\Azureus

  RMDir /r $SMPROGRAMS\Azureus
  Delete "$DESKTOP\Azureus.lnk"

  DeleteRegKey /ifempty HKCU "Software\Azureus"
  
  
  
  Delete "$INSTDIR\Azureus.exe"
  Delete "$INSTDIR\Azureus2.jar"
  Delete "$INSTDIR\License.txt"
  Delete "$INSTDIR\swt.jar"
  Delete "$INSTDIR\swt-win32-${SWT_VERSION}.dll"
  Delete "$INSTDIR\swt-awt-win32-${SWT_VERSION}.dll"
  Delete "$INSTDIR\swt-gdip-win32-${SWT_VERSION}.dll"
  Delete "$INSTDIR\swt-about.html"
  Delete "$INSTDIR\javaw.exe.manifest"
  Delete "$INSTDIR\ChangeLog.txt"
  Delete "$INSTDIR\aereg.dll"
  Delete "$INSTDIR\Azureus.exe.manifest"
  

  RMDir "$INSTDIR"

  ;Display the Finish header
  ;!insertmacro MUI_UNFINISHHEADER
  

SectionEnd