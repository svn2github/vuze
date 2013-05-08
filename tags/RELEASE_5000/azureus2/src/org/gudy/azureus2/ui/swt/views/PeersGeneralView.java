/*
 * Created on 2 juil. 2003
 *
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
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
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.views;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.views.peer.PeerInfoView;
import org.gudy.azureus2.ui.swt.views.peer.RemotePieceDistributionView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;
import org.gudy.azureus2.ui.swt.views.tableitems.peers.DownloadNameItem;

import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagListener;
import com.aelitis.azureus.core.tag.Taggable;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableLifeCycleListener;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;


public class 
PeersGeneralView
	extends TableViewTab<PEPeer>
	implements TagListener, TableLifeCycleListener, TableViewSWTMenuFillListener
{	
	private TableViewSWT<PEPeer> tv;
	
	private Shell shell;

	private Tag	tag;
	
	public 
	PeersGeneralView(
		Tag	_tag )
	{
		super( "AllPeersView" );
		
		tag = _tag;
	}	

	public String 
	getFullTitle() 
	{
		return( tag.getTagName( true ));
	}
	
	public TableViewSWT<PEPeer> 
	initYourTableView() 
	{
		TableColumnCore[] items = PeersView.getBasicColumnItems(TableManager.TABLE_ALL_PEERS);
		TableColumnCore[] basicItems = new TableColumnCore[items.length + 1];
		System.arraycopy(items, 0, basicItems, 0, items.length);
		basicItems[items.length] = new DownloadNameItem(TableManager.TABLE_ALL_PEERS);

		tv = TableViewFactory.createTableViewSWT(Peer.class, TableManager.TABLE_ALL_PEERS,
				getPropertiesPrefix(), basicItems, "connected_time", SWT.MULTI
				| SWT.FULL_SELECTION | SWT.VIRTUAL);
		
		tv.setRowDefaultHeight(16);
		tv.setEnableTabViews(true,true,null);

		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		
		if (uiFunctions != null){
			
			UISWTInstance pluginUI = uiFunctions.getUISWTInstance();

			if (pluginUI != null && !PeersSuperView.registeredCoreSubViews) {

				pluginUI.addView(TableManager.TABLE_ALL_PEERS, "PeerInfoView",
						PeerInfoView.class, null);
				pluginUI.addView(TableManager.TABLE_ALL_PEERS,
						"RemotePieceDistributionView", RemotePieceDistributionView.class,
						null);
				pluginUI.addView(TableManager.TABLE_ALL_PEERS, "LoggerView",
						LoggerView.class, true);

				PeersSuperView.registeredCoreSubViews = true;
			}
		}

		tv.addLifeCycleListener(this);
		tv.addMenuFillListener(this);
		
		return tv;
	}

	public void
	taggableAdded(
		Tag			tag,
		Taggable	tagged )
	{
		 tv.addDataSource((PEPeer)tagged);
	}
	
	public void 
	taggableSync(
		Tag 		tag ) 
	{
		if ( tv.getRowCount() != tag.getTaggedCount()){
					
			Set<PEPeer>	peers_in_table 	= new HashSet<PEPeer>( tv.getDataSources());
			
			Set<PEPeer> peers_in_tag	= new HashSet<PEPeer>((Set)tag.getTagged());
			
			for ( PEPeer peer: peers_in_table ){
				
				if ( !peers_in_tag.contains( peer )){
										
					tv.removeDataSource( peer );
				}
			}
			
			for ( PEPeer peer: peers_in_tag ){
				
				if ( !peers_in_table.contains( peer )){
										
					tv.addDataSource( peer );
				}
			}
		}
	}
	
	public void
	taggableRemoved(
		Tag			tag,
		Taggable	tagged )
	{
		 tv.removeDataSource((PEPeer)tagged);
	}
	
	public void 
	tableViewInitialized() 
	{
		shell = tv.getComposite().getShell();
		
		tag.addTagListener( this, true );
	}

	public void 
	tableViewDestroyed() 
	{
		tag.removeTagListener( this );
	}

	public void 
	fillMenu(
		String 		sColumnName, 
		Menu 		menu )
	{
		PeersView.fillMenu( menu, tv, shell, false );
	}

	public void 
	addThisColumnSubMenu(
			String 	columnName, 
			Menu 	menuThisColumn )
	{	
	}
}
