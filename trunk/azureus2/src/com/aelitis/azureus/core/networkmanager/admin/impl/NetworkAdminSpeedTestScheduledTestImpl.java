/*
 * Created on May 1, 2007
 * Created by Paul Gardner
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.networkmanager.admin.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderFactoryImpl;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTestScheduledTest;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTestScheduledTestListener;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTestScheduler;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTester;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTesterListener;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTesterResult;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.plugins.upnp.UPnPPlugin;

public class 
NetworkAdminSpeedTestScheduledTestImpl 
	implements NetworkAdminSpeedTestScheduledTest
{
	   //Types of requests sent to SpeedTest scheduler.
    private static final String REQUEST_TEST = "0";
    private static final String CHALLENGE_REPLY = "1";
    
    private static int ZERO_DOWNLOAD_SETTING = -1;


    private PluginInterface						plugin;
 	private NetworkAdminSpeedTesterImpl			tester;
	
    private String detectedRouter;

	private SpeedTestDownloadState		preTestSettings;
	
	private long		delay_millis;
	private long		max_speed;
	private TOTorrent	test_torrent;
	
	private volatile boolean	aborted;
	
	private CopyOnWriteList		listeners = new CopyOnWriteList();
	
	protected
	NetworkAdminSpeedTestScheduledTestImpl(
		PluginInterface						_plugin,
		NetworkAdminSpeedTesterImpl			_tester )
	{
		plugin		= _plugin;
		tester		= _tester;
	    
        	//detect the router.
		
        PluginInterface upnp = plugin.getPluginManager().getPluginInterfaceByClass( UPnPPlugin.class );
        
        if( upnp != null ){
        	
            detectedRouter = upnp.getPluginconfig().getPluginStringParameter("plugin.info");
        }
        
		tester.addListener(
			new NetworkAdminSpeedTesterListener()
			{
				public void 
				complete(
					NetworkAdminSpeedTester 		tester, 
					NetworkAdminSpeedTesterResult 	result )
				{
					reportComplete();
				}

				public void 
				stage(
					NetworkAdminSpeedTester 	tester, 
					String 						step ) 
				{
				}
			});
	}
	
	public NetworkAdminSpeedTester
	getTester()
	{
		return( tester );
	}
	
	public boolean
	start()
	{
		if ( schedule()){
		
			new AEThread( "NetworkAdminSpeedTestScheduledTest:delay", true )
			{
				public void
				runSupport()
				{
					long delay_ticks = delay_millis/1000;
			
					for (int i=0;i<delay_ticks;i++){

						if ( aborted ){
			            		
			            	break;
			            }
				       
						reportStage( "test scheduled in ... " + ( delay_ticks - i ) + " seconds" );
						
						try{
							Thread.sleep(1000);
							
						}catch( InterruptedException e ){
							
							e.printStackTrace();
						}
					}
					
					if ( !aborted ){
				
						setSpeedLimits();
						
						if ( tester.getTestType() == NetworkAdminSpeedTestScheduler.TEST_TYPE_BITTORRENT ){
						
							((NetworkAdminSpeedTesterBTImpl)tester).start( test_torrent );
							
						}else{
							
							reportStage( "Unsupported test type!!!!" );
							
							tester.abort();
						}
					}
				}
			}.start();
			
			return( true );
			
		}else{
			
			tester.abort();
		}
		
		return( false );
	}
	
	public void
	abort()
	{
		aborted	= true;
		
		tester.abort();
	}
	
	
	   /**
     * Request a test from the speed testing service, handle the "challenge" if request and then get
     * the id for the test.
     *
     * Per spec all request are BEncoded maps.
     *
     * @param type - test type.
     * @return boolean - true if the test has been reserved with the service.
     */
    private boolean 
    schedule()
    {
        try{
            //Send "schedule test" request.
            Map request = new HashMap();
            request.put("request_type", new Long(REQUEST_TEST) );

            String id = COConfigurationManager.getStringParameter("ID","unknown");
            String ver = COConfigurationManager.getStringParameter("azureus.version","0.0.0.0");

            //ToDo: remove once challenge testing is done.
            String debug = System.getProperty("debug.speed.test.challenge","n");
            if( !"n".equals(debug) ){
                //over-ride the jar version, and location for debugging.
                ver="3.0.1.2";
            }//if

            request.put("az-id",id); //Where to I get the AZ-ID and client version from the Configuration?
            request.put("type","both");
            request.put("jar_ver",ver);
            request.put("ver", new Long(1) );

            String speedTestServiceName = System.getProperty( "speedtest.service.ip.address", "speed.azureusplatform.com" );

            URL urlRequestTest = new URL("http://"+speedTestServiceName+":60000/scheduletest?request="
                    + URLEncoder.encode( new String(BEncoder.encode(request),"ISO-8859-1"),"ISO-8859-1"));

            Map result = getBEncodedMapFromRequest( urlRequestTest );
            Long responseType =  (Long) result.get("reply_type");

            if( responseType.intValue()==1 ){
                //a challenge has occured.
                result = handleChallengeFromSpeedTestService( result );
                responseType = (Long) result.get("reply_type");
            }
            if( responseType.intValue()==0 ){
                	//a test has been scheduled.
                	//set the Map properly.
            	
                Long time = (Long) result.get("time");
                Long limit = (Long) result.get("limit");

                if( time==null || limit==null ){
                    throw new IllegalArgumentException("scheduleTestWithSpeedTestService had a null parameter.");
                }
                
                delay_millis 	= time.longValue();
                max_speed		= limit.longValue();
                
                	// this is test-specific data
                
                Map torrentMap = (Map)result.get("torrent");
    	
                test_torrent = TOTorrentFactory.deserialiseFromMap(torrentMap);

                return true;
            }else{
                throw new IllegalStateException( "Unrecongnized response from speed test scheduling servcie." );
            }

        }catch(Throwable t){
        	
        	reportStage( Debug.getNestedExceptionMessage( t ));
            Debug.printStackTrace(t);
            return false;
        }
    }

 

    /**
     *
     * @param result - Map from the previous response
     * @return Map - from the current response.
     */
    private Map handleChallengeFromSpeedTestService(Map result){
        //verify the following items are in the response.

        //size (in bytes)
        //offset (in bytes)
        //challenge_id
        Map retVal = new HashMap();
        RandomAccessFile raf=null;
        try{

            Long size = (Long) result.get("size");
            Long offset = (Long) result.get("offset");
            byte[] idBytes = (byte[]) result.get("challenge_id");

            if( size==null || offset==null || idBytes==null )
                throw new IllegalStateException("scheduleTestWithSpeedTestService had a null parameter.");

            //get the size.
            String id = bytes2String(idBytes);

            //Find the location of the Azureus2.jar file.
            String azureusJarPath = SystemProperties.getAzureusJarPath();

            //ToDo: remove once challenge testing is done.
            String debug = System.getProperty("debug.speed.test.challenge","n");
            if( !"n".equals(debug) ){
                //over-ride the jar version, and location for debugging.
                azureusJarPath = "C:\\test\\azureus\\Azureus3.0.1.2.jar";
            }//if

            //read the bytes
            raf = new RandomAccessFile( azureusJarPath, "r" );
            byte[] jarBytes = new byte[size.intValue()];

            raf.seek(offset.intValue());
            raf.read( jarBytes );


            //Build the URL.
            Map request = new HashMap();
            request.put("request_type", new Long(CHALLENGE_REPLY) );
            request.put("challenge_id",id);
            request.put("data",jarBytes);
            request.put("ver", new Long(1) );//request version

            String speedTestServiceName = System.getProperty( "speedtest.service.ip.address", "speed.azureusplatform.com" );
            URL urlRequestTest = new URL("http://"+speedTestServiceName+":60000/scheduletest?request="
                    + URLEncoder.encode( new String(BEncoder.encode(request),"ISO-8859-1"),"ISO-8859-1"));

            Debug.out("Speed Test Challenge response: "+urlRequestTest);

            //Get the response.
            retVal = getBEncodedMapFromRequest(urlRequestTest);

        }catch( Throwable t ){
            Debug.printStackTrace(t);
        }finally{
            //close
            try{
                if(raf!=null)
                    raf.close();
            }catch(Throwable t){
                Debug.printStackTrace(t);
            }
        }

        return retVal;
    }//handleChallengeFromSpeedTestService


  
    
    /**
     * Convert byte[] into a String
     * @param data - byte[]
     * @return String
     * @throws IllegalArgumentException - if an error occurs.
     */
    private static String bytes2String(byte[] data){
        if(data==null)
            throw new IllegalArgumentException("bytes2String got null input.");

        return new String(data);
    }

    /**
     * Read from URL and return byte array.
     * @param url -
     * @return byte[] of the results. Max size currently 100k.
     * @throws java.io.IOException -
     */
    private static Map getBEncodedMapFromRequest(URL url)
            throws IOException
    {

        ResourceDownloader rd = ResourceDownloaderFactoryImpl.getSingleton().create( url );

        InputStream is=null;
        Map reply = new HashMap();
        try
        {
            is = rd.download();
            reply = BDecoder.decode( new BufferedInputStream(is) );

            //all replys of this type contains a "result"
            Long res = (Long) reply.get("result");
            if(res==null)
                throw new IllegalStateException("No result parameter in the response!! reply="+reply);
            if(res.intValue()==0){
                StringBuffer msg = new StringBuffer("Error occurred on the server side. ");
                String error = new String( (byte[]) reply.get("error") );
                String errDetail = new String( (byte[]) reply.get("error_detail") );
                msg.append("error: ").append(error);
                msg.append(" ,error detail: ").append(errDetail);
                throw new IllegalStateException( msg.toString() );
            }
        }catch(IllegalStateException ise){
            //rethrow this type of exception.
            throw ise;
        }catch(Throwable t){
            Debug.out(t);
            Debug.printStackTrace(t);
        }finally{
            try{
                if(is!=null)
                    is.close();
            }catch(Throwable e){
                Debug.printStackTrace(e);
            }
        }
        return reply;
    }//getBytesFromRequest

    
    /**
     * Restore all the downloads the state before the speed test started.
     */
    protected synchronized void 
    resetSpeedLimits()
    {
    	if ( preTestSettings != null ){
    	
	        //restore the global settings.
	        preTestSettings.restoreGlobalLimits();
	
	        //restore the individual
	        preTestSettings.restoreIndividualLimits();
	        
	        preTestSettings = null;
    	}
    }

    /**
     * Preserve all the data about the downloads while the test is running.
     * @param dm - DownloadManager.
     */
    protected synchronized void setSpeedLimits(){

        preTestSettings = new SpeedTestDownloadState();

        //preserve the limits for all the downloads and set each to zero.
        Download[] d = plugin.getDownloadManager().getDownloads();
        if(d!=null){
            int len = d.length;
            for(int i=0;i<len;i++){

                plugin.getDownloadManager().getStats();
                int downloadLimit = d[i].getUploadRateLimitBytesPerSecond();
                int uploadLimit = d[i].getUploadRateLimitBytesPerSecond();

                Debug.out("pauseDownloads: "+d[i].getName()+" upload: "+uploadLimit+" downloadLimit: "+downloadLimit);
                
                preTestSettings.set(d[i],uploadLimit,downloadLimit);

                d[i].setUploadRateLimitBytesPerSecond(ZERO_DOWNLOAD_SETTING);
                d[i].setDownloadRateLimitBytesPerSecond( ZERO_DOWNLOAD_SETTING );
            }//for
        }//if

        //preserver the global limits
        preTestSettings.saveGlobalLimits();

        //set global limits for speed test. Should we limit it it 300k here?
        COConfigurationManager.setParameter(TransferSpeedValidator.AUTO_UPLOAD_CONFIGKEY,false);
        COConfigurationManager.setParameter(TransferSpeedValidator.AUTO_UPLOAD_SEEDING_CONFIGKEY,false);

        COConfigurationManager.setParameter( TransferSpeedValidator.UPLOAD_CONFIGKEY, max_speed);
        COConfigurationManager.setParameter( TransferSpeedValidator.UPLOAD_SEEDING_CONFIGKEY, max_speed);
        COConfigurationManager.setParameter( TransferSpeedValidator.DOWNLOAD_CONFIGKEY, max_speed);

    }


    // ---------------    HELPER CLASSES BELOW HERE   ---------------- //

    /**
     * Preservers the state of all the downloads before the speed test started.
     */
    class SpeedTestDownloadState{

        private Map torrentLimits = new HashMap(); //Map <Download , Map<String,Integer> >

        public static final String TORRENT_UPLOAD_LIMIT = "u";
        public static final String TORRENT_DOWNLOAD_LIMIT = "d";

        //global limits.
        int maxUploadKbs;
        int maxUploadSeedingKbs;
        int maxDownloadKbs;

        boolean autoSpeedEnabled;
        boolean autoSpeedSeedingEnabled;


        public SpeedTestDownloadState(){}

        /**
         * Save the upload/download limits of this Download object before the test started.
         * @param d - Download
         * @param uploadLimit - int
         * @param downloadLimit - int
         */
        public void set(Download d, int uploadLimit, int downloadLimit){
            if(d==null)
                throw new IllegalArgumentException("Download should not be null.");

            Map props = new HashMap();//Map<String,Integer>

            props.put(TORRENT_UPLOAD_LIMIT, new Integer(uploadLimit) );
            props.put(TORRENT_DOWNLOAD_LIMIT, new Integer(downloadLimit) );

            torrentLimits.put(d,props);
        }

        /**
         * Get the global limits from the TransferSpeedValidator class. Call before starting a speed test.
         */
        public void saveGlobalLimits(){
            //int settings.
            maxUploadKbs = COConfigurationManager.getIntParameter( TransferSpeedValidator.UPLOAD_CONFIGKEY );
            maxUploadSeedingKbs = COConfigurationManager.getIntParameter( TransferSpeedValidator.UPLOAD_SEEDING_CONFIGKEY );
            maxDownloadKbs = COConfigurationManager.getIntParameter( TransferSpeedValidator.DOWNLOAD_CONFIGKEY );
            //boolean setting.
            autoSpeedEnabled = COConfigurationManager.getBooleanParameter( TransferSpeedValidator.AUTO_UPLOAD_CONFIGKEY );
            autoSpeedSeedingEnabled = COConfigurationManager.getBooleanParameter( TransferSpeedValidator.AUTO_UPLOAD_SEEDING_CONFIGKEY );
        }//saveGlobalLimits

        /**
         * Call this method after a speed test completes to restore the global limits.
         */
        public void restoreGlobalLimits(){
            COConfigurationManager.setParameter(TransferSpeedValidator.AUTO_UPLOAD_CONFIGKEY,autoSpeedEnabled);
            COConfigurationManager.setParameter(TransferSpeedValidator.AUTO_UPLOAD_SEEDING_CONFIGKEY,autoSpeedSeedingEnabled);

            COConfigurationManager.setParameter(TransferSpeedValidator.UPLOAD_CONFIGKEY,maxUploadKbs);
            COConfigurationManager.setParameter(TransferSpeedValidator.UPLOAD_SEEDING_CONFIGKEY,maxUploadSeedingKbs);
            COConfigurationManager.setParameter(TransferSpeedValidator.DOWNLOAD_CONFIGKEY,maxDownloadKbs);
        }//restoreGlobalLimits

        /**
         * Call this method after the speed test is completed to restore the individual download limits
         * before the test started.
         */
        public void restoreIndividualLimits(){
            Download[] downloads = getAllDownloads();
            if(downloads!=null){
                int nDownloads = downloads.length;

                for(int i=0;i<nDownloads;i++){
                    int uploadLimit = get(downloads[i], TORRENT_UPLOAD_LIMIT);
                    int downLimit = get(downloads[i], TORRENT_DOWNLOAD_LIMIT);

                    downloads[i].setDownloadRateLimitBytesPerSecond(downLimit);
                    downloads[i].setUploadRateLimitBytesPerSecond(uploadLimit);

                    System.out.println("restoreDownloads: "+downloads[i].getName()+" upload: "+uploadLimit+" download: "+downLimit);
                }//for
            }//if
        }//restoreIndividualLimits

        /**
         * Get the upload or download limit for this Download object before the test started.
         * @param d - Download
         * @param param - String
         * @return - limit as int.
         */
        public int get(Download d, String param){
            if(d==null || param==null )
                throw new IllegalArgumentException("null inputs.");

            if(!param.equals(TORRENT_UPLOAD_LIMIT) && !param.equals(TORRENT_DOWNLOAD_LIMIT))
                throw new IllegalArgumentException("invalid param. param="+param);

            Map out = (Map) torrentLimits.get(d);
            Integer limit = (Integer) out.get(param);

            return limit.intValue();
        }

        /**
         * Get all the Download keys in this Map.
         * @return - Download[]
         */
        public Download[] getAllDownloads(){
            Download[] a = new Download[0];
            return (Download[]) torrentLimits.keySet().toArray(a);
        }

    }//class SpeedTestDownloadState

	protected void
	reportStage(
		String	str )
	{
		Iterator	it = listeners.iterator();
		
		while( it.hasNext()){
			
			try{
				((NetworkAdminSpeedTestScheduledTestListener)it.next()).stage( this, str );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
	}
	
	protected void
	reportComplete()
	{		
		resetSpeedLimits();

		Iterator	it = listeners.iterator();
		
		while( it.hasNext()){
			
			try{
				((NetworkAdminSpeedTestScheduledTestListener)it.next()).complete( this );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
	}
	
	public void
	addListener(
		NetworkAdminSpeedTestScheduledTestListener	listener )
	{
		listeners.add( listener );
	}
	
	public void
	removeListener(
		NetworkAdminSpeedTestScheduledTestListener	listener )
	{
		listeners.remove( listener );
	}
}
