/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */

package org.gudy.azureus2.ui.common;

import java.util.Date;
import java.util.HashMap;

import com.aelitis.azureus.core.*;

import org.apache.log4j.Logger;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.ui.common.IUserInterface;
import org.gudy.azureus2.ui.common.UserInterfaceFactory;

/**
 * @author tobi
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class 
UIConst 
{
  public static Date 			startTime;
  public static HashMap 		UIS;
  
  private static AzureusCore	azureus_core;
  private static boolean        must_init_core;
  
  public static synchronized void
  setAzureusCore(
  	AzureusCore		_azureus_core )
  {
  	azureus_core	= _azureus_core;
  	must_init_core = !azureus_core.isStarted();
  }
  
  public static synchronized AzureusCore
  getAzureusCore()
  {
	  if (must_init_core) {
	        try {azureus_core.start();}
	        catch( AzureusCoreException e ) {
	      		Logger.getLogger("azureus2").error("Start fails", e);
	        }
	        must_init_core = false;
	  }
  	return( azureus_core );
  }
  
  public static synchronized GlobalManager
  getGlobalManager()
  {
  	return( azureus_core.getGlobalManager());
  }
  
  public static void shutdown() {
    Main.shutdown();
  }
  
  public static synchronized boolean 
  startUI(String ui, String[] args) {
    if (UIS.containsKey(ui))
      return false;
    IUserInterface uif = UserInterfaceFactory.getUI(ui);
    uif.init(false, true);
    if (args!=null)
      uif.processArgs(args);
    uif.startUI();
    UIS.put(ui, uif);
    return true;
  }

}
