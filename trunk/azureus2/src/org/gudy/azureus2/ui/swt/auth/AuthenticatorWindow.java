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
import java.util.*;

import sun.misc.BASE64Encoder;

import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.security.*;

public class 
AuthenticatorWindow 
	implements SEPasswordListener
{
	protected Map	auth_cache = new HashMap();
	
	protected AEMonitor	this_mon	= new AEMonitor( "AuthWind" );
	
	public
	AuthenticatorWindow()
	{
		SESecurityManager.addPasswordListener( this );
		
		// System.out.println( "AuthenticatorWindow");
	}
	
	public PasswordAuthentication
	getAuthentication(
		String		realm,
		URL			tracker )
	{
		try{
			this_mon.enter();
	
			return( getAuthentication( realm, tracker.getProtocol(), tracker.getHost(), tracker.getPort()));
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	setAuthenticationOutcome(
		String		realm,
		URL			tracker,
		boolean		success )
	{
		try{
			this_mon.enter();
		
			setAuthenticationOutcome( realm, tracker.getProtocol(), tracker.getHost(), tracker.getPort(), success );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	setAuthenticationOutcome(
		String		realm,
		String		protocol,
		String		host,
		int			port,
		boolean		success )
	{
		try{
			this_mon.enter();
		
			String	tracker = protocol + "://" + host + ":" + port + "/";
			
			String auth_key = realm+":"+tracker;
			
			authCache	cache = (authCache)auth_cache.get( auth_key );
	
			if ( cache != null ){
	
				cache.setOutcome( success );
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	public PasswordAuthentication
	getAuthentication(
		String		realm,
		String		protocol,
		String		host,
		int			port )
	{
		try{
			this_mon.enter();
	
			String	tracker = protocol + "://" + host + ":" + port + "/";
	
			String bind_ip = COConfigurationManager.getStringParameter("Bind IP", "");
			
			String	self_addr;
	
			// System.out.println( "auth req for " + realm + " - " + tracker );
			
			if ( bind_ip.length() < 7 ){
		
				self_addr = "127.0.0.1";
		
			}else{
		
				self_addr = bind_ip;
			}
	
				// when the tracker is connected to internally we don't want to prompt
				// for the password. Here we return a special user and the password hash
				// which is picked up in the tracker auth code - search for "<internal>"!
				
				// also include the tracker IP as well as for scrapes these can occur on
				// a raw torrent which hasn't been modified to point to localhost
			
			if ( 	host.equals(self_addr) ||
					host.equals(COConfigurationManager.getStringParameter("Tracker IP", ""))){
			
				try{
					byte[]	pw	= COConfigurationManager.getByteParameter("Tracker Password", new byte[0]);
				
					String str_pw = new BASE64Encoder().encode(pw);
					
					return( new PasswordAuthentication( "<internal>", str_pw.toCharArray()));
						
				}catch( Throwable e ){
					
					Debug.printStackTrace( e );
				}	
			}
			
			String auth_key = realm+":"+tracker;
								
			authCache	cache = (authCache)auth_cache.get( auth_key );
						
			if ( cache != null ){
				
				PasswordAuthentication	auth = cache.getAuth();
				
				if ( auth != null ){
					
					return( auth );
				}
			}
				
			String[]	res = getAuthenticationDialog( realm, tracker );
				
			if ( res == null ){
				
				return( null );
				
			}else{
								
				PasswordAuthentication auth =  new PasswordAuthentication( res[0], res[1].toCharArray());
				
				auth_cache.put( auth_key, new authCache( auth ));
				
				return( auth );
			}	
		}finally{
			
			this_mon.exit();
		}
	}
	
	
	protected String[]
	getAuthenticationDialog(
		final String		realm,
		final String		tracker )
	{
		final Display	display = SWTThread.getInstance().getDisplay();
		
		if ( display.isDisposed()){
			
			return( null );
		}
		
		final AESemaphore	sem = new AESemaphore("SWTAuth");
		
		final authDialog[]	dialog = new authDialog[1];
		
		try{
			display.asyncExec(
				new AERunnable()
				{
					public void
					runSupport()
					{
						dialog[0] = new authDialog( sem, display, realm, tracker );
					}
				});
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			return( null );
		}
		sem.reserve();
		
		String	user 	= dialog[0].getUsername();
		String	pw		= dialog[0].getPassword();
		
		if ( user == null ){
			
			return( null );
		}
		
		return( new String[]{ user, pw == null?"":pw });
	}
	
	protected class
	authDialog
	{
		protected Shell			shell;
		protected AESemaphore	sem;
		
		protected String		username;
		protected String		password;
		
		protected
		authDialog(
			AESemaphore		_sem,
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
			   
			// line
			
			Label labelSeparator = new Label(shell,SWT.SEPARATOR | SWT.HORIZONTAL);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan = 3;
			labelSeparator.setLayoutData(gridData);
			
				// buttons
				
			new Label(shell,SWT.NULL);

			Button bOk = new Button(shell,SWT.PUSH);
		 	bOk.setText(MessageText.getString("Button.ok"));
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
		 	bCancel.setText(MessageText.getString("Button.cancel"));
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
	
	protected class
	authCache
	{
		protected PasswordAuthentication	auth;
		protected int						life = 5;
		protected boolean					succeeded;
		
		protected
		authCache(
			PasswordAuthentication		_auth )
		{
			auth		= _auth;
		}
		
		protected void
		setOutcome(
			boolean	success)
		{
			if ( success ){
				
				succeeded	= true;
				
			}else{
				
				if ( !succeeded ){
					
					auth	= null;
				}
			}
		}
		
		protected PasswordAuthentication
		getAuth()
		{
			if ( succeeded ){
				
				return( auth );
			}
			
			life--;
			
			if ( life >= 0 ){
				
				return( auth );
			}
			
			return( null );
		}
	}
		
}
