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

package org.gudy.azureus2.pluginsimpl.update;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.ui.model.*;

public class 
PluginUpdatePlugin
	implements Plugin
{
	protected PluginInterface		plugin_interface;
	protected LoggerChannel 		log;
	
	public void
	initialize(
		PluginInterface	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		log = plugin_interface.getLogger().getChannel("Plugin Update");

		final BasicPluginViewModel model = 
			plugin_interface.getUIManager().getBasicPluginViewModel( 
					"Plugin Update");
		
		model.getStatus().setText( "Running" );
		model.getActivity().setVisible( false );
		model.getProgress().setVisible( false );
		
		log.addListener(
			new LoggerChannelListener()
			{
				public void
				messageLogged(
					int		type,
					String	message )
				{
					model.getLogArea().setText( model.getLogArea().getText()+message+"\n");
				}
				
				public void
				messageLogged(
					String		str,
					Throwable	error )
				{
					model.getLogArea().setText( model.getLogArea().getText()+error.toString()+"\n");
				}
			});
		
		PluginView view = plugin_interface.getUIManager().createPluginView( model );
		
		plugin_interface.addView( view );	
		
		Thread t = new Thread("PluginUpdate" )
			{
				public void
				run()
				{
					updater();
				}
			};
			
		t.setDaemon(true);
		
		t.start();
	}
	
	protected void
	updater()
	{
		PluginInterface[]	plugins = plugin_interface.getPluginManager().getPlugins();
		
		log.log( LoggerChannel.LT_INFORMATION, "Currently loaded plugins:");

		for (int i=0;i<plugins.length;i++){
			
			PluginInterface	pi = plugins[i];
			
			String	version = pi.getPluginVersion();
			
			log.log( LoggerChannel.LT_INFORMATION, "    " + pi.getPluginName() + (version==null?"":(", version = " + pi.getPluginVersion())));
		}
	}
}
