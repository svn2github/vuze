/*
 * Created on 24.07.2003
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.core3.internat;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Arbeiten
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class MessageText {

  public static final Locale LOCALE_ENGLISH = new Locale("en", "EN");
  public static final Locale LOCALE_DEFAULT = new Locale("", ""); // == english
  private static Locale LOCALE_CURRENT = LOCALE_DEFAULT;
  private static final String BUNDLE_NAME = "org.gudy.azureus2.internat.MessagesBundle"; //$NON-NLS-1$
  private static Map pluginLocalizationPaths = new HashMap();
  private static ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, LOCALE_DEFAULT, MessageText.class.getClassLoader());
//  private static ResourceBundle RESOURCE_BUNDLE = new IntegratedResourceBundle(ResourceBundle.getBundle(BUNDLE_NAME, LOCALE_DEFAULT), pluginLocalizationPaths);
  private static ResourceBundle DEFAULT_BUNDLE = RESOURCE_BUNDLE;

  /**
   * @param key
   * @return
   */
  public static String getString(String key) {
    // TODO Auto-generated method stub
    try {
      return RESOURCE_BUNDLE.getString(key);
    } catch (MissingResourceException e) {
      return '!' + key + '!';
    }
  }

  public static String getDefaultLocaleString(String key) {
    // TODO Auto-generated method stub
    try {
      return DEFAULT_BUNDLE.getString(key);
    } catch (MissingResourceException e) {
      return '!' + key + '!';
    }
  }

  public static Locale getCurrentLocale() {
    return LOCALE_DEFAULT.equals(LOCALE_CURRENT) ? LOCALE_ENGLISH : LOCALE_CURRENT;
  }

  public static boolean isCurrentLocale(Locale locale) {
    return LOCALE_ENGLISH.equals(locale) ? LOCALE_CURRENT.equals(LOCALE_DEFAULT) : LOCALE_CURRENT.equals(locale);
  }

  public static Locale[] getLocales() {
    String bundleFolder = BUNDLE_NAME.replace('.', '/');
    final String prefix = BUNDLE_NAME.substring(BUNDLE_NAME.lastIndexOf('.') + 1);
    final String extension = ".properties";

    String urlString = MessageText.class.getClassLoader().getResource(bundleFolder.concat(extension)).toExternalForm();
    //System.out.println("urlString: " + urlString);
    String[] bundles = null;
    if (urlString.startsWith("jar:file:")) {
    	
    		// java web start returns a url like "jar:file:c:/sdsd" which then fails as the file
    		// part doesn't start with a "/". Add it in!
    		
    	if ( !urlString.startsWith("jar:file:/")){
    		urlString = "jar:file:/" + urlString.substring(9);
    	}
      try {
        int posDirectory = urlString.indexOf(".jar!", 11);
        String jarName = urlString.substring(4, posDirectory + 4);
        //        System.out.println("jarName: " + jarName);
        URI uri = URI.create(jarName);
        File jar = new File(uri);
        //        System.out.println("jar: " + jar.getAbsolutePath());
        JarFile jarFile = new JarFile(jar);
        Enumeration entries = jarFile.entries();
        ArrayList list = new ArrayList(250);
        while (entries.hasMoreElements()) {
          JarEntry jarEntry = (JarEntry) entries.nextElement();
          if (jarEntry.getName().startsWith(bundleFolder) && jarEntry.getName().endsWith(extension) && jarEntry.getName().length() < bundleFolder.length() + extension.length() + 7) {
            //            System.out.println("jarEntry: " + jarEntry.getName());
            list.add(jarEntry.getName().substring(bundleFolder.length() - prefix.length()));
            // "MessagesBundle_de_DE.properties"
          }
        }
        bundles = (String[]) list.toArray(new String[list.size()]);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      File bundleDirectory = new File(URI.create(urlString)).getParentFile();
      //      System.out.println("bundleDirectory: " +
      // bundleDirectory.getAbsolutePath());

      bundles = bundleDirectory.list(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.startsWith(prefix) && name.endsWith(extension);
        }
      });
    }

    Locale[] foundLocales = new Locale[bundles.length];
    for (int i = 0; i < bundles.length; i++) {
      //      System.out.println("ResourceBundle: " + bundles[i]);
      String locale = bundles[i].substring(prefix.length(), bundles[i].length() - extension.length());
      //      System.out.println("Locale: " + locale);
      foundLocales[i] = locale.length() == 0 ? LOCALE_ENGLISH : new Locale(locale.substring(1, 3), locale.substring(4, 6));
    }
    return foundLocales;
  }

  public static boolean changeLocale(Locale newLocale) {
    if (LOCALE_ENGLISH.equals(newLocale))
      newLocale = LOCALE_DEFAULT;
    if (!LOCALE_CURRENT.equals(newLocale)) {
      ResourceBundle newResourceBundle = null;
      try {
        newResourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, newLocale, MessageText.class.getClassLoader());
      } catch (Exception e) {
        System.out.println("changeLocale: no resource bundle for " + newLocale);
        e.printStackTrace();
        return false;
      }
      if (newResourceBundle.getLocale().equals(newLocale)) {
        Locale.setDefault(newLocale);
        LOCALE_CURRENT = newLocale;
        RESOURCE_BUNDLE = new IntegratedResourceBundle(newResourceBundle, pluginLocalizationPaths);
        return true;
      } else
        System.out.println("changeLocale: no message properties for Locale " + newLocale.getDisplayLanguage());
    }
    return false;
  }

  public static boolean integratePluginMessages(String localizationPath,ClassLoader classLoader) {
    boolean integratedSuccessfully = false;
    if (null != localizationPath && localizationPath.length() != 0 && !pluginLocalizationPaths.containsKey(localizationPath)) {
      pluginLocalizationPaths.put(localizationPath,classLoader);
      RESOURCE_BUNDLE = new IntegratedResourceBundle(RESOURCE_BUNDLE, pluginLocalizationPaths);
      integratedSuccessfully = true;
    }
    return integratedSuccessfully;
  }
}
