/*
 * Created on May 1, 2004
 * Created by Olivier Chalouhi
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
package org.gudy.azureus2.ui.swt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.shells.MessageSlideShell;
import org.gudy.azureus2.core3.util.*;

/**
 * Utility methods to display popup window
 * 
 * TODO: Finish up moving from LGLogger to Logger/LogAlert.  ie alert_queue
 *        could store LogAlert instead of an object array.
 */
public class Alerts {

  private static Alerts instance;
  private static AEMonitor	class_mon	= new AEMonitor( "Alerts:class" );
  
  
  private static List			alert_queue 			= new ArrayList();
  private static AEMonitor		alert_queue_mon	= new AEMonitor("Alerts:Q");
  
  private static List			alert_history		= new ArrayList();
  private static AEMonitor		alert_history_mon	= new AEMonitor("Alerts:H");
  private static boolean 		initialisation_complete = false;
  
  private static transient boolean	stopping;
  private static transient int		stopping_alert_count;
  
  private Alerts() 
  {
  }
  
  public static Alerts getInstance() 
  {
  	try{
  		class_mon.enter();
    
  		if(instance == null) {
  			instance = new Alerts();
  		}
    
  		return instance;
  		
  	}finally{
  		
  		class_mon.exit();
  	}
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
  
  	if ( display.isDisposed()){
  		return;
  	}
  		
  	if ( stopping ){
  		
  		stopping_alert_count++;
  	}
  	
	display.asyncExec( 
	  	new AERunnable() 
	  	{
	  	  	public void 
	  	  	runSupport()
	  	  	{
	  	      new MessageSlideShell(display, type, title, message==null?"":message, details);
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
  	String error_message = Debug.getStackTrace(error);
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
  	String		message,
	Throwable	exception,
	boolean		repeatable )
  {
  	try{
  		alert_history_mon.enter();
  		 		
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
  	}finally{
  		
  		alert_history_mon.exit();
  	}
  	
  	showErrorMessageBox( message, exception );
  }

  
  
  public static void
  showAlert(
  	int		type,
	String	message,
	boolean	repeatable )
  {
  	try{
  		alert_history_mon.enter();
  		
		if ( !repeatable ){
			  
	  		if ( alert_history.contains( message )){
	  			
	  			return;
	  		}
	  	
	  		alert_history.add( message );
	  		
	  		if ( alert_history.size() > 512 ){
	  			
	  			alert_history.remove(0);
	  		}
	  	}
  	}finally{
  		
  		alert_history_mon.exit();
  	}
  	
  if ( type == LogAlert.AT_INFORMATION ){
  		
  	showCommentMessageBox( message );
  	
  }else if ( type == LogAlert.AT_WARNING ){
  	
  	showWarningMessageBox( message );
  		       						
  }else{
  			
      showErrorMessageBox( message );
  }
  }
  
  public static void 
  initComplete() 
  {
    new AEThread("Init Complete")
    {
    	public void
    	runSupport()
    	{   		
    		try{
    			alert_queue_mon.enter();
    			
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
    			
    		}finally{
    			
    			alert_queue_mon.exit();
    		}
    	}
    }.start();
  }
  

  public static void 
  stopInitiated()
  {
	 stopping	= true;
  }
  
  public static void 
  stopCompleted()
  {
	  	// give the users a chance to see alerts generated on shutdown, only if this happens to be
	  	// the swt thread (can't risk sync-exec due to potential deadlock and async is pointless
	  	// as UI will go ahead and close immediately )
	  
	  final Display display = SWTThread.getInstance().getDisplay();
		
	  if (	stopping_alert_count > 0 &&
			Thread.currentThread() == display.getThread()){
		  
		  long	start = SystemTime.getCurrentTime();
		  
		  while( true ){
			  
			  long	now = SystemTime.getCurrentTime();
			  
			  if ( 	now >= start && 
				  	now - start < 5000 && 
				  	!display.isDisposed()){
		
				  try{
					  if (!display.readAndDispatch()){
						  
						  display.sleep();
					  }
					  
				  }catch( Throwable e ){
					  
					  break;
				  }
			  }else{
				  
				  break;
			  }
		  }
	  }
  }
  
  public static void 
  init() 
  {
		Logger.addListener(new ILogAlertListener() {
			/* (non-Javadoc)
			 * @see org.gudy.azureus2.core3.logging.ILogAlertListener#alertRaised(org.gudy.azureus2.core3.logging.LogAlert)
			 */
			public void alertRaised(LogAlert alert) {
				try {
					alert_queue_mon.enter();

					if (!initialisation_complete) {

						if (alert.err == null)
							alert_queue.add(new Object[] { new Integer(alert.entryType),
									alert.text, new Boolean(alert.repeatable) });
						else
							alert_queue.add(new Object[] { alert.text, alert.err,
									new Boolean(alert.repeatable) });

						return;
					}
				} finally {

					alert_queue_mon.exit();
				}

				if (alert.err == null)
					showAlert(alert.entryType, alert.text, alert.repeatable);
				else
					showAlert(alert.text, alert.err, alert.repeatable);
			}
		});
	}
}
