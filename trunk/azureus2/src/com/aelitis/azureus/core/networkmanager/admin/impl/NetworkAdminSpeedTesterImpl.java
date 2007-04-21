package com.aelitis.azureus.core.networkmanager.admin.impl;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTester;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTestListener;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.AETemporaryFileHandler;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadStats;
import org.gudy.azureus2.plugins.download.DownloadRemovalVetoException;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentImpl;

import java.util.*;
import java.net.URL;
import java.net.URLConnection;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.RandomAccessFile;


public class NetworkAdminSpeedTesterImpl 
	implements NetworkAdminSpeedTester
{
    private PluginInterface plugin;

    private static Result lastResult=null;

    private List listenerList = new ArrayList();//<NetworkAdminSpeedTestListener>


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
	 * The downloads have been stopped just need to do the testing.
	 * @return true if the test seems to have stated successfully.
	 */
	public synchronized boolean start(){

        //OK lets start the test.
        try{
            //Get the file from
            URL urlTestService = new URL("http://seed20.azureusplatform.com:60000/speedtest?");
            byte[] torrentBytes = getTestTorrentFromService(urlTestService);

            Map m = BDecoder.decode(torrentBytes);
            Map torrentMap = (Map) m.get("torrent");
            TOTorrent tot = TOTorrentFactory.deserialiseFromMap(torrentMap);

            Torrent torrent = new TorrentImpl(tot);

            long fileSize = tot.getSize();
            byte[] fileNameBytes = tot.getName();
            String fileName = new String(fileNameBytes);

            //create a blank file of specified size. (using the temporary name.)
            File saveLocation = AETemporaryFileHandler.createTempFile();
            File baseDir = saveLocation.getParentFile();
            File blankFile = createBlankFileOfSize( baseDir, fileName, fileSize );
            File blankTorrentFile = new File( baseDir, "speedTestTorrent.torrent" );
            torrent.writeToFile(blankTorrentFile);

            //Now write half of the file with one.
            //ToDo:  implement a better method that will mark each piece as done in the future.
            long pieceCount = tot.getNumberOfPieces();
            long pieceSize = tot.getPieceLength();
            writeHalfFileWithOnes(blankFile,pieceCount, pieceSize);

            plugin.getDownloadManager().addDownload( torrent, blankTorrentFile ,blankFile);

            TorrentSpeedTestMonitorThread monitor = new TorrentSpeedTestMonitorThread(plugin,torrent,listenerList);
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
        listenerList.add( listener );
    }

    /**
     * Send a Result to all of the NetworkAdminSpeedTestListeners.
     * @param r - Result of the test.
     */
    private void sendResultToListeners(Result r){
        int n = listenerList.size();
        for( int i=0; i<n; i++){
            NetworkAdminSpeedTestListener nas = (NetworkAdminSpeedTestListener)listenerList.get(i);
            nas.complete(r);
        }
    }

    /**
     * Send a status update to all of the listeners.
     * @param status - String to send to the UI.
     */
    private void sendStageUpdateToListeners(String status){
        int n = listenerList.size();
        for( int i=0; i<n; i++){
            NetworkAdminSpeedTestListener nas = (NetworkAdminSpeedTestListener)listenerList.get(i);
            nas.stage(status);
        }
    }


    // ------------------ private methods ---------------

    /**
     * Take the blank file and put ones into half of it.
     * @param blankFile - file created on local file system for testing. Should have all zeros at this point.
     * @param pieceCount - number of pieces
     * @param pieceSize - size of each piece
     * @throws IOException - FileNotFound Exception possible.
     */
    private static void writeHalfFileWithOnes(File blankFile,  long pieceCount, long pieceSize)
        throws IOException
    {
        RandomAccessFile raf = new RandomAccessFile(blankFile,"rw");

        byte[] byteSeq = createByteSequence( (int)pieceSize );
        for(int i=0; i<pieceCount/2;i++){
            raf.write(byteSeq);
        }
        raf.close();

    }//writeHalfFileWithOnes

    /**
     * Return byte[] with all bits set to one.
     * @param size - of byte array.
     * @return byte[]
     */
    private static byte[] createByteSequence(int size)
    {
        //NOTE: the real purpose for this is to random bytes from a seed.
        byte[] retVal = new byte[size];
        for(int i=0;i<size;i++){
            retVal[i] = (byte) 0xFF;
        }//for
        return retVal;
    }//createByteSequence


    /**
     *
     * @param baseDir - base directory.
     * @param name - torrent file name.
     * @param size - torrent file size which should be a multiple of 1024.
     * @return File that has all zeros in it.
     * @throws IOException -
     */
    private static File createBlankFileOfSize(File baseDir, String name, long size)
        throws IOException
    {
        File retVal = new File(baseDir,name);

        //to ensure the file is deleted when JVM exits.
        retVal.deleteOnExit();

        //Write file with zeros
        RandomAccessFile raf = new RandomAccessFile(retVal,"rw");
        try{
            byte[] buffer = new byte[1024*1024];
            long remaining = size;
            while( remaining>0 ){
                if( remaining < buffer.length ){
                    //int offset = (int) (size-remaining);//maybe offest is from the current pointer?
                    raf.write(buffer,0,(int)remaining);
                    break;
                }//if
                raf.write(buffer);
                remaining -= buffer.length;
            }//while
        }finally{
            raf.close();
        }

        return retVal;
    }//createBlankFileOfSize


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


    /**   -------------------- helper class to monitor test. ------------------- **/
    public class TorrentSpeedTestMonitorThread
        extends Thread
    {
        List historyDownloadSpeed = new LinkedList();  //<Long>
        List historyUploadSpeed = new LinkedList();    //<Long>
        List timestamps = new LinkedList();            //<Long>

        Torrent testTorrent;
        PluginInterface plugin;
        List listenerList; //List<NetworkAdminSpeedTestListener>

        public static final long MAX_TEST_TIME = 2*60*1000; //Limit test to 2 minutes.
        public static final long MAX_PEAK_TIME = 30 * 1000; //Limit to 30 seconds at peak.
        long startTime;
        long peakTime;
        long peakRate;

        public static final String AVE = "ave";
        public static final String STD_DEV = "stddev";

        public TorrentSpeedTestMonitorThread(PluginInterface pi, Torrent t, List listeners)
        {
            plugin = pi;
            testTorrent = t;
            listenerList = listeners;
        }

        public void run()
        {
            try
            {
                startTime = SystemTime.getCurrentTime();
                peakTime = startTime;

                boolean testDone=false;
                Download d = plugin.getDownloadManager().getDownload(testTorrent);
                long lastTotalDownloadBytes=0;

                //ToDo: use this condition to signal a manual abort.
                while( !testDone ){

                    long currTime = SystemTime.getCurrentTime();
                    DownloadStats stats = d.getStats();
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
                        //To//Do: Replace with an error result.
                        //System.out.println("TorrentSpeedTestMonitorThread was interrupted before test completed.");
                        String msg = "TorrentSpeedTestMonitorThread was interrupted before test completed.";
                        //ToDo: unfortunately we cannot send the Result to the listeners, since we are NOT done yet!!
                        //ToDo: This will be the same condition on a manual abort, find one way to handle both conditions.
                        break;
                    }

                }//while

                //It is time to stop the test.
                try{
                    d.stop();
                    d.remove();
                }catch(DownloadException de){
                    String msg = "TorrentSpeedTestMonitorThread could not stop the torrent "+testTorrent.getName();
                    sendResultToListeners( new BitTorrentResult(msg) );
                }catch(DownloadRemovalVetoException drve){
                    String msg = "TorrentSpeedTestMonitorTheard could not remove the torrent "+testTorrent.getName();
                    sendResultToListeners( new BitTorrentResult(msg) );
                }

            }catch(Exception e){
                System.out.println("Error: "+e);
            }

            //calculate the measured download rate.
            Result r = calculateDownloadRate();

            sendResultToListeners(r);

            //To//Do: call listeners!! We are done!!
            System.out.println("Finished with bandwidth testing. "+r.toString() );
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
