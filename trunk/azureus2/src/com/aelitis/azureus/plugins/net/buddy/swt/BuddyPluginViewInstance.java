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

import java.text.SimpleDateFormat;
import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;

import com.aelitis.azureus.plugins.net.buddy.BuddyPlugin;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBuddy;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBuddyReplyListener;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBuddyRequestListener;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginException;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginListener;

public class 
BuddyPluginViewInstance 
	implements BuddyPluginListener, BuddyPluginBuddyRequestListener
{
	private static final int LOG_NORMAL 	= 1;
	private static final int LOG_SUCCESS 	= 2;
	private static final int LOG_ERROR 		= 3;


	private BuddyPlugin	plugin;

	private Composite	composite;
	private Table 		buddy_table;
	private StyledText 	log;

	private List	buddies = new ArrayList();

	private boolean	init_complete;
	
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

		final Composite form = new Composite(main, SWT.NONE);
		FormLayout flayout = new FormLayout();
		flayout.marginHeight = 0;
		flayout.marginWidth = 0;
		form.setLayout(flayout);
		GridData gridData;
		gridData = new GridData(GridData.FILL_BOTH);
		form.setLayoutData(gridData);


		final Composite child1 = new Composite(form,SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		child1.setLayout(layout);

		final Sash sash = new Sash(form, SWT.HORIZONTAL);
	
		final Composite child2 = new Composite(form,SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		child2.setLayout(layout);

		FormData formData;

			// child1
		
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.top = new FormAttachment(0, 0);
		child1.setLayoutData(formData);

		final FormData child1Data = formData;
		
		final int SASH_WIDTH = 4;
		
			// sash
		
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.top = new FormAttachment(child1);
		formData.height = SASH_WIDTH;
		sash.setLayoutData(formData);

			// child2
		
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.bottom = new FormAttachment(100, 0);
		formData.top = new FormAttachment(sash);
		child2.setLayoutData(formData);

		final PluginConfig pc = plugin.getPluginInterface().getPluginconfig();
		
		sash.setData( "PCT", new Float( pc.getPluginFloatParameter( "swt.sash.position", 0.7f )));
		
		sash.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent e ) 
				{
					if (e.detail == SWT.DRAG){
						return;
					}
					
					child1Data.height = e.y + e.height - SASH_WIDTH;
					
					form.layout();
	
					Float l = new Float((double)child1.getBounds().height / form.getBounds().height);
					
					sash.setData( "PCT", l );
					
					pc.setPluginParameter( "swt.sash.position", l.floatValue());
				}
			});

		form.addListener(
			SWT.Resize, 
			new Listener() 
			{
				public void 
				handleEvent(Event e) 
				{
					Float l = (Float) sash.getData( "PCT" );
					
					if ( l != null ){
						
						child1Data.height = (int) (form.getBounds().height * l.doubleValue());
					
						form.layout();
					}
				}
			});
			
			// table
		
		buddy_table = new Table(child1, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL);

		String[] headers = { "buddy.name", "buddy.online", "buddy.last_ygm" };

		int[] sizes = { 400, 100, 100 };

		int[] aligns = { SWT.LEFT, SWT.CENTER, SWT.CENTER };

		for (int i = 0; i < headers.length; i++){

			TableColumn tc = new TableColumn(buddy_table, aligns[i]);

			tc.setWidth(sizes[i]);

			Messages.setLanguageText(tc, headers[i]);
		}	

	    buddy_table.setHeaderVisible(true);

	    gridData = new GridData(GridData.FILL_BOTH);
	    gridData.heightHint = buddy_table.getHeaderHeight() * 3;
		buddy_table.setLayoutData(gridData);
		
		
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
					
					item.setText(1, buddy.isOnline()?"yes":"no");
					
					long	last_ygm = buddy.getLastMessagePending();;
					
					item.setText(2, last_ygm==0?"":new SimpleDateFormat().format(new Date( last_ygm )));
					
					item.setData( buddy );
				}
			});
		
		Menu menu = new Menu(buddy_table);
		
		MenuItem remove_item = new MenuItem(menu, SWT.PUSH);
		
		remove_item.setText( "Remove" );

		remove_item.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent e) 
				{
					TableItem[] selection = buddy_table.getSelection();
					
					for (int i=0;i<selection.length;i++){
						
						BuddyPluginBuddy buddy = (BuddyPluginBuddy)selection[i].getData();
						
						buddy.remove();
					}
				};
			});
		
		MenuItem send_msg_item = new MenuItem(menu, SWT.PUSH);

		send_msg_item.setText( "Send Message" );

		send_msg_item.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent event ) 
				{
					TableItem[] selection = buddy_table.getSelection();
					
					String	msg = "Hello @ " + new SimpleDateFormat().format(new Date());
					
					for (int i=0;i<selection.length;i++){
						
						BuddyPluginBuddy buddy = (BuddyPluginBuddy)selection[i].getData();
						
						plugin.getAZ2Handler().sendAZ2Message( buddy, msg );
					}
				};
			});
		
		MenuItem ping_item = new MenuItem(menu, SWT.PUSH);

		ping_item.setText( "Ping" );

		ping_item.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent event ) 
				{
					TableItem[] selection = buddy_table.getSelection();
					
					for (int i=0;i<selection.length;i++){
						
						BuddyPluginBuddy buddy = (BuddyPluginBuddy)selection[i].getData();
						
						try{
						
							buddy.ping();
							
						}catch( Throwable e ){
							
							print( "Ping failed", e );
						}
					}
				};
			});
		
		MenuItem ygm_item = new MenuItem(menu, SWT.PUSH);

		ygm_item.setText( "Set YGM" );

		ygm_item.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent event ) 
				{
					TableItem[] selection = buddy_table.getSelection();
					
					for (int i=0;i<selection.length;i++){
						
						BuddyPluginBuddy buddy = (BuddyPluginBuddy)selection[i].getData();
						
						try{
							buddy.setMessagePending();
							
						}catch( Throwable e ){
							
							print( "YGM failed", e );
						}
					}
				};
			});
		
		buddy_table.setMenu( menu );
		
			// log area

		log = new StyledText(child2,SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		grid_data = new GridData(GridData.FILL_BOTH);
		grid_data.horizontalSpan = 1;
		grid_data.horizontalIndent = 4;
		log.setLayoutData(grid_data);
		log.setIndent( 4 );

		buddies = plugin.getBuddies();
		
		plugin.addListener( this );
		
		plugin.addRequestListener( this );
		
		init_complete	= true;
	    
		updateTable();
	}

	protected void
	updateTable()
	{
		if ( init_complete ){
			
			buddy_table.setItemCount( buddies.size());
			buddy_table.clearAll();
			buddy_table.redraw();
		}
	}
	
	public void 
	initialised(
		boolean available ) 
	{
		print( "Initialisation complete: available=" + available );
	}
	
	public void
	buddyAdded(
		final BuddyPluginBuddy	buddy )
	{
		buddy_table.getDisplay().syncExec(
				new Runnable()
				{
					public void
					run()
					{
						if ( !buddy_table.isDisposed()){
							
							if ( !buddies.contains( buddy )){
								
								buddies.add( buddy );
								
								updateTable();
							}
						}
					}
				});
	}

	public void
	buddyRemoved(
		final BuddyPluginBuddy	buddy )
	{
		buddy_table.getDisplay().syncExec(
				new Runnable()
				{
					public void
					run()
					{
						if ( !buddy_table.isDisposed()){
							
							if ( buddies.remove( buddy )){
																
								updateTable();
							}
						}
					}
				});	
	}

	public void
	buddyChanged(
		final BuddyPluginBuddy	buddy )
	{
		buddy_table.getDisplay().syncExec(
				new Runnable()
				{
					public void
					run()
					{
						if ( !buddy_table.isDisposed()){
																							
							updateTable();
						}
					}
				});	
	}
	
	public void
	messageLogged(
		String		str )
	{
		print( str, LOG_NORMAL, false, false );
	}

	public Map
	requestReceived(
		BuddyPluginBuddy	from_buddy,
		int					subsystem,
		Map					request )
	
		throws BuddyPluginException
	{
		return( null );
	}
	
	public void
	pendingMessages(
		BuddyPluginBuddy[]	from_buddies )
	{
		String	str = "";
		
		for (int i=0;i<from_buddies.length;i++){
			
			str += (str.length()==0?"":",") + from_buddies[i].getName();
		}
		
		print( "YGM received: " + str );
	}
	
	protected void
	print(
		String		str,
		Throwable	e )
	{
		print( str + ": " + Debug.getNestedExceptionMessage( e ));
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
		
		plugin.removeRequestListener( this );

	}
}
