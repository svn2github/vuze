package org.gudy.azureus2.ui.swt.osx;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.C;
import org.eclipse.swt.internal.Callback;
import org.eclipse.swt.internal.cocoa.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.config.wizard.ConfigureWizard;
import org.gudy.azureus2.ui.swt.help.AboutWindow;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.nat.NatTestWindow;
import org.gudy.azureus2.ui.swt.speedtest.SpeedTestWizard;

import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;

/**
 * You can exclude this file (or this whole path) for non OSX builds
 * 
 * Hook some Cocoa specific abilities:
 * - App->About        <BR>
 * - App->Preferences  <BR>
 * - App->Exit         <BR>
 * <BR>
 * - OpenDocument  (possible limited to only files?) <BR>
 *
 * This code was influenced by the
 * <a href="http://www.transparentech.com/opensource/cocoauienhancer">
 * CocoaUIEnhancer</a>, which was influenced by the 
 * <a href="http://www.simidude.com/blog/2008/macify-a-swt-application-in-a-cross-platform-way/">
 * CarbonUIEnhancer from Agynami</a>.
 * 
 * Both cocoa implementations are modified from the 
 * <a href="http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.ui.cocoa/src/org/eclipse/ui/internal/cocoa/CocoaUIEnhancer.java">
 * org.eclipse.ui.internal.cocoa.CocoaUIEnhancer</a>, although my additions
 * aren't fully reflection-ized yet :(
 */
public class CocoaUIEnhancer
{
	private static final boolean DEBUG = false;

	private static Callback callBack3;

	private static long callBack3Addr;

	private static Callback callBack4;

	private static long callBack4Addr;

	private static CocoaUIEnhancer instance;

	private static final int kAboutMenuItem = 0;

	private static final int kPreferencesMenuItem = 2;

	private static final int kServicesMenuItem = 4;

	// private static final int kHideApplicationMenuItem = 6;
	// private static final int kQuitMenuItem = 10;

	private static int NSWindowCloseButton = 0;

	private static int NSWindowDocumentIconButton = 4;

	private static int NSWindowMiniaturizeButton = 1;

	private static int NSWindowToolbarButton = 3;

	private static int NSWindowZoomButton = 2;

	private static long sel_aboutMenuItemSelected_;

	private static long sel_application_openFile_;

	private static long sel_application_openFiles_;

	private static long sel_preferencesMenuItemSelected_;

	private static long sel_toolbarButtonClicked_;

	private static long sel_restartMenuSelected_;

	private static long sel_wizardMenuSelected_;

	private static long sel_natMenuSelected_;

	private static long sel_speedMenuSelected_;

	private static int clsSWTCocoaEnhancerDelegate;

	static final byte[] SWT_OBJECT = {
		'S',
		'W',
		'T',
		'_',
		'O',
		'B',
		'J',
		'E',
		'C',
		'T',
		'\0'
	};

	private long delegateIdSWTApplication;

	private int /*long*/delegateJniRef;

	private SWTCocoaEnhancerDelegate delegate;

	static {
		Class<CocoaUIEnhancer> clazz = CocoaUIEnhancer.class;

		try {
			callBack3 = new Callback(clazz, "actionProc", 3);
			Method getAddress;
			getAddress = Callback.class.getMethod("getAddress", new Class[0]);
			Object object = getAddress.invoke(callBack3, (Object[]) null);
			callBack3Addr = convertToLong(object);
			if (callBack3Addr == 0) {
				SWT.error(SWT.ERROR_NO_MORE_CALLBACKS);
			}

			callBack4 = new Callback(clazz, "actionProc", 4);
			getAddress = Callback.class.getMethod("getAddress", new Class[0]);
			object = getAddress.invoke(callBack4, (Object[]) null);
			callBack4Addr = convertToLong(object);
			if (callBack4Addr == 0) {
				SWT.error(SWT.ERROR_NO_MORE_CALLBACKS);
			}
		} catch (Throwable e) {
		}

		String className = "SWTCocoaEnhancerDelegate";

		byte[] types = {
			'*',
			'\0'
		};
		int size = C.PTR_SIZEOF, align = C.PTR_SIZEOF == 4 ? 2 : 3;

		// TODO: Reflect
		clsSWTCocoaEnhancerDelegate = OS.objc_allocateClassPair(OS.class_NSObject,
				className, 0);
		OS.class_addIvar(clsSWTCocoaEnhancerDelegate, SWT_OBJECT, size,
				(byte) align, types);

		OS.objc_registerClassPair(clsSWTCocoaEnhancerDelegate);
	}

