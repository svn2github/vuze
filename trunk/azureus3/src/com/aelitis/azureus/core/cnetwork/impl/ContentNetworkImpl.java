/*
 * Created on Nov 20, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.cnetwork.impl;

import java.util.*;

import com.aelitis.azureus.core.cnetwork.*;
import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.core.vuzefile.VuzeFileComponent;
import com.aelitis.azureus.core.vuzefile.VuzeFileHandler;

public class 
ContentNetworkImpl
	implements ContentNetwork
{
	protected static ContentNetworkImpl
	importFromBencodedMap(
		Map		map )
	{
		return( new ContentNetworkImpl(((Long)map.get("id")).longValue()));
	}
	
	private long		id;
	
	protected
	ContentNetworkImpl(
		long			_id )
	{
		id		= _id;
	}
	
	protected Map
	exportToBencodedMap()
	{
		Map	result = new HashMap();
		
		result.put( "id", new Long( id ));
		
		return( result );
	}
	
	public long 
	getID() 
	{
		return( id );
	}
	
	public VuzeFile
	getVuzeFile()
	{
		VuzeFile	vf = VuzeFileHandler.getSingleton().create();
		
		vf.addComponent(
			VuzeFileComponent.COMP_TYPE_CONTENT_NETWORK,
			exportToBencodedMap());
		
		return( vf );
	}
}
