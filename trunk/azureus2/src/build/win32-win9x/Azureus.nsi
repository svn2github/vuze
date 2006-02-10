;NSIS Modern User Interface version 1.69
;Header Bitmap Example Script
;Written by Joost Verburg


!define AZ_VERSION "2.4.0.0"        ;Define your own software version here
!define SWT_VERSION "3064"          ;SWT lib version
!define AZPLUGINS_VERSION "1.9"   ;azplugins plugin version
!define AZUPDATER_VERSION "1.8.3"   ;azupdater plugin version
!define AZRATING_VERSION "1.3"        ;torrent rating plugin version


!include "MUI.nsh"

;--------------------------------
;Configuration

  ;General
  Name "Azureus ${AZ_VERSION}"
  OutFile "Azureus_${AZ_VERSION}_Win9x.setup.exe"

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
  !insertmacro MUI_PAGE_COMPONENTS
  !insertmacro MUI_PAGE_DIRECTORY
  !insertmacro MUI_PAGE_INSTFILES
  
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

;--------------------------------
;Data
  
  LicenseData "License.txt"


;--------------------------------
;Installer Sections

Section "Azureus Core Files" SecCopyUI

  SetOverwrite on
  
  SetOutPath "$INSTDIR"
  File "Azureus.exe"
  File "Azureus2.jar"
  File "License.txt"
  File "swt.jar"
  File "swt-win32-${SWT_VERSION}.dll"
  File "swt-awt-win32-${SWT_VERSION}.dll"
  File "swt-about.html"
  File "javaw.exe.manifest"
  File "ChangeLog.txt"
  File "aereg.dll"
  File "Azureus.exe.manifest"
  
  ;CreateDirectory "$INSTDIR\plugins"
  
  ;CreateDirectory "$INSTDIR\plugins\azplugins"
  SetOutPath "$INSTDIR\plugins\azplugins"
  File "plugins\azplugins\azplugins_${AZPLUGINS_VERSION}.jar"
  
  ;CreateDirectory "$INSTDIR\plugins\azupdater"
  SetOutPath "$INSTDIR\plugins\azupdater"
  File "plugins\azupdater\Updater.jar"
  File "plugins\azupdater\plugin.properties"
  File "plugins\azupdater\azupdaterpatcher_${AZUPDATER_VERSION}.jar"
  
  ;CreateDirectory "$INSTDIR\plugins\azrating"
  SetOutPath "$INSTDIR\plugins\azrating"
  File "plugins\azrating\azrating_${AZRATING_VERSION}.jar"
  
  
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
!insertmacro MUI_FUNCTION_DESCRIPTION_END
 
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
  Delete "$INSTDIR\swt-about.html"
  Delete "$INSTDIR\javaw.exe.manifest"
  Delete "$INSTDIR\ChangeLog.txt"
  Delete "$INSTDIR\aereg.dll"
  Delete "$INSTDIR\Azureus.exe.manifest"
  

  RMDir "$INSTDIR"

  ;Display the Finish header
  ;!insertmacro MUI_UNFINISHHEADER
  

SectionEnd