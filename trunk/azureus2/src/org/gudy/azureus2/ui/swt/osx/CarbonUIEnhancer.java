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

import java.io.IOException;
import java.lang.reflect.Method;

import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.Callback;
import org.eclipse.swt.internal.carbon.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.platform.macosx.access.jnilib.OSXAccess;
import org.gudy.azureus2.ui.swt.UIExitUtilsSWT;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.config.wizard.ConfigureWizard;
import org.gudy.azureus2.ui.swt.help.AboutWindow;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.nat.NatTestWindow;
import org.gudy.azureus2.ui.swt.speedtest.SpeedTestWizard;

import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.apple.cocoa.application.NSApplication;

//import com.apple.eawt.*; //Application and ApplicationAdapter

public class CarbonUIEnhancer
{
	private static final int kHICommandPreferences = ('p' << 24) + ('r' << 16)
			+ ('e' << 8) + 'f';

	private static final int kHICommandAbout = ('a' << 24) + ('b' << 16)
			+ ('o' << 8) + 'u';

	private static final int kHICommandServices = ('s' << 24) + ('e' << 16)
			+ ('r' << 8) + 'v';

	private static final int kHICommandWizard = ('a' << 24) + ('z' << 16)
			+ ('c' << 8) + 'n';

	private static final int kHICommandNatTest = ('a' << 24) + ('z' << 16)
			+ ('n' << 8) + 't';

	private static final int kHICommandSpeedTest = ('a' << 24) + ('z' << 16)
			+ ('s' << 8) + 't';

	private static final int kHICommandRestart = ('a' << 24) + ('z' << 16)
			+ ('r' << 8) + 's';

	private static final int typeAEList = ('l' << 24) + ('i' << 16) + ('s' << 8)
			+ 't';

	private static final int kCoreEventClass = ('a' << 24) + ('e' << 16)
			+ ('v' << 8) + 't';

	private static final int kAEOpenDocuments = ('o' << 24) + ('d' << 16)
			+ ('o' << 8) + 'c';

	private static final int kAEReopenApplication = ('r' << 24) + ('a' << 16)
			+ ('p' << 8) + 'p';

	private static final int kAEOpenContents = ('o' << 24) + ('c' << 16)
			+ ('o' << 8) + 'n';

	private static final int kURLEventClass = ('G' << 24) + ('U' << 16)
			+ ('R' << 8) + 'L';

	private static final int typeText = ('T' << 24) + ('E' << 16) + ('X' << 8)
			+ 'T';

	private static final String RESOURCE_BUNDLE = "org.eclipse.ui.carbon.Messages"; //$NON-NLS-1$

	private static String fgAboutActionName;

	private static String fgWizardActionName;

	private static String fgNatTestActionName;

	private static String fgRestartActionName;

	private static String fgSpeedTestActionName;

	private static int memmove_type = 0;

	/**
	 * KN: Some of the menu items have been removed for the Vuze and Vuze Advanced UI's;
	 * the classic UI still retains all its menu items as before.  Follow this flag in the code
	 * to see which menu items are effected.
	 */
	private boolean isAZ3 = "az3".equalsIgnoreCase(COConfigurationManager.getStringParameter("ui"));

	public static final int BOUNCE_SINGLE = NSApplication.UserAttentionRequestInformational;

	public static final int BOUNCE_CONTINUOUS = NSApplication.UserAttentionRequestCritical;

	public CarbonUIEnhancer() {
		if (fgAboutActionName == null) {
			fgAboutActionName = MessageText.getString("MainWindow.menu.help.about").replaceAll(
					"&", "");
		}

		if (false == isAZ3) {
			if (fgWizardActionName == null) {
				fgWizardActionName = MessageText.getString(
						"MainWindow.menu.file.configure").replaceAll("&", "");
			}
			if (fgNatTestActionName == null) {
				fgNatTestActionName = MessageText.getString(
						"MainWindow.menu.tools.nattest").replaceAll("&", "");
			}

			if (fgSpeedTestActionName == null) {
				fgSpeedTestActionName = MessageText.getString(
						"MainWindow.menu.tools.speedtest").replaceAll("&", "");
			}
		}

		if (fgRestartActionName == null) {
			fgRestartActionName = MessageText.getString(
					"MainWindow.menu.file.restart").replaceAll("&", "");
		}
		earlyStartup();
		registerTorrentFile();
	}

