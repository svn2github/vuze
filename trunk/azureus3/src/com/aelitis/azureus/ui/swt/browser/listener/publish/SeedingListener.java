
package com.aelitis.azureus.ui.swt.browser.listener.publish;


import java.util.*;

import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.json.JSONArray;
import org.json.JSONObject;

import com.aelitis.azureus.ui.swt.browser.msg.AbstractMessageListener;
import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;

public class SeedingListener extends AbstractMessageListener {

	private static final long INFINITE_ETA = 31535999;  //1 year
	private static final long STOPPED_ETA  = -88;
	private static final long ERROR_ETA	   = -99;

	private static final String JS_UPLOAD_PROGRESS_MSG_KEY = "upload-progress";
	private static final String JS_INDIVI_UPDATE_MSG_OP = "torrents";
	private static final String JS_GLOBAL_UPDATE_MSG_OP = "global";
	
	public static final String DEFAULT_LISTENER_ID = "seeding";

	public static final String OP_SEND_UPDATE = "send-update";
	public static final String OP_REMOVE = "remove";
	public static final String OP_STOP = "stop";
	public static final String OP_START = "start";
	
	private PluginInterface pluginInterface;
	
	//private DownloadWillBeRemovedListener downloadWillBeRemovedistener;	
	private DownloadStateAndRemoveListener downloadListener;
	
	public SeedingListener(PluginInterface pi,DownloadStateAndRemoveListener downloadListener) {
        this(DEFAULT_LISTENER_ID, pi,downloadListener);
	}
	
    public SeedingListener(String id, PluginInterface pi,DownloadStateAndRemoveListener downloadListener) {
        super(id);
        this.pluginInterface = pi;
        
        this.downloadListener = downloadListener;
        
    }
	
	public void handleMessage(BrowserMessage message) {
		if ( OP_SEND_UPDATE.equals(message.getOperationId()) ) {
            sendUpdate();
        } else if ( OP_REMOVE.equals(message.getOperationId()) ) {
            removeTorrent(message.getDecodedObject().getString("id"));
        } else if ( OP_START.equals(message.getOperationId()) ) {
            startTorrent(message.getDecodedObject().getString("id"));
        } else if ( OP_STOP.equals(message.getOperationId()) ) {
            stopTorrent(message.getDecodedObject().getString("id"));
        }
        else {
            throw new IllegalArgumentException("Unknown operation: " + message.getOperationId());
        }
	}
	
	private Download getDownloadByMagnet(String magnet) {
		DownloadManager dm = pluginInterface.getDownloadManager();
		
		Download[] downloads = dm.getDownloads();
		
		for( int i = downloads.length -1; i >=0; i-- ) {
			Download d = downloads[i];
			
			Torrent torrent = d.getTorrent();
			
			if (torrent != null) {
  			String downloadId = Base32.encode(torrent.getHash());
  			
  			if(downloadId.equals(magnet)) {
  				return d;
  			}
			}
		}
		
		return null;
	}
	
