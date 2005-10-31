/*
 * Created on 29.11.2003
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.core3.internat;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * @author Rene Leonhardt
 */
public class IntegratedResourceBundle extends ListResourceBundle {

	private Locale	locale;

  private Object[][] contents;

  public 
  IntegratedResourceBundle(
		ResourceBundle main, 
		Map localizationPaths) 
  {
	  locale = main.getLocale();
	  
	  Hashtable messages = new Hashtable();
	  
	  addResourceMessages(main, messages);

	  for (Iterator iter = localizationPaths.keySet().iterator(); iter.hasNext();) {
	      String localizationPath = (String) iter.next();
	      ClassLoader classLoader = (ClassLoader) localizationPaths.get(localizationPath);
	      ResourceBundle newResourceBundle = null;
	      try {
	        if(classLoader != null)
	          newResourceBundle = ResourceBundle.getBundle(localizationPath, locale ,classLoader);
	        else
	        newResourceBundle = ResourceBundle.getBundle(localizationPath, locale,IntegratedResourceBundle.class.getClassLoader());
	      } catch (Exception e) {
	        //        System.out.println(localizationPath+": no resource bundle for " +
					// main.getLocale());
	        try {
	          if(classLoader != null)
	            newResourceBundle = ResourceBundle.getBundle(localizationPath, MessageText.LOCALE_DEFAULT,classLoader);
	          else 
	          newResourceBundle = ResourceBundle.getBundle(localizationPath, MessageText.LOCALE_DEFAULT,IntegratedResourceBundle.class.getClassLoader());
	        } catch (Exception e2) {
	          System.out.println(localizationPath + ": no default resource bundle");
	          continue;
	        }
	      }
	      addResourceMessages(newResourceBundle, messages);
	  }

	 contents = new Object[messages.size()][2];
	  
	 int i = 0;
	  
	 for (Enumeration enumeration = messages.keys(); enumeration.hasMoreElements();) {
	      String key = (String) enumeration.nextElement();
	      contents[i][0] = key;
	      contents[i++][1] = messages.get(key);
	 }
  }

  public Locale getLocale() 
  {
      return locale;
  }
  
  public Object[][] getContents() {
	    return contents;
  }

  private void 
  addResourceMessages(ResourceBundle bundle, Hashtable messages) 
  {
    if (bundle != null) {
      for (Enumeration enumeration = bundle.getKeys(); enumeration.hasMoreElements();) {
        String key = (String) enumeration.nextElement();
        messages.put(key, bundle.getObject(key));
      }
    }
  }
}
