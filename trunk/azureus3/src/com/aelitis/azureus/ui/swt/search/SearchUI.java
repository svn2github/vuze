/*
 * Created on Dec 14, 2016
 * Created by Paul Gardner
 * 
 * Copyright 2016 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or 
 * (at your option) any later version.
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


package com.aelitis.azureus.ui.swt.search;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.utils.DelayedTask;
import org.gudy.azureus2.plugins.utils.Utilities;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.core.metasearch.MetaSearchManager;
import com.aelitis.azureus.core.metasearch.MetaSearchManagerFactory;

public class 
SearchUI 
{
	private static final String CONFIG_SECTION_ID 	= "Search";

	private UIManager ui_manager;
	
	public
	SearchUI()
	{
		final PluginInterface	default_pi = PluginInitializer.getDefaultInterface();

		ui_manager = default_pi.getUIManager();
	
		ui_manager.addUIListener(
			new UIManagerListener()
			{
				public void
				UIAttached(
					UIInstance		instance )
				{
					if (!( instance instanceof UISWTInstance )){
						return;

					}
					
					Utilities utilities = default_pi.getUtilities();

					final DelayedTask dt = utilities.createDelayedTask(new Runnable()
					{
						public void 
						run() 
						{
							Utils.execSWTThread(new AERunnable() {

								public void 
								runSupport() 
								{
									delayedInit();
								}
							});
						}
					});

					dt.queue();		
				}

				public void UIDetached(UIInstance instance) {
				}
			});
	}
	
	private void 
	delayedInit() 
	{
		final MetaSearchManager manager = MetaSearchManagerFactory.getSingleton();
		
		if ( manager == null ){
			
			return;
		}
		
		BasicPluginConfigModel configModel = ui_manager.createBasicPluginConfigModel(
				ConfigSection.SECTION_ROOT, CONFIG_SECTION_ID);

			// proxy enable
		
		final BooleanParameter proxy_enable = 
			configModel.addBooleanParameter2( 
				"search.proxy.enable", "search.proxy.enable",
				manager.getProxyRequestsEnabled());
		
		proxy_enable.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param) 
				{
					manager.setProxyRequestsEnabled( proxy_enable.getValue());
				}
			});
	}
}
