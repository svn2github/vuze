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
package org.gudy.azureus2.ui.swt.updater2;

import java.io.InputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.gudy.azureus2.plugins.update.UpdatableComponent;
import org.gudy.azureus2.plugins.update.Update;
import org.gudy.azureus2.plugins.update.UpdateChecker;
import org.gudy.azureus2.plugins.update.UpdateInstaller;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderListener;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderFactoryImpl;
import org.gudy.azureus2.ui.swt.updater.SWTDownloader;

/**
 * @author Olivier Chalouhi
 *
 */
public class SWTUpdateChecker implements UpdatableComponent
{
  //TODO : Make 1. SF , 2. aelitis.com 3. keecall.com and 4. gudy.org
  public static String[] swtURLProviders = {      
      "http://www.gudy.org/azureus/swt_version.php" ,
      "http://azureus.sourceforge.net/swt_version.php" ,
      "http://www.keecall.com/azureus/swt_version.php"
  };
  
  
  public SWTUpdateChecker() {    
  }
  
  public void checkForUpdate(final UpdateChecker checker) {
    SWTVersionGetter versionGetter = new SWTVersionGetter();
    if( versionGetter.needsUpdate()) {
      
      String[] mirrors = versionGetter.getMirrors();
      
      //TODO : Create the correct downloader for the URLs ...
      ResourceDownloader swtDownloader = null;
      try {
        ResourceDownloaderFactoryImpl.getSingleton().create(new URL(mirrors[0]));
      } catch(Exception e) {
        e.printStackTrace();
      }
      
      /*swtDownloader.addListener(new ResourceDownloaderListener() {
        
        public boolean completed(ResourceDownloader downloader, InputStream data) {
          //On completion, process the InputStream to store temp files
          return processData(checker,data);
        }
        
        public void failed(ResourceDownloader downloader,
            ResourceDownloaderException e) {
          // We're not interested in failure

        }

        public void reportActivity(ResourceDownloader downloader,
            String activity) {
          // We're not interested in activity

        }

        public void reportPercentComplete(ResourceDownloader downloader,
            int percentage) {
          // We're not interested in percent

        }
      });*/
      
      checker.addUpdate("SWT Libray for " + versionGetter.getPlatform(),
          new String[] {"SWT is the graphical library used by Azureus"},
          "" + versionGetter.getLatestVersion(),
          swtDownloader,
          Update.RESTART_REQUIRED_YES
          );      
      
    }
    
  }
  
  private boolean processData(UpdateChecker checker,InputStream data) {
    try {
      UpdateInstaller installer = checker.createInstaller();    
      ZipInputStream zip = new ZipInputStream(data);
      ZipEntry entry = null;
      while((entry = zip.getNextEntry()) != null) {
        System.out.println(entry.getName());        
      }
    } catch(Exception e) {
      e.printStackTrace();
      return false;
    }
        
    return true;
  }
  
  public String
  getName()
  {
    return( "SWT library" );
  }
  
  public int
  getMaximumCheckTime()
  {
    return( 30 ); // !!!! TODO: fix this
  } 
}
