/*
 * Created on Oct 21, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.plugins.net.buddy;

import java.util.Map;

import org.gudy.azureus2.core3.download.DownloadManager;

import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBeta.ChatInstance;

public interface 
BuddyPluginViewInterface 
{
	public void
	openChat(
		ChatInstance		chat );
	
	public static final String	VP_SWT_COMPOSITE	= "swt_comp";
	public static final String	VP_DOWNLOAD			= "download";		// DownloadAdapter
	
	public View
	buildView(
		Map<String,Object>	properties,
		ViewListener		listener );
	
	public interface
	DownloadAdapter
	{
		public DownloadManager
		getCoreDownload();
		
		public String[]
		getNetworks();
		
		public String
		getChatKey();
	}
	
	public interface
	View
	{
		public void
		destroy();
	}
	
	public interface
	ViewListener
	{
		public void
		chatActivated(
			ChatInstance		chat );
	}
}
