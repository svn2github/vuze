/*
 * Created on 13-Jul-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.tracker.host.TRHost;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.ipfilter.IpFilterManager;
import org.gudy.azureus2.core3.internat.LocaleUtil;

import org.gudy.azureus2.plugins.*;

public interface 
AzureusCore 
{	
	public void
	start()
	
		throws AzureusCoreException;
	
		/**
		 * stop the core and inform lifecycle listeners of stopping
		 * @throws AzureusCoreException
		 */
	
	public void
	stop()
	
		throws AzureusCoreException;
	
		/**
		 * ask lifecycle listeners to perform a stop. they may veto this by throwing an exception, or do nothing
		 * if nothing is done then it will be stopped as per "stop" above
		 * @throws AzureusCoreException
		 */
	
	public void
	requestStop()
	
		throws AzureusCoreException;
	
		/**
		 * request a restart of the system - currently only available for swt based systems 
		 * @throws AzureusCoreException
		 */
	
	public void
	requestRestart()
	
		throws AzureusCoreException;
	
	public LocaleUtil
	getLocaleUtil();
	
	public GlobalManager
	getGlobalManager()
	
		throws AzureusCoreException;
	
	public PluginManagerDefaults
	getPluginManagerDefaults()
	
		throws AzureusCoreException;
	
	public PluginManager
	getPluginManager()
	
		throws AzureusCoreException;
	
	public TRHost
	getTrackerHost()
	
		throws AzureusCoreException;
	
	public IpFilterManager
	getIpFilterManager()
	
		throws AzureusCoreException;
	
	public void
	addLifecycleListener(
		AzureusCoreLifecycleListener	l );
	
	public void
	removeLifecycleListener(
		AzureusCoreLifecycleListener	l );
	
	public void
	addListener(
		AzureusCoreListener	l );
	
	public void
	removeListener(
		AzureusCoreListener	l );
}
