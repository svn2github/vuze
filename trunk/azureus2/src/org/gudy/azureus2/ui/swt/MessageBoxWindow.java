/*
 * Created on 02-Oct-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.ui.swt;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Constants;



public class 
MessageBoxWindow 
{
	public static final String ICON_ERROR 		= "error";
	public static final String ICON_WARNING 	= "warning";
	public static final String ICON_INFO	 	= "info";

	public static int
	getRememberedDecision(
		String		id,
		int			remember_map )
	{
		Map	remembered_decisions = COConfigurationManager.getMapParameter( "MessageBoxWindow.decisions", new HashMap());
		
		Long	l = (Long)remembered_decisions.get( id );
		
		if ( l != null ){
			
			int	remembered_value = l.intValue();
			
			if (( remembered_value & remember_map) != 0 ){
				
				return( remembered_value );
			}
		}
		
		return( SWT.NULL );
	}
	
	public static int 
	open(
		String	id,
		int		options,
		int		remember_map,
		Display display,
		String	icon,
		String	title,
		String	message ) 
	{
		int	remembered = getRememberedDecision( id, remember_map );
		
		if ( remembered != SWT.NULL ){
			
			return( remembered );
		}
		
		return( new MessageBoxWindow( id, options, display, icon, title, message ).getResult());
	}
  
	private Shell shell;
	
	private AESemaphore	result_sem = new AESemaphore( "MessageBoxWindow" );
	
	private volatile int			result;
	private volatile boolean		result_set;
	
	protected 
	MessageBoxWindow(
		final String	id,
		final int		options,
		final Display 	display,
		final String	icon,
		final String	title,
		final String	message )
	{	
		shell = new Shell(display,SWT.APPLICATION_MODAL | SWT.TITLE | SWT.CLOSE );

		shell.setText( title );
		
		if(! Constants.isOSX) {
			shell.setImage(ImageRepository.getImage("azureus"));
		}
		
	    GridLayout layout = new GridLayout();
	    layout.numColumns = 3;
	    shell.setLayout(layout);
	    
	    	// image and text
	    
	    Label label = new Label(shell,SWT.NONE);

	    Image image = ImageRepository.getImage(icon);

	    if ( image != null ){
	    	
	    	label.setImage( image );
	    }
	    
	    label = new Label(shell,SWT.NONE);
	    label.setText(message);
	    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
	    gridData.horizontalSpan = 2;
	    label.setLayoutData(gridData);
	    
	    	// remember decision
	    
	    final Button checkBox = new Button(shell, SWT.CHECK);
	    checkBox.setSelection(false);
	    checkBox.setText( MessageText.getString( "MessageBoxWindow.rememberdecision" ));
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		checkBox.setLayoutData(gridData);

	    
			// line
		
		Label labelSeparator = new Label(shell,SWT.SEPARATOR | SWT.HORIZONTAL);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		labelSeparator.setLayoutData(gridData);

			// buttons
			
		label = new Label(shell,SWT.NULL);

		final int yes_option = options & ( SWT.OK | SWT.YES );
		
		Button bYes = new Button(shell,SWT.PUSH);
	 	bYes.setText(MessageText.getString( yes_option==SWT.YES?"Button.yes":"Button.ok"));
	 	gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END | GridData.HORIZONTAL_ALIGN_FILL);
	 	gridData.grabExcessHorizontalSpace = true;
	 	gridData.widthHint = 70;
	 	bYes.setLayoutData(gridData);
	 	bYes.addListener(SWT.Selection,new Listener() {
	  		public void handleEvent(Event e) {
	  			setResult( id, yes_option, checkBox.getSelection());
	   		}
		 });
    
		final int no_option = options & ( SWT.CANCEL | SWT.NO );

	 	Button bNo = new Button(shell,SWT.PUSH);
	 	bNo.setText(MessageText.getString(no_option==SWT.NO?"Button.no":"Button.cancel"));
	 	gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
	 	gridData.grabExcessHorizontalSpace = false;
	 	gridData.widthHint = 70;
	 	bNo.setLayoutData(gridData);    
	 	bNo.addListener(SWT.Selection,new Listener() {
	 		public void handleEvent(Event e) {
	 			setResult( id, no_option, checkBox.getSelection());
	   		}
	 	});
	 	
		shell.setDefaultButton( bYes );
		
		shell.addListener(SWT.Traverse, new Listener() {	
			public void handleEvent(Event e) {
				if ( e.character == SWT.ESC){
					setResult( id, SWT.NULL, false );
				}
			}
		});
    
	    shell.addListener(
	    	SWT.Close,
	    	new Listener() 
	    	{
	    		public void 
	    		handleEvent(
	    			Event arg0) 
	    		{
	    			setResult( id, SWT.NULL, false );
	    		}
	    	});
    
		
	 	shell.pack ();
	 	
		Utils.centreWindow( shell );
        
	    shell.open();
	    
	    while( !shell.isDisposed()) {
	       
	    	if (!display.readAndDispatch()){
	    		
	              display.sleep();
	        }
	    }
	}      

	protected void
	setRemembered(
		String		id,
		int			value )
	{
		Map	remembered_decisions = COConfigurationManager.getMapParameter( "MessageBoxWindow.decisions", new HashMap());

		remembered_decisions.put( id, new Long( value ));
		
		COConfigurationManager.setParameter( "MessageBoxWindow.decisions", remembered_decisions );
	}
	
	protected void
	setResult(
		String		id,
		int			option,
		boolean		remember )
	{
		if ( !result_set ){
			
			result	= option;
			
			result_set	= true;
			
			if ( remember ){
				
				setRemembered( id, result );
			}
			
			result_sem.release();
			
			close();
		}
	}
	
	protected void
	close()
	{
		if ( !shell.isDisposed()){
			
			shell.dispose();
		}
	}
	
	protected int
	getResult()
	{
		result_sem.reserve();
		
		return( result );
	}
}
