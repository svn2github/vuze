/*
 * Created on Feb 13, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.devices;

import java.io.File;

import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;

public interface 
TranscodeFile 
{
		// don't change these, they are serialised
	
	public static final String PT_COMPLETE		= "comp";
	public static final String PT_COPIED		= "copied";
	public static final String PT_COPY_FAILED	= "copy_fail";
	
	public File
	getCacheFile()
	
		throws TranscodeException;
	
	public DiskManagerFileInfo
	getSourceFile()
	
		throws TranscodeException;
	
	public String
	getProfileName();
	
	public long
	getCreationDateMillis();
	
	public boolean
	isComplete();
	
	public boolean
	isCopiedToDevice();
	
	public long
	getCopyToDeviceFails();
	
	public void
	retryCopyToDevice();
	
	public boolean
	isTemplate();
	
	public Device
	getDevice();
	
		/**
		 * Will return null unless there is a job in existance for this file
		 * @return
		 */
	
	public TranscodeJob
	getJob();
	
	public void
	delete(
		boolean	delete_cache_file )
	
		throws TranscodeException;
	
	public void
	setTransientProperty(
		Object		key,
		Object		value );
			
	public Object
	getTransientProperty(
		Object		key );
}
