/*
 * Created on May 1, 2004
 * Created by Olivier Chalouhi
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
package org.gudy.azureus2.ui.swt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LGAlertListener;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.shells.MessagePopupShell;
import org.gudy.azureus2.core3.util.AEThread;
/**
 * Utility methods to display popup window
 */
public class Alerts {

  private static Alerts instance;
  
  private List			alert_queue = new ArrayList();
  private List			alert_history	= new ArrayList();
  private boolean initialisation_complete = false;
  
  private Alerts() {
  }
  
  public static synchronized Alerts getInstance() {
    if(instance == null) {
      instance = new Alerts();
    }
    
    return instance;
  }
  
  public static void
  showWarningMessageBox(
  	String		message )
  {
  	showMessageBoxUsingResourceString( SWT.ICON_WARNING, "AlertMessageBox.warning", message );
  }

  public static void
  showMessageBoxUsingResourceString(
  	int			type,
    String		key,
  String		message )
  {
  	showMessageBox( type,MessageText.getString(key), message,null);
  }

  public static void
  showMessageBox(
  	final int			type,
    final String		title,
    final String		message,
    final String 		details )
  {
  	final Display display = SWTThread.getInstance().getDisplay();
  
  display.asyncExec(new Runnable() {
  	public void 
  	run()
  	{
      
  		String icon_str;
  		
  		if ( type == SWT.ICON_INFORMATION ){
  			
  			icon_str = MessagePopupShell.ICON_INFO;
  			
  		}else if ( type == SWT.ICON_WARNING ){
  			
  			icon_str = MessagePopupShell.ICON_WARNING;
  			
  		}else{
  			
  			icon_str = MessagePopupShell.ICON_ERROR;
  		}
  
  		new MessagePopupShell(display,icon_str,title,message==null?"":message,details);
  	}
  });
   }

  public static void
  showErrorMessageBoxUsingResourceString(
  	String		title_key,
  Throwable	error )
  {
  	showErrorMessageBox(MessageText.getString(title_key), error);
  }

  public static void
  showErrorMessageBox(
  	String		title,
  Throwable	error )
  {
  	String error_message = LGLogger.exceptionToString( error );
  	showMessageBox( SWT.ICON_ERROR, title, error.getMessage(),error_message );
  }

  public static void
  showErrorMessageBox(
  	String		message )
  {
  	showMessageBoxUsingResourceString( SWT.ICON_ERROR, "AlertMessageBox.error", message );
  }

  public static void
  showCommentMessageBox(
  	String		message )
  {
  	showMessageBoxUsingResourceString( SWT.ICON_INFORMATION, "AlertMessageBox.information", message );
  }
  
  public static void 
  showAlert(
      int 		type,
      String 	message,
	  boolean	repeatable ) 
  {
    getInstance().showAlertI(type,message,repeatable);
  }
  
  public static void
  showAlert(
  	String		message,
	Throwable	exception,
	boolean		repeatable )
  {
    getInstance().showAlertI(message,exception,repeatable);
  }

  private void
  showAlertI(
  	String		message,
	Throwable	exception,
	boolean		repeatable )
  {
  	synchronized( alert_history ){
  		 		
  		if ( !repeatable ){
  			
  			String	key = message + ":" + exception.toString();
  		
	  			if ( alert_history.contains( key )){
	  			
	  			return;
	  		}
	  	
	  		alert_history.add( key );
	  		
	  		if ( alert_history.size() > 512 ){
	  			
	  			alert_history.remove(0);
	  		}
  		}
  	}
  	
  	showErrorMessageBox( message, exception );
  }

  
  
  private void
  showAlertI(
  	int		type,
	String	message,
	boolean	repeatable )
  {
  	synchronized( alert_history ){
  		
		if ( !repeatable ){
			  
	  		if ( alert_history.contains( message )){
	  			
	  			return;
	  		}
	  	
	  		alert_history.add( message );
	  		
	  		if ( alert_history.size() > 512 ){
	  			
	  			alert_history.remove(0);
	  		}
	  	}
  	}
  	
  if ( type == LGLogger.AT_COMMENT ){
  		
  	showCommentMessageBox( message );
  	
  }else if ( type == LGLogger.AT_WARNING ){
  	
  	showWarningMessageBox( message );
  		       						
  }else{
  			
      showErrorMessageBox( message );
  }
  }
  
  public static void initComplete() {
    getInstance().initCompleteI();
  }
  
  private void initCompleteI() {
    new AEThread("Init Complete")
    {
    	public void
    	run()
    	{   		
    		synchronized( alert_queue ){
    			
    			initialisation_complete	= true;
    			
    			for (int i=0;i<alert_queue.size();i++){
    				
    				Object[]	x = (Object[])alert_queue.get(i);
    				
    				boolean	repeatable = ((Boolean)x[2]).booleanValue();
    				
    				if ( x[0] instanceof String ){
    					
    					String		message 	= (String)x[0];
    					Throwable	exception 	= (Throwable)x[1];
    					
    					showAlert( message, exception, repeatable );
    					
    				}else{
    					
    					int		type 	= ((Integer)x[0]).intValue();
    					String	message = (String)x[1];
    				
    					showAlert( type, message, repeatable );
    				}
    			}
    			
    			alert_queue.clear();
    		}
    	}
    }.start();
  }
  
  public static void init() {
    getInstance().initI();
  }
  
  private void initI() {
    LGLogger.addAlertListener(
  			new LGAlertListener()
			{
  				public void
				alertRaised(
					int			type,
					String		message,
					boolean		repeatable )
				{
  					synchronized( alert_queue ){
  						
  						if ( !initialisation_complete ){
  							
  							alert_queue.add( new Object[]{ new Integer(type), message, new Boolean(repeatable)});
  							
  							return;
  						}
  					}
  					
  					showAlert( type, message, repeatable );
  				}
				
				public void
				alertRaised(
					String		message,
					Throwable	exception,
					boolean		repeatable )
				{
  					synchronized( alert_queue ){
  						
  						if ( !initialisation_complete ){
  							
  							alert_queue.add( new Object[]{ message, exception, new Boolean(repeatable)});
  							
  							return;
  						}
  					}
  					
  					showAlert( message, exception, repeatable );
				}
  			});
  }
}
