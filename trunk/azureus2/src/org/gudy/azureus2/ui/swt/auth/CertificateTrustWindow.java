/*
 * File    : CertificateTrustWindow.java
 * Created : 29-Dec-2003
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

import java.security.cert.X509Certificate;

import org.gudy.azureus2.core3.security.*;

import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.mainwindow.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;


public class 
CertificateTrustWindow 
	implements SECertificateListener
{
	public
	CertificateTrustWindow()
	{
		SESecurityManager.addCertificateListener( this );
	}
	
	public boolean
	trustCertificate(
		final String			resource,
		final X509Certificate	cert )
	{
		final Display	display = MainWindow.getWindow().getDisplay();
		
		if ( display.isDisposed()){
			
			return( false );
		}
		
		final Semaphore	sem = new Semaphore();
				
		final trustDialog[]	dialog = new trustDialog[1];
		
		try{
			display.asyncExec(
					new Runnable()
					{
						public void
						run()
						{
							dialog[0] = new trustDialog( sem, display, resource, cert );
						}
					});
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			return( false );
		}
		
		sem.reserve();
		
		return(dialog[0].getTrusted());
	}
	
	protected class
	trustDialog
	{
		protected Shell			shell;
		protected Semaphore		sem;
		
		protected boolean		trusted;
		
		protected
		trustDialog(
				Semaphore			_sem,
				Display				display,
				String				resource,
				X509Certificate		cert )
		{
			sem	= _sem;
			
			if ( display.isDisposed()){
				
				sem.release();
				
				return;
			}
			
			shell = new Shell (display,SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			
			shell.setImage(ImageRepository.getImage("azureus"));
			shell.setText(MessageText.getString("security.certtruster.title"));
			
			GridLayout layout = new GridLayout();
			layout.numColumns = 3;
			
			shell.setLayout (layout);
			
			GridData gridData;
			
			// info
			
			Label info_label = new Label(shell,SWT.NULL);
			info_label.setText(MessageText.getString("security.certtruster.intro"));
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 3;
			info_label.setLayoutData(gridData);
			
			// resource
			
			Label resource_label = new Label(shell,SWT.NULL);
			resource_label.setText(MessageText.getString("security.certtruster.resource"));
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 1;
			resource_label.setLayoutData(gridData);
			
			Label resource_value = new Label(shell,SWT.NULL);
			resource_value.setText(resource);
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 2;
			resource_value.setLayoutData(gridData);
			
			// issued by
			
			Label issued_by_label = new Label(shell,SWT.NULL);
			issued_by_label.setText(MessageText.getString("security.certtruster.issuedby"));
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 1;
			issued_by_label.setLayoutData(gridData);
			
			Label issued_by_value = new Label(shell,SWT.NULL);
			issued_by_value.setText(extractCN(cert.getIssuerDN().getName()));
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 2;
			issued_by_value.setLayoutData(gridData);
			
			// issued to
			
			Label issued_to_label = new Label(shell,SWT.NULL);
			issued_to_label.setText(MessageText.getString("security.certtruster.issuedto"));
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 1;
			issued_to_label.setLayoutData(gridData);
			
			Label issued_to_value = new Label(shell,SWT.NULL);
			issued_to_value.setText(extractCN(cert.getSubjectDN().getName()));
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 2;
			issued_to_value.setLayoutData(gridData);
			
			// prompt
			
			Label prompt_label = new Label(shell,SWT.NULL);
			prompt_label.setText(MessageText.getString("security.certtruster.prompt"));
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 3;
			prompt_label.setLayoutData(gridData);
			
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
			bYes.setText(MessageText.getString("security.certtruster.yes"));
			gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END | GridData.HORIZONTAL_ALIGN_FILL);
			gridData.grabExcessHorizontalSpace = true;
			gridData.widthHint = 70;
			bYes.setLayoutData(gridData);
			bYes.addListener(SWT.Selection,new Listener() {
				public void handleEvent(Event e) {
					close(true);
				}
			});
			
			Button bNo = new Button(comp,SWT.PUSH);
			bNo.setText(MessageText.getString("security.certtruster.no"));
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
			trusted = ok;
			
			shell.dispose();
			sem.release();
		}

		protected String
		extractCN(
			String		dn )
		{
			int	p1 = dn.indexOf( "CN=");
			
			if ( p1 == -1 ){
				return( dn );
			}
			
			int	p2 = dn.indexOf(",", p1 );
			
			if ( p2 == -1 ){
				
				return( dn.substring(p1+3).trim());
			}
			
			return( dn.substring(p1+3,p2).trim());
		}
		
		public boolean
		getTrusted()
		{
			return( trusted );
		}
	}	
}
