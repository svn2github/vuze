/*
 * Created on 20 mai 2004
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.updater;

import java.io.InputStream;

import org.eclipse.swt.SWT;
import org.gudy.azureus2.plugins.update.UpdatableComponent;
import org.gudy.azureus2.plugins.update.Update;
import org.gudy.azureus2.plugins.update.UpdateChecker;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;

/**
 * @author Olivier Chalouhi
 *
 */
public class SWTUpdateChecker implements UpdatableComponent
{
  public static String[] swtURLProviders = {
      "http://azureus.sourceforge.net/swt_version.php" ,
      "http://www.gudy.org/azureus/swt_version.php" ,
      "http://www.keecall.com/azureus/swt_version.php"
  };
  
  
  public SWTUpdateChecker() {    
  }
  
  public void checkForUpdate(UpdateChecker checker) {
    SWTVersionGetter versionGetter = new SWTVersionGetter();
    if( versionGetter.needsUpdate()) {
      
      ResourceDownloader swtDownloader = new SWTDownloader(versionGetter.getPlatform());
      
      checker.addUpdate("SWT Libray for " + versionGetter.getPlatform(),
          new String[] {"SWT is the graphical library used by Azureus"},
          "" + versionGetter.getLatestVersion(),
          swtDownloader,
          Update.RESTART_REQUIRED_YES
          );
    }
    
  }
  
	public String
	getName()
	{
		return( "SWT library" );
	}
	
	public int
	getMaximumCheckTime()
	{
		return( 30 );	// !!!! TODO: fix this
	}	
}
