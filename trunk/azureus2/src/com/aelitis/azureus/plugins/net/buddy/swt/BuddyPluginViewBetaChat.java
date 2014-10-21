/*
 * Created on Apr 26, 2008
 * Created by Paul Gardner
 * 
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.pluginsimpl.local.utils.FormattersImpl;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.BufferedLabel;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;

import com.aelitis.azureus.plugins.net.buddy.BuddyPlugin;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBeta;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBuddy;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBeta.*;

public class 
BuddyPluginViewBetaChat 
	implements ChatListener
{
	private BuddyPlugin			plugin;
	private ChatInstance		chat;
	
	private LocaleUtilities		lu;
	
	private Shell 					shell;
	private StyledText 				log;
	private Table					buddy_table;
	private BufferedLabel		 	status;
	
	private Button 			shared_nick_button;
	private Text 			nickname;
	
	private Text 		input_area;
	
	private List<ChatMessage>			messages		= new ArrayList<ChatMessage>();
	private List<ChatParticipant>		participants 	= new ArrayList<ChatParticipant>();
	
	private Map<ChatParticipant,ChatMessage>	participant_last_message_map = new HashMap<ChatParticipant, ChatMessage>();
	
	private boolean		table_resort_required;
	
	protected
	BuddyPluginViewBetaChat(
		BuddyPlugin		_plugin,
		Display 		_display,
		ChatInstance	_chat )
	{
		plugin	= _plugin;
		chat	= _chat;
		
		lu		= plugin.getPluginInterface().getUtilities().getLocaleUtilities();
		
		shell = ShellFactory.createMainShell( SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX );

		shell.addDisposeListener(
			new DisposeListener()
			{
				public void 
				widgetDisposed(
					DisposeEvent arg0 ) 
				{
					closed();
				}
			});
		
		shell.setText( lu.getLocalisedMessageText( "azbuddy.chat.title" ) + ": " + chat.getName());
				
		Utils.setShellIcon(shell);
		
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		shell.setLayout(layout);
		GridData grid_data = new GridData(GridData.FILL_BOTH );
		shell.setLayoutData(grid_data);

		Composite lhs = new Composite(shell, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		lhs.setLayout(layout);
		grid_data = new GridData(GridData.FILL_BOTH );
		grid_data.widthHint = 300;
		lhs.setLayoutData(grid_data);
		
		Composite temp = new Composite(lhs, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		temp.setLayout(layout);
		grid_data = new GridData(GridData.FILL_HORIZONTAL);
		grid_data.horizontalIndent = 8;
		grid_data.heightHint = 20;
		temp.setLayoutData(grid_data);
		
		status = new BufferedLabel( temp, SWT.LEFT | SWT.DOUBLE_BUFFERED );
		grid_data = new GridData(GridData.FILL_BOTH);
		
		status.setLayoutData(grid_data);
		status.setText( "Pending" );
		
		Composite log_holder = new Composite(lhs, SWT.BORDER);
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.marginLeft = 4;
		log_holder.setLayout(layout);
		grid_data = new GridData(GridData.FILL_BOTH );
		log_holder.setLayoutData(grid_data);
		
		log = new StyledText(log_holder,SWT.READ_ONLY | SWT.V_SCROLL | SWT.WRAP | SWT.NO_FOCUS );
		grid_data = new GridData(GridData.FILL_BOTH);
		grid_data.horizontalSpan = 1;
		//grid_data.horizontalIndent = 4;
		log.setLayoutData(grid_data);
		//log.setIndent( 4 );
		
		log.setEditable( false );

		log_holder.setBackground( log.getBackground());
		
		Composite rhs = new Composite(shell, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		rhs.setLayout(layout);
		grid_data = new GridData(GridData.FILL_VERTICAL );
		grid_data.widthHint = 150;
		rhs.setLayoutData(grid_data);

			// options
		
		Composite top_right = new Composite(rhs, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		top_right.setLayout(layout);
		grid_data = new GridData( GridData.FILL_HORIZONTAL );
		grid_data.heightHint = 50;
		top_right.setLayoutData(grid_data);
		
		Label label = new Label( top_right, SWT.NULL );
		
		label.setText( "Nickname: shared" );

		shared_nick_button = new Button( top_right, SWT.CHECK );
		
		shared_nick_button.setSelection( chat.isSharedNickname());
		
		nickname = new Text( top_right, SWT.BORDER );
		grid_data = new GridData( GridData.FILL_HORIZONTAL );
		grid_data.horizontalSpan = 2;
		nickname.setLayoutData( grid_data );
		
		nickname.setText( chat.getNickname());

		shared_nick_button.addSelectionListener(
			new SelectionAdapter() 
			{
				public void widgetSelected(SelectionEvent arg0) {
					
					boolean shared = shared_nick_button.getSelection();
					
					chat.setSharedNickname( shared );
				}
			});
		
		nickname.addListener(SWT.FocusOut, new Listener() {
	        public void handleEvent(Event event) {
	        	String nick = nickname.getText().trim();
	        	
	        	if ( chat.isSharedNickname()){
	        		
	        		if ( chat.getNetwork() == AENetworkClassifier.AT_PUBLIC ){
	        		
	        			plugin.getBeta().setSharedPublicNickname( nick );
	        			
	        		}else{
	        			
	        			plugin.getBeta().setSharedAnonNickname( nick );
	        		}
	        	}else{
	        		
	        		chat.setInstanceNickname( nick );
	        	}
	        }
	    });
		
			// table
		
		buddy_table = new Table(rhs, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL);

		String[] headers = { 
				"azbuddy.ui.table.name" };

		int[] sizes = { 120 };

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
	
					if ( index < 0 || index >= participants.size()){
						
						return;
					}
					
					ChatParticipant	participant = (BuddyPluginBeta.ChatParticipant)participants.get(index);
					
					item.setData( participant );
					
					item.setText(0, participant.getName());		
					
					setProperties( item, participant );
				}
			});
		
		final Menu menu = new Menu(buddy_table);
		
		buddy_table.setMenu( menu );
		
		menu.addMenuListener(
			new MenuListener() 
			{
				public void 
				menuShown(
					MenuEvent e ) 
				{
					MenuItem[] items = menu.getItems();
					
					for (int i = 0; i < items.length; i++){
						
						items[i].dispose();
					}

					final TableItem[] selection = buddy_table.getSelection();
					
					boolean	can_ignore 	= false;
					boolean	can_listen	= false;
					boolean	can_pin		= false;
					boolean	can_unpin	= false;
					
					for (int i=0;i<selection.length;i++){
						
						ChatParticipant	participant = (ChatParticipant)selection[i].getData();
						
						if ( participant.isIgnored()){
						
							can_listen = true;
							
						}else{
							
							can_ignore = true;
						}
						
						if ( participant.isPinned()){
							
							can_unpin = true;
							
						}else{
							
							can_pin = true;
						}
					}
					
					final MenuItem ignore_item = new MenuItem(menu, SWT.PUSH);
					
					ignore_item.setText( "Mute" );

					ignore_item.addSelectionListener(
						new SelectionAdapter() 
						{
							public void 
							widgetSelected(
								SelectionEvent e) 
							{
								for (int i=0;i<selection.length;i++){
									
									ChatParticipant	participant = (ChatParticipant)selection[i].getData();
									
									if ( !participant.isIgnored()){
										
										participant.setIgnored( true );
										
										setProperties( selection[i], participant );
									}
								}
							};
						});
					
					ignore_item.setEnabled( can_ignore );
					
					final MenuItem listen_item = new MenuItem(menu, SWT.PUSH);
					
					listen_item.setText( "Listen" );

					listen_item.addSelectionListener(
						new SelectionAdapter() 
						{
							public void 
							widgetSelected(
								SelectionEvent e) 
							{
								for (int i=0;i<selection.length;i++){
									
									ChatParticipant	participant = (ChatParticipant)selection[i].getData();
									
									if ( participant.isIgnored()){
										
										participant.setIgnored( false );
										
										setProperties( selection[i], participant );
									}
								}
							};
						});
					
					listen_item.setEnabled( can_listen );
					
					new MenuItem(menu, SWT.SEPARATOR );
					
					final MenuItem pin_item = new MenuItem(menu, SWT.PUSH);
					
					pin_item.setText( "Pin" );

					pin_item.addSelectionListener(
						new SelectionAdapter() 
						{
							public void 
							widgetSelected(
								SelectionEvent e) 
							{
								for (int i=0;i<selection.length;i++){
									
									ChatParticipant	participant = (ChatParticipant)selection[i].getData();
									
									if ( !participant.isPinned()){
										
										participant.setPinned( true );
										
										setProperties( selection[i], participant );
									}
								}
							};
						});
					
					pin_item.setEnabled( can_pin );
					
					final MenuItem unpin_item = new MenuItem(menu, SWT.PUSH);
					
					unpin_item.setText( "Unpin" );

					unpin_item.addSelectionListener(
						new SelectionAdapter() 
						{
							public void 
							widgetSelected(
								SelectionEvent e) 
							{
								for (int i=0;i<selection.length;i++){
									
									ChatParticipant	participant = (ChatParticipant)selection[i].getData();
									
									if ( participant.isPinned()){
										
										participant.setPinned( false );
										
										setProperties( selection[i], participant );
									}
								}
							};
						});
					
					unpin_item.setEnabled( can_unpin );
				}
				
				public void menuHidden(MenuEvent e) {
				}
			});
		
	
		
			// Text
		
		input_area = new Text( shell, SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | SWT.BORDER);
		grid_data = new GridData(GridData.FILL_HORIZONTAL );
		grid_data.horizontalSpan = 2;
		grid_data.heightHint = 50;
		input_area.setLayoutData(grid_data);
			
		input_area.setTextLimit( 256 );
		
		input_area.addKeyListener(
			new KeyListener()
			{
				public void 
				keyPressed(
					KeyEvent e) 
				{
					if ( e.keyCode == SWT.CR ){
				
						e.doit = false;
						
						sendMessage( input_area.getText());
						
						input_area.setText( "" );
					}
				}
				
				public void 
				keyReleased(
					KeyEvent e ) 
				{
				}
			});
		
		input_area.setFocus();
		
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
		
		BuddyPluginBeta.ChatParticipant[] existing_participants = chat.getParticipants();
		
		synchronized( participants ){
			
			participants.addAll( Arrays.asList( existing_participants ));
		}
		
		table_resort_required = true;
		
		updateTable( false );
		
		BuddyPluginBeta.ChatMessage[] history = chat.getHistory();
		
		for (int i=0;i<history.length;i++){
			
			logChatMessage( history[i] );
		}
		
		chat.addListener( this );
		
	    shell.setSize( 400, 500 );
	    
	    Utils.createURLDropTarget(shell, input_area);
	    Utils.centreWindow(shell);
	    shell.open();
	}
	
	private void
	setProperties(
		TableItem			item,
		ChatParticipant		p )
	{
		if ( p.isIgnored()){
		
			item.setForeground( 0, Colors.grey );
			
		}else{
			
			if ( p.isPinned()){
			
				item.setForeground( 0, Colors.fadedGreen );
				
			}else{
			
				if ( p.hasNickname()){
					
					item.setForeground( 0, Colors.blues[Colors.FADED_DARKEST] );
					
				}else{
					
					item.setForeground( 0, Colors.black );
				}
			}
		}
	}
	
	protected void
	addDisposeListener(
		final DisposeListener	listener )
	{
		if ( shell.isDisposed()){
			
			listener.widgetDisposed( null );
			
		}else{
								
			shell.addDisposeListener( listener );
		}
	}
	
	protected void
	updateTable(
		boolean	async )
	{
		if ( async ){
			
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
							
							updateTable( false );
						}
					});
			}					
		}else{
			
			if ( table_resort_required ){
				
				table_resort_required = false;
				
				sortParticipants();
			}
			
			buddy_table.setItemCount( participants.size());
			buddy_table.clearAll();
			buddy_table.redraw();
		}
	}
	
	protected void
	close()
	{
		shell.dispose();
	}
	
	protected void
	closed()
	{
		chat.removeListener( this );
		
		chat.destroy();
	}
	
	public void 
	stateChanged(
		final boolean avail ) 
	{
		if ( buddy_table.isDisposed()){
			
			return;
		}
	
		buddy_table.getDisplay().asyncExec(
			new Runnable()
			{
				public void
				run()
				{
					if ( buddy_table.isDisposed()){
						
						return;
					}
					
					input_area.setEnabled( avail );
				}
			});
	}
	
	public void 
	updated() 
	{
		if ( status.isDisposed()){
			
			return;
		}
	
		status.getControl().getDisplay().asyncExec(
			new Runnable()
			{
				public void
				run()
				{
					if ( status.isDisposed()){
						
						return;
					}
				
					status.setText( chat.getStatus());
					
					boolean	is_shared = chat.isSharedNickname();
					
					if ( is_shared != shared_nick_button.getSelection()){
						
						shared_nick_button.setSelection( is_shared );
					}
						
					if ( !nickname.isFocusControl()){
						
						String nick = nickname.getText().trim();
							
						if ( !chat.getNickname().equals( nick )){
								
							nickname.setText( chat.getNickname());
						}
					}
					
					if ( table_resort_required ){
						
						updateTable( false );
					}
				}
			});
	}
	
	private void
	sortParticipants()
	{
		Collections.sort(
			participants,
			new Comparator<ChatParticipant>()
			{
				private Comparator<String> comp = new FormattersImpl().getAlphanumericComparator( true );
				
				public int 
				compare(
					ChatParticipant p1, 
					ChatParticipant p2 ) 
				{
					boolean	b_p1 = p1.hasNickname();
					boolean	b_p2 = p2.hasNickname();
					
					if ( b_p1 == b_p2 ){
					
						return( comp.compare( p1.getName(), p2.getName()));
						
					}else if ( b_p1 ){
						
						return( -1 );
						
					}else{
						
						return( 1 );
					}
				}
			});
	}
	
	public void
	participantAdded(
		ChatParticipant		participant )
	{
		synchronized( participants ){
			
			participants.add( participant );
			
			table_resort_required = true;
		}
		
		updateTable( true );
	}
	
	public void
	participantChanged(
		final ChatParticipant		participant )
	{
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
						
						TableItem[] items = buddy_table.getItems();
						
						String	name = participant.getName();
						
						for ( TableItem item: items ){
							
							if ( item.getData() == participant ){
								
								String old_name = item.getText(0);
								
								if ( !old_name.equals( name )){
								
									item.setText( 0, name );
									
									table_resort_required = true;
								}
							}
						}
					}
				});
		}			
	}
	
	public void
	participantRemoved(
		ChatParticipant		participant )
	{
		synchronized( participants ){
			
			participants.remove( participant );
			
			participant_last_message_map.remove( participant );
		}
		
		updateTable( true );
	}
	
	protected void
	sendMessage(
		String		text )
	{
		//logChatMessage( plugin.getNickname(), Colors.green, text );
		
		chat.sendMessage( text );
	}
	
	public void
	messageReceived(
		final ChatMessage	message )
	{
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
													
						logChatMessage( message );
					}
				});
		}
	}
	
	public void 
	messagesChanged() 
	{
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
						
						try{								
							resetChatMessages();
							
							BuddyPluginBeta.ChatMessage[] history = chat.getHistory();
															
							logChatMessages( history );
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
						}
					}
				});
		}
	}
	
	private void
	resetChatMessages()
	{
		log.setText( "" );
		
		messages.clear();
	}
	
	private void
	logChatMessage(
		ChatMessage		message )
	{
		logChatMessages( new ChatMessage[]{ message } );
	}
	
	private void
	logChatMessages(
		ChatMessage[]		all_messages )
	{
		boolean	changed = false;
		
		for ( ChatMessage message: all_messages ){
			
			if ( messages.contains( message )){
				
				return;
			}
			
			messages.add( message );
			
			if ( !message.isIgnored()){
				
				changed = true;
				
				String	nick 	= message.getNickName();
				String	msg		= message.getMessage();
				
				boolean	is_error = message.isError();
				
				ChatParticipant participant = message.getParticipant();
				
				Color colour = Colors.blues[Colors.FADED_DARKEST];
				
				if ( is_error ){
					
					colour = Colors.red;		
					
				}else if ( participant.isPinned()){
					
					colour = Colors.fadedGreen;
				}
				
				long time = message.getTimeStamp();
				
				String stamp = new SimpleDateFormat( "HH:mm" ).format( new Date( time ));
				
				ChatMessage	last_message;
				
				String says = stamp + " " + (nick.length()>20?(nick.substring(0,16) + "..."):nick);

				synchronized( participants ){
					
					last_message = participant_last_message_map.get( participant );
					
					participant_last_message_map.put( participant, message );
				}
				
				if ( last_message != null ){
					
					String last_nick = last_message.getNickName();
					
					if ( !nick.equals(last_nick)){
						
						says += " (was " + (last_nick.length()>20?(last_nick.substring(0,16) + "..."):last_nick) + ")";
					}
				}
				
				says += "\n";
				
				int	start = log.getText().length();
						
				log.append( says ); 
				
				if ( colour != Colors.black ){
					
					StyleRange styleRange = new StyleRange();
					styleRange.start = start;
					styleRange.length = says.length();
					styleRange.foreground = colour;
					log.setStyleRange(styleRange);
				}
				
				log.append( msg + "\n" ); 
			}
		}

		if ( changed ){
		
			log.setSelection( log.getText().length());
		}
	}
}
