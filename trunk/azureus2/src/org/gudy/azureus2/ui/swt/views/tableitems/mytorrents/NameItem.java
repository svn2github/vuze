/*
 * File    : NameItem.java
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
import org.gudy.azureus2.ui.swt.ImageRepository;

/**
 * @author Olivier
 *
 */
public class NameItem extends TorrentItem {

  
  
  public NameItem(
    TorrentRow torrentRow,
    int position) {
    super(torrentRow, position);
  }

  public void refresh() {
    //setText returns true only if the text is updated
    if(setText(torrentRow.getManager().getName())) {
      //in which case we also update the icon
      String name = torrentRow.getManager().getName();
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

}
