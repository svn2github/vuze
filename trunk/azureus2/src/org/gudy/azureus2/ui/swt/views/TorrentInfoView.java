/*
 * Created on 16-Jan-2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.ui.swt.views;

import java.net.URL;
import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.LocaleTorrentUtil;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLGroup;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLSet;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.SWT.GraphicSWT;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellDisposeListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent;
import org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellToolTipListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.components.BufferedTableItem;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.views.table.TableCellCore;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.views.table.TableRowCore;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager;

public class 
TorrentInfoView
	extends AbstractIView
{
	private static final String	TEXT_PREFIX	= "TorrentInfoView.";
		
	private DownloadManager			download_manager;
		
	private DownloadManager			core_data_source;
	private Download				plugin_data_source;
	
	private Composite 		outer_panel;
	
	private Font 			headerFont;
	private ExternalCell[]	cells;
	
	private ExternalRow		external_row = new ExternalRow();
	
	protected
	TorrentInfoView(
		DownloadManager		_download_manager )
	{
		download_manager	= _download_manager;
		
		core_data_source = download_manager;
		
	    try{
	    	plugin_data_source = DownloadManagerImpl.getDownloadStatic(download_manager);
	        
	    }catch( Throwable e ){
	    }
	}
	
	public void 
	initialize(
		Composite composite) 
	{
		ScrolledComposite sc = new ScrolledComposite(composite, SWT.V_SCROLL | SWT.H_SCROLL );
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, true, 2, 1);
		sc.setLayoutData(gridData);	
		
		outer_panel = sc;
		
		Composite panel = new Composite(sc, SWT.NULL);
		
		sc.setContent( panel );
		
		
		
		GridLayout  layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 1;
		panel.setLayout(layout);

		//int userMode = COConfigurationManager.getIntParameter("User Mode");

			// header 
		
		Composite cHeader = new Composite(panel, SWT.BORDER);
		GridLayout configLayout = new GridLayout();
		configLayout.marginHeight = 3;
		configLayout.marginWidth = 0;
		cHeader.setLayout(configLayout);
		gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		cHeader.setLayoutData(gridData);
		
		Display d = panel.getDisplay();
		cHeader.setBackground(d.getSystemColor(SWT.COLOR_LIST_SELECTION));
		cHeader.setForeground(d.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
		
		Label lHeader = new Label(cHeader, SWT.NULL);
		lHeader.setBackground(d.getSystemColor(SWT.COLOR_LIST_SELECTION));
		lHeader.setForeground(d.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
		FontData[] fontData = lHeader.getFont().getFontData();
		fontData[0].setStyle(SWT.BOLD);
		int fontHeight = (int)(fontData[0].getHeight() * 1.2);
		fontData[0].setHeight(fontHeight);
		headerFont = new Font(d, fontData);
		lHeader.setFont(headerFont);
		lHeader.setText( " " + MessageText.getString( "authenticator.torrent" ) + " : " + download_manager.getDisplayName().replaceAll("&", "&&"));
		gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		lHeader.setLayoutData(gridData);
		
		Composite gTorrentInfo = new Composite(panel, SWT.NULL);
		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
		gTorrentInfo.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 2;
		gTorrentInfo.setLayout(layout);

			// torrent encoding
		
		
		Label label = new Label(gTorrentInfo, SWT.NULL);
		gridData = new GridData();
		label.setLayoutData( gridData );
		label.setText( MessageText.getString( TEXT_PREFIX + "torrent.encoding" ) + ": " );

		TOTorrent	torrent = download_manager.getTorrent();
		label = new Label(gTorrentInfo, SWT.NULL);
		gridData = new GridData();
		
		label.setLayoutData( gridData );
		label.setText(torrent==null?"":LocaleTorrentUtil.getCurrentTorrentEncoding( torrent ));
		
			// trackers
		
		label = new Label(gTorrentInfo, SWT.NULL);
		gridData = new GridData();
		label.setLayoutData( gridData );
		label.setText( MessageText.getString( "MyTrackerView.tracker" ) + ": " );

		String	trackers = "";
		
		if ( torrent != null ){
			
			TOTorrentAnnounceURLGroup group = torrent.getAnnounceURLGroup();
			
			TOTorrentAnnounceURLSet[]	sets = group.getAnnounceURLSets();
			
			List	tracker_list = new ArrayList();
			
			URL	url = torrent.getAnnounceURL();
			
			tracker_list.add( url.getHost() + (url.getPort()==-1?"":(":"+url.getPort())));
				
			for (int i=0;i<sets.length;i++){
										
				TOTorrentAnnounceURLSet	set = sets[i];
				
				URL[]	urls = set.getAnnounceURLs();
				
				for (int j=0;j<urls.length;j++){
				
					url = urls[j];
					
					String	str = url.getHost() + (url.getPort()==-1?"":(":"+url.getPort()));
					
					if ( !tracker_list.contains(str )){
						
						tracker_list.add(str);
					}
				}
			}
				
			TRTrackerAnnouncer announcer = download_manager.getTrackerClient();
			
			URL	active_url = null;
			
			if ( announcer != null ){
				
				active_url = announcer.getTrackerUrl();
				
			}else{
				
				TRTrackerScraperResponse scrape = download_manager.getTrackerScrapeResponse();
				
				if ( scrape != null ){
					
					active_url = scrape.getURL();
				}
			}
			
			if ( active_url == null ){
				
				active_url = torrent.getAnnounceURL();
			}
			
			trackers = active_url.getHost() + (active_url.getPort()==-1?"":(":"+active_url.getPort()));
		
			tracker_list.remove( trackers );
			
			if ( tracker_list.size() > 0 ){
				
				trackers += " (";
				
				for (int i=0;i<tracker_list.size();i++){
					
					trackers += (i==0?"":", ") + tracker_list.get(i);
				}
				
				trackers += ")";
			}
		}
		
		label = new Label(gTorrentInfo, SWT.NULL);
		gridData = new GridData();
		label.setLayoutData( gridData );
		label.setText( trackers );

		
			// columns
				 
		Group gColumns = new Group(panel, SWT.NULL);
		Messages.setLanguageText(gColumns, TEXT_PREFIX + "columns" );
		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
		gColumns.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 4;
		gColumns.setLayout(layout);
		
		Map	usable_cols = new HashMap();
		
		TableColumnManager col_man = TableColumnManager.getInstance();
		
		TableColumnCore[][] cols_sets = {
				col_man.getAllTableColumnCoreAsArray( TableManager.TABLE_MYTORRENTS_INCOMPLETE ),
				col_man.getAllTableColumnCoreAsArray( TableManager.TABLE_MYTORRENTS_COMPLETE ),
		};
				
		for (int i=0;i<cols_sets.length;i++){
			
			int	position_base 			= i==0?0:1000;
			int position_invisible_base	= i==0?10000:20000;

			TableColumnCore[]	cols = cols_sets[i];
			
			for (int j=0;j<cols.length;j++){
				
				TableColumnCore	col = cols[j];
			
				if ( usable_cols.containsKey( col.getClass())){
					
					continue;
				}
				
				ExternalCell	ext_cell = null;
				
				int	position = col.getPosition();
				
				if ( position == -1 ){
					
					position = position_invisible_base++;
					
				}else{
					position = position_base + position;
				}
								
				List	refresh_listeners = col.getCellRefreshListeners();
				
				if ( refresh_listeners.size() > 0 ){
				
					TableCellRefreshListener tcrl	= (TableCellRefreshListener)refresh_listeners.get(0);
										
					ext_cell = new ExternalCell( col, tcrl, position );
					
				}else{
										
					List	add_listeners = col.getCellAddedListeners();

					if ( add_listeners.size() > 0 ){
						
						TableCellAddedListener tcal = (TableCellAddedListener)add_listeners.get(0);
						
							// see if we can prod the cell into identifying its refresh listener
						
						ext_cell =  new ExternalCell( col, null, position );
						
						try{
							tcal.cellAdded( ext_cell );
							
							List	l = ext_cell.getRefreshListeners();
							
							if ( l.size() > 0 ){
								
								TableCellRefreshListener tcrl = (TableCellRefreshListener)l.get(0);
								
								ext_cell.setTarget( tcrl );
								
							}else{
								
								// System.out.println( "not usable (add->no listener) col: " + col.getName());
		
								ext_cell.dispose();
								
								ext_cell = null;
							}
						}catch(	Throwable e ){
								
							ext_cell.dispose();
							
							ext_cell = null;
							
							// System.out.println( "not usable (add) col: " + col.getName() + " - " + e.getMessage());
						}
					}else{
						
						// System.out.println( "not usable col: " + col.getName() + " - no listeners" );

					}
				}

				if ( ext_cell != null ){
						
					try{
						ext_cell.refresh();
						
						usable_cols.put( col.getClass(), ext_cell );
						
						// System.out.println( "usable col:" + col.getName());
						
					}catch( Throwable e ){
						
						ext_cell.dispose();
						
						// System.out.println( "not usable (refresh) col: " + col.getName() + " - " + e.getMessage());
					}
				}
			}
		}
		
		Collection values = usable_cols.values();
		
		cells = new ExternalCell[values.size()];
		
		values.toArray( cells );
		
		Arrays.sort( 
				cells,
				new Comparator()
				{
					public int
					compare(
						Object	o1,
						Object	o2 )
					{
						ExternalCell	c1 = (ExternalCell)o1;
						ExternalCell	c2 = (ExternalCell)o2;
						
						return( c1.getPosition() - c2.getPosition());
					}
				});
						
		for (int i=0;i<cells.length;i++){
			
			ExternalCell	cell = cells[i];
			
			label = new Label(gColumns, SWT.NULL);
			gridData = new GridData();
			if ( i%2 == 1 ){
				gridData.horizontalIndent = 16;
			}
			label.setLayoutData( gridData );
			label.setText( cell.getName() + ": " );

			label = new Label(gColumns, SWT.NULL);
			cell.setLabel( label );
			gridData = new GridData();
			gridData = new GridData( GridData.FILL_HORIZONTAL);
			
			label.setLayoutData( gridData );
		}
		
		refresh();
		
		sc.setMinSize( panel.computeSize( SWT.DEFAULT, SWT.DEFAULT ));
	}
	
	public void
	refresh()
	{
		if ( cells != null ){
			
			for (int i=0;i<cells.length;i++){
				
				ExternalCell	cell = cells[i];
				
				cell.refresh();
			}
		}
	}

	
	public Composite 
	getComposite() 
	{
		return outer_panel;
	}
	
	public String 
	getFullTitle() 
	{
		return MessageText.getString("GeneralView.section.info");
	}

	public String 
	getData() 
	{
		return( "GeneralView.section.info" );
	}
	
	public void 
	delete()
	{
		super.delete();
		
		if ( headerFont != null ){
			
			headerFont.dispose();
		}
		
		if ( cells != null ){
			
			for (int i=0;i<cells.length;i++){
				
				ExternalCell	cell = cells[i];
				
				cell.dispose();
			}
		}
	}
	
	protected class
	ExternalCell
		implements TableCellCore
	{
		private TableColumnCore				column;
		private TableCellRefreshListener	target;
		private int							position;
		
		private Label	label;
		private Image	label_image;
		
		private List	refresh_listeners	= new ArrayList();
		private List	dispose_listeners	= new ArrayList();
		
		private boolean	refresh_failed;
		
		private Graphic	graphic;
		
		protected
		ExternalCell(
			TableColumnCore				_column,
			TableCellRefreshListener	_target,
			int							_position )
		{
			column		= _column;
			target		= _target;
			position	= _position;
		}
		
		protected void
		setTarget(
			TableCellRefreshListener	_target )
		{
			target	= _target;
		}
		
		protected void
		setLabel(
			Label	_label )
		{
			label	= _label;
		}
		
		protected String
		getName()
		{
			return( MessageText.getString( column.getTitleLanguageKey()));
		}
		
		public void
		refresh()
		{
			if ( refresh_failed ){
				
				label.setText( "Not Available" );
				
				return;
			}
			try{
				
				target.refresh( this );
				
			}catch( RuntimeException e ){
				
				refresh_failed = true;
								
				throw( e );
			}
		}
		
		protected int
		getPosition()
		{
			return( position );
		}
		
		protected List
		getRefreshListeners()
		{
			return( refresh_listeners );
		}
		
		public Object 
		getDataSource()
		{
			return( column.getUseCoreDataSource()?(Object)core_data_source:plugin_data_source );
		}
		  
		public TableColumn 
		getTableColumn()
		{
			throw( new RuntimeException( "getTableColumn not imp" ));
		}
		  	
		public TableRow 
		getTableRow()
		{
			throw( new RuntimeException( "getTableRow not imp" ));
		}
		  
		public String 
		getTableID()
		{
			throw( new RuntimeException( "getTableID not imp" ));
		}
	
		public boolean 
		setText(
			String original_text)
		{
			if ( label != null && !label.isDisposed()){
				
				String text = DisplayFormatters.truncateString( original_text.replaceAll("&", "&&" ), 64 );
				
				label.setText( text);
				
				label.setToolTipText( original_text );
			}
			
			return( true );
		}

		public String 
		getText()
		{
			return( label==null||label.isDisposed()?"":label.getText());
		}

		public boolean 
		setForeground(
			int red, int green, int blue)
		{
			return( true );
		}

		public boolean 
		setSortValue(
			Comparable valueToSort)
		{	
			return( true );
		}

		public boolean 
		setSortValue(
			long valueToSort)
		{
			return( true );
		}

		public boolean 
		setSortValue( 
			float valueToSort )
		{
			return( true );
		}
		  
		public Comparable 
		getSortValue()
		{
			throw( new RuntimeException( "getSortValue not imp" ));
		}

		public boolean 
		isShown()
		{
			return( true );
		}

		public boolean 
		isValid()
		{
			return( false );
		}
	
		public void 
		invalidate()
		{
		}

		public void 
		setToolTip(
			Object tooltip)
		{	
		}

		public Object 
		getToolTip()
		{
			return( null );
		}

		public boolean 
		isDisposed()
		{
			return( false );
		}

		public int 
		getWidth()
		{
			return( label==null||label.isDisposed()?0:label.getBounds().width );
		}

		public int 
		getHeight()
		{
			return( label==null||label.isDisposed()?0:label.getBounds().height );
		}
		  
		public boolean 
		setGraphic(
			Graphic img )
		{
			graphic = img;
			
			if (img instanceof GraphicSWT){
				Image imgSWT = ((GraphicSWT)img).getImage();
				setImage( imgSWT );
			}

			if (img instanceof UISWTGraphic){
				Image imgSWT = ((UISWTGraphic)img).getImage();
				setImage( imgSWT );
			}
			    
			return( true );
		}
		
		public Graphic 
		getGraphic()
		{
			return( graphic );
		}

		public void 
		setFillCell(
				boolean bFillCell)
		{			  
		}

		public void 
		setMarginHeight(
				int height)
		{
		}

		public void 
		setMarginWidth(
				int width)
		{
		}

		public void 
		addRefreshListener(
			TableCellRefreshListener listener)
		{
			refresh_listeners.add( listener );
		}

		public void 
		removeRefreshListener(
			TableCellRefreshListener listener)
		{
			refresh_listeners.remove( listener );
		}

		public void 
		addDisposeListener(
				TableCellDisposeListener listener)
		{	
			dispose_listeners.add( listener );
		}

		public void 
		removeDisposeListener(
			TableCellDisposeListener listener)
		{  
			dispose_listeners.remove( listener );

		}

		public void 
		addToolTipListener(
			TableCellToolTipListener listener)
		{
		}

		public void 
		removeToolTipListener(
			TableCellToolTipListener listener)
		{
		}

		public void 
		addMouseListener(
			TableCellMouseListener listener)
		{  
		}

		public void 
		removeMouseListener(
			TableCellMouseListener listener)
		{		  
		}

		public void 
		addListeners(
			Object listener)
		{
			if ( listener instanceof TableCellRefreshListener ){
				refresh_listeners.add( listener );
			}
			
			if ( listener instanceof TableCellDisposeListener ){
				dispose_listeners.add( listener );
			}
		}
	
			// TableCellCore
		
		public void 
		invalidate(
			boolean bMustRefresh)
		{
		}

		public boolean 
		setForeground(
			Color color)
		{
			if ( label != null && !label.isDisposed()){
				label.setForeground( color );
			}
			return( true );
		}

		public void 
		refresh(
			boolean bDoGraphics)
		{
		}

		public void 
		refresh(
			boolean bDoGraphics, 
			boolean bRowVisible)
		{
		}

		public void 
		dispose()
		{
			for (int i=0;i<dispose_listeners.size();i++){
				
				try{
					((TableCellDisposeListener)dispose_listeners.get(i)).dispose( this );
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
			
			column.invokeCellDisposeListeners( this );
		}

		public void 
		setImage(
			Image img)
		{
			if ( label != null && !label.isDisposed()){
				
				label_image = img;

				if ( label.getText().length() == 0 ){

					label.setImage( img );
				}
			}
		}

		public boolean 
		needsPainting()
		{
			return( true );
		}

	
		public void 
		doPaint(GC gc)
		{
		}

		public void 
		locationChanged()
		{	
		}
		
		public TableRowCore 
		getTableRowCore()
		{
			return( external_row );
		}

		public Point 
		getSize()
		{
			throw( new RuntimeException( "getSize not imp" ));

		}
		
		public Rectangle 
		getBounds()
		{
			throw( new RuntimeException( "getBounds not imp" ));
		}

		public boolean 
		setGraphic(
			Image img)
		{
			setImage( img );
			
			return( true );
		}
		
		public Image 
		getGraphicSWT()
		{	
			return( label_image );
		}

		public void 
		invokeToolTipListeners(
			int type)
		{
		}
		
		public void 
		invokeMouseListeners(
			TableCellMouseEvent event)
		{
		}

		public void 
		setUpToDate(
			boolean upToDate)
		{
		}

		public String 
		getObfusticatedText()
		{
			return( null );
		}

		public Image 
		getBackgroundImage()
		{
			return( null );
		}

		public Color 
		getForegroundSWT()
		{
			return( null );
		}
		
		public int[] getForeground() {
			return new int[3];
		}

		/**
		 * @return
		 */
		public BufferedTableItem 
		getBufferedTableItem()
		{
			throw( new RuntimeException( "getBufferedTableItem not imp" ));
		}

		public int 
		getCursorID()
		{
			return(0);
		}

		public void 
		setCursorID(
			int cursorID)
		{
		}

	
		public boolean 
		isUpToDate()
		{
			return( true );
		}

		public int 
		compareTo(
			Object arg0 ) 
		{
			return 0;
		}
	}
	
	protected class
	ExternalRow
		implements TableRowCore
	{
			// table row core
		
		public void delete(boolean bDeleteSWTObject) {
		}
		public void doPaint(GC gc) {	
		}
		public Object getDataSource() {
			return null;
		}
		public Color getForeground() {
			return null;
		}
		public String getTableID() {
			return null;
		}
		public void invalidate() {
		}
		public boolean isValid() {
			return false;
		}
		public void refresh(boolean bDoGraphics) {
		}
		public void refresh(boolean bDoGraphics, boolean bVisible) {
		}
		public void setForeground(Color c) {
		}
		public void setUpToDate(boolean upToDate) {
		}
		public void doPaint(GC gc, boolean bVisible) {
		}
		public Color getBackground() {
			return null;
		}
		public Object getDataSource(boolean bCoreObject) {
			return null;
		}
		public int getIndex() {
			return 0;
		}
		public TableCell getTableCell(String sColumnName) {
			return null;
		}
		public TableCellCore getTableCellCore(String field) {
			return null;
		}
		public boolean isRowDisposed() {
			return false;
		}
		public boolean isSelected() {
			return false;
		}
		public boolean isVisible() {
			return false;
		}
		public void locationChanged(int iStartColumn) {		
		}
		public void repaint() {
		}
		public void setAlternatingBGColor(boolean bEvenIfNotVisible) {
		}
		public boolean setHeight(int iHeight) {
			return false;
		}
		public boolean setIconSize(Point pt) {
			return false;
		}
		public void setSelected(boolean bSelected) {
		}
		public boolean setTableItem(int newIndex) {
			return false;
		}
	}
}
