/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials  * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *  * Contributors:
 *     IBM Corporation - initial API and implementation
 * 		 Aelitis - Adaptation for Azureus
 *******************************************************************************/
package org.gudy.azureus2.ui.swt.osx;

import org.eclipse.swt.internal.Callback;
import org.eclipse.swt.internal.carbon.HICommand;
import org.eclipse.swt.internal.carbon.OS;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.help.AboutWindow;
import org.gudy.azureus2.ui.swt.nat.NatTestWindow;
import org.gudy.azureus2.ui.swt.config.wizard.ConfigureWizard;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;

import java.io.IOException;

//import com.apple.eawt.*; //Application and ApplicationAdapter

public class CarbonUIEnhancer {
	private static final int kHICommandPreferences= ('p'<<24) + ('r'<<16) + ('e'<<8) + 'f';
   private static final int kHICommandAbout= ('a'<<24) + ('b'<<16) + ('o'<<8) + 'u';
   private static final int kHICommandServices= ('s'<<24) + ('e'<<16) + ('r'<<8) + 'v';
   private static final int kHICommandWizard = ('a'<<24) + ('z'<<16) + ('c' << 8) + 'n';
   private static final int kHICommandNatTest = ('a'<<24) + ('z'<<16) + ('n' << 8) + 't';
   private static final int kHICommandRestart = ('a'<<24) + ('z'<<16) + ('r'<<8) + 's';

   private static final String RESOURCE_BUNDLE= "org.eclipse.ui.carbon.Messages"; //$NON-NLS-1$
   private static String fgAboutActionName;
   private static String fgWizardActionName;
   private static String fgNatTestActionName;
   private static String fgRestartActionName;

   public CarbonUIEnhancer() {
      if (fgAboutActionName == null) {
         fgAboutActionName = MessageText.getString("MainWindow.menu.help.about");
      }
      if(fgWizardActionName == null) {
          fgWizardActionName = MessageText.getString("MainWindow.menu.file.configure").replaceAll("&", "");
      }
      if(fgNatTestActionName == null) {
      	fgNatTestActionName = MessageText.getString("MainWindow.menu.tools.nattest").replaceAll("&", "");
      }
      if(fgRestartActionName == null) {
          fgRestartActionName = MessageText.getString("MainWindow.menu.file.restart").replaceAll("&", "");
      }
      earlyStartup();
      registerTorrentFile();
   }
   
   
   
   private void registerTorrentFile() {
   
     /* SWT cannot work with AWT (see Eclipse bug #67384)
      * The Thread stalls in the class loader (for awt).
      */
     /*
     Application app = Application.getApplication();     
     app.addApplicationListener(new ApplicationAdapter() {
         public void handleOpenFile(ApplicationEvent evt) {
           String filename = evt.getFilename();
           TorrentOpener.openTorrent( filename );
           evt.setHandled( true );
         }
     });
     */
   }

