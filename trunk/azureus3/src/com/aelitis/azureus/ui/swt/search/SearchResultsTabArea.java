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
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
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
	private boolean					isBrowserView	= true;
	
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
			
			Composite control_area = controlArea.getComposite();
			
			Utils.disposeComposite( control_area, false );
			
			control_area.setLayout( new RowLayout());
			
			final Button button = new Button( control_area, SWT.TOGGLE );
			
			button.setText( "Switch To Native View" );
			
			button.setSelection( !isBrowserView );
			
			button.addSelectionListener(
				new SelectionAdapter(){
					@Override
					public void widgetSelected(SelectionEvent e) {
						isBrowserView = !button.getSelection();
						
						selectView( skinObject );
					}
				});
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
		
		browserSkinObject 	= (SWTSkinObjectBrowser)skin.getSkinObject(SkinConstants.VIEWID_BROWSER_SEARCHRESULTS, skinObject);
		
		browserImpl.init( browserSkinObject );

		nativeSkinObject 	= (SWTSkinObjectContainer)skin.getSkinObject( "searchresults2", skinObject);
		
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

	private void
	selectView(
		SWTSkinObject		parent )
	{		

		Control[] kids = ((Composite)nativeSkinObject.getControl().getParent()).getChildren();
			
		Control visible_parent = isBrowserView?browserSkinObject.getControl():nativeSkinObject.getControl();
		
		for ( Control kid: kids ){
			kid.setVisible( kid == visible_parent );
		}
		
		browserSkinObject.setVisible( isBrowserView );
		nativeSkinObject.setVisible( !isBrowserView );	

		parent.relayout();	
		
		if ( activeImpl != null ){
		
			activeImpl.hideView();
		}
		
		activeImpl = isBrowserView?browserImpl:nativeImpl;
			
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
