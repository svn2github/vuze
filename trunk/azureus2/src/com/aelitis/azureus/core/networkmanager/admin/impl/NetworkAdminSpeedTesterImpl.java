/**
* Created on Apr 17, 2007
* Created by Alan Snyder
* Copyright (C) 2007 Aelitis, All Rights Reserved.
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
*
* AELITIS, SAS au capital de 63.529,40 euros
* 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
*
*/

package com.aelitis.azureus.core.networkmanager.admin.impl;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTester;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTestListener;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTestScheduler;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadStats;
import org.gudy.azureus2.plugins.download.DownloadRemovalVetoException;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentImpl;

import java.util.*;
import java.net.URL;
import java.net.URLConnection;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;


public class NetworkAdminSpeedTesterImpl 
	implements NetworkAdminSpeedTester
{
    private PluginInterface plugin;
    private Map mapForTest=null;

    private static Result lastResult=null;

    NetworkAdminSpeedTestScheduler scheduler = NetworkAdminSpeedTestSchedulerImpl.getInstance();

    public static final String DOWNLOAD_AVE = "download-ave";
    public static final String UPLOAD_AVE = "upload-ave";
    public static final String DOWNLOAD_STD_DEV = "download-std-dev";
    public static final String UPLOAD_STD_DEV = "upload-std-dev";

    /**
     *
     * @param pi - PluginInterface is used to get Manager classes.
     */
    public NetworkAdminSpeedTesterImpl(PluginInterface pi){
        plugin = pi;
    }

    /**
     * This is the
     * @param pi - PluginInterface
     * @param testMap - Map with test details including the torrent.
     */
    public NetworkAdminSpeedTesterImpl(PluginInterface pi, Map testMap){
        this(pi);
        mapForTest = testMap;
    }//

    /**
	 * The downloads have been stopped just need to do the testing.
	 * @return true if the test seems to have stated successfully.
	 */
	public synchronized boolean start(){

        //OK lets start the test.
        try{
            byte[] torrentBytes;
            sendStageUpdateToListeners("requesting test...");
            //Get the file from
            Map torrentMap;
            if(mapForTest==null){

                String speedTestName = System.getProperty( "speedtest.service.ip.address", "speed.azureusplatform.com" );
                URL urlTestService = new URL("http://"+speedTestName+":60000/speedtest?");
                torrentBytes = getTestTorrentFromService(urlTestService);
                Map m = BDecoder.decode(torrentBytes);
                torrentMap = (Map) m.get("torrent");
            }else{
                torrentMap = getTestTorrentFromMap(mapForTest);
            }

            long startTime = calculateTestStartTimeFromMap(mapForTest);

            TOTorrent tot = TOTorrentFactory.deserialiseFromMap(torrentMap);
            Torrent torrent = new TorrentImpl(tot);

             String fileName = torrent.getName();

            sendStageUpdateToListeners("preparing test...");
            	
            //create a blank file of specified size. (using the temporary name.)
            File saveLocation = AETemporaryFileHandler.createTempFile();
            File baseDir = saveLocation.getParentFile();
            File blankFile = new File(baseDir,fileName);
            File blankTorrentFile = new File( baseDir, "speedTestTorrent.torrent" );
            torrent.writeToFile(blankTorrentFile);

            //wait to start the test.
            long currTime = SystemTime.getCurrentTime();
            while( currTime < startTime ){
                //calculation how long till the test starts.
                long secondsTillTestStarts = (startTime-currTime)/1000;
                sendStageUpdateToListeners("test scheduled in ... "+secondsTillTestStarts);
                Thread.sleep(1000);
                currTime = SystemTime.getCurrentTime();
            }//while

            Download speed_download = plugin.getDownloadManager().addDownloadStopped( torrent, blankTorrentFile ,blankFile);

            TorrentAttribute ta = NetworkAdminSpeedTestSchedulerImpl.getInstance().getTestTorrentAttribute();
            speed_download.setBooleanAttribute(ta,true);

            DownloadManager core_download = PluginCoreUtils.unwrap( speed_download );
            core_download.setPieceCheckingEnabled( false );
            core_download.addPeerListener(
            		new DownloadManagerPeerListener()
            		{
            			public void
            			peerManagerAdded( PEPeerManager	peer_manager )
            			{
            				DiskManager	disk_manager = peer_manager.getDiskManager();
                			DiskManagerPiece[]	pieces = disk_manager.getPieces();
                			for ( int i=(pieces.length/2);i<pieces.length;i++ ){
                				pieces[i].setDone( true );
                			}
            			}
            			
            			public void
            			peerManagerRemoved(PEPeerManager	manager )
            			{    				
            			}
            			
            			public void
            			peerAdded(PEPeer 	peer )
            			{	
            			}
            				
            			public void
            			peerRemoved(PEPeer	peer )
            			{	
            			}
            				
            			public void
            			pieceAdded(PEPiece 	piece )
            			{	
            			}
            				
            			public void
            			pieceRemoved(PEPiece		piece )
            			{	
            			}
                	});
 
            core_download.initialize();
            
            TorrentSpeedTestMonitorThread monitor = new TorrentSpeedTestMonitorThread( speed_download );
            monitor.start();

            //The test has now started!!

        }catch(Exception e){
            sendResultToListeners( new BitTorrentResult("Could not start test due to: "+e) );
            return false;
        }
        return true;
    }

	
	/**
	 * 
	 * @return true abort is successful.
	 */
	public boolean abort(){
        return false;
    }


    /**
	 * Get the result for 
	 * @return Result object of speed test.
	 */
	public NetworkAdminSpeedTester.Result getResult(){
        return lastResult;
    }

    /**
     * To know when result is complete or at a new stage.
     *
     * @param listener -
     */
    public void addListener(NetworkAdminSpeedTestListener listener) {
        scheduler.addSpeedTestListener(listener);
    }

    /**
     * Send a Result to all of the NetworkAdminSpeedTestListeners.
     * @param r - Result of the test.
     */
    private void sendResultToListeners(Result r){
        scheduler.sendResultToListeners(r);
    }


    public void sendStageUpdateToListeners(String status){
        scheduler.sendStageUpdateToListeners(status);
    }

    // ------------------ private methods ---------------

    /**
     * Read from URL and return byte array.
     * @param url -
     * @return byte[] of the results. Max size currently 100k.
     * @throws IOException -
     */
    private static byte[] getTestTorrentFromService(URL url) throws IOException {
        URLConnection urlConn = url.openConnection();
        InputStream is = urlConn.getInputStream();

        byte[] b = new byte[1024*100]; //make 100k torrent size.
        int character;
        int i=0;
        while(  (character = is.read())!=-1 ){
            b[i]=(byte)character;
            i++;
        }
        return b;
    }//getTestTorrentFromService

    /**
     * The Speed Test scheduler service returns a Map with the "torrent" parameter.
     * @param m - Map for this test. It is expected to contain the key "torrent".
     * @return byte[] of the results.
     */
    private static Map getTestTorrentFromMap(Map m){

        if( !m.containsKey("torrent") ){
            Debug.out("The speed test map recieved from the service should contain the torrent.");
            Debug.out("map="+m);
            throw new IllegalStateException("Map for speed test failed to contain the key torrent.");
        }

        return (Map) m.get("torrent");
    }

    /**
     * Read the Map from the test scheduler, and return the time for starting this test. If the input is null
     * then it is assumed that the start time is immediate.
     * @param m - Map recieved from the service which should contain the key "time"
     * @return long of start time for the test.
     */
    private static long calculateTestStartTimeFromMap(Map m){
        long currTime = SystemTime.getCurrentTime();

        if(m==null){
            return currTime;
        }

        //verify that the "time" parameter exists.
        if( !m.containsKey("time") ){
            Debug.out("NetworkAdminSpeedTesterImpl didn't have an param: time.");
            return currTime;
        }

        Long delay = (Long) m.get("time");

        return ( currTime + delay.longValue() );
    }//calculateTestStartTimeFromMap

    /**   -------------------- helper class to monitor test. ------------------- **/
    public class TorrentSpeedTestMonitorThread
        extends Thread
    {
        List historyDownloadSpeed = new LinkedList();  //<Long>
        List historyUploadSpeed = new LinkedList();    //<Long>
        List timestamps = new LinkedList();            //<Long>

        Download testDownload;
 
        public static final long MAX_TEST_TIME = 2*60*1000; //Limit test to 2 minutes.
        public static final long MAX_PEAK_TIME = 30 * 1000; //Limit to 30 seconds at peak.
        long startTime;
        long peakTime;
        long peakRate;

        public static final String AVE = "ave";
        public static final String STD_DEV = "stddev";

        public TorrentSpeedTestMonitorThread( Download d )
        {
            testDownload = d;
        }

        public void run()
        {
            try
            {
                startTime = SystemTime.getCurrentTime();
                peakTime = startTime;

                boolean testDone=false;
                long lastTotalDownloadBytes=0;

                sendStageUpdateToListeners("starting test...");

                //ToDo: use this condition to signal a manual abort.
                while( !testDone ){

                    long currTime = SystemTime.getCurrentTime();
                    DownloadStats stats = testDownload.getStats();
                    historyDownloadSpeed.add( autoboxLong(stats.getDownloaded()) );
                    historyUploadSpeed.add( autoboxLong(stats.getUploaded()) );
                    timestamps.add( autoboxLong(currTime) );

                    lastTotalDownloadBytes = checkForNewPeakValue( stats, lastTotalDownloadBytes, currTime );

                    testDone = checkForTestDone();
                    if(testDone)
                        break;

                    try{ Thread.sleep(1000); }
                    catch(InterruptedException ie){
                        //someone interrupted this thread for a reason. "test is now over"
                        String msg = "TorrentSpeedTestMonitorThread was interrupted before test completed.";
                        Debug.out(msg);
                        sendStageUpdateToListeners(msg);
                        //ToDo: unfortunately we cannot send the Result to the listeners, since we are NOT done yet!!
                        //ToDo: This will be the same condition on a manual abort, find one way to handle both conditions.
                        break;
                    }

                }//while

                //It is time to stop the test.
                try{
                	testDownload.stop();
                	testDownload.remove();
                }catch(DownloadException de){
                    String msg = "TorrentSpeedTestMonitorThread could not stop the torrent "+testDownload.getName();
                    sendResultToListeners( new BitTorrentResult(msg) );
                }catch(DownloadRemovalVetoException drve){
                    String msg = "TorrentSpeedTestMonitorTheard could not remove the torrent "+testDownload.getName();
                    sendResultToListeners( new BitTorrentResult(msg) );
                }

            }catch(Exception e){
                System.out.println("Error: "+e);
            }

            //calculate the measured download rate.
            Result r = calculateDownloadRate();

            //Log the result.
            AEDiagnosticsLogger diagLogger = AEDiagnostics.getLogger("v3.STres");
            diagLogger.log(r.toString());

            sendResultToListeners(r);

            Debug.out("Finished with bandwidth testing. "+r.toString() );
            System.out.println("Finished with bandwidth testing. "+r.toString() );//ToDo: remove.
        }//run.

        /**
         * Calculate the avererage and standard deviation for a history.
         * @param history - calculate average from this list.
         * @return Map<String,Double> with values "ave" and "stddev" set
         */
//        private Map<String,Double> calculateAverageAndStdDevFromHistory(List<Long> history){
          private Map calculateAverageAndStdDevFromHistory(List history){

            long thisTime;
            //find the first element to inlcude in the stat.
            int numStats = history.size();
            int i;
            for(i=0;i<numStats;i++ ){
                thisTime = autoboxLong( timestamps.get(i) );
                if(thisTime>=peakTime){
                    break;
                }
            }//for

            //calculate the average.
            long sumBytes = autoboxLong( history.get(numStats-1) ) - autoboxLong( history.get(i) );
            double aveDownloadRate = (double) sumBytes/(numStats-i);

            //calculate the standard deviation.
            double variance = 0.0;
            double s;
            long thisBytesSent;

            long lastTotalBytes=0;
            if(i>0)
                lastTotalBytes = autoboxLong( history.get(i-1) )-lastTotalBytes;

            for(int j=i;j<numStats;j++){
                thisBytesSent = autoboxLong( history.get(j) )-lastTotalBytes;
                lastTotalBytes = autoboxLong(history.get(j));

                //now do the calculations.
                s = (double) thisBytesSent - aveDownloadRate;
                variance += s*s;
            }//for

            double stddev = Math.sqrt( variance/(numStats-1) );

            //if average is zero, then don't use the standard deviation calculation.
            if(aveDownloadRate==0.0){
                stddev = 0.0;
            }//if

            //Map<String,Double> retVal = new HashMap<String,Double>();
            Map retVal = new HashMap();
            retVal.put(AVE, autoboxDouble(aveDownloadRate));
            retVal.put(STD_DEV,autoboxDouble(stddev));
            return retVal;
        }//calculateAverageAndStdDevFromHistory

        /**
         * Based on the previous data cancluate an average and a standard deviation.
         * Return this data in a Map object.
         * @return Map<String,Float> as a contain for stats. Map keys are "ave" and "dev".
         */
        Result calculateDownloadRate()
        {
            //calculate the BT download rate.
            //Map<String,Double> resDown = calculateAverageAndStdDevFromHistory(historyDownloadSpeed);
            Map resDown = calculateAverageAndStdDevFromHistory(historyDownloadSpeed);

            //calculate the BT upload rate.
            //Map<String,Double> resUp = calculateAverageAndStdDevFromHistory(historyUploadSpeed);
            Map resUp = calculateAverageAndStdDevFromHistory(historyUploadSpeed);

            return new BitTorrentResult(resUp,resDown);
        }//calculateDownloadRate


        /**
         * In this version the test is limited to MAX_TEST_TIME since the start of the test
         * of MAX_PEAK_TIME (i.e. time since the peak download rate has been reached). Which
         * ever condition is first will finish the download.
         * @return true if the test done condition has been reached.
         */
        boolean checkForTestDone(){

            long currTime = SystemTime.getCurrentTime();
            //have we reached the max time for this test?
            if( (currTime-startTime)>MAX_TEST_TIME ){
                return true;
            }

            //have we been near the peak download value for max time?
            return (currTime - peakTime) > MAX_PEAK_TIME;
        }//checkForTestDone


        /**
         * We set a new "peak" value if it has exceeded the previous peak value by 10%.
         * @param stat -
         * @param lastTotalDownload -
         * @param currTime -
         * @return total downloaded so far.
         */
        long checkForNewPeakValue(DownloadStats stat, long lastTotalDownload, long currTime)
        {
            long totDownload = stat.getDownloaded();
            long currDownloadRate = totDownload-lastTotalDownload;

            //if the current rate is 10% greater then the previous max, reset the max, and test timer.
            if( currDownloadRate > peakRate ){
                peakRate = (long) (currDownloadRate*1.1);
                peakTime = currTime;
            }

            return totDownload;
        }//checkForNewPeakValue


    }//class TorrentSpeedTestMonitorThread

    class BitTorrentResult implements NetworkAdminSpeedTester.Result{

        long time;
        int downspeed;
        int upspeed;
        boolean hadError = false;
        String lastError = "";

        /**
         * Build a Result for a successful test.
         * @param uploadRes - Map<String,Double> of upload results.
         * @param downloadRes - Map<String,Double> of download results.
         */
        public BitTorrentResult(Map uploadRes, Map downloadRes){
            time = SystemTime.getCurrentTime();
            Double dAve = (Double)downloadRes.get(TorrentSpeedTestMonitorThread.AVE);
            Double uAve = (Double)uploadRes.get(TorrentSpeedTestMonitorThread.AVE);
            downspeed = dAve.intValue();
            upspeed = uAve.intValue();
        }

        /**
         * Build a Result if the test failed with an error.
         * @param errorMsg - why the test failed.
         */
        public BitTorrentResult(String errorMsg){
            time = SystemTime.getCurrentTime();
            hadError=true;
            lastError = errorMsg;
        }

        public long getTestTime() {
            return time;
        }

        public int getDownloadSpeed() {
            return downspeed;
        }

        public int getUploadSpeed() {
            return upspeed;
        }

        public boolean hadError() {
            return hadError;
        }

        public String getLastError() {
            return lastError;
        }

        public String toString(){
            StringBuffer sb = new StringBuffer("[com.aelitis.azureus.core.networkmanager.admin.impl.NetworkAdminSpeedTesterImpl");

            if(hadError){
                sb.append(" Last Error: ").append(lastError);
            }else{
                sb.append(" download speed: ").append(downspeed);
                sb.append(" upload speed: ").append(upspeed);
            }
            sb.append(" time=").append(time);
            sb.append("]");

            return sb.toString();
        }
    }//class BitTorrentResult


    private static long autoboxLong(Object o){
        return autoboxLong( (Long) o );
    }

    private static long autoboxLong(Long l){
        return l.longValue();
    }

    private static Long autoboxLong(long l){
        return new Long(l);
    }

    private static Double autoboxDouble(double d){
        return new Double(d);
    }
}//class
