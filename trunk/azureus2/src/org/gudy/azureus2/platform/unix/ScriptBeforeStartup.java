package org.gudy.azureus2.platform.unix;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gudy.azureus2.core3.util.FileUtil;

public class ScriptBeforeStartup
{
	public static void main(String[] args) {
		String moz = getNewGreDir();

		if (moz != null) {
			System.out.println("export MOZILLA_FIVE_HOME=\"" + moz + "\"\n"
					+ "if [ \"$LD_LIBRARY_PATH x\" = \" x\" ] ; then\n"
					+ "	export LD_LIBRARY_PATH=$MOZILLA_FIVE_HOME;\n" + "else\n"
					+ "	export LD_LIBRARY_PATH=$MOZILLA_FIVE_HOME:$LD_LIBRARY_PATH\n"
					+ "fi\n");
		} else {
			log("GRE/XULRunner automatically found");
		}
	}

	public static String getNewGreDir() {
		String grePath = null;
		final String[] confList = {
			"/etc/gre64.conf",
			"/etc/gre.d/gre64.conf",
			"/etc/gre.conf",
			"/etc/gre.d/gre.conf",
			"/etc/gre.d/xulrunner.conf",
			"/etc/gre.d/libxul0d.conf"
		};

		if (canOpenBrowser()) {
			return null;
		}

		log("Auto-scanning for GRE/XULRunner.  You can skip this by setting MOZILLA_FIVE_HOME to your GRE path.");
		try {
			Pattern pat = Pattern.compile("GRE_PATH=(.*)", Pattern.CASE_INSENSITIVE);
			for (int i = 0; i < confList.length; i++) {
				File file = new File(confList[i]);
				if (file.isFile() && file.canRead()) {
					log("  checking " + file + " for GRE_PATH");
					String fileText = FileUtil.readFileAsString(file, 16384);
					if (fileText != null) {
						Matcher matcher = pat.matcher(fileText);
						if (matcher.find()) {
							String possibleGrePath = matcher.group(1);
							if (isValidGrePath(new File(possibleGrePath))) {
								grePath = possibleGrePath;
								break;
							}
						}
					}
				}
			}

			if (grePath == null) {
				final ArrayList possibleDirs = new ArrayList();
				File libDir = new File("/usr");
				libDir.listFiles(new FileFilter() {
					public boolean accept(File pathname) {
						if (pathname.getName().startsWith("lib")) {
							possibleDirs.add(pathname);
						}
						return false;
					}
				});
				possibleDirs.add(new File("/usr/local"));
				possibleDirs.add(new File("/opt"));

				final String[] possibleDirNames = {
					"mozilla",
					"firefox",
					"seamonkey",
					"xulrunner",
				};

				FileFilter ffIsPossibleDir = new FileFilter() {
					public boolean accept(File pathname) {
						String name = pathname.getName().toLowerCase();
						for (int i = 0; i < possibleDirNames.length; i++) {
							if (name.startsWith(possibleDirNames[i])) {
								return true;
							}
						}
						return false;
					}
				};

				for (Iterator iter = possibleDirs.iterator(); iter.hasNext();) {
					File dir = (File) iter.next();

					File[] possibleFullDirs = dir.listFiles(ffIsPossibleDir);

					for (int i = 0; i < possibleFullDirs.length; i++) {
						log("  checking " + possibleFullDirs[i] + " for GRE");
						if (isValidGrePath(possibleFullDirs[i])) {
							grePath = possibleFullDirs[i].getAbsolutePath();
							break;
						}
					}
					if (grePath != null) {
						break;
					}
				}
			}

			if (grePath != null) {
				log("GRE found at " + grePath
						+ ".  If this is incorrect, you should set MOZILLA_FIVE_HOME.");
				System.setProperty("org.eclipse.swt.browser.XULRunnerPath", grePath);
			}
		} catch (Throwable t2) {
			//t2.printStackTrace();
			grePath = null;
		}

		if (!canOpenBrowser()) {
			log("Can't create browser.");
		}

		return grePath;
	}

	private static boolean canOpenBrowser() {
		try {
			Class claDisplay = Class.forName("org.eclipse.swt.widgets.Display");
			Object display = claDisplay.newInstance();

			Class claShell = Class.forName("org.eclipse.swt.widgets.Shell");
			Constructor shellConstruct = claShell.getConstructor(new Class[] {
				claDisplay,
			});
			Object shell = shellConstruct.newInstance(new Object[] {
				display
			});

			Class claBrowser = Class.forName("org.eclipse.swt.browser.Browser");
			Constructor[] constructors = claBrowser.getConstructors();
			for (int i = 0; i < constructors.length; i++) {
				if (constructors[i].getParameterTypes().length == 2) {
					Object browser = constructors[i].newInstance(new Object[] {
						shell,
						new Integer(0)
					});

					Method methSetUrl = claBrowser.getMethod("setUrl", new Class[] {
						String.class
					});
					methSetUrl.invoke(browser, new Object[] {
						"about:blank"
					});

					break;
				}
			}

			return true;
		} catch (Throwable e) {
			e.printStackTrace();
			return false;
		}

	}

	private static boolean isValidGrePath(File dir) {
		if (!dir.isDirectory()) {
			return false;
		}
		if (new File(dir, "components/libwidget_gtk.so").exists()
				|| new File(dir, "libwidget_gtk.so").exists()) {
			log("Can not use GRE from " + dir
					+ " as it's too old (GTK2 version required).");
			return false;
		}
		if (!new File(dir, "components/libwidget_gtk2.so").exists()
				&& !new File(dir, "libwidget_gtk2.so").exists()) {
			log("Can not use GRE from " + dir
					+ " because it's missing components/libwidget_gtk2.so.");
			return false;
		}
		return true;
	}

	private static void log(String string) {
		System.out.println("echo \"" + string.replaceAll("\"", "\\\"") + "\"");
	}
}
