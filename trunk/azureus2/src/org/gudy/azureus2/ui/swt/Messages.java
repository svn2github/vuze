/*
 * Created on 21.07.2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.ui.swt;

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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Widget;

/**
 * @author Arbeiten
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Messages {

  private static final String BUNDLE_NAME = "org.gudy.azureus2.ui.swt.MessagesBundle"; //$NON-NLS-1$

  private static ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);
  public static Locale LOCALE_ENGLISH = new Locale("en", "EN");
  public static Locale LOCALE_DEFAULT = new Locale("", ""); // == english 

  /**
   * 
   */
  private Messages() {

    // TODO Auto-generated constructor stub
  }
  /**
   * @param key
   * @return
   */
  public static String getString(String key) {
    // TODO Auto-generated method stub
    try {
      return RESOURCE_BUNDLE.getString(key);
    }
    catch (MissingResourceException e) {
      return '!' + key + '!';
    }
  }

  public static boolean isCurrentLocale(Locale locale) {
    if (LOCALE_ENGLISH.equals(locale))
      locale = LOCALE_DEFAULT;
    return RESOURCE_BUNDLE.getLocale().equals(locale);
  }

  public static Locale[] getLocales() {
    String bundleFolder = BUNDLE_NAME.replace('.', '/');
    final String prefix = BUNDLE_NAME.substring(BUNDLE_NAME.lastIndexOf('.')+1);
    final String extension = ".properties";

    String urlString = ClassLoader.getSystemResource(bundleFolder.concat(extension)).toString();
//    System.out.println("urlString: " + urlString);
    String[] bundles = null;
    if(urlString.startsWith("jar:file:/")) {
      try {
        int posDirectory = urlString.indexOf(".jar!", 11);
        String jarName = urlString.substring(4, posDirectory+4);
//        System.out.println("jarName: " + jarName);
        URI uri = URI.create(jarName);
        File jar = new File(uri);
//        System.out.println("jar: " + jar.getAbsolutePath());
        JarFile jarFile = new JarFile(jar);
        Enumeration entries = jarFile.entries();
        ArrayList list = new ArrayList(250);
        while (entries.hasMoreElements()) {
          JarEntry jarEntry = (JarEntry) entries.nextElement();
          if(jarEntry.getName().startsWith(bundleFolder) && jarEntry.getName().endsWith(extension) && jarEntry.getName().length() < bundleFolder.length()+extension.length()+7) {
//            System.out.println("jarEntry: " + jarEntry.getName());
            list.add(jarEntry.getName().substring(bundleFolder.length() - prefix.length())); // "MessagesBundle_de_DE.properties"
          }
        }
        bundles = (String[]) list.toArray(new String[list.size()]);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      File bundleDirectory = new File(URI.create(urlString)).getParentFile();
//      System.out.println("bundleDirectory: " + bundleDirectory.getAbsolutePath());
        
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
      foundLocales[i] =
        locale.length() == 0 ? LOCALE_ENGLISH : new Locale(locale.substring(1, 3), locale.substring(4, 6));
    }
    return foundLocales;
  }

  public static boolean changeLocale(Locale newLocale) {
    if (LOCALE_ENGLISH.equals(newLocale))
      newLocale = LOCALE_DEFAULT;
    if (!newLocale.equals(RESOURCE_BUNDLE.getLocale())) {
      ResourceBundle newResourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, newLocale);
      if (newResourceBundle.getLocale().equals(newLocale)) {
        RESOURCE_BUNDLE = newResourceBundle;
        Locale.setDefault(newLocale);
        return true;
      }
      else
        System.out.println("Messages: no message properties for Locale " + newLocale.getDisplayLanguage());
    }
    return false;
  }

  public static void updateLanguageForControl(Widget composite) {
    if (composite == null)
      return;

    updateLanguageFromData(composite);

    if (composite instanceof CTabFolder) {
      CTabFolder folder = (CTabFolder) composite;
      CTabItem[] items = folder.getItems();
      for (int i = 0; i < items.length; i++) {
        updateLanguageForControl(items[i]);
        updateLanguageForControl(items[i].getControl());
      }
    }
    else if (composite instanceof Composite) {
      Composite group = (Composite) composite;
      Control[] controls = group.getChildren();
      for (int i = 0; i < controls.length; i++) {
        updateLanguageForControl(controls[i]);
      }
      if (composite instanceof Table) {
        Table table = (Table) composite;
        TableColumn[] columns = table.getColumns();
        for (int i = 0; i < columns.length; i++) {
          updateLanguageFromData(columns[i]);
        }
        updateLanguageForControl(table.getMenu());
      }
      group.layout();
    }
    else if (composite instanceof MenuItem) {
      MenuItem menuItem = (MenuItem) composite;
      updateLanguageForControl(menuItem.getMenu());
    }
    else if (composite instanceof Menu) {
      Menu menu = (Menu) composite;
      if (menu.getStyle() == SWT.POP_UP)
        System.out.println("POP_UP");

      MenuItem[] items = menu.getItems();
      for (int i = 0; i < items.length; i++) {
        updateLanguageForControl(items[i]);
      }
    }
  }

  public static void setLanguageText(Widget widget, String key) {
    widget.setData(key);
    updateLanguageFromData(widget);
  }

  public static void updateLanguageFromData(Widget widget) {
    if (widget.getData() != null) {
      if (widget instanceof MenuItem)
         ((MenuItem) widget).setText(getString((String) widget.getData()));
      else if (widget instanceof TableColumn)
         ((TableColumn) widget).setText(Messages.getString((String) widget.getData()));
      else if (widget instanceof Label)
         ((Label) widget).setText(Messages.getString((String) widget.getData()));
      else if (widget instanceof Group)
         ((Group) widget).setText(Messages.getString((String) widget.getData()));
      else if (widget instanceof Button)
         ((Button) widget).setText(getString((String) widget.getData()));
      else
        System.out.println("No cast for " + widget.getClass().getName());
    }
  }

}
