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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.gudy.azureus2.core3.util.SystemProperties;

/**
 * @author Arbeiten
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class MessageText {

  public static final Locale LOCALE_ENGLISH = new Locale("en", "");
  public static final Locale LOCALE_DEFAULT = new Locale("", ""); // == english
  private static Locale LOCALE_CURRENT = LOCALE_DEFAULT;
  private static final String BUNDLE_NAME = "org.gudy.azureus2.internat.MessagesBundle"; //$NON-NLS-1$
  private static Map pluginLocalizationPaths = new HashMap();
  private static ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, LOCALE_DEFAULT, MessageText.class.getClassLoader());
//  private static ResourceBundle RESOURCE_BUNDLE = new IntegratedResourceBundle(ResourceBundle.getBundle(BUNDLE_NAME, LOCALE_DEFAULT), pluginLocalizationPaths);
  private static ResourceBundle DEFAULT_BUNDLE = RESOURCE_BUNDLE;

  public static boolean keyExists(String key) {
    try {
      RESOURCE_BUNDLE.getString(key);
      return true;
    } catch (MissingResourceException e) {
      return false;
    }
  }

  /**
   * @param key
   * @return
   */
  public static String getString(String key, String sDefault) {
    try {
      return RESOURCE_BUNDLE.getString(key);
    } catch (MissingResourceException e) {
      return sDefault;
    }
  }

  public static String getString(String key) {
    // TODO Auto-generated method stub
    try {
      return RESOURCE_BUNDLE.getString(key);
    } catch (MissingResourceException e) {
      return '!' + key + '!';
    }
  }

  /**
   * Expands a message text and replaces occurrences of %1 with first param, %2 with second...
   * @param key
   * @param params
   * @return
   */
  
  public static String 
  getString(
  		String		key,
		String[]	params )
  {
  	String	res = getString(key);
  	
  	for(int i=0;i<params.length;i++){
  		
  		res = res.replaceAll("%"+(i+1), params[i]);
  	}
  	
  	return( res );
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
			// here's an example 
			// jar:file:C:/Documents%20and%20Settings/stuff/.javaws/cache/http/Dparg.homeip.net/P9090/DMazureus-jnlp/DMlib/XMAzureus2.jar1070487037531!/org/gudy/azureus2/internat/MessagesBundle.properties
			
    		// also on Mac we don't get the spaces escaped
    	
    	urlString = urlString.replaceAll(" ", "%20" );
    	
    	if ( !urlString.startsWith("jar:file:/")){
    		urlString = "jar:file:/".concat(urlString.substring(9));
    	}
      try {
      	// you can see that the '!' must be present and that we can safely use the last occurrence of it
      	
        int posPling = urlString.lastIndexOf('!');
        
        String jarName = urlString.substring(4, posPling);
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
    
    HashSet bundleSet = new HashSet();
    
    // Add local first
    File localDir = new File(SystemProperties.getUserPath());
    String localBundles[] = localDir.list(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.startsWith(prefix) && name.endsWith(extension);
      }
    });
    bundleSet.addAll(Arrays.asList(localBundles));
    // Add AppDir 2nd
    File appDir = new File(SystemProperties.getApplicationPath());
    String appBundles[] = appDir.list(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.startsWith(prefix) && name.endsWith(extension);
      }
    });
    bundleSet.addAll(Arrays.asList(appBundles));
    // Any duplicates will be ignored
    bundleSet.addAll(Arrays.asList(bundles));

    Locale[] foundLocales = new Locale[bundleSet.size()];
    Iterator val = bundleSet.iterator();
    int i = 0;
    while (val.hasNext()) {
      String sBundle = (String)val.next();
      
      //      System.out.println("ResourceBundle: " + bundles[i]);
      String locale = sBundle.substring(prefix.length(), sBundle.length() - extension.length());
      //      System.out.println("Locale: " + locale);
      if (locale.length() >= 6) {
        foundLocales[i++] = new Locale(locale.substring(1, 3), locale.substring(4, 6));
      } else if (locale.length() >= 3) {
        foundLocales[i++] = new Locale(locale.substring(1, 3));
      } else {
        foundLocales[i++] = LOCALE_ENGLISH;
      }
    }

    Arrays.sort(foundLocales, new Comparator() {
      public final int compare (Object a, Object b) {
        return ((Locale)a).getDisplayName((Locale)a).compareToIgnoreCase(((Locale)b).getDisplayName((Locale)b));
      }
    });
    return foundLocales;
  }

  public static boolean changeLocale(Locale newLocale) {
    return changeLocale(newLocale, false);
  }

  public static boolean changeLocale(Locale newLocale, boolean force) {
    if (LOCALE_ENGLISH.equals(newLocale))
      newLocale = LOCALE_DEFAULT;
    if (!LOCALE_CURRENT.equals(newLocale) || force) {
      ResourceBundle newResourceBundle = null;
      String bundleFolder = BUNDLE_NAME.replace('.', '/');
      final String prefix = BUNDLE_NAME.substring(BUNDLE_NAME.lastIndexOf('.') + 1);
      final String extension = ".properties";

      try {
        File userBundleFile = new File(SystemProperties.getUserPath());
        File appBundleFile = new File(SystemProperties.getApplicationPath());
        
        // Get the jarURL
        // XXX Is there a better way to get the JAR name?
        ClassLoader cl = MessageText.class.getClassLoader();
        String sJar = cl.getResource(bundleFolder + extension).toString();
        sJar = sJar.substring(0, sJar.length() - prefix.length() - extension.length());
        URL jarURL = new URL(sJar);

        // User dir overrides app dir which overrides jar file bundles
        URL[] urls = {userBundleFile.toURL(), appBundleFile.toURL(), jarURL};
        newResourceBundle = ResourceBundle.getBundle("MessagesBundle", newLocale, 
                                                      new URLClassLoader(urls));
        if (newResourceBundle == null || 
            !newResourceBundle.getLocale().getLanguage().equals(newLocale.getLanguage())) {
          // try it without the country
          Locale localeJustLang = new Locale(newLocale.getLanguage());
          newResourceBundle = ResourceBundle.getBundle("MessagesBundle", localeJustLang, 
                                                        new URLClassLoader(urls));
          
          if (newResourceBundle == null ||
              !newResourceBundle.getLocale().getLanguage().equals(localeJustLang.getLanguage())) {
            // find first language we have in our list
            Locale[] locales = getLocales();
            for (int i = 0; i < locales.length; i++) {
              if (locales[i].getLanguage() == newLocale.getLanguage()) {
                newResourceBundle = ResourceBundle.getBundle("MessagesBundle", locales[i], 
                                                              new URLClassLoader(urls));
                break;
              }
            }
          }
        }
      } catch (MissingResourceException e) {
        System.out.println("changeLocale: no resource bundle for " + newLocale);
        e.printStackTrace();
        return false;
      } catch (Exception e) {
        e.printStackTrace();
      }

      if (newResourceBundle != null) {
        if (!newLocale.getLanguage().equals("en") && 
            !newResourceBundle.getLocale().equals(newLocale))
        {
          String sNewLanguage = newResourceBundle.getLocale().getDisplayName();
          if (sNewLanguage == null || sNewLanguage.trim().equals(""))
            sNewLanguage = "default (English)";
          System.out.println("changeLocale: no message properties for Locale '"+ 
                             newLocale.getDisplayName() +
                             "', using '" + sNewLanguage + "'");
        }
        newLocale = newResourceBundle.getLocale();
        Locale.setDefault(newLocale);
        LOCALE_CURRENT = newLocale;
        RESOURCE_BUNDLE = new IntegratedResourceBundle(newResourceBundle, pluginLocalizationPaths);
        return true;
      } else
        return false;
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
