/*
 * Created on 16-Jan-2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.swt.config;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.ui.swt.config.generic.GenericParameterAdapter;

public class 
ConfigParameterAdapter
	extends GenericParameterAdapter
{
	private Parameter	owner;

	protected
	ConfigParameterAdapter(
		Parameter		_owner )
	{
		owner	= _owner;
	}

	public int
	getIntValue(
		String	key )
	{
		return( COConfigurationManager.getIntParameter(key));
	}
	
	public int
	getIntValue(
		String	key,
		int		def )
	{
		return( COConfigurationManager.getIntParameter(key,def));
	}
	
	public void
	setIntValue(
		String	key,
		int		value )
	{
		COConfigurationManager.setParameter( key, value );
	}
	
	public boolean
	getBooleanValue(
		String	key )
	{
		return( COConfigurationManager.getBooleanParameter(key));
	}
	
	public boolean
	getBooleanValue(
		String		key,
		boolean		def )
	{
		return( COConfigurationManager.getBooleanParameter(key,def));
	}
	
	public void
	setBooleanValue(
		String		key,
		boolean		value )
	{
		COConfigurationManager.setParameter(key,value);
	}
	
	public void
	informChanged(
		boolean	internally )
	{
       	if( owner.change_listeners != null ) {
            for (int i=0;i< owner.change_listeners.size();i++){
              ((ParameterChangeListener) owner.change_listeners.get(i)).parameterChanged(owner,internally);
            }
       	}
	}
}
