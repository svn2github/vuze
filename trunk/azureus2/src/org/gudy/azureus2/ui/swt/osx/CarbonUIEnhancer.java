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
import org.eclipse.swt.internal.carbon.*;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.help.AboutWindow;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;


public class CarbonUIEnhancer {
         private static final int kHICommandPreferences= ('p'<<24) + ('r'<<16) + ('e'<<8) + 'f';
   private static final int kHICommandAbout= ('a'<<24) + ('b'<<16) + ('o'<<8) + 'u';
   private static final int kHICommandServices= ('s'<<24) + ('e'<<16) + ('r'<<8) + 'v';

   private static final String RESOURCE_BUNDLE= "org.eclipse.ui.carbon.Messages"; //$NON-NLS-1$
   private static String fgAboutActionName;

   public CarbonUIEnhancer() {
      if (fgAboutActionName == null) {
         fgAboutActionName = MessageText.getString("MainWindow.menu.help.about");
      }
      earlyStartup();
   }

   /* (non-Javadoc)
    * @see org.eclipse.ui.IStartup#earlyStartup()
    */
   public void earlyStartup() {
      final Display display= Display.getDefault();
      display.syncExec(
         new Runnable() {
            public void run() {
               hookApplicationMenu(display);
            }
         }
      );
   }
      /**
    * See Apple Technical Q&A 1079 (http://developer.apple.com/qa/qa2001/qa1079.html)
    */
   public void hookApplicationMenu(final Display display) {
            // Callback target
      Object target= new Object() {
         int commandProc(int nextHandler, int theEvent, int userData) {
            if (OS.GetEventKind(theEvent) == OS.kEventProcessCommand) {
               HICommand command= new HICommand();
               OS.GetEventParameter(theEvent, OS.kEventParamDirectObject, OS.typeHICommand, null, HICommand.sizeof, null, command);
               switch (command.commandID) {
               case kHICommandPreferences:
                  MainWindow.getWindow().showConfig();
                  return OS.noErr;
               case kHICommandAbout:
                 AboutWindow.show(display);
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

      // create About Eclipse menu command
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
      }

      // schedule disposal of callback object
      display.disposeExec(
         new Runnable() {
            public void run() {
               commandCallback.dispose();
            }
         }
      );
   }
}
