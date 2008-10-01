/*
 * Created on 02-Oct-2005
 * Created by Paul Gardner
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.components.BufferedLabel;


public class 
PropertiesWindow 
{
	private final Shell		shell;
	
	public 
	PropertiesWindow(
		String		object_name,
		String[]	keys,
		String[]	values )
	{	
		final Shell any_shell = Utils.findAnyShell();

		shell = new Shell( any_shell.getDisplay(),SWT.APPLICATION_MODAL | SWT.TITLE | SWT.CLOSE |SWT.RESIZE );

		shell.setText( MessageText.getString( "props.window.title", new String[]{ object_name }));
		
		Utils.setShellIcon(shell);
		
	    GridLayout layout = new GridLayout();
	    layout.numColumns = 3;
	    shell.setLayout(layout);

	    

	    Composite main = new Composite( shell, SWT.V_SCROLL );
	    layout = new GridLayout();
	    layout.numColumns = 2;
	    main.setLayout(layout);
	    GridData gridData = new GridData(GridData.FILL_BOTH);
	    gridData.horizontalSpan = 3;
	    main.setLayoutData(gridData);

	    for (int i=0;i<keys.length;i++){
	    	
		    BufferedLabel	msg_label = new BufferedLabel(main, SWT.NULL);
		    msg_label.setText( MessageText.getString( keys[i] ) + ":" );
		    gridData = new GridData();
		    gridData.verticalAlignment = GridData.VERTICAL_ALIGN_FILL;
		    msg_label.setLayoutData(gridData);
	
		    BufferedLabel	val_label = new BufferedLabel(main,SWT.WRAP);
		    val_label.setText( values[i] );
		    gridData = new GridData(GridData.FILL_HORIZONTAL);
		    gridData.horizontalIndent = 6;
		    val_label.setLayoutData(gridData);
	    }

			// separator
			
		Label labelSeparator = new Label(shell,SWT.SEPARATOR | SWT.HORIZONTAL);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		labelSeparator.setLayoutData(gridData);
		
			// buttons
			
		new Label(shell,SWT.NULL);

		Button bOk = new Button(shell,SWT.PUSH);
	 	Messages.setLanguageText(bOk, "Button.ok");
	 	gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END | GridData.HORIZONTAL_ALIGN_FILL);
	 	gridData.grabExcessHorizontalSpace = true;
	 	gridData.widthHint = 70;
	 	bOk.setLayoutData(gridData);
	 	bOk.addListener(SWT.Selection,new Listener() {
	  		public void handleEvent(Event e) {
		 		close();
	   		}
		 });
    
	 	Button bCancel = new Button(shell,SWT.PUSH);
	 	Messages.setLanguageText(bCancel, "Button.cancel");
	 	gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
	 	gridData.grabExcessHorizontalSpace = false;
	 	gridData.widthHint = 70;
	 	bCancel.setLayoutData(gridData);    
	 	bCancel.addListener(SWT.Selection,new Listener() {
	 		public void handleEvent(Event e) {
		 		close();
	   		}
	 	});
    
	 	shell.setDefaultButton( bOk );
		
	 	shell.addListener(SWT.Traverse, new Listener() {	
			public void handleEvent(Event e) {
				if ( e.character == SWT.ESC){
					close();
				}
			}
		});

		 	
	 	shell.pack();
	
	 	shell.setSize( 400, 300 );

		Utils.centreWindow( shell );

		shell.open();   
	}      

	protected void
	close()
	{
		if ( !shell.isDisposed()){
			
			shell.dispose();
		}
	}
}
