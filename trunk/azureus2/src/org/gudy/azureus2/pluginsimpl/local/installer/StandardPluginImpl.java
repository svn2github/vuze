/*
 * Created on 28-Nov-2004
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

package org.gudy.azureus2.pluginsimpl.local.installer;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.installer.*;
import org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetails;

public class 
StandardPluginImpl 
	implements StandardPlugin
{
	protected PluginInstallerImpl	installer;
	protected SFPluginDetails		details;
	
	protected
	StandardPluginImpl(
		PluginInstallerImpl	_installer,
		SFPluginDetails		_details )
	{
		installer	= _installer;
		details		= _details;
	}
	
	public String
	getId()
	{
		return( details.getName());
	}
	
	public String
	getVersion()
	{
		String	version = details.getVersion();
		
		return( version==null?"":version );
	}
	
		/**
		 * Returns the plugin's interface if already installed, null if it isn't
		 * @return
		 */
	
	public PluginInterface
	getAlreadyInstalledPlugin()
	{
		PluginInterface[]	ifs = installer.getPluginManager().getPluginInterfaces();
		
		for (int i=0;i<ifs.length;i++){
			
			if ( ifs[i].getPluginID().equals( getId())){
				
				return( ifs[i]);
			}
		}
		
		return( null );
	}
}
