/*
 * Created on 29.11.2003
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 * 
 */
package org.gudy.azureus2.core3.internat;

import java.util.Collection;
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
  
  public IntegratedResourceBundle(ResourceBundle main, Map localizationPaths) {
	  this(main, localizationPaths, null);
  }

  public 
  IntegratedResourceBundle(
		ResourceBundle main, 
		Map localizationPaths,
		Collection resource_bundles) 
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

	  if (resource_bundles != null) {
		  for (Iterator itr = resource_bundles.iterator(); itr.hasNext();) {
			  addResourceMessages((ResourceBundle)itr.next(), messages);
		  }
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
