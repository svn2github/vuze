/*
 * Created on 28.11.2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.ui.common.util;

import java.applet.Applet;
import java.applet.AudioClip;
import java.net.*;
import java.io.*;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.impl.DownloadManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.impl.GlobalManagerAdpater;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.logging.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;

/**
 * Contains methods to alert the user of certain events.
 * @author Rene Leonhardt
 */

public class 
UserAlerts 
{
  	private AudioClip 	audio_clip 		= null;
  	private String		audio_resource	= "";
  	
    private AEMonitor	this_mon 	= new AEMonitor( "UserAlerts" );
    
	public 
	UserAlerts(
		GlobalManager	global_manager ) 
 	{	
		final DownloadManagerAdapter download_manager_listener = new
			DownloadManagerAdapter()
			{
				public void
				downloadComplete(DownloadManager manager)
				{
					downloadFinished();
				}
			}; 
			
    	global_manager.addListener(
    		new GlobalManagerAdpater()
    		{
				public void downloadManagerAdded(DownloadManager manager) 
				{
					manager.addListener( download_manager_listener );
				}

				public void downloadManagerRemoved(DownloadManager removed) 
				{
					removed.removeListener(download_manager_listener);
				}  
				
				public void
				destroyed()
				{
					tidyUp();
				} 		
			}); 			
     }

  	protected void
  	downloadFinished()
  	{
  		try{
  			this_mon.enter();
  		
	    	if( COConfigurationManager.getBooleanParameter("Play Download Finished", false)){
	    
	    		String	file = COConfigurationManager.getStringParameter( "Play Download Finished File" );
	    		
    			file = file.trim();

	    			// turn "<default>" into blank
	    		
	    		if ( file.startsWith( "<" )){
	    			
	    			file	= "";
	    		}
	    		
	    		if ( audio_clip == null || !file.equals( audio_resource )){
	    			    
	    			audio_clip	= null;
	    			
	    				// try explicit file
	    			
	    			if ( file.length() != 0 ){
	    				
	    				File	f = new File( file );
	    				
	    				try{
		    					
			    			if ( f.exists()){
			    					
		    					URL	file_url = f.toURL();
		    							    					
		    					audio_clip = Applet.newAudioClip( file_url );
			    			}
			    			
	    				}catch( Throwable  e ){
	    					
	    					Debug.printStackTrace(e);
	    					
	    				}finally{
	    					
	    					if ( audio_clip == null ){
	    						
	    						LGLogger.logAlert( 
	    								LGLogger.AT_ERROR,
	    								"Failed to load audio file '" + file + "'" );
	    					}
	    				}
	    			}
	    			
	    				// either non-explicit or explicit missing
	    			
	    			if ( audio_clip == null ){
	    				
	    				audio_clip = Applet.newAudioClip(UserAlerts.class.getClassLoader().getResource("org/gudy/azureus2/ui/icons/downloadFinished.wav"));
	    					    				
	    			}
	    			
	    			audio_resource	= file;
	    		}
	    		
	    		if ( audio_clip != null ){
	    		    			
	            	new AEThread("DownloadSound")
					{
		        		public void
						runSupport()
		        		{
		        			try{
		        				audio_clip.play();
		        			
		        				Thread.sleep(2500);
		        				
		        			}catch( Throwable e ){
		        				
		        			}
		        		}
		        	}.start();	    		
		        }
	    	}
  		}catch( Throwable e ){
  			
  			Debug.printStackTrace( e );
  			
  		}finally{
  			
  			this_mon.exit();
  		}
  	}
  	
  	protected void
  	tidyUp()
  	{
		/*
		The Java audio system keeps some threads running even after playback is finished.
		One of them, named "Java Sound event dispatcher", is *not* a daemon
		thread and keeps the VM alive.
		We have to locate and interrupt it explicitely.
		*/
		
		try{
		
			ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
			
			Thread[] threadList = new Thread[threadGroup.activeCount()];
			
			threadGroup.enumerate(threadList);
			
			for (int i = 0;	i < threadList.length;	i++){
			
				if(threadList[i] != null && "Java Sound event dispatcher".equals(threadList[i].getName())){
									
					threadList[i].interrupt();
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
  	}
}