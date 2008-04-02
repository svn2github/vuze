/*
 * Created on 12-Jun-2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.ui.swt.views.configsections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.auth.CertificateCreatorWindow;
import org.gudy.azureus2.ui.swt.config.StringParameter;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.Cursors;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

import com.aelitis.azureus.core.security.CryptoHandler;
import com.aelitis.azureus.core.security.CryptoManager;
import com.aelitis.azureus.core.security.CryptoManagerFactory;
import com.aelitis.azureus.core.security.CryptoManagerKeyChangeListener;

/**
 * @author parg
 *
 */
public class 
ConfigSectionSecurity 
	implements UISWTConfigSection 
{
	public String 
	configSectionGetParentSection() 
	{
	    return ConfigSection.SECTION_ROOT;
	}

	public String 
	configSectionGetName() 
	{
		return( "security" );
	}

	public void 
	configSectionSave() 
	{
	}

	public void 
	configSectionDelete() 
	{
	}
	
	public int maxUserMode() {
		return 2;
	}
	  
	public Composite 
	configSectionCreate(
		final Composite parent) 
	{
		int userMode = COConfigurationManager.getIntParameter("User Mode");

	    GridData gridData;

	    Composite gSecurity = new Composite(parent, SWT.NULL);
	    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
	    gSecurity.setLayoutData(gridData);
	    GridLayout layout = new GridLayout();
	    layout.numColumns = 3;
	    gSecurity.setLayout(layout);

	    // row
	    
	    Label cert_label = new Label(gSecurity, SWT.NULL );
	    Messages.setLanguageText(cert_label, "ConfigView.section.tracker.createcert");

	    Button cert_button = new Button(gSecurity, SWT.PUSH);

	    Messages.setLanguageText(cert_button, "ConfigView.section.tracker.createbutton");

	    cert_button.addListener(SWT.Selection, 
	    		new Listener() 
				{
			        public void 
					handleEvent(Event event) 
			        {
			        	new CertificateCreatorWindow();
			        }
			    });
	    
	    new Label(gSecurity, SWT.NULL );
	    
	    // row

	    Label	info_label = new Label( gSecurity, SWT.WRAP );
	    Messages.setLanguageText( info_label, "ConfigView.section.security.toolsinfo" );
	    info_label.setLayoutData(Utils.getWrappableLabelGridData(3, 0));
	
	    // row
	    
	    Label lStatsPath = new Label(gSecurity, SWT.NULL);
	    
	    Messages.setLanguageText(lStatsPath, "ConfigView.section.security.toolsdir"); //$NON-NLS-1$

	    Image imgOpenFolder = ImageRepository.getImage("openFolderButton");
	    
	    gridData = new GridData();
	    
	    gridData.widthHint = 150;
	    
	    final StringParameter pathParameter = new StringParameter(gSecurity, "Security.JAR.tools.dir", ""); //$NON-NLS-1$ //$NON-NLS-2$
	    
	    pathParameter.setLayoutData(gridData);
	    
	    Button browse = new Button(gSecurity, SWT.PUSH);
	    
	    browse.setImage(imgOpenFolder);
	    
	    imgOpenFolder.setBackground(browse.getBackground());
	    
	    browse.setToolTipText(MessageText.getString("ConfigView.button.browse"));
	    
	    browse.addListener(SWT.Selection, new Listener() {
	      public void handleEvent(Event event) {
	        DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.APPLICATION_MODAL);
	
	        dialog.setFilterPath(pathParameter.getValue());
	      
	        dialog.setText(MessageText.getString("ConfigView.section.security.choosetoolssavedir")); //$NON-NLS-1$
	      
	        String path = dialog.open();
	      
	        if (path != null) {
	        	pathParameter.setValue(path);
	        }
	      }
	    });
	    
	   
	    	// row
	    
	    Label pw_label = new Label(gSecurity, SWT.NULL );
	    Messages.setLanguageText(pw_label, "ConfigView.section.security.clearpasswords");

	    Button pw_button = new Button(gSecurity, SWT.PUSH);

	    Messages.setLanguageText(pw_button, "ConfigView.section.security.clearpasswords.button");

	    pw_button.addListener(SWT.Selection, 
	    		new Listener() 
				{
			        public void 
					handleEvent(Event event) 
			        {
			        	SESecurityManager.clearPasswords();
			        	
			        	CryptoManagerFactory.getSingleton().clearPasswords();
			        }
			    });
	    
	    new Label(gSecurity, SWT.NULL );
	
	    if ( userMode >= 2 ){
	    	
	    	final CryptoManager crypt_man = CryptoManagerFactory.getSingleton();
	    	
	    	Group crypto_group = new Group(gSecurity, SWT.NULL);
		    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.FILL_HORIZONTAL);
		    gridData.horizontalSpan = 3;
		    crypto_group.setLayoutData(gridData);
		    layout = new GridLayout();
		    layout.numColumns = 3;
		    crypto_group.setLayout(layout);
		    
			Messages.setLanguageText(crypto_group,"ConfigView.section.security.group.crypto");
			
				// row

			byte[]	public_key = crypt_man.getECCHandler().peekPublicKey( null );
			
		    Label public_key_label = new Label(crypto_group, SWT.NULL );
		    Messages.setLanguageText(public_key_label, "ConfigView.section.security.publickey");

		    final Label public_key_value = new Label(crypto_group, SWT.NULL );
		    
			if ( public_key == null ){
				
			    Messages.setLanguageText(public_key_value, "ConfigView.section.security.publickey.undef");

			}else{
			    			    			    
			    public_key_value.setText( Base32.encode( public_key ));
			}
			
		    Messages.setLanguageText(public_key_value, "ConfigView.copy.to.clipboard.tooltip", true);

		    public_key_value.setCursor(Cursors.handCursor);
		    public_key_value.setForeground(Colors.blue);
		    public_key_value.addMouseListener(new MouseAdapter() {
		    	public void mouseDoubleClick(MouseEvent arg0) {
		    		copyToClipboard();
		    	}
		    	public void mouseDown(MouseEvent arg0) {
		    		copyToClipboard();
		    	}
		    	protected void
		    	copyToClipboard()
		    	{
	    			new Clipboard(parent.getDisplay()).setContents(new Object[] {public_key_value.getText()}, new Transfer[] {TextTransfer.getInstance()});
		    	}
		    });
			
			crypt_man.addKeyChangeListener(
					new CryptoManagerKeyChangeListener()
					{
						public void 
						keyChanged(
							CryptoHandler handler ) 
						{
							if ( parent.isDisposed()){
								
								crypt_man.removeKeyChangeListener( this );
								
							}else{
								if ( handler.getType() == CryptoManager.HANDLER_ECC ){
									
									byte[]	public_key = handler.peekPublicKey( null );

									if ( public_key == null ){
										
											// shouldn't happen...
										
										 Messages.setLanguageText(public_key_value, "ConfigView.section.security.publickey.undef");
										
									}else{
										
										public_key_value.setText( Base32.encode( public_key ));
									}
								}
							}
						}
					});
			
		    new Label(crypto_group, SWT.NULL );
		    
	    		// row
		    
		    Label reset_key_label = new Label(crypto_group, SWT.NULL );
		    Messages.setLanguageText(reset_key_label, "ConfigView.section.security.resetkey");
	
		    Button reset_key_button = new Button(crypto_group, SWT.PUSH);
		    Messages.setLanguageText(reset_key_button, "ConfigView.section.security.clearpasswords.button");
	
		    reset_key_button.addListener(SWT.Selection, 
		    		new Listener() 
					{
				        public void 
						handleEvent(Event event) 
				        {
		 					MessageBox mb = new MessageBox( parent.getShell(),SWT.ICON_WARNING | SWT.OK | SWT.CANCEL );
		 					
		 					mb.setText(MessageText.getString("ConfigView.section.security.resetkey.warning.title"));
		 				
		 					mb.setMessage(	MessageText.getString("ConfigView.section.security.resetkey.warning"));
		 					
		 					if ( mb.open() == SWT.OK ){
		 					
					        	try{
					        		crypt_man.getECCHandler().resetKeys( null );
					        					        		
					        	}catch( Throwable e ){
					        		
					        		Debug.out( "Failed to create keys", e );
					        		
				 					MessageBox mb2 = new MessageBox( parent.getShell(),SWT.ICON_ERROR | SWT.OK );
				 					
				 					mb2.setText(MessageText.getString( "ConfigView.section.security.resetkey.error.title"));
				 				
				 					mb2.setMessage(	MessageText.getString( "ConfigView.section.security.resetkey.error" ) + ": " + Debug.getNestedExceptionMessage(e));

				 					mb2.open();
					        	}
		 					}
				        }
				    });
		    
		    new Label(crypto_group, SWT.NULL );
		    	
		    	// row
		    
		    Label priv_key_label = new Label(crypto_group, SWT.NULL );
		    Messages.setLanguageText(priv_key_label, "ConfigView.section.security.unlockkey");
	
		    Button priv_key_button = new Button(crypto_group, SWT.PUSH);
		    Messages.setLanguageText(priv_key_button, "ConfigView.section.security.unlockkey.button");
	
		    priv_key_button.addListener(SWT.Selection, 
		    		new Listener() 
					{
				        public void 
						handleEvent(Event event) 
				        {
				        	try{
				        		byte[] result = crypt_man.getECCHandler().getEncryptedPrivateKey( "Testing!" );
				        		
				        		System.out.println( "ECC private key=" + ByteFormatter.encodeString( result ));
				        		
				        	}catch( Throwable e ){
				        		
				        		Debug.out( "Failed to unlock key", e );
				        		
			 					MessageBox mb = new MessageBox( parent.getShell(),SWT.ICON_ERROR | SWT.OK  );
			 					
			 					mb.setText(MessageText.getString( "ConfigView.section.security.resetkey.error.title" ));
			 				
			 					mb.setMessage(	MessageText.getString( "ConfigView.section.security.unlockkey.error" ));

			 					mb.open();
				        	}
				        }
				    });
		    
		    new Label(crypto_group, SWT.NULL );
	    }
	    
	    return gSecurity;
	  }
	}
