package com.vuze.tests.swt;

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.bouncycastle.util.Strings;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreLifecycleAdapter;

public class testBrowserFunc
{

	public static void main(String[] args) {
		Display display = new Display();
		
		AzureusCore core = AzureusCoreFactory.create();
		core.addLifecycleListener(new AzureusCoreLifecycleAdapter() {
			public void started(final AzureusCore core) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						open(core);
					}

				});
			}
		});
		core.start();

		while (!display.isDisposed ()) {
			if (!display.readAndDispatch ())
				display.sleep ();
		}

	}
	
	private static void open(final AzureusCore core) {


		Display display = Display.getDefault();
		final Shell shell = new Shell(display, SWT.SHELL_TRIM);
		shell.setLayout(new FillLayout());

		final Browser b1 = new Browser(shell, SWT.NONE);
		
		String s = "" +
		"<SCRIPT type=\"text/javascript\">" +
		"" +
		"var btapp =  {" +
		"peer_id : eval(bt2vuze('peer_id')), " +
		"settings : \n {" +
		"  all : function() { return eval(bt2vuze('settings.all')) }, \n" +
		"  keys : function() { return eval(bt2vuze('settings.keys')) }, \n" +
		"  get : function(key) { return eval(bt2vuze('settings.get', key)) }, \n" +
		"  set : function(key, val) { return eval(bt2vuze('settings.set', key, val)) }, \n" +
		"},\n" +
		"add : \n {" +
		"  torrent : function(tor) { return eval(bt2vuze('add.torrent', tor)) }, \n" +
		"  rss_feed : function() { return eval(bt2vuze('add.rss_feed')) }, \n" +
		"  rss_filter : function() { return eval(bt2vuze('add.rss_filter')) }, \n" +
		"},\n" +
		"events : \n {" +
		"  set : function(key, func) { return eval(bt2vuze('events.set', key, func)) }, \n" +
		"},\n" +
		"torrent : \n {" +
		"  all : function() { return eval(bt2vuze('torrent.all')) }, \n" +
		"  keys : function() { return eval(bt2vuze('torrent.keys')) }, \n" +
		"  get : function(key) { return eval(bt2vuze('torrent.get', key)) }, \n" +
		"},\n" +
		"}\n" +
//		"var foo =btapp.peer_id; " +
//		"alert(foo);\n" +
//		"alert(btapp.settings.all());\n" +
		"</script>\n" +
		"<A HREF=\"\" ONCLICK=\"alert(btapp.peer_id); return false;\">peer_id</A><BR>" +
		"<A HREF=\"\" ONCLICK=\"alert(btapp.settings.all()); return false;\">settings.all()</A><BR>" +
		"<A HREF=\"\" ONCLICK=\"alert(btapp.settings.keys()); return false;\">settings.keys()</A><BR>" +
		"<A HREF=\"\" ONCLICK=\"alert(btapp.settings.get('max_downloads')); return false;\">settings.get('max_downloads')</A><BR>" +
		"<A HREF=\"\" ONCLICK=\"var foo = btapp.settings.all(); alert(foo.toString()); return false;\">Test1</A><BR>" +
		"<A HREF=\"\" ONCLICK=\"alert(String(btapp.settings.all())); return false;\">Test1</A><BR>" +
		"<A HREF=\"\" ONCLICK=\"alert(btapp.settings.all()); return false;\">Test1</A><BR>" +
			"";
		

		new BrowserFunction(b1, "btapp") {
			public Object function(Object[] arguments) {
				System.out.println("GOO");
				return "{ id : 'foo' }";
			}
		};
		
		new BrowserFunction(b1, "bt2vuze_peer_id") {
			public Object function(Object[] arguments) {
				System.out.println("GOO");
				return "({ id : \"foo\" })";
			}
		};
		 new BrowserFunction(b1, "bt2vuze") {
			
			public Object function(Object[] arguments) {
				if (arguments.length == 0) {
					return null;
				}
				if (!(arguments[0] instanceof String)) {
					return null;
				}
				String func = (String) arguments[0];
				String result = process(func, arguments);
				System.out.println("Func " + Arrays.toString(arguments) + ": " + result);
				return result;
			}
			
			private String process(String func, Object[] args) {
				String lfunc = func.toLowerCase();
				if (lfunc.equals("peer_id")) {
					// We generate peer_id per torrent.. so just fudge this
					return "('VuzeConstantPeerID')";
				} else if (lfunc.equals("settings.all")) {
					StringBuffer sb = new StringBuffer();
					sb.append("({");
					Map<String, Object> allSettings = getAllSettings();
					boolean first = true;
					for (Iterator<String> iter = allSettings.keySet().iterator(); iter.hasNext();) {
						String key = iter.next();
						if (first) {
							first = false;
						} else {
							sb.append(", ");
						}
						sb.append('\"');
						sb.append(key);
						sb.append("\" : \"");
						sb.append(allSettings.get(key));
						sb.append('\"');
					}
					sb.append("})");
					return sb.toString();
				} else if (lfunc.equals("settings.keys")) {
					StringBuffer sb = new StringBuffer();
					sb.append("([");
					Map<String, Object> allSettings = getAllSettings();
					boolean first = true;
					for (Iterator<String> iter = allSettings.keySet().iterator(); iter.hasNext();) {
						String key = iter.next();
						if (first) {
							first = false;
						} else {
							sb.append(", ");
						}
						sb.append('\"');
						sb.append(key);
						sb.append('\"');
					}
					sb.append("])");
					return sb.toString();
				} else if (lfunc.equals("settings.get")) {
					Map<String, Object> allSettings = getAllSettings();
					Object object = allSettings.get(args[1]);
					
					
					StringBuffer sb = new StringBuffer();
					sb.append("(\"");
					sb.append(object);
					sb.append("\")");
					return sb.toString();
				} else if (lfunc.equals("settings.set")) {
				} else if (lfunc.equals("add.torrent")) {
				}
				return null;
			}
		};
		
		shell.open();


		b1.setText(s);
		
		
		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ())
				display.sleep ();
		}
		display.dispose ();
		core.stop();
	
	}

	protected static Map<String, Object> getAllSettings() {
		Map<String, Object> map = new HashMap<String, Object>();
		String[] keys = { "max_downloads", "max downloads" };
		for (int i = 0; i < keys.length; i += 2) {
			String btKey = keys[i];
			String azKey = keys[i + 1];
			map.put(btKey, COConfigurationManager.getParameter(azKey));
		}
		return map;
	}
}
