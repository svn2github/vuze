/*
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.myshares;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

/**
 *
 * @author TuxPaper
 * @since 2.0.8.5
 */
public class TypeItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
  /** Default Constructor */
  public TypeItem() {
    super("type", POSITION_LAST, 100, TableManager.TABLE_MYSHARES);
  }

  public void refresh(TableCell cell) {
    ShareResource item = (ShareResource)cell.getDataSource();
    if (item == null) {
      cell.setText("");
    } else {
      long type = item.getType();
      String sText = "";
      if (type == ShareResource.ST_DIR)
        sText = "MySharesView.type.dir";
      else if (type == ShareResource.ST_FILE)
        sText = "MySharesView.type.file";
      else if (type == ShareResource.ST_DIR_CONTENTS) {
        ShareResourceDirContents s = (ShareResourceDirContents)item;
        if (s.isRecursive())
          sText = "MySharesView.type.dircontentsrecursive";
        else
          sText = "MySharesView.type.dircontents";
      }

      cell.setText((sText == "") ? "" : MessageText.getString(sText));
    }
  }
}
