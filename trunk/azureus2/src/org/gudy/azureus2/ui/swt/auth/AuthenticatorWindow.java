/*
 * File    : AuthenticatorWindow.java
 * Created : 25-Nov-2003
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.ui.swt.auth;

/**
 * @author parg
 *
 */

import java.net.*;

import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.ImageRepository;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;

public class 
AuthenticatorWindow 
{
	public
	AuthenticatorWindow()
	{
		Authenticator.setDefault(
			new Authenticator()
			{
				protected PasswordAuthentication
				getPasswordAuthentication()
				{
					String	realm = getRequestingPrompt();
					
					String	tracker = getRequestingProtocol() + "://" + 
										getRequestingHost() + ":" + 
										getRequestingPort() + "/";
										
					String[]	res = getAuth( realm, tracker );
						
					if ( res == null ){
						
						return( null );
						
					}else{
										
						return( new PasswordAuthentication( res[0], res[1].toCharArray()));
					}
				}
			});
	}
	
	protected String[]
	getAuth(
		final String		realm,
		final String		tracker )
	{
		final Display	display = MainWindow.getWindow().getDisplay();
		
		if ( display.isDisposed()){
			
			return( null );
		}
		
		final Semaphore	sem = new Semaphore();
		
		final String[]	result = new String[2];
		
		final authDialog[]	dialog = new authDialog[1];
		
		try{
			display.asyncExec(
				new Runnable()
				{
					public void
					run()
					{
						dialog[0] = new authDialog( sem, display, realm, tracker );
					}
				});
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			return( null );
		}
		sem.reserve();
		
		String	user 	= dialog[0].getUsername();
		String	pw		= dialog[0].getPassword();
		
		if ( user == null || pw == null ){
			
			return( null );
		}
		
		return( new String[]{ user, pw });
	}
	
	protected class
	authDialog
	{
		protected Shell			shell;
		protected Semaphore		sem;
		
		protected String		username;
		protected String		password;
		
		protected
		authDialog(
			Semaphore		_sem,
			Display			display,
			String			realm,
			String			tracker )
		{
			sem	= _sem;
			
			if ( display.isDisposed()){
				
				sem.release();
				
				return;
			}
			
	 		shell = new Shell (display,SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
	 	
	 		shell.setImage(ImageRepository.getImage("azureus"));
		 	shell.setText(MessageText.getString("authenticator.title"));
    		
		 	GridLayout layout = new GridLayout();
		 	layout.numColumns = 3;
		        
		 	shell.setLayout (layout);
	    
		 	GridData gridData;
	    
	    		// realm
	    		
			Label realm_label = new Label(shell,SWT.NULL);
			realm_label.setText(MessageText.getString("authenticator.realm"));
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 1;
			realm_label.setLayoutData(gridData);
			
			Label realm_value = new Label(shell,SWT.NULL);
			realm_value.setText(realm);
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 2;
			realm_value.setLayoutData(gridData);
	    
	    		// tracker
			
			Label tracker_label = new Label(shell,SWT.NULL);
			tracker_label.setText(MessageText.getString("authenticator.tracker"));
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 1;
			tracker_label.setLayoutData(gridData);
			
			Label tracker_value = new Label(shell,SWT.NULL);
			tracker_value.setText(tracker);
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 2;
			tracker_value.setLayoutData(gridData);
	    		
	    		// user
	    		
			Label user_label = new Label(shell,SWT.NULL);
			user_label.setText(MessageText.getString("authenticator.user"));
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 1;
			user_label.setLayoutData(gridData);
	
			final Text user_value = new Text(shell,SWT.BORDER);
			user_value.setText("");
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 2;
			user_value.setLayoutData(gridData);

			user_value.addListener(SWT.Modify, new Listener() {
			   public void handleEvent(Event event) {
				 username = user_value.getText();
			   }});

				// password
	    		
			Label password_label = new Label(shell,SWT.NULL);
			password_label.setText(MessageText.getString("authenticator.password"));
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 1;
			password_label.setLayoutData(gridData);
			
			final Text password_value = new Text(shell,SWT.BORDER);
			password_value.setEchoChar('*');
			password_value.setText("");
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 2;
			password_value.setLayoutData(gridData);

			password_value.addListener(SWT.Modify, new Listener() {
			   public void handleEvent(Event event) {
				 password = password_value.getText();
			   }});
			   
				// buttons
				
			Label label = new Label(shell,SWT.NULL);

			Button bOk = new Button(shell,SWT.PUSH);
		 	bOk.setText(MessageText.getString("authenticator.ok"));
		 	gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END | GridData.HORIZONTAL_ALIGN_FILL);
		 	gridData.grabExcessHorizontalSpace = true;
		 	gridData.widthHint = 70;
		 	bOk.setLayoutData(gridData);
		 	bOk.addListener(SWT.Selection,new Listener() {
		  		public void handleEvent(Event e) {
			 		close(true);
		   		}
			 });
	    
		 	Button bCancel = new Button(shell,SWT.PUSH);
		 	bCancel.setText(MessageText.getString("authenticator.cancel"));
		 	gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		 	gridData.grabExcessHorizontalSpace = false;
		 	gridData.widthHint = 70;
		 	bCancel.setLayoutData(gridData);    
		 	bCancel.addListener(SWT.Selection,new Listener() {
		 		public void handleEvent(Event e) {
			 		close(false);
		   		}
		 	});
	    
			shell.setDefaultButton( bOk );

		 	shell.pack ();
			shell.open ();   
		}
   
		protected void
		close(
			boolean		ok )
	 	{
	 		if ( !ok ){
	 			
	 			username	= null;
	 			password	= null;
	 		}
	 		
	 		shell.dispose();
	 		sem.release();
	 	}
	 	
	 	protected String
	 	getUsername()
	 	{
	 		return( username );
	 	}
	 	
	 	protected String
	 	getPassword()
	 	{
	 		return( password );
	 	}
	}
}
