/*
 * File    : MDDownloadFilterModel.java
 * Created : 01-Apr-2004
 * By      : parg
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

package org.gudy.azureus2.ui.webplugin.remoteui.applet.model;

/**
 * @author parg
 *
 */

import java.util.*;
import javax.swing.table.*;
import javax.swing.event.*;

import org.gudy.azureus2.plugins.download.*;


public class 
MDDownloadFilterModel
	extends		AbstractTableModel
	implements 	MDDownloadModel
{
	protected MDDownloadModel		basis;
	protected MDDownloadFilter		filter;
	
	protected Download[]			downloads;
	protected Integer[]				download_indexes;
	
	protected
	MDDownloadFilterModel(
		MDDownloadModel		_basis,
		MDDownloadFilter	_filter )
	{
		basis		= _basis;
		filter		= _filter;
		
		basis.addTableModelListener(
			new TableModelListener()
			{
				public void 
				tableChanged(
					TableModelEvent e )
				{				
					build();
				
					fireTableDataChanged();
				}
			});
		
		build();
	}
	
	protected void
	build()
	{
		Download[]	full = basis.getDownloads();
		
		List	sd 	= new ArrayList();
		List	si	= new ArrayList();
		
		for (int i=0;i<full.length;i++){
			
			Download	dl = full[i];
			
			if ( filter.isSelected( dl )){
			
				sd.add( dl );
				si.add( new Integer(i));
			}
		}
		
		downloads	 		= new Download[sd.size()];
		download_indexes	= new Integer[sd.size()];
		
		sd.toArray( downloads );
		si.toArray( download_indexes );
	}
	
	protected int[]
	mapIndexes(
		int[]	rows )
	{
		int[]	res = new int[rows.length];
		
		for (int i=0;i<rows.length;i++){
			
			res[i] = download_indexes[rows[i]].intValue();
		}
		
		return( res );
	}
	protected int
	mapIndex(
		int		row )
	{
		return( download_indexes[row].intValue());
	}
	
	public int 
	getColumnCount() 
	{ 
		return( basis.getColumnCount());
	}

	public int 
	getRowCount() 
	{ 
		return( downloads.length );
	}
	
	public Object 
	getValueAt(
		int row, 
		int col ) 
	{
		return( basis.getValueAt( mapIndex(row), col ));
	}
	
	public String 
	getColumnName(
		int column ) 
	{
		return( basis.getColumnName( column ));
	}
	
	public Class 
	getColumnClass(
		int col ) 
	{
		return getValueAt(0,col).getClass();
	}
	
	public boolean 
	isCellEditable(
		int row, 
		int col )
	{
		return( false );
	}
	
	public Download[]
	getDownloads()
	{
		return( downloads );
	}
					
	public Download
	getDownload(
		int		row )
	{
		return( downloads[row]);
	}
	
	public void
	refresh()
	{
		basis.refresh();
	}
	
	public void
	start(
		int[]		rows )
	{
		basis.start(mapIndexes(rows));
	}
	
	public void
	forceStart(
		int[]		rows )
	{
		basis.forceStart(mapIndexes(rows));
		
	}
	
	public void
	stop(
		int[]		rows )
	{
		basis.stop(mapIndexes(rows));
		
	}
	
	public void
	remove(
		int[]		rows )
	{
		basis.remove(mapIndexes(rows));
		
	}
	
	public void
	moveUp(
		int[]		rows )
	{
		basis.moveUp(mapIndexes(rows));
		
	}
	
	public void
	moveDown(
		int[]		rows )
	{
		basis.moveDown(mapIndexes(rows));	
	}
}
