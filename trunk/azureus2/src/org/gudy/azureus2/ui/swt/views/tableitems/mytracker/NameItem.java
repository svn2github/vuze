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
 
package org.gudy.azureus2.ui.swt.views.tableitems.mytracker;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.program.Program;

import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.TableCellCore;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

/**
 *
 * @author TuxPaper
 * @since 2.0.8.5
 */
public class NameItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
  /** Default Constructor */
  public NameItem() {
    super("name", POSITION_LAST, 250, TableManager.TABLE_MYTRACKER);
  }

  public void refresh(TableCell cell) {
    TRHostTorrent item = (TRHostTorrent)cell.getDataSource();
    String name = (item == null) ? "" 
                                 : TorrentUtils.getLocalisedName(item.getTorrent());
    //setText returns true only if the text is updated
    if (cell.setText(name)) {
      //in which case we also update the icon
			int sep = name.lastIndexOf('.');
			if(sep < 0) sep = 0;
			
      name = name.substring(sep);
      Program program = Program.findProgram(name);
      Image icon = ImageRepository.getIconFromProgram(program);
      // cheat for core, since we really know it's a TabeCellImpl and want to use
      // those special functions not available to Plugins
      ((TableCellCore)cell).setImage(icon);
    }
  }

}
