/*
 * Created on 27-Apr-2004
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

package org.gudy.azureus2.pluginsimpl.update.sf.impl2;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.pluginsimpl.update.sf.*;

public class 
SFPluginDetailsImpl
	implements SFPluginDetails
{
	protected SFPluginDetailsLoaderImpl		loader;
	protected boolean						fully_loaded;
	
	protected String		name;
	protected String		version;
	protected String		download_url;
	protected String		author;
	protected String		cvs_version;
	protected String		cvs_download_url;

	protected String		desc;
	protected String		comment;
	
	protected
	SFPluginDetailsImpl(
		SFPluginDetailsLoaderImpl	_loader,
		String						_name,
		String						_version )
	{
		loader				= _loader;
		name				= _name;
		version				= _version;
	}
	
	protected void
	setDetails(
		String	_name,
		String	_version,
		String	_download_url,
		String	_author,
		String	_cvs_version,
		String	_cvs_download_url,
		String	_desc,
		String	_comment )
	{
		fully_loaded		= true;
		
		name				= _name;
		version				= _version;
		download_url		= _download_url;
		author				= _author;
		cvs_version			= _cvs_version;
		cvs_download_url	= _cvs_download_url;
		desc				= _desc;
		comment				= _comment;
	}
	
	protected boolean
	isFullyLoaded()
	{
		return( fully_loaded );
	}
	
	protected void
	checkLoaded()
	
		throws SFPluginDetailsException
	{
		if ( !fully_loaded ){
			
			loader.loadPluginDetails( this );
		}
	}
	
	public String
	getName()
	{
		return( name );
	}
	
	public String
	getVersion()
	
		throws SFPluginDetailsException
	{
		return( version );
	}

	public String
	getDownloadURL()
	
		throws SFPluginDetailsException
	{
		checkLoaded();
		
		return( download_url );
	}
	
	public String
	getAuthor()
	
		throws SFPluginDetailsException
	{
		checkLoaded();
		
		return( author );
	}	
	
	public String
	getCVSVersion()
	
		throws SFPluginDetailsException
	{
		checkLoaded();
		
		return( cvs_version );
	}
	
	public String
	getCVSDownloadURL()
	
		throws SFPluginDetailsException
	{
		checkLoaded();
		
		return( cvs_download_url );
	}
	
	public String
	getDescription()
	
	throws SFPluginDetailsException
	{
		checkLoaded();
		
		return( desc );
	}
	
	public String
	getComment()
	
		throws SFPluginDetailsException
	{
		checkLoaded();
		
		return( comment );
	}
}