	public static void registerToolbarToggle(Shell shell) {
		final Callback toolbarToggleCB = new Callback(target, "toolbarToggle", 3);
		int toolbarToggle = toolbarToggleCB.getAddress();
		if (toolbarToggle == 0) {
			Debug.out("OSX: Could not find callback 'toolbarToggle'");
			toolbarToggleCB.dispose();
			return;
		}

		shell.getDisplay().disposeExec(new Runnable() {
			public void run() {
				toolbarToggleCB.dispose();
			}
		});

		//	 add the button to the window trim
		int windowHandle = OS.GetControlOwner(shell.handle);
		OS.ChangeWindowAttributes(windowHandle, OS.kWindowToolbarButtonAttribute, 0);

		int[] mask = new int[] {
			OS.kEventClassWindow,
			OS.kEventWindowToolbarSwitchMode
		};
		// register the handler with the OS
		OS.InstallEventHandler(OS.GetApplicationEventTarget(), toolbarToggle,
				mask.length / 2, mask, 0, null);
	}

	private void registerTorrentFile() {
		int result;

		Callback clickDockIconCallback = new Callback(target, "clickDockIcon", 3);
		int clickDocIcon = clickDockIconCallback.getAddress();
		if (clickDocIcon == 0) {
			clickDockIconCallback.dispose();
		} else {
			result = OS.AEInstallEventHandler(kCoreEventClass, kAEReopenApplication,
					clickDocIcon, 0, false);

			if (result != OS.noErr) {
				Debug.out("OSX: Could Install ReopenApplication Event Handler. Error: "
						+ result);
			}
		}

		Callback openContentsCallback = new Callback(target, "openContents", 3);
		int openContents = openContentsCallback.getAddress();
		if (openContents == 0) {
			openContentsCallback.dispose();
		} else {
			result = OS.AEInstallEventHandler(kCoreEventClass, kAEOpenContents,
					openContents, 0, false);

			if (result != OS.noErr) {
				Debug.out("OSX: Could Install OpenContents Event Handler. Error: "
						+ result);
			}
		}

		Callback openDocCallback = new Callback(target, "openDocProc", 3);
		int openDocProc = openDocCallback.getAddress();
		if (openDocProc == 0) {
			Debug.out("OSX: Could not find Callback 'openDocProc'");
			openDocCallback.dispose();
			return;
		}

		result = OS.AEInstallEventHandler(kCoreEventClass, kAEOpenDocuments,
				openDocProc, 0, false);

		if (result != OS.noErr) {
			Debug.out("OSX: Could not Install OpenDocs Event Handler. Error: "
					+ result);
			return;
		}

		result = OS.AEInstallEventHandler(kURLEventClass, kURLEventClass,
				openDocProc, 0, false);
		if (result != OS.noErr) {
			Debug.out("OSX: Could not Install URLEventClass Event Handler. Error: "
					+ result);
			return;
		}

		///

		Callback quitAppCallback = new Callback(target, "quitAppProc", 3);
		int quitAppProc = quitAppCallback.getAddress();
		if (quitAppProc == 0) {
			Debug.out("OSX: Could not find Callback 'quitApp'");
			quitAppCallback.dispose();
		} else {
			result = OS.AEInstallEventHandler(kCoreEventClass, OS.kAEQuitApplication,
					quitAppProc, 0, false);
			if (result != OS.noErr) {
				Debug.out("OSX: Could not install QuitApplication Event Handler. Error: "
						+ result);
			}
		}

		///

		int appTarget = OS.GetApplicationEventTarget();
		Callback appleEventCallback = new Callback(this, "appleEventProc", 3);
		int appleEventProc = appleEventCallback.getAddress();
		int[] mask3 = new int[] {
			OS.kEventClassAppleEvent,
			OS.kEventAppleEvent,
			kURLEventClass,
			kAEReopenApplication,
			kAEOpenContents,
		};
		result = OS.InstallEventHandler(appTarget, appleEventProc,
				mask3.length / 2, mask3, 0, null);
		if (result != OS.noErr) {
			Debug.out("OSX: Could Install Event Handler. Error: " + result);
			return;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IStartup#earlyStartup()
	 */
	public void earlyStartup() {
		final Display display = Display.getDefault();
		display.syncExec(new AERunnable() {
			public void runSupport() {
				hookApplicationMenu(display);
			}
		});
	}

	/**
	* See Apple Technical Q&A 1079 (http://developer.apple.com/qa/qa2001/qa1079.html)<br />
	* Also http://developer.apple.com/documentation/Carbon/Reference/Menu_Manager/menu_mgr_ref/function_group_10.html
	*/
	public void hookApplicationMenu(final Display display) {
		// Callback target
		Object target = new Object() {
			int commandProc(int nextHandler, int theEvent, int userData) {
				if (OS.GetEventKind(theEvent) == OS.kEventProcessCommand) {
					HICommand command = new HICommand();
					OS.GetEventParameter(theEvent, OS.kEventParamDirectObject,
							OS.typeHICommand, null, HICommand.sizeof, null, command);
					switch (command.commandID) {
						case kHICommandPreferences: {
							UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
							if (uiFunctions != null) {
								uiFunctions.openView(UIFunctions.VIEW_CONFIG, null);
							}
							return OS.noErr;
						}
						case kHICommandAbout:
							AboutWindow.show();
							return OS.noErr;
						case kHICommandRestart: {
							UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
							if (uiFunctions != null) {
								uiFunctions.dispose(true, false);
							}
							return OS.noErr;
						}
						case kHICommandWizard:
							new ConfigureWizard(false);
							return OS.noErr;
						case kHICommandNatTest:
							new NatTestWindow();
							return OS.noErr;
						case kHICommandSpeedTest:
							new SpeedTestWizard();
							return OS.noErr;

						case OS.kAEQuitApplication:
							UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
							if (uiFunctions != null) {
								uiFunctions.dispose(false, false);
								return OS.noErr;
							} else {
								UIExitUtilsSWT.setSkipCloseCheck(true);
							}
						default:
							break;
					}
				}
				return OS.eventNotHandledErr;
			}
		};
		final Callback commandCallback = new Callback(target, "commandProc", 3); //$NON-NLS-1$
		int commandProc = commandCallback.getAddress();
		if (commandProc == 0) {
			commandCallback.dispose();
			return; // give up
		}

		// Install event handler for commands
		int[] mask = new int[] {
			OS.kEventClassCommand,
			OS.kEventProcessCommand
		};
		OS.InstallEventHandler(OS.GetApplicationEventTarget(), commandProc,
				mask.length / 2, mask, 0, null);

		// create About menu command
		int[] outMenu = new int[1];
		short[] outIndex = new short[1];
		if (OS.GetIndMenuItemWithCommandID(0, kHICommandPreferences, 1, outMenu,
				outIndex) == OS.noErr
				&& outMenu[0] != 0) {
			int menu = outMenu[0];

			int l = fgAboutActionName.length();
			char buffer[] = new char[l];
			fgAboutActionName.getChars(0, l, buffer, 0);
			int str = OS.CFStringCreateWithCharacters(OS.kCFAllocatorDefault, buffer,
					l);
			OS.InsertMenuItemTextWithCFString(menu, str, (short) 0, 0,
					kHICommandAbout);
			OS.CFRelease(str);
			// add separator between About & Preferences
			OS.InsertMenuItemTextWithCFString(menu, 0, (short) 1,
					OS.kMenuItemAttrSeparator, 0);

			// enable pref menu
			OS.EnableMenuCommand(menu, kHICommandPreferences);
			// disable services menu
			OS.DisableMenuCommand(menu, kHICommandServices);

			if (false == isAZ3) {
				// wizard menu
				l = fgWizardActionName.length();
				buffer = new char[l];
				fgWizardActionName.getChars(0, l, buffer, 0);
				str = OS.CFStringCreateWithCharacters(OS.kCFAllocatorDefault, buffer, l);
				OS.InsertMenuItemTextWithCFString(menu, str, (short) 3, 0,
						kHICommandWizard);
				OS.CFRelease(str);

				// NAT test menu
				l = fgNatTestActionName.length();
				buffer = new char[l];
				fgNatTestActionName.getChars(0, l, buffer, 0);
				str = OS.CFStringCreateWithCharacters(OS.kCFAllocatorDefault, buffer, l);
				OS.InsertMenuItemTextWithCFString(menu, str, (short) 4, 0,
						kHICommandNatTest);
				OS.CFRelease(str);

				//SpeedTest
				l = fgSpeedTestActionName.length();
				buffer = new char[l];
				fgSpeedTestActionName.getChars(0, l, buffer, 0);
				str = OS.CFStringCreateWithCharacters(OS.kCFAllocatorDefault, buffer, l);
				OS.InsertMenuItemTextWithCFString(menu, str, (short) 5, 0,
						kHICommandSpeedTest);
				OS.CFRelease(str);
			}

			OS.InsertMenuItemTextWithCFString(menu, 0, (short) 6,
					OS.kMenuItemAttrSeparator, 0);

			// restart menu
			l = fgRestartActionName.length();
			buffer = new char[l];
			fgRestartActionName.getChars(0, l, buffer, 0);
			str = OS.CFStringCreateWithCharacters(OS.kCFAllocatorDefault, buffer, l);
			OS.InsertMenuItemTextWithCFString(menu, str, (short) 7, 0,
					kHICommandRestart);
			OS.CFRelease(str);

			OS.InsertMenuItemTextWithCFString(menu, 0, (short) 8,
					OS.kMenuItemAttrSeparator, 0);
		}

		// schedule disposal of callback object
		display.disposeExec(new AERunnable() {
			public void runSupport() {
				commandCallback.dispose();
				//               stopSidekick();
			}
		});
	}

	private static void stopSidekick() {
		try {
			Runtime.getRuntime().exec(new String[] {
				"osascript",
				"-e",
				"tell application \"Azureus\" to quit"
			});
		} catch (IOException e) {
			Debug.printStackTrace(e);
		}
	}

	int appleEventProc(int nextHandler, int theEvent, int userData) {
		int eventClass = OS.GetEventClass(theEvent);
		//int eventKind = OS.GetEventKind(theEvent);

		//System.out.println("appleEventProc " + OSXtoString(eventClass) + ";"
		//		+ OS.GetEventKind(theEvent) + ";" + OSXtoString(theEvent) + ";"
		//		+ OSXtoString(userData));

		// Process teh odoc event
		if (eventClass == OS.kEventClassAppleEvent) {
			int[] aeEventID = new int[1];
			if (OS.GetEventParameter(theEvent, OS.kEventParamAEEventID, OS.typeType,
					null, 4, null, aeEventID) != OS.noErr) {
				return OS.eventNotHandledErr;
			}
			//System.out.println("EventID = " + OSXtoString(aeEventID[0]));
			if (aeEventID[0] != kAEOpenDocuments && aeEventID[0] != kURLEventClass
					&& aeEventID[0] != kAEReopenApplication
					&& aeEventID[0] != kAEOpenContents
					&& aeEventID[0] != OS.kAEQuitApplication) {
				return OS.eventNotHandledErr;
			}

			// Handle Event
			EventRecord eventRecord = new EventRecord();
			OS.ConvertEventRefToEventRecord(theEvent, eventRecord);
			OS.AEProcessAppleEvent(eventRecord);

			// Tell Mac we are handling this event
			return OS.noErr;
		}

		return OS.eventNotHandledErr;
	}

	private static String OSXtoString(int i) {
		char[] c = new char[4];
		c[0] = (char) ((i >> 24) & 0xff);
		c[1] = (char) ((i >> 16) & 0xff);
		c[2] = (char) ((i >> 8) & 0xff);
		c[3] = (char) (i & 0xff);
		return new String(c);
	}

	private static void memmove(byte[] dest, int src, int size) {
		switch (memmove_type) {
			case 0:
				try {
					OSXAccess.memmove(dest, src, size);
					memmove_type = 0;
					return;
				} catch (Throwable e) {
				}
				// FALL THROUGH

			case 1:
				try {
					Class cMemMove = Class.forName("org.eclipse.swt.internal.carbon.OS");

					Method method = cMemMove.getMethod("memmove", new Class[] {
						byte[].class,
						Integer.TYPE,
						Integer.TYPE
					});

					method.invoke(null, new Object[] {
						dest,
						new Integer(src),
						new Integer(size)
					});
					memmove_type = 1;
					return;
				} catch (Throwable e) {
				}

				// FALL THROUGH
			case 2:
				try {
					Class cMemMove = Class.forName("org.eclipse.swt.internal.carbon.OS");

					Method method = cMemMove.getMethod("memcpy", new Class[] {
						byte[].class,
						Integer.TYPE,
						Integer.TYPE
					});

					method.invoke(null, new Object[] {
						dest,
						new Integer(src),
						new Integer(size)
					});

					memmove_type = 2;
					return;
				} catch (Throwable e) {
				}

				// FALL THROUGH

			default:
				break;
		}

		memmove_type = 3;
	}

	final static Object target = new Object() {
		int quitAppProc(int theAppleEvent, int reply, int handlerRefcon) {
			UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
			if (uiFunctions != null) {
				uiFunctions.dispose(false, false);
			} else {
				UIExitUtilsSWT.setSkipCloseCheck(true);
				Display.getDefault().dispose();
			}
			return OS.noErr;
		}

		int openDocProc(int theAppleEvent, int reply, int handlerRefcon) {
			AEDesc aeDesc = new AEDesc();
			EventRecord eventRecord = new EventRecord();
			OS.ConvertEventRefToEventRecord(theAppleEvent, eventRecord);
			try {
				int result = OSXAccess.AEGetParamDesc(theAppleEvent,
						OS.kEventParamDirectObject, typeAEList, aeDesc);
				if (result != OS.noErr) {
					Debug.out("OSX: Could call AEGetParamDesc. Error: " + result);
					return OS.noErr;
				}
			} catch (java.lang.UnsatisfiedLinkError e) {
				Debug.out("OSX: AEGetParamDesc not available.  Can't open sent file");
				return OS.noErr;
			}

			int[] count = new int[1];
			OS.AECountItems(aeDesc, count);
			//System.out.println("COUNT: " + count[0]);
			if (count[0] > 0) {
				String[] fileNames = new String[count[0]];
				int maximumSize = 80; // size of FSRef
				int dataPtr = OS.NewPtr(maximumSize);
				int[] aeKeyword = new int[1];
				int[] typeCode = new int[1];
				int[] actualSize = new int[1];
				for (int i = 0; i < count[0]; i++) {
					if (OS.AEGetNthPtr(aeDesc, i + 1, OS.typeFSRef, aeKeyword, typeCode,
							dataPtr, maximumSize, actualSize) == OS.noErr) {
						byte[] fsRef = new byte[actualSize[0]];
						memmove(fsRef, dataPtr, actualSize[0]);
						int dirUrl = OS.CFURLCreateFromFSRef(OS.kCFAllocatorDefault, fsRef);
						int dirString = OS.CFURLCopyFileSystemPath(dirUrl,
								OS.kCFURLPOSIXPathStyle);
						OS.CFRelease(dirUrl);
						int length = OS.CFStringGetLength(dirString);
						char[] buffer = new char[length];
						CFRange range = new CFRange();
						range.length = length;
						OS.CFStringGetCharacters(dirString, range, buffer);
						OS.CFRelease(dirString);
						fileNames[i] = new String(buffer);
					}

					if (OS.AEGetNthPtr(aeDesc, i + 1, typeText, aeKeyword, typeCode,
							dataPtr, maximumSize, actualSize) == OS.noErr) {
						byte[] urlRef = new byte[actualSize[0]];
						memmove(urlRef, dataPtr, actualSize[0]);
						fileNames[i] = new String(urlRef);
					}

					//System.out.println(fileNames[i]);
				}

				TorrentOpener.openTorrents(fileNames);
			}

			return OS.noErr;
		}

		int clickDockIcon(int nextHandler, int theEvent, int userData) {
			UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
			if (uiFunctions != null) {
				uiFunctions.bringToFront();
				return OS.noErr;
			}
			return OS.eventNotHandledErr;
		}

		int openContents(int nextHandler, int theEvent, int userData) {
			Debug.out("openDocContents");
			return OS.noErr;
		}

		int toolbarToggle(int nextHandler, int theEvent, int userData) {
			int eventKind = OS.GetEventKind(theEvent);
			if (eventKind != OS.kEventWindowToolbarSwitchMode) {
				return OS.eventNotHandledErr;
			}

			int[] theWindow = new int[1];
			OS.GetEventParameter(theEvent, OS.kEventParamDirectObject,
					OS.typeWindowRef, null, 4, null, theWindow);

			int[] theRoot = new int[1];
			OS.GetRootControl(theWindow[0], theRoot);
			final Widget widget = Display.getCurrent().findWidget(theRoot[0]);

			if (!(widget instanceof Shell)) {
				return OS.eventNotHandledErr;
			}
			final Shell shellAffected = (Shell) widget;

			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					int type;
					Long l = (Long) shellAffected.getData("OSX.ToolBarToggle");
					if (l == null || l.longValue() == 0) {
						type = SWT.Collapse;
					} else {
						type = SWT.Expand;
					}

					Event event = new Event();
					event.type = type;
					event.display = widget.getDisplay();
					event.widget = widget;
					shellAffected.notifyListeners(type, event);

					shellAffected.setData("OSX.ToolBarToggle", new Long(
							type == SWT.Collapse ? 1 : 0));
				}
			});

			return OS.noErr;
		}
	};

}