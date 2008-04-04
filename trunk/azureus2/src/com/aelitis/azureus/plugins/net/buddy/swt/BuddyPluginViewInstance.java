/*
 * Created on Apr 2, 2008
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


package com.aelitis.azureus.plugins.net.buddy.swt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;

import com.aelitis.azureus.plugins.net.buddy.BuddyPlugin;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBuddy;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginListener;

public class 
BuddyPluginViewInstance 
	implements BuddyPluginListener
{
	private static final int LOG_NORMAL 	= 1;
	private static final int LOG_SUCCESS 	= 2;
	private static final int LOG_ERROR 		= 3;


	private BuddyPlugin	plugin;

	private Composite	composite;
	private StyledText 	log;


	protected
	BuddyPluginViewInstance(
		BuddyPlugin		_plugin,
		Composite		_composite )
	{
		plugin		= _plugin;
		composite	= _composite;

		Composite main = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		main.setLayout(layout);
		GridData grid_data = new GridData(GridData.FILL_BOTH );
		main.setLayoutData(grid_data);

			// log area

		log = new StyledText(main,SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		grid_data = new GridData(GridData.FILL_BOTH);
		grid_data.horizontalSpan = 1;
		grid_data.horizontalIndent = 4;
		log.setLayoutData(grid_data);
		log.setIndent( 4 );

		print( "View initialised" );

		plugin.addListener( this );
		
		List buddies = plugin.getBuddies();

		for (int i=0;i<buddies.size();i++){

			buddyAdded((BuddyPluginBuddy)buddies.get(i));
		}
	}

	public void
	buddyAdded(
		BuddyPluginBuddy	buddy )
	{
		print( "Buddy added: " + buddy.getString());
	}

	public void
	buddyRemoved(
		BuddyPluginBuddy	buddy )
	{
		print( "Buddy removed: " + buddy.getString());
	}

	public void
	messageLogged(
		String		str )
	{
		print( str, LOG_NORMAL, false, false );
	}

	protected void
	print(
			String		str )
	{
		print( str, LOG_NORMAL, false, true );
	}

	protected void
	print(
		final String		str,
		final int			log_type,
		final boolean		clear_first,
		boolean				log_to_plugin )
	{
		if ( log_to_plugin ){

			plugin.log( str );
		}

		if ( !log.isDisposed()){

			final int f_log_type = log_type;

			log.getDisplay().asyncExec(
					new Runnable()
					{
						public void
						run()
						{
							if ( log.isDisposed()){

								return;
							}

							int	start;

							if ( clear_first ){

								start	= 0;

								log.setText( str + "\n" );

							}else{

								String	text = log.getText();
								
								start = text.length();

								if ( start > 32000 ){
									
									log.replaceTextRange( 0, 1024, "" );
									
									start = log.getText().length();
								}
								
								log.append( str + "\n" );
							}

							Color 	color;

							if ( f_log_type == LOG_NORMAL ){

								color = Colors.black;

							}else if ( f_log_type == LOG_SUCCESS ){

								color = Colors.green;

							}else{

								color = Colors.red;
							}

							if ( color != Colors.black ){
								
								StyleRange styleRange = new StyleRange();
								styleRange.start = start;
								styleRange.length = str.length();
								styleRange.foreground = color;
								log.setStyleRange(styleRange);
							}
							
							log.setSelection( log.getText().length());
						}
					});
		}
	}

	protected void
	destroy()
	{
		composite = null;
		
		plugin.removeListener( this );
	}
}
