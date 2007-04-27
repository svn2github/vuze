package com.aelitis.azureus.core.networkmanager.admin.impl;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTestScheduler;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTester;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTestListener;
import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderFactoryImpl;
import org.eclipse.swt.widgets.Label;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.net.URL;
import java.net.URLEncoder;
import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.RandomAccessFile;

/**
 * User: asnyder
 * Date: Apr 17, 2007
 * Time: 5:43:45 PM
 * Azureus - 2007
 */
public class NetworkAdminSpeedTestSchedulerImpl
        implements NetworkAdminSpeedTestScheduler
{

    private static NetworkAdminSpeedTestSchedulerImpl instance = null;

    private static PluginInterface plugin;
    private NetworkAdminSpeedTester.Result lastResult;

    private NetworkAdminSpeedTester currentTest = null;
    private SpeedTestDownloadState preTestSettings;
    private List testListeners = new ArrayList(); //List<NetworkAdminSpeedTestListener>

    private Boolean testRunning=Boolean.FALSE;
    private String testStatus = NOT_RUNNING;
    private Map mapForTest;

    /** Types of speed tests **/
    public static final int BIT_TORRENT_UPLOAD_AND_DOWNLOAD = 7777;
    public static final int BIT_TORRENT_DOWNLOAD = 7778;
    public static final int TCP_DOWNLOAD = 7779;
    public static final int TCP_UPLOAD = 7780;

    //Types of requests sent to SpeedTest scheduler.
    private static final String REQUEST_TEST = "0";
    private static final String CHALLENGE_REPLY = "1";

    private static final String NOT_RUNNING = "Not Running.";
    private static long ONE_HOUR = 60 * 60 * 1000;

    private static int ZERO_DOWNLOAD_SETTING = -1;

    public static synchronized NetworkAdminSpeedTestScheduler getInstance(){
        if(instance==null){
            AzureusCore ac = AzureusCoreFactory.getSingleton();
            PluginManager pm = ac.getPluginManager();
                        
            instance = new NetworkAdminSpeedTestSchedulerImpl(pm.getDefaultPluginInterface());
        }
        return instance;
    }

    private NetworkAdminSpeedTestSchedulerImpl(PluginInterface pi){
        plugin = pi;
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
    public boolean requestTestFromService(int type){

        try{
            //Send "schedule test" request.
            Map request = new HashMap();
            request.put("request_type", new Long(REQUEST_TEST) );

            String id = COConfigurationManager.getStringParameter("ID","unknown");
            String ver = COConfigurationManager.getStringParameter("azureus.version","0.0.0.0");
            request.put("az-id",id); //Where to I get the AZ-ID and client version from the Configuration?
            request.put("type","both");
            request.put("jar_ver",ver);
            request.put("ver", new Long(1) );

            String speedTestServiceName = System.getProperty( "speedtest.service.ip.address", "seed20.azureusplatform.com" );

            URL urlRequestTest = new URL("http://"+speedTestServiceName+":60000/scheduletest?request="
                    + URLEncoder.encode( new String(BEncoder.encode(request)),"ISO-8859-1"));

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
                scheduleTestWithSpeedTestService( result );
                return true;
            }else{
                throw new IllegalStateException( "Unrecongnized response from speed test scheduling servcie." );
            }

        }catch(Throwable t){
            Debug.printStackTrace(t);
            return false;
        }
    }//requestTestFromService.

    /**
     * Just verify that the Map is valid for starting the test.
     * @param result -
     * @return int - time to wait before starting test in milliseconds.
     */
    private int scheduleTestWithSpeedTestService(Map result){

        //Expected values in this Map are:
        //torrent
        //time (in milliseconds)
        //limit (in kpbs)
        //result = 1 (success)

        Long time = (Long) result.get("time");
        Long limit = (Long) result.get("limit");

        if( time==null || limit==null )
            throw new IllegalArgumentException("scheduleTestWithSpeedTestService had a null parameter.");

        mapForTest = result;

        return time.intValue();
    }//scheduleTestWithSpeedTestService

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
            //read the bytes
            raf = new RandomAccessFile( azureusJarPath, "r" );
            byte[] jarBytes = new byte[size.intValue()];
            raf.read( jarBytes, offset.intValue(), size.intValue() );

            //Build the URL.
            Map request = new HashMap();
            request.put("request_type", new Long(CHALLENGE_REPLY) );
            request.put("challenge_id",id);
            request.put("data",jarBytes);
            request.put("ver", new Long(1) );//request version

            String speedTestServiceName = System.getProperty( "speedtest.service.ip.address", "seed20.azureusplatform.com" );
            URL urlRequestTest = new URL("http://"+speedTestServiceName+":60000/scheduletest?request="
                    + URLEncoder.encode( new String(BEncoder.encode(request)),"ISO-8859-1"));

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
     * Create a test of type.
     *
     * @param type -
     */
    public synchronized void start(int type) {

        testRunning=Boolean.TRUE;

        //ToDo: change the interface to a boolean incase it fails to start!!!

        //stop the downloads.
        DownloadManager dm = plugin.getDownloadManager();
        pauseAllDownloads(dm);

        //create the test
        if(mapForTest==null)
            currentTest = createTest(type);
        else
            currentTest = createTest(mapForTest);

        //add Scheduler's listeners here so it knows when it stops, aborts, crashes!!
        EndOfTestListener testListener = new EndOfTestListener(testRunning,lastResult,testStatus,dm);
        currentTest.addListener( testListener );

        int nListeners = testListeners.size();
        for(int i=0;i<nListeners; i++){
            currentTest.addListener( (NetworkAdminSpeedTestListener) testListeners.get(i) );
        }

        //start it.
        currentTest.start();
    }

    /**
     * If a test is currently running abort it.
     */
    public synchronized void abort() {

        if(currentTest!=null){
            currentTest.abort();
        }
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * @return true is a test is already running.
     */
    public boolean isRunning() {

        return testRunning.booleanValue();
    }

    /**
     * @return true if a test has not been run in the past hour.
     */
    public boolean isComplete() {

        //check for a result.
        if(lastResult==null)
            return false;

        //has it been longer then an hour?
        long currTime = SystemTime.getCurrentTime();
        //return currTime <= lastResult.getTestTime() + ONE_HOUR;
        //ToDo: restore one hour setting after testing is done.
        final long ONE_MIN = 60 * 1000;
        return currTime <= lastResult.getTestTime() + ONE_MIN;        
    }

    /**
     * Get the most recent result for the test.
     *
     * @return - Result
     */
    public NetworkAdminSpeedTester.Result getLastResult() {
        return lastResult;
    }

    /**
     * Add NetworkSpeedTestListener to this test. Pass along to test. This method
     * must be called after the test is created.
     *
     * @param listener -
     * @throws IllegalStateException
     */
    public void addSpeedTestListener(NetworkAdminSpeedTestListener listener)
        throws IllegalStateException
    {
        testListeners.add( listener );
    }//addSpeedTestListener

    /**
     * Send a stage message to NetworkAdminSpeedTestListeners
     *
     * @param message - text to send. Keep it short.
     */
    public void sendStateMessageToListeners(String message) {
        //ToDo: implement. Move listener from Test in Schedule class.
    }

    /**
     * @deprecated
     * @param type -
     * @return -
     */
    protected NetworkAdminSpeedTester createTest(int type){
        if( type == BIT_TORRENT_UPLOAD_AND_DOWNLOAD){
            return new NetworkAdminSpeedTesterImpl(plugin);
        }
        //else if(){}

        throw new IllegalArgumentException("failed to create NetworkAdminSpeedTester test.");
    }


    /**
     * A factory method to select a test implementation.
     * @param m - Map file recieved from the service.
     * @return NetworkAdminSpeedTester class for this test.
     */
    protected NetworkAdminSpeedTester createTest(Map m){

        //Currently only the BitTorrent upload and download test is implemented.
        //if the map contains a torrent then it will do the upload and download.
        if( m.containsKey("torrent") ){
            return new NetworkAdminSpeedTesterImpl(plugin,m);    
        }else{
            throw new IllegalStateException("failed to create NetworkAdminSpeedTest.");
        }

    }

    /**
     * Restore all the downloads the state before the speed test started.
     */
    private synchronized void restoreAllDownloads(){

        //restore the global settings.
        preTestSettings.restoreGlobalLimits();

        //restore the individual
        preTestSettings.restoreIndividualLimits();
    }//restoreAllDownloads

    /**
     * Preserve all the data about the downloads while the test is running.
     * @param dm - DownloadManager.
     */
    private synchronized void pauseAllDownloads(DownloadManager dm){

        preTestSettings = new SpeedTestDownloadState();

        //preserve the limits for all the downloads and set each to zero.
        Download[] d = dm.getDownloads();
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

        COConfigurationManager.setParameter(TransferSpeedValidator.UPLOAD_CONFIGKEY,300);
        COConfigurationManager.setParameter(TransferSpeedValidator.UPLOAD_SEEDING_CONFIGKEY,300);
        COConfigurationManager.setParameter(TransferSpeedValidator.DOWNLOAD_CONFIGKEY,300);

    }//pauseAllDownloads


    // ---------------    HELPER CLASSES BELOW HERE   ---------------- //

    /**
     * Listener class to get status and completion events from the speed test.
     */
    class EndOfTestListener implements NetworkAdminSpeedTestListener{

        Boolean running;
        NetworkAdminSpeedTester.Result result;
        String testStatus;
        DownloadManager downloadManager;

        public EndOfTestListener(Boolean testRunning, NetworkAdminSpeedTester.Result res,
                                 String status, DownloadManager dm)
        {
            running = testRunning;
            result = res;
            testStatus = status;
            downloadManager = dm;
        }

        /**
         * When a test completes.
         *
         * @param res - String with the result
         */
        public void complete(NetworkAdminSpeedTester.Result res)
        {
            result=res;
            //stage("Not Running");

            restoreAllDownloads();
            testRunning=Boolean.FALSE;

            //clear list of handler - don't want this list to grow with each test.
            testListeners = new ArrayList();
            
            Debug.out( res.toString() );
        }//complete.

        /**
         * Informs listener when the test is at a new stage.
         *
         * @param update - String with stage.
         */
        public void stage(String update) {
            testStatus = update;
        }

    }//class EndOfTestListener

    public static class TextLabelListener implements NetworkAdminSpeedTestListener {
        final Label label;
        String origText;

        public TextLabelListener(final Label l){
            label = l;

            Utils.execSWTThread(new AERunnable(){
                public void runSupport() {
                    origText = l.getText();
                }
            });                           
        }

        /**
         * Restore the Label to its Original contents.
         * @param res - Result of the test.
         */
        public void complete(NetworkAdminSpeedTester.Result res){
            Utils.execSWTThread(new AERunnable(){
                public void runSupport() {
                    label.setText( origText );
                }
            });
        }//complete

        public void stage(final String update){
            Utils.execSWTThread(new AERunnable(){
                public void runSupport() {
                    label.setText(update);
                }
            });
        }//stage
    }//class TextLableListener

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

}//class NetworkAdminSpeedTestSchedulerImpl
