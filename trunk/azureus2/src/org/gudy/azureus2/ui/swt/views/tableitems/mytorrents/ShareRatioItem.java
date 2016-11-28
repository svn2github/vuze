/*
 * File    : ShareRatioItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
 *
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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

import org.eclipse.swt.graphics.Color;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.SimpleTextEntryWindow;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.views.table.CoreTableColumnSWT;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.*;

import com.aelitis.azureus.ui.common.table.TableRowCore;

/**
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 */
public class ShareRatioItem
       extends CoreTableColumnSWT 
       implements TableCellRefreshListener, ParameterListener
{
	public static final Class DATASOURCE_TYPE = Download.class;

  private final static String CONFIG_ID = "StartStopManager_iFirstPriority_ShareRatio";
	public static final String COLUMN_ID = "shareRatio";
  private int iMinShareRatio;
  private boolean changeFG = true;

  public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] { CAT_SHARING, CAT_SWARM });
	}

	/** Default Constructor */
  public ShareRatioItem(String sTableID) {
    super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_TRAIL, 73, sTableID);
		setType(TableColumn.TYPE_TEXT);
    setRefreshInterval(INTERVAL_LIVE);
    setMinWidthAuto(true);

    setPosition(POSITION_LAST);

    iMinShareRatio = COConfigurationManager.getIntParameter(CONFIG_ID);
    COConfigurationManager.addParameterListener(CONFIG_ID, this);
    
    TableContextMenuItem menuItem = addContextMenuItem("label.set.share.ratio");
	
	menuItem.setStyle(MenuItem.STYLE_PUSH);
	
	menuItem.addMultiListener(new MenuItemListener() {
		public void selected(MenuItem menu, Object target) {
			final Object[] dms = (Object[])target;
			
			SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow(
					"set.share.ratio.win.title", "set.share.ratio.win.msg");
			
			entryWindow.setPreenteredText( "1.000", false );
			entryWindow.selectPreenteredText( true );
			
			entryWindow.prompt();
			
			if ( entryWindow.hasSubmittedInput()){
				
				try{
					String str = entryWindow.getSubmittedInput().trim();
					
					int share_ratio = (int)( Float.parseFloat( str ) * 1000 );
					
					for ( Object object: dms ){
						if (object instanceof TableRowCore) {
							object = ((TableRowCore) object).getDataSource(true);
						}
						
					    DownloadManager dm = (DownloadManager)object;
					    
					    dm.getStats().setShareRatio( share_ratio );
					}
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
		}
	});
  }

  protected void finalize() throws Throwable {
    super.finalize();
    COConfigurationManager.removeParameterListener(CONFIG_ID, this);
  }

  public void refresh(TableCell cell) {
    DownloadManager dm = (DownloadManager)cell.getDataSource();
                       
    int sr = (dm == null) ? 0 : dm.getStats().getShareRatio();
    
    if ( sr == Integer.MAX_VALUE ){
    	sr = Integer.MAX_VALUE-1;
    }
    if ( sr == -1 ){
      sr = Integer.MAX_VALUE;
    }
    
    if (!cell.setSortValue(sr) && cell.isValid())
      return;
    
    String shareRatio = "";
    
    if (sr == Integer.MAX_VALUE ) {
      shareRatio = Constants.INFINITY_STRING;
    } else {
      shareRatio = DisplayFormatters.formatDecimal((double) sr / 1000, 3);
    }
    
    if( cell.setText(shareRatio) && changeFG ) {
    	Color color = sr < iMinShareRatio ? Colors.colorWarning : null;
    	cell.setForeground(Utils.colorToIntArray(color));
    }
  }

  public void parameterChanged(String parameterName) {
    iMinShareRatio = COConfigurationManager.getIntParameter(CONFIG_ID);
    invalidateCells();
  }

  public boolean isChangeFG() {
		return changeFG;
	}

	public void setChangeFG(boolean changeFG) {
		this.changeFG = changeFG;
	}
}
