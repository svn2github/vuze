/*
 * Created on Jan 10, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
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
 */


package com.aelitis.azureus.ui.swt.search;

import java.util.Locale;

import org.eclipse.swt.SWT;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.webplugin.WebPlugin;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.views.skin.SearchResultsTabArea;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.util.ConstantsVuze;

public class 
SearchHandler 
{
	public static void
	handleSearch(
		String		sSearchText,
		boolean		toSubscribe )
	{
		if ( !toSubscribe ){
			
			try{
				
				if ( 	COConfigurationManager.getBooleanParameter("rcm.overall.enabled",true) &&
						COConfigurationManager.getBooleanParameter( "Plugin.aercm.rcm.search.enable", false ) && 
						AzureusCoreFactory.isCoreRunning()){
					
					final PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID( "aercm");

					if (	pi != null && 
							pi.getPluginState().isOperational() &&
							pi.getIPC().canInvoke("lookupByExpression", new Object[]{ "" })){

						pi.getIPC().invoke("lookupByExpression", new Object[]{ sSearchText });
					}
				}
			}catch (Throwable e ){

				Debug.out(e);
			}
		}
		
		boolean	internal_search = !COConfigurationManager.getBooleanParameter( "browser.external.search" );
		
		if ( internal_search ){
			
			SearchResultsTabArea.SearchQuery sq = new SearchResultsTabArea.SearchQuery(
					sSearchText, toSubscribe);
	
			MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
			String id = MultipleDocumentInterface.SIDEBAR_SECTION_SEARCH;
			MdiEntry existingEntry = mdi.getEntry(id);
			if (existingEntry != null && existingEntry.isAdded()) {
				SearchResultsTabArea searchClass = (SearchResultsTabArea) SkinViewManager.getByClass(SearchResultsTabArea.class);
				if (searchClass != null) {
					searchClass.anotherSearch(sSearchText, toSubscribe);
				}
				mdi.showEntry(existingEntry);
				return;
			}
	
			final MdiEntry entry = mdi.createEntryFromSkinRef(
					MultipleDocumentInterface.SIDEBAR_HEADER_DISCOVERY, id,
					"main.area.searchresultstab", sSearchText, null, sq, true, MultipleDocumentInterface.SIDEBAR_POS_FIRST );
			if (entry != null) {
				entry.setImageLeftID("image.sidebar.search");
				entry.setDatasource(sq);
				entry.setViewTitleInfo(new ViewTitleInfo() {
					public Object getTitleInfoProperty(int propertyID) {
						if (propertyID == TITLE_TEXT) {
							SearchResultsTabArea searchClass = (SearchResultsTabArea) SkinViewManager.getByClass(SearchResultsTabArea.class);
							if (searchClass != null && searchClass.sq != null) {
								return searchClass.sq.term;
							}
						}
						return null;
					}
				});
			}
			
			mdi.showEntryByID(id);
			
		}else{
			
			PluginInterface xmweb_ui = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID( "xmwebui" );
			
			if (xmweb_ui == null || !xmweb_ui.getPluginState().isOperational()){
				
				UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
				
				MessageBoxShell mb = new MessageBoxShell( 
	    				SWT.ICON_ERROR | SWT.OK,
	    				MessageText.getString( "external.browser.failed" ),
	    				MessageText.getString( "xmwebui.required" ));
				
				mb.setParent(uiFunctions.getMainShell());
				
	    		mb.open(null);
	    		
			}else{
			
				WebPlugin wp = (WebPlugin)xmweb_ui.getPlugin();
				
				String remui = wp.getProtocol().toLowerCase( Locale.US ) + "://127.0.0.1:" + wp.getPort() + "/";
				
				String test_url = ConstantsVuze.getDefaultContentNetwork().getServiceURL( ContentNetwork.SERVICE_XSEARCH, new Object[]{ "", false });
				
				int	pos = test_url.indexOf( '?' );
				
				String mode = xmweb_ui.getUtilities().getFeatureManager().isFeatureInstalled( "core" )?"plus":"trial";
					
				String search_url = 
						test_url.substring( 0, pos+1 ) + 
							"q=" + UrlUtils.encode( sSearchText ) + "&" +
							"mode=" + mode + "&" +
							"search_source=" + UrlUtils.encode( remui );
				
				Utils.launch( search_url );
			}
		}
	}
}
