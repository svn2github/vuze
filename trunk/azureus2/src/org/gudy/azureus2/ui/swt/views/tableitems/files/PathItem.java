/*
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.files;

import java.io.File;
import java.io.IOException;


import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.swt.views.FilesView;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;



public class PathItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
  
  
  /** Default Constructor */
  public PathItem() {
    super("path", ALIGN_LEAD, POSITION_LAST, 200, TableManager.TABLE_TORRENT_FILES);
  }

  public void refresh(TableCell cell) {
    DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)cell.getDataSource();
    
    String path = "";
    
    if( fileInfo != null ) {
    	
      File file = fileInfo.getFile(true);
        
      try {
          path = file.getParentFile().getCanonicalPath();
      }
      catch( IOException e ) {
          path = file.getParentFile().getAbsolutePath();
      }
      
      if ( !path.endsWith( File.separator )){
    	  
    	  path += File.separator;
      }
      
      if( !FilesView.show_full_path ) { //display as full disk path
 
        DownloadManager dm = fileInfo.getDownloadManager();

        File	loc = dm.getSaveLocation();
        
        String root;
        if( dm.getTorrent().isSimpleTorrent() ) {
        	root = loc.getParent();
        }else{
        	root = loc.toString();
        }
         
        int pos = path.indexOf( root );
         
        if( pos >= 0 ) {
        	path = path.substring( pos + root.length() );
        }else{
        	path = File.separator;
        }
      }
    }
    
    cell.setText( path );
  }
}