	static int actionProc(int id, int sel, int arg0) {
		if (DEBUG) {
			System.err.println("id=" + id + ";sel=" + sel);
		}

		if (sel == sel_aboutMenuItemSelected_) {
			AboutWindow.show();
		} else if (sel == sel_restartMenuSelected_) {
			UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
			if (uiFunctions != null) {
				uiFunctions.dispose(true, false);
			}
		} else if (sel == sel_wizardMenuSelected_) {
			new ConfigureWizard(false);
		} else if (sel == sel_natMenuSelected_) {
			new NatTestWindow();
		} else if (sel == sel_speedMenuSelected_) {
			new SpeedTestWizard();
		} else if (sel == sel_toolbarButtonClicked_) {
			Class<?> osCls = classForName("org.eclipse.swt.internal.cocoa.OS");
			Object windowId = invoke(osCls, "objc_msgSend", new Object[] {
				wrapPointer(arg0),
				OS.sel_window
			});
			final Shell shellAffected = (Shell) invoke(Display.class,
					Display.getCurrent(), "findWidget", new Object[] {
						windowId
					});

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
					event.display = shellAffected.getDisplay();
					event.widget = shellAffected;
					shellAffected.notifyListeners(type, event);

					shellAffected.setData("OSX.ToolBarToggle", new Long(
							type == SWT.Collapse ? 1 : 0));
				}
			});

		} else if (sel == sel_preferencesMenuItemSelected_) {
			UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
			if (uiFunctions != null) {
				uiFunctions.openView(UIFunctions.VIEW_CONFIG, null);
			}
		}
		return 0;
	}

	static int /*long*/actionProc(int /*long*/id, int /*long*/sel,
			int /*long*/arg0, int /*long*/arg1)
			throws Throwable {
		if (DEBUG) {
			System.err.println("actionProc 4 " + id + "/" + sel);
		}
		Display display = Display.getCurrent();
		if (display == null)
			return 0;

		if (sel == sel_application_openFile_) {
			Class<?> nsstringCls = classForName("org.eclipse.swt.internal.cocoa.NSString");
			Constructor<?> conNSString = nsstringCls.getConstructor(new Class[] {
				int.class
			});
			Object file = conNSString.newInstance(arg1);
			String fileString = (String) invoke(file, "getString");
			if (DEBUG) {
				System.err.println("OMG GOT OpenFile " + fileString);
			}
			fileOpen(new String[] {
				fileString
			});
		} else if (sel == sel_application_openFiles_) {
			Class<?> nsarrayCls = classForName("org.eclipse.swt.internal.cocoa.NSArray");
			Class<?> nsstringCls = classForName("org.eclipse.swt.internal.cocoa.NSString");
			Constructor<?> conNSArray = nsarrayCls.getConstructor(new Class[] {
				int.class
			});
			Constructor<?> conNSString = nsstringCls.getConstructor(new Class[] {
				org.eclipse.swt.internal.cocoa.id.class
			});

			Object arrayOfFiles = conNSArray.newInstance(arg1);
			int count = ((Number) invoke(arrayOfFiles, "count")).intValue();

			String[] files = new String[count];
			for (int i = 0; i < count; i++) {
				Object fieldId = invoke(nsarrayCls, arrayOfFiles, "objectAtIndex",
						new Object[] {
							i
						});
				Object nsstring = conNSString.newInstance(fieldId);
				files[i] = (String) invoke(nsstring, "getString");

				if (DEBUG) {
					System.err.println("OMG GOT OpenFiles " + files[i]);
				}
			}
			fileOpen(files);
		}
		return 0;
	}

	private static Class<?> classForName(String classname) {
		try {
			Class<?> cls = Class.forName(classname);
			return cls;
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}

	private static long convertToLong(Object object) {
		if (object instanceof Integer) {
			Integer i = (Integer) object;
			return i.longValue();
		}
		if (object instanceof Long) {
			Long l = (Long) object;
			return l.longValue();
		}
		return 0;
	}

	protected static void fileOpen(String[] files) {
		TorrentOpener.openTorrents(files);
	}

	public static CocoaUIEnhancer getInstance() {
		if (instance == null) {
			try {
				instance = new CocoaUIEnhancer();
			} catch (Throwable e) {
				Debug.out(e);
			}
		}
		return instance;
	}

	private static Object invoke(Class<?> clazz, Object target,
			String methodName, Object[] args) {
		try {
			Class<?>[] signature = new Class<?>[args.length];
			for (int i = 0; i < args.length; i++) {
				Class<?> thisClass = args[i].getClass();
				if (thisClass == Integer.class)
					signature[i] = int.class;
				else if (thisClass == Long.class)
					signature[i] = long.class;
				else if (thisClass == Byte.class)
					signature[i] = byte.class;
				else if (thisClass == Boolean.class)
					signature[i] = boolean.class;
				else
					signature[i] = thisClass;
			}
			Method method = clazz.getMethod(methodName, signature);
			return method.invoke(target, args);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static Object invoke(Class<?> clazz, String methodName, Object[] args) {
		return invoke(clazz, null, methodName, args);
	}

	private static Object invoke(Object obj, String methodName) {
		return invoke(obj, methodName, (Class<?>[]) null, (Object[]) null);
	}

	private static Object invoke(Object obj, String methodName,
			Class<?>[] paramTypes, Object... arguments) {
		try {
			Method m = obj.getClass().getMethod(methodName, paramTypes);
			return m.invoke(obj, arguments);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static long registerName(Class<?> osCls, String name)
			throws IllegalArgumentException, SecurityException,
			IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Object object = invoke(osCls, "sel_registerName", new Object[] {
			name
		});
		return convertToLong(object);
	}

	////////////////////////////////////////////////////////////

	private static Object wrapPointer(long value) {
		Class<?> PTR_CLASS = C.PTR_SIZEOF == 8 ? long.class : int.class;
		if (PTR_CLASS == long.class)
			return new Long(value);
		else
			return new Integer((int) value);
	}

	private CocoaUIEnhancer()
			throws Throwable {

		// Instead of creating a new delegate class in objective-c,
		// just use the current SWTApplicationDelegate. An instance of this
		// is a field of the Cocoa Display object and is already the target
		// for the menuItems. So just get this class and add the new methods
		// to it.
		Class<?> osCls = classForName("org.eclipse.swt.internal.cocoa.OS");
		Object delegateObjSWTApplication = invoke(osCls, "objc_lookUpClass",
				new Object[] {
					"SWTApplicationDelegate"
				});
		delegateIdSWTApplication = convertToLong(delegateObjSWTApplication);
	}

	/**
	 * Hook the given Listener to the Mac OS X application Quit menu and the IActions to the About
	 * and Preferences menus.
	 * 
	 * @param display
	 *            The Display to use.
	 * @param quitListener
	 *            The listener to invoke when the Quit menu is invoked.
	 * @param aboutAction
	 *            The action to run when the About menu is invoked.
	 * @param preferencesAction
	 *            The action to run when the Preferences menu is invoked.
	 */
	public void hookApplicationMenu() {
		Display display = Display.getCurrent();
		try {
			// Initialize the menuItems.
			initialize();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		// Schedule disposal of callback object
		display.disposeExec(new Runnable() {
			public void run() {
				invoke(callBack3, "dispose");
				callBack3 = null;
				invoke(callBack4, "dispose");
				callBack4 = null;

				if (delegateJniRef != 0)
					OS.DeleteGlobalRef(delegateJniRef);
				delegateJniRef = 0;

				if (delegate != null)
					delegate.release();
				delegate = null;
			}
		});
	}

	public void hookDocumentOpen()
			throws Throwable {

		Class<?> osCls = classForName("org.eclipse.swt.internal.cocoa.OS");
		if (sel_application_openFile_ == 0) {
			sel_application_openFile_ = registerName(osCls, "application:openFile:");
		}
		invoke(osCls, "class_addMethod", new Object[] {
			wrapPointer(delegateIdSWTApplication),
			wrapPointer(sel_application_openFile_),
			wrapPointer(callBack4Addr),
			"@:@:@"
		});

		if (sel_application_openFiles_ == 0) {
			sel_application_openFiles_ = registerName(osCls, "application:openFiles:");
		}
		invoke(osCls, "class_addMethod", new Object[] {
			wrapPointer(delegateIdSWTApplication),
			wrapPointer(sel_application_openFiles_),
			wrapPointer(callBack4Addr),
			"@:@:@"
		});
	}

	private void initialize()
			throws Exception {

		Class<?> osCls = classForName("org.eclipse.swt.internal.cocoa.OS");

		// Register names in objective-c.
		if (sel_preferencesMenuItemSelected_ == 0) {
			sel_preferencesMenuItemSelected_ = registerName(osCls,
					"preferencesMenuItemSelected:");
			sel_aboutMenuItemSelected_ = registerName(osCls, "aboutMenuItemSelected:");
			sel_restartMenuSelected_ = registerName(osCls, "restartMenuItemSelected:");
			sel_natMenuSelected_ = registerName(osCls, "natMenuItemSelected:");
			sel_speedMenuSelected_ = registerName(osCls, "speedMenuItemSelected:");
			sel_wizardMenuSelected_ = registerName(osCls, "wizardMenuItemSelected:");
		}

		Class<?> nsmenuCls = classForName("org.eclipse.swt.internal.cocoa.NSMenu");
		Class<?> nsmenuitemCls = classForName("org.eclipse.swt.internal.cocoa.NSMenuItem");
		Class<?> nsapplicationCls = classForName("org.eclipse.swt.internal.cocoa.NSApplication");

		// Add the action callbacks for Preferences and About menu items.
		invoke(osCls, "class_addMethod", new Object[] {
			wrapPointer(delegateIdSWTApplication),
			wrapPointer(sel_preferencesMenuItemSelected_),
			wrapPointer(callBack3Addr),
			"@:@"
		});
		invoke(osCls, "class_addMethod", new Object[] {
			wrapPointer(delegateIdSWTApplication),
			wrapPointer(sel_aboutMenuItemSelected_),
			wrapPointer(callBack3Addr),
			"@:@"
		});
		invoke(osCls, "class_addMethod", new Object[] {
			wrapPointer(clsSWTCocoaEnhancerDelegate),
			wrapPointer(sel_restartMenuSelected_),
			wrapPointer(callBack3Addr),
			"@:@"
		});
		invoke(osCls, "class_addMethod", new Object[] {
			wrapPointer(clsSWTCocoaEnhancerDelegate),
			wrapPointer(sel_wizardMenuSelected_),
			wrapPointer(callBack3Addr),
			"@:@"
		});
		invoke(osCls, "class_addMethod", new Object[] {
			wrapPointer(clsSWTCocoaEnhancerDelegate),
			wrapPointer(sel_speedMenuSelected_),
			wrapPointer(callBack3Addr),
			"@:@"
		});
		invoke(osCls, "class_addMethod", new Object[] {
			wrapPointer(clsSWTCocoaEnhancerDelegate),
			wrapPointer(sel_natMenuSelected_),
			wrapPointer(callBack3Addr),
			"@:@"
		});

		// Get the Mac OS X Application menu.
		Object sharedApplication = invoke(nsapplicationCls, "sharedApplication");
		Object mainMenu = invoke(sharedApplication, "mainMenu");
		Object mainMenuItem = invoke(nsmenuCls, mainMenu, "itemAtIndex",
				new Object[] {
					wrapPointer(0)
				});
		Object appMenu = invoke(mainMenuItem, "submenu");

		// Create the About <application-name> menu command
		Object aboutMenuItem = invoke(nsmenuCls, appMenu, "itemAtIndex",
				new Object[] {
					wrapPointer(kAboutMenuItem)
				});

		// Enable the Preferences menuItem.
		Object prefMenuItem = invoke(nsmenuCls, appMenu, "itemAtIndex",
				new Object[] {
					wrapPointer(kPreferencesMenuItem)
				});
		invoke(nsmenuitemCls, prefMenuItem, "setEnabled", new Object[] {
			true
		});

		Object servicesMenuItem = invoke(nsmenuCls, appMenu, "itemAtIndex",
				new Object[] {
					wrapPointer(kServicesMenuItem)
				});
		invoke(nsmenuitemCls, servicesMenuItem, "setEnabled", new Object[] {
			false
		});

		// Set the action to execute when the About or Preferences menuItem is invoked.
		//
		// We don't need to set the target here as the current target is the SWTApplicationDelegate
		// and we have registerd the new selectors on it. So just set the new action to invoke the
		// selector.
		invoke(nsmenuitemCls, prefMenuItem, "setAction", new Object[] {
			wrapPointer(sel_preferencesMenuItemSelected_)
		});
		invoke(nsmenuitemCls, aboutMenuItem, "setAction", new Object[] {
			wrapPointer(sel_aboutMenuItemSelected_)
		});

		// Add other menus
		int menuId = ((NSMenu) appMenu).id;
		boolean isAZ3 = "az3".equalsIgnoreCase(COConfigurationManager.getStringParameter("ui"));

		if (!isAZ3) {
			// add Wizard, NAT Test, Speed Test

			addMenuItem(menuId, 5, (int) sel_wizardMenuSelected_,
					MessageText.getString("MainWindow.menu.file.configure").replaceAll(
							"&", ""));

			addMenuItem(menuId, 6, (int) sel_natMenuSelected_, MessageText.getString(
					"MainWindow.menu.tools.nattest").replaceAll("&", ""));

			addMenuItem(menuId, 7, (int) sel_speedMenuSelected_,
					MessageText.getString("MainWindow.menu.tools.speedtest").replaceAll(
							"&", ""));

		}

		int numOfItems = ((Number) invoke(((NSMenu) appMenu), "numberOfItems")).intValue();

		NSMenuItem sep = NSMenuItem.separatorItem();
		sep.retain();
		OS.objc_msgSend(menuId, OS.sel_insertItem_atIndex_, sep.id, numOfItems - 1);

		numOfItems++;

		addMenuItem(menuId, numOfItems - 1, (int) sel_restartMenuSelected_,
				MessageText.getString("MainWindow.menu.file.restart").replaceAll("&",
						""));
	}

	private void addMenuItem(int menuId, int index, int selector, String title) {
		NSMenuItem nsItem = (NSMenuItem) new SWTMenuItem().alloc();
		nsItem.initWithTitle(NSString.stringWith(title), 0, NSString.stringWith(""));
		nsItem.setTarget(delegate);
		nsItem.setAction(selector);
		OS.objc_msgSend(menuId, OS.sel_insertItem_atIndex_, nsItem.id, index);
	}

	private Object invoke(Class<?> cls, String methodName) {
		return invoke(cls, methodName, (Class<?>[]) null, (Object[]) null);
	}

	private Object invoke(Class<?> cls, String methodName, Class<?>[] paramTypes,
			Object... arguments) {
		try {
			Method m = cls.getMethod(methodName, paramTypes);
			return m.invoke(null, arguments);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public void registerToolbarToggle(Shell shell)
			throws Throwable {
		delegate = new SWTCocoaEnhancerDelegate();
		delegate.alloc().init();
		delegateJniRef = OS.NewGlobalRef(CocoaUIEnhancer.this);
		if (delegateJniRef == 0)
			SWT.error(SWT.ERROR_NO_HANDLES);
		OS.object_setInstanceVariable(delegate.id, SWT_OBJECT, delegateJniRef);

		Class<?> osCls = classForName("org.eclipse.swt.internal.cocoa.OS");
		if (sel_toolbarButtonClicked_ == 0) {
			sel_toolbarButtonClicked_ = registerName(osCls, "toolbarButtonClicked:");
		}

		invoke(osCls, "class_addMethod", new Object[] {
			wrapPointer(clsSWTCocoaEnhancerDelegate),
			wrapPointer(sel_toolbarButtonClicked_),
			wrapPointer(callBack3Addr),
			"@:@"
		});

		Class<?> nsstringCls = classForName("org.eclipse.swt.internal.cocoa.NSString");
		Class<?> nstoolbarCls = classForName("org.eclipse.swt.internal.cocoa.NSToolbar");
		Class<?> nsbuttonCls = classForName("org.eclipse.swt.internal.cocoa.NSButton");
		Class<?> nsidCls = classForName("org.eclipse.swt.internal.cocoa.id");

		//NSToolbar dummyBar = new NSToolbar();
		Object dummyBar = nstoolbarCls.newInstance();
		//dummyBar.alloc();
		invoke(dummyBar, "alloc");
		//dummyBar.initWithIdentifier(NSString.stringWith("SWTToolbar"));
		Object nsStrDummyToolbar = invoke(nsstringCls, "stringWith", new Object[] {
			"SWTToolbar"
		});
		invoke(dummyBar, "initWithIdentifier", new Class<?>[] {
			nsstringCls
		}, new Object[] {
			nsStrDummyToolbar
		});
		//dummyBar.setVisible(false);
		invoke(dummyBar, "setVisible", new Class<?>[] {
			boolean.class
		}, new Object[] {
			Boolean.FALSE
		});

		// reflect me
		//NSWindow nsWindow = shell.view.window();
		Object nsWindow = shell.view.window();
		//nsWindow.setToolbar(dummyBar);
		invoke(nsWindow, "setToolbar", new Class<?>[] {
			nstoolbarCls
		}, new Object[] {
			dummyBar
		});
		//nsWindow.setShowsToolbarButton(true);
		invoke(nsWindow, "setShowsToolbarButton", new Class<?>[] {
			boolean.class
		}, new Object[] {
			Boolean.TRUE
		});

		//NSButton toolbarButton = nsWindow.standardWindowButton(NSWindowToolbarButton);
		Object toolbarButton = invoke(nsWindow, "standardWindowButton",
				new Class<?>[] {
					int.class
				}, new Object[] {
					new Integer(NSWindowToolbarButton)
				});

		//toolbarButton.setTarget(delegate);
		invoke(toolbarButton, "setTarget", new Class[] {
			nsidCls
		}, new Object[] {
			delegate
		});

		//toolbarButton.setAction((int) sel_toolbarButtonClicked_);
		invoke(nsbuttonCls, toolbarButton, "setAction", new Object[] {
			wrapPointer(sel_toolbarButtonClicked_)
		});
	}

}