   /* (non-Javadoc)
    * @see org.eclipse.ui.IStartup#earlyStartup()
    */
   public void earlyStartup() {
      final Display display= Display.getDefault();
      display.syncExec(
      		new AERunnable() {
            public void runSupport() {
               hookApplicationMenu(display);
            }
         }
      );
   }
      /**
    * See Apple Technical Q&A 1079 (http://developer.apple.com/qa/qa2001/qa1079.html)<br />
    * Also http://developer.apple.com/documentation/Carbon/Reference/Menu_Manager/menu_mgr_ref/function_group_10.html
    */
   public void hookApplicationMenu(final Display display) {
            // Callback target
      Object target= new Object() {
         int commandProc(int nextHandler, int theEvent, int userData) {
            if (OS.GetEventKind(theEvent) == OS.kEventProcessCommand) {
               HICommand command= new HICommand();
               OS.GetEventParameter(theEvent, OS.kEventParamDirectObject, OS.typeHICommand, null, HICommand.sizeof, null, command);
               switch (command.commandID) {
               case kHICommandPreferences: {
              	 UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
              	 if (uiFunctions != null) {
              		 uiFunctions.showConfig(null);
              	 }
                  return OS.noErr;
               }
               case kHICommandAbout:
                 AboutWindow.show(display);
                 return OS.noErr;
               case kHICommandRestart: {
              	 UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
              	 if (uiFunctions != null) {
              		 uiFunctions.dispose(true, false);
              	 }
                  return OS.noErr;
               }
               case kHICommandWizard:
                  new ConfigureWizard(AzureusCoreFactory.getSingleton(), display);
                  return OS.noErr;
               case kHICommandNatTest:
                 new NatTestWindow();
                 return OS.noErr;
               default:
                  break;
               }
            }
            return OS.eventNotHandledErr;
         }
      };
            final Callback commandCallback= new Callback(target, "commandProc", 3); //$NON-NLS-1$
      int commandProc= commandCallback.getAddress();
      if (commandProc == 0) {
         commandCallback.dispose();
         return;  // give up
      }

      // Install event handler for commands
      int[] mask= new int[] {
         OS.kEventClassCommand, OS.kEventProcessCommand
      };
      OS.InstallEventHandler(OS.GetApplicationEventTarget(), commandProc, mask.length / 2, mask, 0, null);

      // create About menu command
      int[] outMenu= new int[1];
      short[] outIndex= new short[1];
      if (OS.GetIndMenuItemWithCommandID(0, kHICommandPreferences, 1, outMenu, outIndex) == OS.noErr && outMenu[0] != 0) {
         int menu= outMenu[0];

         int l= fgAboutActionName.length();
         char buffer[]= new char[l];
         fgAboutActionName.getChars(0, l, buffer, 0);
         int str= OS.CFStringCreateWithCharacters(OS.kCFAllocatorDefault, buffer, l);
         OS.InsertMenuItemTextWithCFString(menu, str, (short) 0, 0, kHICommandAbout);
         OS.CFRelease(str);
                  // add separator between About & Preferences
         OS.InsertMenuItemTextWithCFString(menu, 0, (short) 1, OS.kMenuItemAttrSeparator, 0);

         // enable pref menu
         OS.EnableMenuCommand(menu, kHICommandPreferences);
               // disable services menu
         OS.DisableMenuCommand(menu, kHICommandServices);

          // wizard menu
         l= fgWizardActionName.length();
         buffer= new char[l];
         fgWizardActionName.getChars(0, l, buffer, 0);
         str= OS.CFStringCreateWithCharacters(OS.kCFAllocatorDefault, buffer, l);
         OS.InsertMenuItemTextWithCFString(menu, str, (short) 3, 0, kHICommandWizard);
         OS.CFRelease(str);
         
         
         // NAT test menu
         l= fgNatTestActionName.length();
         buffer= new char[l];
         fgNatTestActionName.getChars(0, l, buffer, 0);
         str= OS.CFStringCreateWithCharacters(OS.kCFAllocatorDefault, buffer, l);
         OS.InsertMenuItemTextWithCFString(menu, str, (short) 4, 0, kHICommandNatTest);
         OS.CFRelease(str);
         

          OS.InsertMenuItemTextWithCFString(menu, 0, (short) 5, OS.kMenuItemAttrSeparator, 0);

          // restart menu
         l= fgRestartActionName.length();
         buffer= new char[l];
         fgRestartActionName.getChars(0, l, buffer, 0);
         str= OS.CFStringCreateWithCharacters(OS.kCFAllocatorDefault, buffer, l);
         OS.InsertMenuItemTextWithCFString(menu, str, (short) 6, 0, kHICommandRestart);
         OS.CFRelease(str);

          OS.InsertMenuItemTextWithCFString(menu, 0, (short) 7, OS.kMenuItemAttrSeparator, 0);
      }

      // schedule disposal of callback object
      display.disposeExec(
         new AERunnable() {
            public void runSupport() {
               commandCallback.dispose();
//               stopSidekick();
            }
         }
      );
   }

   private static void stopSidekick()
   {
       try
       {
           Runtime.getRuntime().exec(new String[]{"osascript", "-e", "tell application \"Azureus\" to quit"});
       }
       catch (IOException e)
       {
           Debug.printStackTrace(e);
       }
   }
}
