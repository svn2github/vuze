/*
 * Created on 30-Nov-2004
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

import java.io.File;

import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.installer.FilePluginInstaller;
import org.gudy.azureus2.plugins.installer.PluginInstaller;

/**
 * @author parg
 *
 */

public class 
FilePluginInstallerImpl
	implements FilePluginInstaller
{
	protected PluginInstallerImpl		installer;
	protected File						file;
	protected String					id;
	protected String					version;
	protected boolean					is_jar;
	
	protected
	FilePluginInstallerImpl(
		PluginInstallerImpl	_installer,
		File				_file,
		String				_id,
		String				_version,
		boolean				_is_jar )
	{
		installer	= _installer;
		file		= _file;
		id			= _id;
		version		= _version;
		is_jar		= _is_jar;
	}
	
	public File
	getFile()
	{
		return( file );
	}
	
	public String
	getId()
	{
		return( id );
	}
	
	public String
	getVersion()
	{
		return( version );
	}
	
	public String
	getName()
	{
		return( "" );
	}
	
	public String
	getDescription()
	{
		return( file.toString());
	}
		
	public PluginInterface
	getAlreadyInstalledPlugin()
	{
		return( installer.getAlreadyInstalledPlugin( getId()));
	}
	
	public void
	install(
		boolean		shared )
	
		throws PluginException
	{
		installer.install( new String[]{ getId() }, shared, file, version, is_jar );
	}	
	
	
	public boolean
	uninstall()
	
		throws PluginException
	{
		return( installer.uninstall( this ));
	}	
	
	public PluginInstaller
	getInstaller()
	{
		return( installer );
	}
}
