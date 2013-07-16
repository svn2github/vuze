/*
 * Created on Jul 5, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
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


package org.gudy.azureus2.plugins.download;

import java.io.File;

import org.gudy.azureus2.plugins.torrent.TorrentAttribute;

public interface 
DownloadStub 
{
	public boolean
	isStub();
	
	public Download
	destubbify()
	
		throws DownloadException;
	
	public String
	getName();
	
	public byte[]
	getTorrentHash();
	
	public DownloadStubFile[]
	getStubFiles();
	
		/**
		 * There are logically separate from the un-stubbed download, if you want to 
		 * synchronize values from non-stub and stub then it is up to you to do it. So I suggest
		 * that you infact don't do this but just use these methods as a means to cache essential
		 * attributes from unstubbed ones in the 'will-be-added' listener event
		 * @param attribute
		 * @return
		 */
	
	public long 
	getLongAttribute(
		TorrentAttribute 	attribute );
	  
	public void 
	setLongAttribute(
		TorrentAttribute 	attribute, 
		long 				value);
	  
	public void
	remove()
	
		throws DownloadException, DownloadRemovalVetoException;
	
	public interface 
	DownloadStubFile 
	{
		public File
		getFile();
		
		public long
		getLength();
	}
}
