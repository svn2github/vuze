/*
 * Created on 29.11.2003
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.plugins;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Every Plugin makes its own instance to use i18n
 * 
 * @author Rene Leonhardt
 */
public class MessageText {

  public static final Locale LOCALE_ENGLISH = new Locale("en", "EN");
  public static final Locale LOCALE_DEFAULT = new Locale("", ""); // == english
  private String BUNDLE_NAME;
  private ResourceBundle RESOURCE_BUNDLE;
  private ResourceBundle DEFAULT_BUNDLE;

  private MessageText() {}

  public MessageText(String locationOfMessages) {
    BUNDLE_NAME = locationOfMessages;
    RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, LOCALE_DEFAULT);
    DEFAULT_BUNDLE = RESOURCE_BUNDLE;
  }

  /**
	 * @param key
	 * @return
	 */
  public String getString(String key) {
    // TODO Auto-generated method stub
    try {
      return RESOURCE_BUNDLE.getString(key);
    } catch (MissingResourceException e) {
      return '!' + key + '!';
    }
  }

  public String getDefaultLocaleString(String key) {
    // TODO Auto-generated method stub
    try {
      return DEFAULT_BUNDLE.getString(key);
    } catch (MissingResourceException e) {
      return '!' + key + '!';
    }
  }

  public Locale getCurrentLocale() {
    Locale currentLocale = RESOURCE_BUNDLE.getLocale();
    if (LOCALE_DEFAULT.equals(currentLocale))
      currentLocale = LOCALE_ENGLISH;
    return currentLocale;
  }

  public boolean isCurrentLocale(Locale locale) {
    if (LOCALE_ENGLISH.equals(locale))
      locale = LOCALE_DEFAULT;
    return RESOURCE_BUNDLE.getLocale().equals(locale);
  }

  public Locale[] getLocales() {
    String bundleFolder = BUNDLE_NAME.replace('.', '/');
    final String prefix = BUNDLE_NAME.substring(BUNDLE_NAME.lastIndexOf('.') + 1);
    final String extension = ".properties";

    String urlString = ClassLoader.getSystemResource(bundleFolder.concat(extension)).toString();
    //    System.out.println("urlString: " + urlString);
    String[] bundles = null;
    if (urlString.startsWith("jar:file:/")) {
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

  public boolean changeLocale(Locale newLocale) {
    if (LOCALE_ENGLISH.equals(newLocale))
      newLocale = LOCALE_DEFAULT;
    if (!RESOURCE_BUNDLE.getLocale().equals(newLocale)) {
      ResourceBundle newResourceBundle = null;
      try {
        newResourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, newLocale);
      } catch (Exception e) {
        System.out.println("changeLocale: no resource bundle for " + newLocale);
        e.printStackTrace();
        return false;
      }
      if (newResourceBundle.getLocale().equals(newLocale)) {
        RESOURCE_BUNDLE = newResourceBundle;
        Locale.setDefault(newLocale);
        return true;
      } else
        System.out.println("changeLocale: no message properties for Locale " + newLocale.getDisplayLanguage());
    }
    return false;
  }
}