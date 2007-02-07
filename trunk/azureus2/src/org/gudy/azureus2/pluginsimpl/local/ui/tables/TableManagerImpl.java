/*
 * Created on 19-Apr-2004
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.pluginsimpl.local.ui.tables;


import java.util.*;

import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerEvent;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.UIRuntimeException;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.pluginsimpl.local.ui.UIManagerEventAdapter;
import org.gudy.azureus2.pluginsimpl.local.ui.UIManagerImpl;


/** Manage Tables
 *
 * @author TuxPaper
 * @since 2.0.8.5
 */
public class 
TableManagerImpl 
    implements TableManager
{	
	private UIManagerImpl		ui_manager;
	
	public
	TableManagerImpl(
		UIManagerImpl	_ui_manager )
	{
		ui_manager	= _ui_manager;
	}
	
	
	public TableColumn 
	createColumn(
		String tableID, String cellID ) 
	{
	  return( new TableColumnDelegate( tableID, cellID ));
	}

	public void 
	addColumn(
		TableColumn tableColumn) 
	{
		if ( tableColumn instanceof TableColumnDelegate){
			
			((TableColumnDelegate)tableColumn).addToUI();
			
		}else{
		
			throw( new UIRuntimeException( "table column must have been created via createColumn" ));
		}
	}
	
	public TableContextMenuItem addContextMenuItem(
			TableContextMenuItem parent,
			String resourceKey) {
		if (!(parent instanceof TableContextMenuItemImpl)) {
			throw new UIRuntimeException("parent must have been created by addContextMenuItem");
		}
		if (parent.getStyle() != TableContextMenuItemImpl.STYLE_MENU) {
			throw new UIRuntimeException("parent menu item must have the menu style associated");
		}
		TableContextMenuItemImpl item = new TableContextMenuItemImpl((TableContextMenuItemImpl)parent, resourceKey);
		UIManagerImpl.fireEvent( UIManagerEvent.ET_ADD_TABLE_CONTEXT_SUBMENU_ITEM, item );
		return item;
	}
  
	public TableContextMenuItem 
	addContextMenuItem(
		String tableID, 
		String resourceKey) 
	{
		TableContextMenuItemImpl item = new TableContextMenuItemImpl(tableID, resourceKey);
    
			// this event is replayed for us on UI attaches so no extra work
		
		UIManagerImpl.fireEvent( UIManagerEvent.ET_ADD_TABLE_CONTEXT_MENU_ITEM, item );
    
		return item;
	}
  
  	protected class
  	TableColumnDelegate
  		implements TableColumn, UIManagerListener
  	{
  		private TableColumn		delegate;
  		private boolean			added_to_ui;
  		
  		private String		cell_id;
  		private String		table_id;
  		
  		private int		type		= TYPE_TEXT_ONLY;
		private int 	alignment	= ALIGN_LEAD;
		private int 	position	= POSITION_INVISIBLE;
		private int 	width		= 50;
		private int 	interval	= INTERVAL_INVALID_ONLY;
		
		private List	cell_added_listeners	= new ArrayList();
		private List	cell_dispose_listeners	= new ArrayList();
		private List    cell_mouse_listeners    = new ArrayList();
		private List	cell_refresh_listeners	= new ArrayList();
		private List	cell_tooltip_listeners	= new ArrayList();
		private List	context_memu_items		= new ArrayList();
		private List    general_listeners       = new ArrayList(); // used by addListeners
		
  		protected
  		TableColumnDelegate(
  			String	t,
  			String	c )
  		{
  			table_id	= t;
 			cell_id		= c;
 			
 			ui_manager.addUIListener( this );
  		}
  		
  		public void
  		UIAttached(
  			UIInstance		ui_instance )
  		{
  			if ( delegate != null ){
  				
  				return;
  			}
  			
 			
  			UIManagerEventAdapter	event = 
  			  new UIManagerEventAdapter( UIManagerEvent.ET_CREATE_TABLE_COLUMN, new String[]{ table_id, cell_id });
  		
  			if ( UIManagerImpl.fireEvent( event )){
  			  
  				delegate = (TableColumn)event.getResult();
  				
  				delegate.setAlignment( alignment );
  				delegate.setPosition( position );
  				delegate.setRefreshInterval( interval );
  				delegate.setType( type );
  				delegate.setWidth( width );
  				
  				for (int i=0;i<cell_added_listeners.size();i++){
  					delegate.addCellAddedListener((TableCellAddedListener)cell_added_listeners.get(i));
  				}
  				
  				for (int i=0;i<cell_dispose_listeners.size();i++){
  					delegate.addCellDisposeListener((TableCellDisposeListener)cell_dispose_listeners.get(i));
  				}
  				
  				for (int i=0;i<cell_refresh_listeners.size();i++){
  					delegate.addCellRefreshListener((TableCellRefreshListener)cell_refresh_listeners.get(i));
  				}
  			
  				for (int i=0;i<cell_tooltip_listeners.size();i++){
  					delegate.addCellToolTipListener((TableCellToolTipListener)cell_tooltip_listeners.get(i));
  				}

  				for (int i=0;i<cell_mouse_listeners.size();i++){
  					delegate.addCellMouseListener((TableCellMouseListener)cell_mouse_listeners.get(i));
  				}
  				
  				for (int i=0;i<general_listeners.size();i++){
  					delegate.addListeners(general_listeners.get(i));
  				}
  				
  				for (int i=0;i<context_memu_items.size();i++){
  					
  					TableContextMenuItemDelegate	d = (TableContextMenuItemDelegate)context_memu_items.get(i);
  					
  					d.setDelegate( delegate );
  				}
  
 				if ( added_to_ui ){	 				
  	 				 
  					addToUI();
  				}
  			}
  		}
  		
		public void
  		UIDetached(
  			UIInstance		instance )
  		{
  		}
		
  		protected void
  		addToUI()
  		{
  			added_to_ui	= true;
  			
  			if ( delegate != null ){
  				
  	 			UIManagerEventAdapter	event = 
  	  			  new UIManagerEventAdapter( UIManagerEvent.ET_ADD_TABLE_COLUMN, delegate );
  	 			
  	 			UIManagerImpl.fireEvent( event );
  			}
  		}
  		
  		public void 
  		initialize(
  			int iAlignment, int iPosition, 
  			int iWidth, 	int iInterval)
  		{ 
  			if ( delegate == null ){
  				alignment	= iAlignment;
  				position	= iPosition;
  				width		= iWidth;
  				interval	= iInterval;
  			}else{
  				delegate.initialize( iAlignment, iPosition, iWidth, iInterval );
  			}
  		}

		public void 
		initialize(
			int iAlignment, 
			int iPosition, 
			int iWidth)
		{
			if ( delegate == null ){
  				alignment	= iAlignment;
  				position	= iPosition;
  				width		= iWidth;
  			}else{
  				delegate.initialize(iAlignment,iPosition,iWidth);
  			}
		}
		
		public String 
		getName()
		{ 
			return( cell_id ); 
		}
		
		public String 
		getTableID()
		{ 
			return( table_id ); 
		}
		
		public void 
		setType(
			int _type)
		{
			if ( delegate == null ){
				type = _type;
			}else{
				delegate.setType( _type );
			}
		}
		
		public int 
		getType()
		{ 
			return( delegate==null?type:delegate.getType());
		}
		
		public void 
		setWidth(
			int _width )
		{
			if ( delegate == null ){
				width = _width;
			}else{
				delegate.setWidth( _width );
			}
		}
		
		public int 
		getWidth()
		{ 
			return( delegate==null?width:delegate.getWidth());
		}
		
		public void 
		setPosition(
			int _position )
		{
			if ( delegate == null ){
				position = _position;
			}else{
				delegate.setPosition( _position );
			}
		}
		
		public int 
		getPosition()
		{ 
			return( delegate==null?position:delegate.getPosition());
		}
		
		public void 
		setAlignment(
			int _alignment )
		{
			if ( delegate == null ){
				alignment = _alignment;
			}else{
				delegate.setAlignment( _alignment );
			}		
		}
		
		public int 
		getAlignment()
		{ 
			return( delegate==null?alignment:delegate.getAlignment());
		}
		
		public void 
		setRefreshInterval(
			int _interval)
		{
			if ( delegate == null ){
				interval = _interval;
			}else{
				delegate.setRefreshInterval( _interval );
			}					
		}
		
		public int 
		getRefreshInterval()
		{ 
			return( delegate==null?interval:delegate.getRefreshInterval());
		}	
		
		public void 
		invalidateCells()
		{
			if ( delegate != null ){
				delegate.invalidateCells();
			}
		}

		public void 
		addCellRefreshListener(
			TableCellRefreshListener listener )
		{
			if ( delegate == null ){
				cell_refresh_listeners.add( listener );
			}else{
				delegate.addCellRefreshListener( listener );
			}
		}
		
		public void 
		removeCellRefreshListener(
			TableCellRefreshListener listener)
		{
			if ( delegate == null ){
				cell_refresh_listeners.remove( listener );
			}else{
				delegate.removeCellRefreshListener( listener );
			}
		}
		
		public void 
		addCellAddedListener(
			TableCellAddedListener listener)
		{
			if ( delegate == null ){
				cell_added_listeners.add( listener );
			}else{
				delegate.addCellAddedListener( listener );
			}
		}
		
		public void 
		removeCellAddedListener(
			TableCellAddedListener listener)
		{
			if ( delegate == null ){
				cell_added_listeners.remove( listener );
			}else{
				delegate.removeCellAddedListener( listener );
			}
		}
		
		public void 
		addCellDisposeListener(
			TableCellDisposeListener listener)
		{
			if ( delegate == null ){
				cell_dispose_listeners.add( listener );
			}else{
				delegate.addCellDisposeListener( listener );
			}
		}
		
		public void 
		removeCellDisposeListener(
			TableCellDisposeListener listener)
		{
			if ( delegate == null ){
				cell_dispose_listeners.remove( listener );
			}else{
				delegate.removeCellDisposeListener( listener );
			}
		}
		
		public void 
		addCellToolTipListener(
			TableCellToolTipListener listener)
		{
			if ( delegate == null ){
				cell_tooltip_listeners.add( listener );
			}else{
				delegate.addCellToolTipListener( listener );
			}
		}
		
		public void 
		removeCellToolTipListener(
			TableCellToolTipListener listener)
		{
			if ( delegate == null ){
				cell_tooltip_listeners.remove( listener );
			}else{
				delegate.removeCellToolTipListener( listener );
			}
		}
		
		public TableContextMenuItem 
		addContextMenuItem(
			String resourceKey )
		{ 
			if ( delegate == null ){
				
				TableContextMenuItem	res = new TableContextMenuItemDelegate( resourceKey );
				
				context_memu_items.add( res );
				
				return( res );
				
			}else{
				
				return( delegate.addContextMenuItem( resourceKey ));
			}
		} 
  	
		public void addCellMouseListener(TableCellMouseListener listener) {
			if ( delegate == null ){
				cell_mouse_listeners.add( listener );
			}else{
				delegate.addCellMouseListener( listener );
			}
		}

		public void removeCellMouseListener(TableCellMouseListener listener) {
			if ( delegate == null ){
				cell_mouse_listeners.remove( listener );
			}else{
				delegate.removeCellMouseListener( listener );
			}
		}

		public void addListeners(Object listenerObject) {
			if ( delegate == null ){
				general_listeners.add( listenerObject );
			}else{
				delegate.addListeners( listenerObject );
			}
		}

		public boolean isObfusticated() {
			return delegate == null ? false : delegate.isObfusticated();
		}

		public void setObfustication(boolean hideData) {
			if (delegate != null) {
				delegate.setObfustication(hideData);
			}
		}

	  	protected class
	  	TableContextMenuItemDelegate
	  		implements TableContextMenuItem
	  	{
	  		private TableContextMenuItem	delegate;
	  		private String					resource_key;
	  		
		  	private int		style		= STYLE_PUSH;
		  	private boolean	enabled		= true;
		  	private Object	data;
		  	private Graphic	graphic;
		  	
		  	private List 	listeners 		= new ArrayList();
		  	private List 	m_listeners 	= new ArrayList();
		  	private List	fill_listeners	= new ArrayList();
		  	
		  	private String   display_text  = null;
	  	  
	  		protected
	  		TableContextMenuItemDelegate(
	  			String	_resource_key )
	  		{
	  			resource_key	= _resource_key;
	  		}
	  		
	  		protected void
	  		setDelegate(
	  			TableColumn	_delegate )
	  		{
	  			delegate	= _delegate.addContextMenuItem( resource_key );
	  			
	  			delegate.setData( data );
	  			delegate.setEnabled( enabled );
	  			delegate.setGraphic( graphic );
	  			delegate.setStyle( style );
	  			
	  			for (int i=0;i<listeners.size();i++){
	  				delegate.addListener((MenuItemListener)listeners.get(i));
	  			}

	  			for (int i=0;i<listeners.size();i++){
	  				delegate.addMultiListener((MenuItemListener)m_listeners.get(i));
	  			}
	  			
	  			for (int i=0;i<fill_listeners.size();i++){
	  				delegate.addFillListener((MenuItemFillListener)fill_listeners.get(i));
	  			}
	  		}
	  		
	  		public String getMenuID() {
	  			return MenuManager.MENU_TABLE;
	  		}
	  		
	  		public String 
	  		getTableID()
	  		{
	  			return( table_id );
	  		}
	  		
	  		public String
	  		getResourceKey()
	  		{
	  			return( resource_key );
	  		}
	
	  		public int
	  		getStyle()
	  		{
	  			return(delegate==null?style:delegate.getStyle());
	  		}
	  		
	  		public void
	  		setStyle(
	  			int		_style )
	  		{
	  			if ( delegate == null ){
	  				style	= _style;
	  			}else{
	  				delegate.setStyle( _style );
	  			}
	  		}
	  		
	  		public Object
	  		getData()
	  		{
	  			return(delegate==null?data:delegate.getData());
	  		}
	  		
	  		public void
	  		setData(
	  			Object	_data )
	 		{
	  			if ( delegate == null ){
	  				data	= _data;
	  			}else{
	  				delegate.setData( _data );
	  			}
	  		}
	  		
	  		public boolean
	  		isEnabled()
	  		{
	  			return(delegate==null?enabled:delegate.isEnabled());
	  		}
	  		
	  		public void
	  		setEnabled(
	  			boolean	_enabled )
	 		{
	  			if ( delegate == null ){
	  				enabled	= _enabled;
	  			}else{
	  				delegate.setEnabled( _enabled );
	  			}
	  		}
	  		
	  		public void
	  		setGraphic(
	  			Graphic		_graphic )
	 		{
	  			if ( delegate == null ){
	  				graphic	= _graphic;
	  			}else{
	  				delegate.setGraphic( _graphic );
	  			}
	  		}
	
	  		public Graphic
	  		getGraphic()
	  		{
	  			return(delegate==null?graphic:delegate.getGraphic());
	  		}
	  			
	  		public void
	  		addFillListener(
	  			MenuItemFillListener	listener )
	  		{
	  			if ( delegate==null ){
	  				fill_listeners.add( listener );
	  			}else{
	  				delegate.addFillListener( listener );
	  			}
	  		}
	  		
	  		public void
	  		removeFillListener(
	  			MenuItemFillListener	listener )
	  		{
	  			if ( delegate==null ){
	  				fill_listeners.remove( listener );
	  			}else{
	  				delegate.removeFillListener( listener );
	  			}
	  		}

	  		public void addListener(MenuItemListener listener) {
	  			if (delegate==null) {listeners.add(listener);}
	  			else {delegate.addListener(listener);}
	  		}
	  		
	  		public void removeListener(MenuItemListener listener) {
	  			if (delegate==null) {listeners.remove(listener);}
	  			else {delegate.removeListener(listener);}
	  		}
	  		
	  		public void addMultiListener(MenuItemListener listener) {
	  			if (delegate==null) {m_listeners.add(listener);}
	  			else {delegate.addMultiListener(listener);}
	  		}
	  		
	  		public void removeMultiListener(MenuItemListener listener) {
	  			if (delegate==null) {m_listeners.remove(listener);}
	  			else {delegate.removeMultiListener(listener);}
	  		}
	  		
	  		public MenuItem[] getItems() {
	  			if (delegate == null) {
	  				if (this.getStyle() == TableContextMenuItem.STYLE_MENU) {
	  					return new TableContextMenuItem[0];
	  				}
	  				return null;
	  			}
	  			return delegate.getItems();
	  		}
	  		
	  		public String getText() {
	  			return (delegate == null) ? this.display_text : delegate.getText();
	  		}
	  		
	  		public void setText(String text) {
	  			if (delegate == null) {this.display_text = text;}
	  			else {delegate.setText(text);}
	  		}
	  		
	  		public MenuItem getParent() {
	  			if (delegate == null) {return null;}
	  			return delegate.getParent();
	  		}
	  		
	  		public MenuItem getItem(String key) {
	  			if (delegate == null) {return null;}
	  			return delegate.getItem(key);
	  		}
	  		
	  	}
  	}
}
