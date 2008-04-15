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
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SHA1Simple;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.ui.UIInputReceiver;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.Cursors;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;

import com.aelitis.azureus.core.security.CryptoHandler;
import com.aelitis.azureus.core.security.CryptoManager;
import com.aelitis.azureus.core.security.CryptoManagerFactory;
import com.aelitis.azureus.core.security.CryptoManagerKeyChangeListener;
import com.aelitis.azureus.plugins.net.buddy.BuddyPlugin;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBuddy;
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
	private UIInstance	ui_instance;
	private Composite	composite;
	private Table 		buddy_table;
	private StyledText 	log;

	private List	buddies = new ArrayList();

	private boolean	init_complete;
	
	private String	yes_txt;
	private String	no_txt;
	
	protected
	BuddyPluginViewInstance(
		BuddyPlugin		_plugin,
		UIInstance		_ui_instance,
		Composite		_composite )
	{
		plugin		= _plugin;
		ui_instance	= _ui_instance;
		composite	= _composite;

		final LocaleUtilities lu = plugin.getPluginInterface().getUtilities().getLocaleUtilities();
	
		yes_txt = lu.getLocalisedMessageText( "GeneralView.yes" );
		no_txt 	= lu.getLocalisedMessageText( "GeneralView.no" );
		
		Composite main = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		main.setLayout(layout);
		GridData grid_data = new GridData(GridData.FILL_BOTH );
		main.setLayoutData(grid_data);

		if ( !plugin.isEnabled()){
			
			Label control_label = new Label( main, SWT.NULL );
			control_label.setText( lu.getLocalisedMessageText( "azbuddy.disabled" ));

			return;
		}
			// control area
		
		Composite controls = new Composite(main, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 5;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		controls.setLayout(layout);
		grid_data = new GridData(GridData.FILL_HORIZONTAL );
		controls.setLayoutData(grid_data);
		
		Label control_label = new Label( controls, SWT.NULL );
		control_label.setText( lu.getLocalisedMessageText( "azbuddy.ui.new_buddy" ) + " " );
		
		final Text control_text = new Text( controls, SWT.BORDER );
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		control_text.setLayoutData(gridData);
	
		final Button control_button = new Button( controls, SWT.NULL );
		control_button.setText( lu.getLocalisedMessageText( "azbuddy.ui.add" ));
		
		control_button.setEnabled( false );
		
		control_text.addModifyListener(
			new ModifyListener() {
	        	public void 
	        	modifyText(
	        		ModifyEvent e ) 
	        	{					
					control_button.setEnabled( plugin.verifyPublicKey( control_text.getText().trim())); 
	        	}
	        });
		
		control_button.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent e )
				{
					plugin.addBuddy( control_text.getText().trim());
				}
			});
		
		final Label control_lab_pk = new Label( controls, SWT.NULL );
		control_lab_pk.setText( lu.getLocalisedMessageText( "azbuddy.ui.mykey" ) + " ");

		final Label control_val_pk = new Label( controls, SWT.NULL );
		gridData = new GridData();
		gridData.widthHint = 400;
		
		control_val_pk.setLayoutData(gridData);

    	final CryptoManager crypt_man = CryptoManagerFactory.getSingleton();

		byte[]	public_key = crypt_man.getECCHandler().peekPublicKey( null );
		  
		if ( public_key == null ){
			
		    Messages.setLanguageText(control_val_pk, "ConfigView.section.security.publickey.undef");

		}else{
		    			    			    
			control_val_pk.setText( Base32.encode( public_key ));
		}
		
	    Messages.setLanguageText(control_val_pk, "ConfigView.copy.to.clipboard.tooltip", true);

	    control_val_pk.setCursor(Cursors.handCursor);
	    control_val_pk.setForeground(Colors.blue);
	    control_val_pk.addMouseListener(new MouseAdapter() {
	    	public void mouseDoubleClick(MouseEvent arg0) {
	    		copyToClipboard();
	    	}
	    	public void mouseDown(MouseEvent arg0) {
	    		copyToClipboard();
	    	}
	    	protected void
	    	copyToClipboard()
	    	{
    			new Clipboard(control_val_pk.getDisplay()).setContents(new Object[] {control_val_pk.getText()}, new Transfer[] {TextTransfer.getInstance()});
	    	}
	    });
		
		crypt_man.addKeyChangeListener(
				new CryptoManagerKeyChangeListener()
				{
					public void 
					keyChanged(
						CryptoHandler handler ) 
					{
						if ( control_val_pk.isDisposed()){
							
							crypt_man.removeKeyChangeListener( this );
							
						}else{
							if ( handler.getType() == CryptoManager.HANDLER_ECC ){
								
								byte[]	public_key = handler.peekPublicKey( null );

								if ( public_key == null ){
									
										// shouldn't happen...
									
									 Messages.setLanguageText(control_val_pk, "ConfigView.section.security.publickey.undef");
									
								}else{
									
									control_val_pk.setText( Base32.encode( public_key ));
								}
							}
						}
					}
				});
		
			// table and log
		
		final Composite form = new Composite(main, SWT.NONE);
		FormLayout flayout = new FormLayout();
		flayout.marginHeight = 0;
		flayout.marginWidth = 0;
		form.setLayout(flayout);
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

		String[] headers = { "azbuddy.ui.table.name", "azbuddy.ui.table.online",  "azbuddy.ui.table.lastseen", "azbuddy.ui.table.last_ygm", "azbuddy.ui.table.last_msg" };

		int[] sizes = { 400, 100, 100, 100, 200 };

		int[] aligns = { SWT.LEFT, SWT.CENTER, SWT.CENTER, SWT.CENTER, SWT.LEFT };

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
					
					item.setText(1, buddy.isOnline()?yes_txt:no_txt);
					
					long lo = buddy.getLastTimeOnline();
					
					item.setText(2, lo==0?"":new SimpleDateFormat().format(new Date( lo )));

					long	last_ygm = buddy.getLastMessagePending();;
					
					item.setText(3, last_ygm==0?"":new SimpleDateFormat().format(new Date( last_ygm )));
					
					String	lm = buddy.getLastMessageReceived();
					
					item.setText(4, lm==null?"":lm);
					
					item.setData( buddy );
				}
			});
		
		final Menu menu = new Menu(buddy_table);
		
		MenuItem remove_item = new MenuItem(menu, SWT.PUSH);
		
		remove_item.setText( lu.getLocalisedMessageText( "azbuddy.ui.menu.remove" ));

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
		
			// get public key
				
		MenuItem get_pk_item = new MenuItem(menu, SWT.PUSH);

		get_pk_item.setText( lu.getLocalisedMessageText( "azbuddy.ui.menu.copypk" ) );

		get_pk_item.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent event ) 
				{
					TableItem[] selection = buddy_table.getSelection();
					
					StringBuffer sb = new StringBuffer();
					
					for (int i=0;i<selection.length;i++){
						
						BuddyPluginBuddy buddy = (BuddyPluginBuddy)selection[i].getData();
						
						sb.append( buddy.getPublicKey() + "\r\n" );
					}
					
					writeToClipboard( sb.toString());
				};
			});
		
			// send message
		
		MenuItem send_msg_item = new MenuItem(menu, SWT.PUSH);

		send_msg_item.setText( lu.getLocalisedMessageText( "azbuddy.ui.menu.send" ) );

		send_msg_item.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent event ) 
				{
					TableItem[] selection = buddy_table.getSelection();
					
					UIInputReceiver prompter = ui_instance.getInputReceiver();
					
					prompter.setLocalisedTitle( lu.getLocalisedMessageText( "azbuddy.ui.menu.send" ));
					prompter.setLocalisedMessage( lu.getLocalisedMessageText( "azbuddy.ui.menu.send_msg" ) );
					
					try{
						prompter.prompt();
						
						String text = prompter.getSubmittedInput();
						
						if ( text != null ){
						
							for (int i=0;i<selection.length;i++){
								
								BuddyPluginBuddy buddy = (BuddyPluginBuddy)selection[i].getData();
								
								plugin.getAZ2Handler().sendAZ2Message( buddy, text );
							}
						}
					}catch( Throwable e ){
						
					}
				};
			});
		
		MenuItem ping_item = new MenuItem(menu, SWT.PUSH);

		ping_item.setText( lu.getLocalisedMessageText( "azbuddy.ui.menu.ping" ) );

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
		
			// ygm
		
		MenuItem ygm_item = new MenuItem(menu, SWT.PUSH);

		ygm_item.setText( lu.getLocalisedMessageText( "azbuddy.ui.menu.ygm" ) );

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
		
		
			// encrypt
		
		MenuItem encrypt_item = new MenuItem(menu, SWT.PUSH);

		encrypt_item.setText( lu.getLocalisedMessageText( "azbuddy.ui.menu.enc" ) );

		encrypt_item.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent event ) 
				{
					TableItem[] selection = buddy_table.getSelection();
					
					String	str = readFromClipboard();
					
					if( str != null ){
						
						StringBuffer sb = new StringBuffer();
						
						for (int i=0;i<selection.length;i++){
							
							BuddyPluginBuddy buddy = (BuddyPluginBuddy)selection[i].getData();
							
							try{
								byte[]	contents = str.getBytes( "UTF-8" );
								
								BuddyPlugin.cryptoResult result = buddy.encrypt( contents );
								
								sb.append( "key: " );
								sb.append( buddy.getPublicKey());
								sb.append( "\r\n" );
								
								sb.append( "hash: " );
								sb.append( Base32.encode( result.getChallenge()));
								sb.append( "\r\n" );

								sb.append( "payload: " );
								sb.append( Base32.encode( result.getPayload()));
								sb.append( "\r\n\r\n" );
								
							}catch( Throwable e ){
								
								print( "YGM failed", e );
							}
						}
						
						writeToClipboard( sb.toString());
					}
				};
			});
		
			// decrypt
		
		MenuItem decrypt_item = new MenuItem(menu, SWT.PUSH);

		decrypt_item.setText( lu.getLocalisedMessageText( "azbuddy.ui.menu.dec" ) );

		decrypt_item.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent event ) 
				{
					String	str = readFromClipboard();
					
					if ( str != null ){
						
						String[] 	bits = str.split( "\n" );
						
						StringBuffer sb = new StringBuffer();
	
						BuddyPluginBuddy	buddy 	= null;
						byte[]				hash	= null;
						
						for (int i=0;i<bits.length;i++){
							
							String	bit = bits[i].trim();
							
							if ( bit.length() > 0 ){
							
								int	pos = bit.indexOf( ':' );
								
								String	lhs = bit.substring( 0, pos ).trim();
								String	rhs	= bit.substring( pos+1 ).trim();
								
								if ( lhs.equals( "key" )){
									
									buddy = plugin.getBuddyFromPublicKey( rhs );
									
								}else if ( lhs.equals( "hash" )){
									
									hash	= Base32.decode( rhs );
									
								}else if ( lhs.equals( "payload" )){
								
									byte[]	payload = Base32.decode( rhs );
									
									if ( buddy != null ){
										
										try{
											BuddyPlugin.cryptoResult result = buddy.decrypt( payload );
											
											byte[] sha1 = new SHA1Simple().calculateHash( result.getChallenge());
											
											sb.append( "key: " );
											sb.append( buddy.getPublicKey());
											sb.append( "\r\n" );

											sb.append( "hash_ok: " + Arrays.equals( hash, sha1 ));
											sb.append( "\r\n" );
											
											sb.append( "payload: " );
											sb.append( new String( result.getPayload(), "UTF-8" ));
											sb.append( "\r\n\r\n" );
											
										}catch( Throwable e ){
											
											print( "decrypt failed", e );
										}
									}
								}
							}
						}
						
						if ( sb.length() > 0 ){
						
							writeToClipboard( sb.toString());
						}
					}
				};
			});
		
			// sign
		
		MenuItem sign_item = new MenuItem(menu, SWT.PUSH);

		sign_item.setText( lu.getLocalisedMessageText( "azbuddy.ui.menu.sign" ) );

		sign_item.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent event ) 
				{
					String	str = readFromClipboard();
					
					if ( str != null ){
						
						StringBuffer sb = new StringBuffer();
						
						try{
							sb.append( "key: " );
							sb.append( plugin.getPublicKey());
							sb.append( "\r\n" );

							byte[] payload = str.getBytes( "UTF-8" );
							
							sb.append( "data: " );
							sb.append( Base32.encode( payload ));
							sb.append( "\r\n" );

							byte[]	sig = plugin.sign( payload );

							sb.append( "sig: " );
							sb.append( Base32.encode( sig ));
							sb.append( "\r\n" );

						}catch( Throwable e ){
							
							print( "sign failed", e );
						}
						
						if ( sb.length() > 0 ){
						
							writeToClipboard( sb.toString());
						}
					}
				};
			});
		
			// verify
		
		MenuItem verify_item = new MenuItem(menu, SWT.PUSH);

		verify_item.setText( lu.getLocalisedMessageText( "azbuddy.ui.menu.verify" ) );

		verify_item.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent event ) 
				{
					String	str = readFromClipboard();
					
					if ( str != null ){
						
						String[] 	bits = str.split( "\n" );
						
						StringBuffer sb = new StringBuffer();
	
						String				pk 		= null;
						byte[]				data	= null;
						
						for (int i=0;i<bits.length;i++){
							
							String	bit = bits[i].trim();
							
							if ( bit.length() > 0 ){
							
								int	pos = bit.indexOf( ':' );
								
								String	lhs = bit.substring( 0, pos ).trim();
								String	rhs	= bit.substring( pos+1 ).trim();
								
								if ( lhs.equals( "key" )){
									
									pk = rhs;
									
								}else if ( lhs.equals( "data" )){
									
									data	= Base32.decode( rhs );
									
								}else if ( lhs.equals( "sig" )){
								
									byte[]	sig = Base32.decode( rhs );
									
									if ( pk != null && data != null ){
										
										try{
											
											sb.append( "key: " );
											sb.append( pk );
											sb.append( "\r\n" );

											boolean ok = plugin.verify( pk, data, sig );
											
											sb.append( "sig_ok: " + ok  );
											sb.append( "\r\n" );
											
											sb.append( "data: " );
											sb.append( new String( data, "UTF-8" ));
											sb.append( "\r\n\r\n" );
											
										}catch( Throwable e ){
											
											print( "decrypt failed", e );
										}
									}
								}
							}
						}
						
						if ( sb.length() > 0 ){
						
							writeToClipboard( sb.toString());
						}
					}
				};
			});
		
		buddy_table.setMenu( menu );
			
		menu.addMenuListener(
			new MenuListener()
			{
				public void 
				menuShown(
					MenuEvent arg0 ) 
				{
					boolean	enabled = plugin.isAvailable();
					
					MenuItem[] items = menu.getItems();
					
					for (int i=0;i<items.length;i++){
						
						items[i].setEnabled( enabled );
					}
				}
				
				public void 
				menuHidden(
					MenuEvent arg0 ) 
				{
				}
			});
		
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

	protected String
	readFromClipboard()
	{
		 Object o = 
			 new Clipboard(SWTThread.getInstance().getDisplay()).getContents(
			      TextTransfer.getInstance());
		 
		 if ( o instanceof String ){
			 
			 return((String)o);
		 }
		 
		 return( null );
	}

	protected void
	writeToClipboard(
		String	str )
	{
		 new Clipboard(SWTThread.getInstance().getDisplay()).setContents(
			      new Object[] {str }, 
			      new Transfer[] {TextTransfer.getInstance()});
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
