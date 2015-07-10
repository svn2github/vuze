/*
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

package com.aelitis.azureus.plugins.net.buddy.swt;


import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBeta.ChatAdapter;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBeta.ChatInstance;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.mdi.MdiCloseListener;
import com.aelitis.azureus.ui.mdi.MdiEntry;


public class ChatMDIEntry implements ViewTitleInfo
{
	private final MdiEntry mdi_entry;
	
	private final ChatInstance chat;
	
	private final ChatAdapter adapter = 
		new ChatAdapter()
		{
			@Override
			public void 
			updated() 
			{
				update();
			}
		};
		
	public 
	ChatMDIEntry(
		ChatInstance 	_chat, 
		MdiEntry 		_entry) 
	{
		chat		= _chat;
		
		mdi_entry 	= _entry;
			
		setupMdiEntry();
	}
	
	private void 
	setupMdiEntry() 
	{
		mdi_entry.setViewTitleInfo( this );
		
		mdi_entry.addListener(
			new MdiCloseListener()
			{
				public void 
				mdiEntryClosed(
					MdiEntry 	entry,
					boolean 	user) 
				{
					chat.destroy();
				}
			});
		
		chat.addListener( adapter );
	}

	private void
	update()
	{
		mdi_entry.redraw();
	
		ViewTitleInfoManager.refreshTitleInfo( mdi_entry.getViewTitleInfo());
	}
	
	public Object 
	getTitleInfoProperty(
		int propertyID ) 
	{
		switch( propertyID ){
		
			case ViewTitleInfo.TITLE_TEXT:{
				
				return( chat.getName());
			}
			case ViewTitleInfo.TITLE_INDICATOR_TEXT:{
				
				if ( chat.getMessageOutstanding()){
					
					return( "*" );
					
				}else{
					
					return( null );
				}
						
			}
		}
		
		return( null );
	}
}
