/*
 * Created on 28-Apr-2004
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

package org.gudy.azureus2.plugins.ui.model;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.plugins.ui.config.*;

public interface 
BasicPluginConfigModel 
	extends PluginConfigModel
{
	/**
	 * @deprecated use addBooleanParameter2
	 * @param key
	 * @param resource_name
	 * @param defaultValue
	 * @return
	 */
	
	public void
	addBooleanParameter(
		String 		key,
		String 		resource_name,
		boolean 	defaultValue );
	
	/**
	 * @deprecated user addStringParameter2
	 * @param key
	 * @param resource_name
	 * @param defaultValue
	 * @return
	 */
	
	public void
	addStringParameter(
		String 		key,
		String 		resource_name,
		String	 	defaultValue );
	
	
	
	public BooleanParameter
	addBooleanParameter2(
		String 		key,
		String 		resource_name,
		boolean 	defaultValue );
	
	public StringParameter
	addStringParameter2(
		String 		key,
		String 		resource_name,
		String	 	defaultValue );
	
	public IntParameter
	addIntParameter2(
		String 		key,
		String 		resource_name,
		int	 		defaultValue );
}
