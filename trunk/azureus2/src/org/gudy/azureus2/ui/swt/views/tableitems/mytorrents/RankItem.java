/*
 * File    : RankItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
 *
 * Azureus - a Java Bittorrent client
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
 */
 
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.program.Program;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.components.BufferedTableItem;
import org.gudy.azureus2.ui.swt.components.BufferedTableRow;

/**
 * @author Olivier
 *
 */
public class RankItem extends BufferedTableItem {
  
  DownloadManager manager;
  
  public RankItem(BufferedTableRow row,int position,DownloadManager manager) {
    super(row,position);
    this.manager = manager;
  }
  
  public void refresh() {
    setText("" + (manager.getIndex()+1));
    String name = manager.getName();
    if (name != null ) {
      int sep = name.lastIndexOf('.'); //$NON-NLS-1$
      if(sep < 0) sep = 0;
      name = name.substring(sep);
      Program program = Program.findProgram(name);
      Image icon = ImageRepository.getIconFromProgram(program);
      setImage(icon);
    }
  }
}
