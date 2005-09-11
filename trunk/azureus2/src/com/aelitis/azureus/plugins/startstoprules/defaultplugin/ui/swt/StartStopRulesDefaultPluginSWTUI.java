/*
 * Created on 11-Sep-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.plugins.startstoprules.defaultplugin.ui.swt;

import org.gudy.azureus2.plugins.PluginInterface;

public class 
StartStopRulesDefaultPluginSWTUI 
{
	public
	StartStopRulesDefaultPluginSWTUI(
		PluginInterface		plugin_interface )
	{
		plugin_interface.addConfigSection(new ConfigSectionQueue());
	    plugin_interface.addConfigSection(new ConfigSectionSeeding());
	    plugin_interface.addConfigSection(new ConfigSectionSeedingAutoStarting());
	    plugin_interface.addConfigSection(new ConfigSectionSeedingFirstPriority());
	    plugin_interface.addConfigSection(new ConfigSectionSeedingIgnore());
	}
}
