/*
 * File    : VWDownloadView.java
 * Created : 29-Jan-2004
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

package org.gudy.azureus2.ui.webplugin.remoteui.applet.view;

/**
 * @author parg
 *
 */

import java.util.*;
import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

import org.gudy.azureus2.ui.webplugin.remoteui.applet.model.*;
import org.gudy.azureus2.core3.util.*;

public class 
VWDownloadView 
{
	protected JComponent	component;
	
	protected TableSorter	sorter;
	protected JTable		table;
	
	protected ArrayList	listeners	= new ArrayList();
	
	public
	VWDownloadView(
		MDDownloadModel		model )
	{
		sorter = new TableSorter(model);
		
		table = new JTable(sorter);
		
		table.setAutoscrolls( true );
		
		table.setShowHorizontalLines( true );
		
		table.setShowVerticalLines( true );
		
		table.getSelectionModel().addListSelectionListener(
				new ListSelectionListener()
				{
					public void
					valueChanged(
						ListSelectionEvent	ev )
					{
						informCurrentSelection();
					}
				});
		
		TableColumnModel cm = table.getColumnModel();

		int[]	widths = { 30, -1, 60, 90, 80, 80, 50, 50, 50, 60, 70, 50, 60 };
		
		for (int i=0;i<widths.length;i++){
			
			if ( widths[i] != -1 ){
				
				cm.getColumn(i).setMaxWidth(widths[i]);
			}
		}
	
		int[]	byte_columns 		= { 2, 3, 8 };
		int[]	bytesec_columns 	= { 9, 10 };
		int[]	rhs_columns			= { 5, 6, 7 };
		
		for (int i=0;i<byte_columns.length;i++){
			
			TableColumn column = cm.getColumn( byte_columns[i]);
						
			column.setCellRenderer(
				new DefaultTableCellRenderer()
				{
					public Component 
					getTableCellRendererComponent(
						JTable		table,
						Object 		o_value,
						boolean 	isSelected,
						boolean 	hasFocus,
						int 		row,
						int 		column )
					{
						long	value = ((Long)o_value).longValue();
	
						String	str = DisplayFormatters.formatByteCountToKiBEtc(value);
						
						JLabel	res = (JLabel)super.getTableCellRendererComponent( table, str, isSelected, hasFocus, row, column );
						
						res.setHorizontalAlignment( JLabel.RIGHT );
						
						if ( column%2==1 ){
							
							// res.setBackground(Color.lightGray);
						}
						
						return( res );
					}
				});
		}
		
		for (int i=0;i<bytesec_columns.length;i++){
			
			TableColumn column = cm.getColumn( bytesec_columns[i]);
			
			column.setCellRenderer(
					new DefaultTableCellRenderer()
					{
						public Component 
						getTableCellRendererComponent(
								JTable		table,
								Object 		o_value,
								boolean 	isSelected,
								boolean 	hasFocus,
								int 		row,
								int 		column )
						{
							long	value = ((Long)o_value).longValue();
							
							String	str = DisplayFormatters.formatByteCountToKiBEtcPerSec(value);
							
							JLabel	res = (JLabel)super.getTableCellRendererComponent( table, str, isSelected, hasFocus, row,column );
							
							res.setHorizontalAlignment( JLabel.RIGHT );
							
							if ( column%2==1 ){
								
								// res.setBackground(Color.lightGray);
							}
							
							return( res );
						}
					});
		}
		
		for (int i=0;i<rhs_columns.length;i++){
			
			TableColumn column = cm.getColumn( rhs_columns[i]);
			
			column.setCellRenderer(
					new DefaultTableCellRenderer()
					{
						public Component 
						getTableCellRendererComponent(
								JTable		table,
								Object 		o_value,
								boolean 	isSelected,
								boolean 	hasFocus,
								int 		row,
								int 		column )
						{
							JLabel	res = (JLabel)super.getTableCellRendererComponent( table, o_value, isSelected, hasFocus, row,column );
							
							res.setHorizontalAlignment( JLabel.RIGHT );
							
							if ( column%2==1 ){
								
								// res.setBackground(Color.lightGray);
							}
							

							return( res );
						}
					});
		}
		
		cm.getColumn(4).setCellRenderer(
				new TableCellRenderer()
				{
					public Component 
					getTableCellRendererComponent(
							JTable		table,
							Object 		o_value,
							boolean 	isSelected,
							boolean 	hasFocus,
							int 		row,
							int 		column )
					{
						int	value = ((Integer)o_value).intValue();

						JProgressBar	pb = new JProgressBar();
						
						pb.setMaximum(100);
						
						pb.setValue(value/10);
						
						pb.setStringPainted(true);
						
						pb.setString(""+((double)value)/10+"%");
						
						return( pb );
					}
				});
		
		cm.getColumn(12).setCellRenderer(
				new DefaultTableCellRenderer()
				{
					public Component 
					getTableCellRendererComponent(
							JTable		table,
							Object 		o_value,
							boolean 	isSelected,
							boolean 	hasFocus,
							int 		row,
							int 		column )
					{
						int	value = ((Integer)o_value).intValue();
						
						JLabel	res;
						
						if ( value < 0x7fffffff ){

							double d = ((double)value)/1000;
							
							res = (JLabel)super.getTableCellRendererComponent( table, new Double(d), isSelected, hasFocus, row,column );
							
						}else{
							res = (JLabel)super.getTableCellRendererComponent( table, Constants.INFINITY_STRING, isSelected, hasFocus, row,column );
							
						}
							
						res.setHorizontalAlignment( JLabel.RIGHT );
						
						if ( column%2==1 ){
							
							// res.setBackground(Color.lightGray);
						}
						
	
						return( res );
					}
				});
		
		sorter.addMouseListenerToHeaderInTable(table);

		JScrollPane scrollpane = new JScrollPane(table);
		
		component	= scrollpane;
	}
	
	public JComponent
	getComponent()
	{
		return( component );
	}
	
	public void
	refresh()
	{
		component.revalidate();
		
		component.repaint();
		
		informCurrentSelection();
	}
	
	protected void
	informCurrentSelection()
	{
		for (int i=0;i<listeners.size();i++){
			
			((VWDownloadViewListener)listeners.get(i)).selectionChanged( getSelectedRows());
		}
	}
	
	public int[]
	getSelectedRows()
	{
		return(sorter.mapRows(table.getSelectedRows()));
	}
	
	public void
	setSelectedRows(
		int[]	rows )
	{
		rows = sorter.unmapRows( rows );
		
		int	first = 0x7fffffff;
		
		for (int i=0;i<rows.length;i++){
			
			if ( rows[i] != -1 ){
		
				int	index = rows[i];
				
				if ( index < first ){
					
					first	= index;
				}
				
				table.getSelectionModel().addSelectionInterval(index, index);
			}
		}
		
		if ( first != 0x7fffffff ){
			
			table.scrollRectToVisible(table.getCellRect(first, 0, true ));
		}
	}
	
	public void
	addListener(
		VWDownloadViewListener	l )
	{
		listeners.add( l );
	}
}
