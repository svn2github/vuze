/*
 * Created on 08-Jun-2004
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

package org.gudy.azureus2.ui.swt.auth;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.security.*;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.logging.*;

import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.mainwindow.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;


public class 
CertificateCreatorWindow 
{
	public
	CertificateCreatorWindow()
	{
		createCertificate();
	}
	
	public void
	createCertificate()
	{
		final Display	display = MainWindow.getWindow().getDisplay();
		
		if ( display.isDisposed()){

			return;
		}
		
		try{
			display.asyncExec(
					new Runnable()
					{
						public void
						run()
						{
							 new createDialog( display );
						}
					});
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	protected class
	createDialog
	{
		protected Shell			shell;
				
		protected
		createDialog(
			Display				display )
		{
			if ( display.isDisposed()){
				
				return;
			}
			
			shell = new Shell (display,SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			
			shell.setImage(ImageRepository.getImage("azureus"));
			shell.setText(MessageText.getString("security.certcreate.title"));
			
			GridLayout layout = new GridLayout();
			layout.numColumns = 3;
			
			shell.setLayout (layout);
			
			GridData gridData;
			
			// info
			
			Label info_label = new Label(shell,SWT.NULL);
			info_label.setText(MessageText.getString("security.certcreate.intro"));
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 3;
			info_label.setLayoutData(gridData);
			
			// alias
			
			Label alias_label = new Label(shell,SWT.NULL);
			alias_label.setText(MessageText.getString("security.certcreate.alias"));
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 1;
			alias_label.setLayoutData(gridData);
			
			final Text alias_field =new Text(shell,SWT.BORDER);
			
			alias_field.setText( SESecurityManager.DEFAULT_ALIAS );
			
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 2;
			alias_field.setLayoutData(gridData);
			
			// strength
			
			Label strength_label = new Label(shell,SWT.NULL);
			strength_label.setText(MessageText.getString("security.certcreate.strength"));
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 1;
			strength_label.setLayoutData(gridData);
			
			final Combo strength_combo = new Combo(shell, SWT.SINGLE | SWT.READ_ONLY);
			   
			final int[] strengths = { 512, 1024, 1536, 2048 };
			   			      
			for (int i=0;i<strengths.length;i++){
				
				strength_combo.add(""+strengths[i]);
			}
			      
			strength_combo.select(1);
			
			Label label = new Label(shell,SWT.NULL);
			      
			// first + last name
			
			String[]	field_names = { 
									"security.certcreate.firstlastname",
									"security.certcreate.orgunit",
									"security.certcreate.org",
									"security.certcreate.city",
									"security.certcreate.state",
									"security.certcreate.country"
								};
			
			final String[]		field_rns = {"CN", "OU", "O", "L", "ST", "C" };
			
			final Text[]		fields = new Text[field_names.length];
			
			for (int i=0;i<fields.length;i++){
				
				Label resource_label = new Label(shell,SWT.NULL);
				resource_label.setText(MessageText.getString(field_names[i]));
				gridData = new GridData(GridData.FILL_BOTH);
				gridData.horizontalSpan = 1;
				resource_label.setLayoutData(gridData);
				
				Text field = fields[i] = new Text(shell,SWT.BORDER);
				gridData = new GridData(GridData.FILL_BOTH);
				gridData.horizontalSpan = 2;
				field.setLayoutData(gridData);
			}

				// line
			
			Label labelSeparator = new Label(shell,SWT.SEPARATOR | SWT.HORIZONTAL);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan = 3;
			labelSeparator.setLayoutData(gridData);
			
				// buttons
			
			new Label(shell,SWT.NULL);
			
			Composite comp = new Composite(shell,SWT.NULL);
			gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END | GridData.HORIZONTAL_ALIGN_FILL);
			gridData.grabExcessHorizontalSpace = true;
			gridData.horizontalSpan = 2;
			comp.setLayoutData(gridData);
			GridLayout layoutButtons = new GridLayout();
			layoutButtons.numColumns = 2;
			comp.setLayout(layoutButtons);
			
			
			
			Button bYes = new Button(comp,SWT.PUSH);
			bYes.setText(MessageText.getString("security.certcreate.ok"));
			gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END | GridData.HORIZONTAL_ALIGN_FILL);
			gridData.grabExcessHorizontalSpace = true;
			gridData.widthHint = 70;
			bYes.setLayoutData(gridData);
			bYes.addListener(SWT.Selection,new Listener() {
				public void handleEvent(Event e) {
					
					String	alias	= alias_field.getText().trim();
					
					int		strength	= strengths[strength_combo.getSelectionIndex()];
					
					String	dn = "";
					
					for (int i=0;i<fields.length;i++){
						
						String	rn = fields[i].getText().trim();
						
						if ( rn.length() == 0 ){
							
							rn = "Unknown";
						}
						
						dn += (dn.length()==0?"":",") + field_rns[i] + "=" + rn;
					}
										
					try{
						SESecurityManager.createSelfSignedCertificate( alias, dn, strength );
						
						close(true );
						
						LGLogger.logAlert( LGLogger.AT_COMMENT, MessageText.getString( "security.certcreate.createok") + "\n" + alias +":" + strength + "\n" + dn + "\n" + System.currentTimeMillis());
						
					}catch( Throwable f ){
						
						LGLogger.logAlert( MessageText.getString( "security.certcreate.createfail")+"\n" + System.currentTimeMillis(), f );
					}
				}
			});
			
			Button bNo = new Button(comp,SWT.PUSH);
			bNo.setText(MessageText.getString("security.certcreate.cancel"));
			gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
			gridData.grabExcessHorizontalSpace = false;
			gridData.widthHint = 70;
			bNo.setLayoutData(gridData);    
			bNo.addListener(SWT.Selection,new Listener() {
				public void handleEvent(Event e) {
					close(false);
				}
			});
			
			shell.setDefaultButton( bYes );
			
			shell.addListener(SWT.Traverse, new Listener() {	
				public void handleEvent(Event e) {
					if ( e.character == SWT.ESC){
						close( false );
					}
				}
			});

			
			shell.pack ();
			
			Utils.centreWindow( shell );

			shell.open ();   
		}
		
		protected void
		close(
			boolean		ok )
		{	
			shell.dispose();
		}
	}	
}