/*
 * Created on Apr 26, 2008
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

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;

import com.aelitis.azureus.plugins.net.buddy.BuddyPlugin;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginAZ2;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginAZ2Listener;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBuddy;

public class 
BuddyPluginViewChat 
	implements BuddyPluginAZ2Listener
{
	public static final int CHAT_MSG_TYPE_TEXT	= 1;
	
	private BuddyPlugin			plugin;
	private LocaleUtilities		lu;
	
	private Shell 		shell;
	private StyledText 	log;
	private Table		buddy_table;
	
	private List		buddies = new ArrayList();
	
	protected
	BuddyPluginViewChat(
		BuddyPlugin			_plugin,
		Display 			_display,
		LocaleUtilities		_lu )
	{
		plugin	= _plugin;
		lu		= _lu;
		
		shell = new Shell( _display, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX );

		shell.setText( lu.getLocalisedMessageText( "azbuddy.chat.title" ));
				
		Utils.setShellIcon(shell);
		
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		shell.setLayout(layout);
		GridData grid_data = new GridData(GridData.FILL_BOTH );
		shell.setLayoutData(grid_data);

		
		log = new StyledText(shell,SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.NO_FOCUS );
		grid_data = new GridData(GridData.FILL_BOTH);
		grid_data.horizontalSpan = 1;
		grid_data.horizontalIndent = 4;
		grid_data.widthHint = 300;
		grid_data.heightHint = 400;
		log.setLayoutData(grid_data);
		log.setIndent( 4 );
		
		log.setEditable( false );

		Composite rhs = new Composite(shell, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		rhs.setLayout(layout);
		grid_data = new GridData(GridData.FILL_BOTH );
		grid_data.widthHint = 150;
		rhs.setLayoutData(grid_data);

			// table
		
		buddy_table = new Table(rhs, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL);

		String[] headers = { 
				"azbuddy.ui.table.name" };

		int[] sizes = { 150 };

		int[] aligns = { SWT.LEFT };

		for (int i = 0; i < headers.length; i++){

			TableColumn tc = new TableColumn(buddy_table, aligns[i]);
				
			tc.setWidth(sizes[i]);

			Messages.setLanguageText(tc, headers[i]);
		}	

	    buddy_table.setHeaderVisible(true);

	    grid_data = new GridData(GridData.FILL_BOTH);
	    grid_data.heightHint = buddy_table.getHeaderHeight() * 3;
		buddy_table.setLayoutData(grid_data);
		
		
		buddy_table.addListener(
			SWT.SetData,
			new Listener()
			{
				public void 
				handleEvent(
					Event event) 
				{
					TableItem item = (TableItem)event.item;
					
					int index = buddy_table.indexOf(item);
	
					if ( index < 0 || index >= buddies.size()){
						
						return;
					}
					
					BuddyPluginBuddy	buddy = (BuddyPluginBuddy)buddies.get(index);
					
					item.setText(0, buddy.getName());					
				}
			});
		
		
		
		
			// Text
		
		final Text text = new Text( shell, SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | SWT.BORDER);
		grid_data = new GridData(GridData.FILL_HORIZONTAL );
		grid_data.horizontalSpan = 2;
		grid_data.heightHint = 50;
		text.setLayoutData(grid_data);
		
		text.addKeyListener(
			new KeyListener()
			{
				public void 
				keyPressed(
					KeyEvent e) 
				{
					if ( e.keyCode == SWT.CR ){
				
						e.doit = false;
						
						sendMessage( text.getText());
						
						text.setText( "" );
					}
				}
				
				public void 
				keyReleased(
					KeyEvent e ) 
				{
				}
			});
		
		text.setFocus();
		
		shell.addListener(
			SWT.Traverse, 
			new Listener() 
			{	
				public void 
				handleEvent(
					Event e ) 
				{
					if ( e.character == SWT.ESC){
					
						close();
				}
			}
		});
		
		plugin.getAZ2Handler().addListener( this );
		
	    shell.pack();
	    Utils.createURLDropTarget(shell, text);
	    Utils.centreWindow(shell);
	    shell.open();
	}
	
	protected void
	updateTable()
	{
		buddy_table.setItemCount( buddies.size());
		buddy_table.clearAll();
		buddy_table.redraw();
	}
	
	protected void
	close()
	{
		plugin.getAZ2Handler().removeListener( this );
		
		shell.dispose();
	}
	
	protected void
	addBuddy(
		BuddyPluginBuddy		buddy )
	{
		synchronized( buddies ){
			
			buddies.add( buddy );
		}
		
		if ( !buddy_table.isDisposed()){

			buddy_table.getDisplay().asyncExec(
					new Runnable()
					{
						public void
						run()
						{
							if ( buddy_table.isDisposed()){

								return;
							}
							
							updateTable();
						}
					});
		}					
	}
	
	protected void
	sendMessage(
		String		text )
	{
		logChatMessage( plugin.getMyNick(), Colors.green, text );
		
		Map	msg = new HashMap();
		
		msg.put( "type", new Long( CHAT_MSG_TYPE_TEXT ));
		
		msg.put( "line", text );
		
		for (int i=0;i<buddies.size();i++){
			
			plugin.getAZ2Handler().sendAZ2Chat((BuddyPluginBuddy)buddies.get(i), msg );
		}
	}
	
	public void
	messageReceived(
		final BuddyPluginBuddy		buddy,
		final int					type,
		final Map					msg )
	{
		if ( type != BuddyPluginAZ2.RT_AZ2_REQUEST_CHAT ){
			
			return;
		}
		
		if ( !buddies.contains( buddy )){
			
			return;
		}
		
		if ( !log.isDisposed()){

			log.getDisplay().asyncExec(
					new Runnable()
					{
						public void
						run()
						{
							if ( log.isDisposed()){

								return;
							}
							
							int	type = ((Long)msg.get( "type")).intValue();
							
							if ( type == CHAT_MSG_TYPE_TEXT ){
							
								byte[]	line = (byte[])msg.get( "line" );
								
								try{
									logChatMessage( buddy.getNickName(), Colors.blue,new String( line, "UTF-8" ));
									
								}catch( Throwable e ){
									
									Debug.printStackTrace(e);
								}
							}
						}
					});
		}
	}
	
	protected void
	logChatMessage(
		String		buddy_name,
		Color 		colour,
		String		msg )
	{
		if ( buddy_name.length() > 32 ){
			
			buddy_name = buddy_name.substring(0,16) + "...";
		}
		
		int	start = log.getText().length();
		
		String says = lu.getLocalisedMessageText( "azbuddy.chat.says", new String[]{ buddy_name }) + "\n";
		
		log.append( says ); 
		
		if ( colour != Colors.black ){
			
			StyleRange styleRange = new StyleRange();
			styleRange.start = start;
			styleRange.length = says.length();
			styleRange.foreground = colour;
			log.setStyleRange(styleRange);
		}
		
		log.append( msg + "\n" ); 

		log.setSelection( log.getText().length());
	}
}
