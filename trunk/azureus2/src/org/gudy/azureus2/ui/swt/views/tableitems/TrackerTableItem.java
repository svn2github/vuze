/*
 * File    : TrackerTableItem.java
 * Created : 30-Oct-2003
 * By      : stuff
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

package org.gudy.azureus2.ui.swt.views.tableitems;

/**
 * @author parg
 *
 */

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.DisplayFormatters;

import org.gudy.azureus2.core3.tracker.host.*;


public class 
TrackerTableItem 
{
	private Display 		display;
	private Table 			table;
	private TableItem 		item;
	private TRHostTorrent	torrent;
 
		//Used when sorting
	public boolean selected;  

	public TrackerTableItem(
		Table 			_table, 
		TRHostTorrent 	_torrent ) 
	{
		table		= _table;
		torrent		= _torrent;
		
	  	initialize();
	}

	public TableItem 
	getTableItem()
	{
	  return item;
	}

	private void 
	initialize() 
	{
	  if (table == null || table.isDisposed())
		return;
		
	  display = table.getDisplay();
	  
	  display.syncExec(new Runnable() {
		public void run() {
		  if (table == null || table.isDisposed())
			return;
		  item = new TableItem(table, SWT.NULL);
		}
	  });
	}

	public void delete() {
	  display.syncExec(new Runnable() {
		public void run() {
		  if (table == null || table.isDisposed())
			return;
		  if (item == null || item.isDisposed())
			return;
		  table.remove(table.indexOf(item));
		  item.dispose();
		}
	  });
	}

	public void 
	refresh() 
	{
		if (table == null || table.isDisposed())
			return;
			
	  	if (item == null || item.isDisposed())
			return;

	  	String name = new String(torrent.getTorrent().getName());	// TODO: !!!!
    
		item.setText(0,name);
		
		String	status = "" + torrent.getStatus();
		
		item.setText( 1, status );
	}

	public int 
	getIndex() 
	{
	  return table.indexOf(item);
	}

	public TRHostTorrent 
	getTorrent() 
	{
	  return( torrent ); 
	}
}
