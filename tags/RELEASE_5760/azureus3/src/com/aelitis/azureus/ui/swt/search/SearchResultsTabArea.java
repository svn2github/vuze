/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */

package com.aelitis.azureus.ui.swt.search;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.*;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.skin.SkinConstants;



import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.menus.*;

/**
 * @author TuxPaper
 * @created Sep 30, 2006
 *
 */
public class SearchResultsTabArea
	extends SkinView
	implements ViewTitleInfo
{	
	private boolean					isBrowserView		= COConfigurationManager.getBooleanParameter( "Search View Is Web View", true );
	private boolean					isViewSwitchHidden	= COConfigurationManager.getBooleanParameter( "Search View Switch Hidden", false );

	
	private SWTSkinObjectBrowser 	browserSkinObject;
	private SWTSkinObjectContainer	nativeSkinObject;
	
	private SWTSkin 				skin;
	private MdiEntry				mdi_entry;
	private MdiEntryVitalityImage 	vitalityImage;

	private boolean menu_added;
		
	private SearchQuery 	current_sq;
	
	private SearchQuery 				last_actual_sq;
	private SearchResultsTabAreaBase	last_actual_sq_impl;
	
	private SearchResultsTabAreaBase	activeImpl;
	
	private SearchResultsTabAreaBrowser	browserImpl = new SearchResultsTabAreaBrowser( this );
	private SBC_SearchResultsView	nativeImpl 	= new SBC_SearchResultsView( this );
	private SWTSkinObject soButtonWeb;
	private SWTSkinObject soButtonMeta;
	
	public
	SearchResultsTabArea()
	{
	}
	
	public static class SearchQuery {
		public SearchQuery(String term, boolean toSubscribe) {
			this.term = term;
			this.toSubscribe = toSubscribe;
		}

		public String term;
		public boolean toSubscribe;
	}
	
	public Object 
	skinObjectInitialShow( 
		final SWTSkinObject 	skinObject, 
		Object 					params )
	{
		skin = skinObject.getSkin();
		
		SWTSkinObjectContainer controlArea = (SWTSkinObjectContainer)skin.getSkinObject( "searchresultstop", skinObject);

		if ( controlArea != null ){
			
			if ( isViewSwitchHidden ){
				
				controlArea.setVisible( false );
				
			}else{
				Composite control_area = controlArea.getComposite();
				
				soButtonWeb = skin.getSkinObject( "searchresults-button-web", controlArea);
				soButtonMeta = skin.getSkinObject( "searchresults-button-meta", controlArea);

				SWTSkinButtonUtility btnWeb = new SWTSkinButtonUtility(soButtonWeb);
				btnWeb.addSelectionListener(new ButtonListenerAdapter() {
					public void pressed(SWTSkinButtonUtility buttonUtility,
							SWTSkinObject skinObject, int stateMask) {
						isBrowserView = true;
						COConfigurationManager.setParameter( "Search View Is Web View", isBrowserView );
						selectView( skinObject );
					}
				});

				SWTSkinButtonUtility btnMeta = new SWTSkinButtonUtility(soButtonMeta);
				btnMeta.addSelectionListener(new ButtonListenerAdapter() {
					public void pressed(SWTSkinButtonUtility buttonUtility,
							SWTSkinObject skinObject, int stateMask) {
						isBrowserView = false;
						COConfigurationManager.setParameter( "Search View Is Web View", isBrowserView );
						selectView( skinObject );
					}
				});
				
			}
		}
		
		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		
		if ( mdi != null ){
			
			mdi_entry = mdi.getEntryBySkinView(this);
			
			if ( mdi_entry != null ){
				
				mdi_entry.setViewTitleInfo( this );
				
				vitalityImage = mdi_entry.addVitalityImage("image.sidebar.vitality.dots");
				
				if ( vitalityImage != null ){
					
					vitalityImage.setVisible(false);
				}
			}
		}
		
		browserSkinObject 	= (SWTSkinObjectBrowser)skin.getSkinObject("web-search-results", skinObject);
		
		browserImpl.init( browserSkinObject );

		nativeSkinObject 	= (SWTSkinObjectContainer)skin.getSkinObject( "meta-search-results", skinObject);
		
		nativeImpl.skinObjectInitialShow( skinObject, params );
		
		selectView( skinObject );			

		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(final AzureusCore core) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						initCoreStuff(core);
					}

				});
			}
		});
		
		if ( current_sq != null ){
			
			anotherSearch( current_sq );
		}
						
		return null;
	}

	public Object skinObjectShown(SWTSkinObject skinObject, Object params) {
		if ( activeImpl != null ){
			
			activeImpl.refreshView();
		}
		
		return( super.skinObjectShown(skinObject, params));
	}
	
	private void
	selectView(
		SWTSkinObject		parent )
	{		
		SearchResultsTabAreaBase newImpl = isBrowserView?browserImpl:nativeImpl;
		
		if (newImpl == activeImpl) {
			return;
		}

		Control[] kids = ((Composite)nativeSkinObject.getControl().getParent()).getChildren();
			
		Control visible_parent = isBrowserView?browserSkinObject.getControl():nativeSkinObject.getControl();
		
		for ( Control kid: kids ){
			kid.setVisible( kid == visible_parent );
		}
		
		browserSkinObject.setVisible( isBrowserView );
		nativeSkinObject.setVisible( !isBrowserView );	

		if (soButtonWeb != null) {
			soButtonWeb.switchSuffix(isBrowserView ? "-selected" : "");
		}
		if (soButtonMeta != null) {
			soButtonMeta.switchSuffix(isBrowserView ? "" : "-selected");
		}

		
		parent.relayout();	
		
		if ( activeImpl != null ){
		
			activeImpl.hideView();
		}
		
		activeImpl = newImpl;
			
		activeImpl.showView();
		
		if ( current_sq != null ){
			
			anotherSearch( current_sq );
		}
	}
	
	private void initCoreStuff(AzureusCore core) {
		PluginInterface pi = PluginInitializer.getDefaultInterface();
		UIManager uim = pi.getUIManager();
		
		final MenuManager menuManager = uim.getMenuManager();

		if ( !menu_added ){
			
			menu_added = true;		

			SearchUtils.addMenus( menuManager );
		}
	}

	public Object 
	dataSourceChanged(
		SWTSkinObject 	skinObject, 
		Object 			params) 
	{		
		if ( params instanceof SearchQuery ){
										
			anotherSearch((SearchQuery)params);
		}

		return null;
	}

	public void 
	anotherSearch(
		String 		searchText,
		boolean 	toSubscribe ) 
	{
		anotherSearch(new SearchQuery(searchText, toSubscribe));
	}
	
	public void 
	anotherSearch(
		SearchQuery another_sq ) 
	{
		current_sq = another_sq;
		
		if ( activeImpl != null ){
			
			if ( 	last_actual_sq != null && 
					last_actual_sq.term.equals( current_sq.term ) && 
					last_actual_sq.toSubscribe == current_sq.toSubscribe &&
					last_actual_sq_impl == activeImpl ){
				
					// same search, ignore
			
				return;
			}
		
			last_actual_sq		= current_sq;
			last_actual_sq_impl	= activeImpl;
			
			activeImpl.anotherSearch( current_sq );
						
			ViewTitleInfoManager.refreshTitleInfo( this );
		}
	}
	
	public SearchQuery
	getCurrentSearch()
	{
		return( current_sq );
	}
	
	public Object 
	getTitleInfoProperty(
		int 	pid )
	{
		SearchQuery	sq 						= current_sq;
		SearchResultsTabAreaBase	impl 	= activeImpl;
				
		if ( pid == TITLE_TEXT ){
		
			if ( sq != null ){

				return( sq.term );
			}
		}else if ( pid == TITLE_INDICATOR_TEXT ){
			
			if ( impl != null ){
				
				int results = impl.getResultCount();
				
				if ( results >= 0 ){
					
					return( String.valueOf( results ));
				}
			}
		}
		
		return( null );
	}
	
	protected void
	setBusy(
		boolean	busy )
	{
		if ( vitalityImage != null ){
			
			vitalityImage.setVisible( busy );
		}
	}
	
	protected void
	resultsFound()
	{
		ViewTitleInfoManager.refreshTitleInfo( this );
	}
}
