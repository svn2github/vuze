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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.program.Program;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.views.*;
import org.gudy.azureus2.ui.swt.views.utils.SortableItem;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.components.*;

import org.gudy.azureus2.core3.tracker.host.*;


public class 
TrackerTableItem 
implements SortableItem
{
	private Display 			display;
	private Table 				table;
	private BufferedTableRow 	item;
	private TRHostTorrent		torrent;
 
		//Used when sorting
	public boolean selected;  

	public TrackerTableItem(
    MyTrackerView view,
		Table 			_table, 
		TRHostTorrent 	_torrent ) 
	{
		table		= _table;
		torrent		= _torrent;
		
	  	initialize(view);
	}

	public TableItem 
	getTableItem()
	{
	  return item.getItem();
	}

	private void 
	initialize( final MyTrackerView view) 
	{
	  if (table == null || table.isDisposed())
		return;
		
	  display = table.getDisplay();
	  
	  display.asyncExec(new Runnable() {
		public void run() {
		  if (table == null || table.isDisposed())
			return;
			
		  item = new BufferedTableRow(table, SWT.NULL);
		  
      	  view.putHost(item.getItem(),torrent);
		}
	  });
	}

	public void delete() {
	  display.asyncExec(new Runnable() {
		public void run() {
		  if (table == null || table.isDisposed())
			return;
		  if (item == null || item.isDisposed())
			return;
		  table.remove(table.indexOf(item.getItem()));
		  item.dispose();
		}
	  });
	}

	public void 
	refresh() 
	{
		if (table == null || table.isDisposed()){
		
			return;
		}
			
	  	if (item == null || item.isDisposed()){
	  	
			return;
	  	}
	  		  	
	  	String name = new String(torrent.getTorrent().getName());	// TODO: !!!!
    
		int sep = name.lastIndexOf('.');
		 
		if(sep < 0) sep = 0;
		
		String	key = name.substring(sep);
		
		Program program = Program.findProgram(key);
		
		Image icon = ImageRepository.getIconFromProgram(program);
				
		item.setImage( 0, icon);

		item.setText( 0, name);
		
		String	tracker = torrent.getTorrent().getAnnounceURL().toString();

		item.setText( 1, tracker);
		
		String	status;
		
		int	s = torrent.getStatus();
		
		if ( s == TRHostTorrent.TS_STARTED ){
			
			status = MessageText.getString( "MyTrackerView.status.started");
			
		}else if ( s == TRHostTorrent.TS_STOPPED ){
			
			status = MessageText.getString( "MyTrackerView.status.stopped");
			
		}else if ( s == TRHostTorrent.TS_FAILED ){
			
			status = MessageText.getString( "MyTrackerView.status.failed");
			
		}else if ( s == TRHostTorrent.TS_PUBLISHED ){
			
			status = MessageText.getString( "MyTrackerView.status.published");
			
		}else{
			
			status = "?";
		}
		
		item.setText( 2, status );
		
		TRHostPeer[]	peers = torrent.getPeers();
		
		int		peer_count	= 0;
		int		seed_count	= 0;
		
		long	uploaded	= 0;
		long	downloaded	= 0;
		long	left		= 0;
		
		for (int i=0;i<peers.length;i++){
			
			TRHostPeer	peer = peers[i];
			
			if ( peer.isSeed()){
				
				seed_count++;
			}else{
				
				peer_count++;
			}
			
			uploaded 	+= peer.getUploaded();
			downloaded	+= peer.getDownloaded();
			left		+= peer.getAmountLeft();
		}
		
		item.setText( 3, "" + seed_count );
		item.setText( 4, "" + peer_count );
		
		item.setText( 5, "" + torrent.getAnnounceCount());
		
		item.setText( 6, "" + torrent.getCompletedCount());
		
		item.setText( 7, "" + DisplayFormatters.formatByteCountToKBEtc(uploaded));
		
		item.setText( 8, "" + DisplayFormatters.formatByteCountToKBEtc(downloaded));
		
		item.setText( 9, "" + DisplayFormatters.formatByteCountToKBEtc(left));
		
		if ( seed_count != 0 ){
			
			if ( !item.getForeground().equals( MainWindow.blues[3])){
				
				item.setForeground( MainWindow.blues[3]);
			}
		}
	}

	public int 
	getIndex() 
	{
    if(table == null || table.isDisposed() || item == null || item.isDisposed())
      return -1;
	  return table.indexOf(item.getItem());
	}

	public TRHostTorrent 
	getTorrent() 
	{
	  return( torrent ); 
	}
  
  /*
   * SortableItem implementation
   *
   */
  
  public long getIntField(String field) {
    if(field == null)
      return 0;
    if(field.equals("status"))
      return torrent.getStatus();
    return 0;
  }

  public String getStringField(String field) {
    if(field == null)
      return "";
    if(field.equals("name"))
      return new String(torrent.getTorrent().getName());
    if(field.equals("tracker"))
      return torrent.getTorrent().getAnnounceURL().toString();
    return "";
  }
  
  

  public void invalidate() {
  }

  public void setDataSource(Object dataSource) {
    torrent = (TRHostTorrent) torrent;
  }

}
