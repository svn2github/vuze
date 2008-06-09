/*
 * Created on Feb 15, 2005
 * Created by Alon Rohter
 * Copyright (C) 2004-2005 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.plugins.utils;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderFactoryImpl;

import com.aelitis.azureus.core.AzureusCoreFactory;

/**
 * Plugin utility class for easy access to static helper methods,
 * without the need for a plugin interface instance.
 */
public class StaticUtilities {

  private static Formatters formatters;
  
  static {
    try {
      Class c = Class.forName("org.gudy.azureus2.pluginsimpl.local.utils.FormattersImpl");
      formatters = (Formatters) c.newInstance();
    } catch (Exception e) {
     e.printStackTrace();
    }
  }
  
  /**
   * Get display and byte format utilities.
   * @return formatters
   */
  public static Formatters getFormatters() {  return formatters;  }
   
  public static ResourceDownloaderFactory 
  getResourceDownloaderFactory()
  {
	  return( ResourceDownloaderFactoryImpl.getSingleton());
  }
  
  public static PluginInterface
  getDefaultPluginInterface()
  {
	  return( AzureusCoreFactory.getSingleton().getPluginManager().getDefaultPluginInterface());
  }
  
  	/**
  	 * See UIInstance.promptUser
  	 * @param title
  	 * @param desc
  	 * @param options
  	 * @param default_option
  	 * @return
  	 */
  
  public static int
  promptUser(
	 String		title,
	 String		desc,
	 String[]	options,
	 int		default_option )
  {
	  UIInstance[] instances = getDefaultPluginInterface().getUIManager().getUIInstances();
	  
	  if ( instances.length > 0 ){
		  
		  return( instances[0].promptUser(title, desc, options, default_option ));
		  
	  }else{
		  
		  Debug.out( "No UIInstances to handle prompt: " + title + "/" + desc );
		  
		  return( -1 );
	  }
  }
}