	private void removeTorrent(String id) {
		
		final Download d = getDownloadByMagnet(id);
		
		if(d != null) {
			if(d.getState() != Download.ST_STOPPED) {
				d.addListener(new DownloadListener() {
					public void stateChanged(Download download, int old_state, int new_state) {
						if(new_state == Download.ST_STOPPED) {
							try {		
								d.removeDownloadWillBeRemovedListener(downloadListener);
								download.remove();
							} catch (DownloadException e) {							
								e.printStackTrace();
							} catch (DownloadRemovalVetoException e) {														
								e.printStackTrace();
							} finally {
								download.removeListener(this);
							}
						}
					}
					public void positionChanged(Download download, int oldPosition, int newPosition) {}
				});
				stop(d);
			} else {
				try {
					d.removeDownloadWillBeRemovedListener(downloadListener);
					d.remove();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}			
		}		
	}
	
	private void startTorrent(String id) {
		Download d = getDownloadByMagnet(id);
		
		if(d != null) {
			try {
				d.setForceStart(true);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void stopTorrent(String id) {
		Download d = getDownloadByMagnet(id);		
		stop(d);
	}
	
	private void stop(Download download) {
		try {
			download.removeListener(downloadListener);
			download.stop();
			download.addListener(downloadListener);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
   
	private void sendUpdate() {
		try {
			
			DownloadManager dm = pluginInterface.getDownloadManager();
			
			Download[] downloads = dm.getDownloads();    			
		
			long total_bytes = 0;
			long total_bytes_remaining = 0;
			long total_up_rate_bps = 0;
		
         int num_actually_uploading = 0;
         
         ArrayList indiv_torrents = new ArrayList();			
			
			for( int i = downloads.length -1; i >=0; i-- ) {   //loop through all the running torrents
			
				Download d = downloads[i];
			
				if( isPublished( d ) ) {  //this one we're uploading as published  					  
				
					//TODO use something more explicit than swarm availability / uploaded bytes
					
					long download_size = d.getTorrent().getSize();
					
					int percent_done =  (int)( (d.getStats().getUploaded() * 100) / download_size );							
					
					if( percent_done > 99 ) {	//100% uploaded
                  indiv_torrents.add( new IndividualProgress( d.getTorrent().getHash(), d.getName(), 100, 0 ) );
					}
					else {  //upload still in progress
                  
                  total_bytes += download_size;
                  
                  long bytes_remaining = (long)(download_size * ( (float)(100 - percent_done) /100 ) );  //rough
                  
                  total_bytes_remaining += bytes_remaining;  //global stats
                  
						long eta = INFINITE_ETA;  //so it shows infinity						
						
						if( d.getState() == Download.ST_STOPPED ) {
						   eta = STOPPED_ETA;                                  
						}
						else if( d.getState() == Download.ST_ERROR ) {
						   eta = ERROR_ETA;
						}
						else {                       
                     num_actually_uploading++;  //running and upload still needed
                     
							long up_rate_bps = d.getStats().getUploadAverage();
						
							if( up_rate_bps > 0 ) {
								total_up_rate_bps += up_rate_bps;  //global stats
						
								eta = bytes_remaining / up_rate_bps;  //seconds remaining
							}
						}
				
                  indiv_torrents.add( new IndividualProgress( d.getTorrent().getHash(), d.getName(), percent_done, eta ) );						
					}
				}
			}
		
		
			if( !indiv_torrents.isEmpty() ) {   //there is something to update				
				
				long g_percent = 100;
				long g_eta = INFINITE_ETA;
				
				if( total_bytes > 0 ) {   //there is still torrent data to upload
					g_percent = ((total_bytes - total_bytes_remaining) *100) / total_bytes;				

					if( total_up_rate_bps > 0 ) {					
						g_eta = total_bytes_remaining / total_up_rate_bps;  //seconds remaining					
					}       
               
               if( num_actually_uploading < 1 ) {  //all in stopped state
                  g_eta = STOPPED_ETA;
                  g_percent = 0;
               }               
				}
            else {   //done uploading
               g_eta = 0;
            }

            JSONArray torrents = new JSONArray();
            
            for( Iterator it = indiv_torrents.iterator(); it.hasNext(); ) {
               IndividualProgress ind = (IndividualProgress)it.next();

               long mod_eta = ind.eta;
               
               if( g_eta > 0 && ind.eta > g_eta ) {   //for user display purposes, limit indiv eta to max what the global eta is showing
                  mod_eta = g_eta;                  
               }
               
               torrents.put( constructJSTorrentProgress( ind.infohash, ind.name, ind.percent, mod_eta ) );
            }
                     
				context.sendBrowserMessage( JS_UPLOAD_PROGRESS_MSG_KEY, JS_GLOBAL_UPDATE_MSG_OP, constructJSGlobalProgress( (int)g_percent, g_eta ) );
            
            context.sendBrowserMessage( JS_UPLOAD_PROGRESS_MSG_KEY, JS_INDIVI_UPDATE_MSG_OP, torrents );
			}    			
			
		}
		catch( Throwable tt ) {
			tt.printStackTrace();
		}
	}
	
	
	
	/*
	 * TODO : Thsi method is duplicated in the DirectorPlugin
	 */
	private boolean isPublished( Download d ) {
	  	//get the "content" attribute for the download
	  	final TorrentAttribute attrib = pluginInterface.getTorrentManager().getPluginAttribute( TorrentAttribute.TA_CONTENT_MAP );
	  	
	  	Map content_map = d.getMapAttribute( attrib );
			
	    //this one we're uploading as published
			if( content_map != null && content_map.containsKey( PublishTransaction.PUBLISH_ATTRIBUTE_KEY ) ) {   			
				return true;			
			}
	  	
	  	return false;
	}

	
	private JSONObject constructJSTorrentProgress( byte[] infohash, String name, int percent, long _eta ) {
	    String hash = infohash == null ? "<null>" : Base32.encode( infohash );
       String eta = formatETA( _eta );	    
	    
	    JSONObject torrent = new JSONObject();
	    torrent.put("hash", hash);
	    torrent.put("name", name);
	    torrent.put("percent", percent);
	    torrent.put("eta", eta);
	    
	    return torrent;
	  }

   
	private JSONObject constructJSGlobalProgress( int percent, long _eta ) {
      String eta = formatETA( _eta );
	    
      JSONObject global = new JSONObject();
      global.put("percent", percent);
      global.put("eta", eta);
	    
      return global;
	}
	
	
   
   private String formatETA( long eta ) {     
      if( eta == INFINITE_ETA ) {
         return "";
      }      
      else if( eta == STOPPED_ETA ) {
         return "x";
       }
       else if( eta == ERROR_ETA ) {
         return "e";
       }
      
       return pluginInterface.getUtilities().getFormatters().formatETAFromSeconds( eta );      
   }
   
   
   
   private static class IndividualProgress {
      private final byte[] infohash;
      private final String name;
      private final int percent;
      private final long eta;
      
      private IndividualProgress( byte[] _infohash, String _name, int _percent, long _eta ) {
         this.infohash = _infohash;
         this.name = _name;
         this.percent = _percent;
         this.eta = _eta;
      }
   }
      
   
}
