/*
 * Created on 28.11.2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.ui.common.util;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.DiskManagerListener;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerDiskListener;
import org.gudy.azureus2.core3.download.impl.DownloadManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.impl.GlobalManagerAdpater;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;

import java.applet.Applet;
import java.applet.AudioClip;
import java.io.File;
import java.net.URL;

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
		final DownloadManagerAdapter download_manager_listener = 
			new DownloadManagerAdapter()
			{
				public void
				downloadComplete(DownloadManager manager)
				{
					activityFinished( true );
				}
			}; 
		
		final DiskManagerListener	disk_listener = 
			new DiskManagerListener()
			{
				public void
				stateChanged(
					int oldState, 
					int	newState )
				{	
				}
				
				public void
				filePriorityChanged(
					DiskManagerFileInfo		file )
				{
				}
	
				public void
				pieceDoneChanged(
					DiskManagerPiece		piece )
				{
				}
				
				public void
				fileAccessModeChanged(
					DiskManagerFileInfo		file,
					int						old_mode,
					int						new_mode )
				{
					if ( 	old_mode == DiskManagerFileInfo.WRITE &&
							new_mode == DiskManagerFileInfo.READ ){
						
						activityFinished( false );
					}
				
					/*
					System.out.println( 
						"amc:" + 
						file.getDownloadManager().getDisplayName() + "/" + 
						file.getName() + ":" + old_mode + " -> " + new_mode );
					*/
				}
			};
			
		final DownloadManagerDiskListener dm_disk_listener = 
			new DownloadManagerDiskListener()
			{
				public void
				diskManagerAdded(
					DiskManager	dm )
				{
					dm.addListener( disk_listener );
				}
				
				public void
				diskManagerRemoved(
					DiskManager	dm )
				{
					dm.removeListener( disk_listener );
				}
			};

    	global_manager.addListener(
    		new GlobalManagerAdpater()
    		{
				public void 
				downloadManagerAdded(DownloadManager manager) 
				{
					manager.addListener( download_manager_listener );
					
					manager.addDiskListener( dm_disk_listener );
				}

				public void 
				downloadManagerRemoved(DownloadManager manager) 
				{
					manager.removeListener(download_manager_listener);
					
					manager.removeDiskListener( dm_disk_listener );
				}  
				
				public void
				destroyed()
				{
					tidyUp();
				} 		
			}); 			
     }

  	protected void
  	activityFinished(
  		boolean		download )
  	{
  		final String sound_enabler;
  		final String sound_file;
  		final String default_sound 	= "org/gudy/azureus2/ui/icons/downloadFinished.wav";
  		
  		final String speech_enabler;
  		final String speech_text;
  		
  		if ( download ){
	 		sound_enabler 	= "Play Download Finished";
	  		sound_file		= "Play Download Finished File";
	  		
	  		speech_enabler 	= "Play Download Finished Announcement";
	  		speech_text		= "Play Download Finished Announcement Text";
  		}else{
	 		sound_enabler 	= "Play File Finished";
	  		sound_file		= "Play File Finished File";
	  		
	  		speech_enabler 	= "Play File Finished Announcement";
	  		speech_text		= "Play File Finished Announcement Text";
  		}
  		
  		try{
  			this_mon.enter();

            if(Constants.isOSX) { // OS X cannot concurrently use SWT and AWT
                new AEThread("DownloadSound") {
                    public void runSupport()
                    {
                        try {
                            if(COConfigurationManager.getBooleanParameter( speech_enabler ))
                                Runtime.getRuntime().exec(new String[]{"say", COConfigurationManager.getStringParameter( speech_text )}); // Speech Synthesis services

                            if(COConfigurationManager.getBooleanParameter( sound_enabler ))
                                Runtime.getRuntime().exec(new String[]{"osascript", "-e" ,"beep"}); // Beep alert type is in accordance with System Preferences

                            Thread.sleep(2500);
                        }
                        catch(Throwable e) {}
                    }
                }.start();
            }
	    	else if( COConfigurationManager.getBooleanParameter( sound_enabler, false)){

	    		String	file = COConfigurationManager.getStringParameter( sound_file );

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

	    						LGLogger.logUnrepeatableAlert(
	    								LGLogger.AT_ERROR,
	    								"Failed to load audio file '" + file + "'" );
	    					}
	    				}
	    			}

	    				// either non-explicit or explicit missing

	    			if ( audio_clip == null ){

	    				audio_clip = Applet.newAudioClip(UserAlerts.class.getClassLoader().getResource( default_sound ));

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