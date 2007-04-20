package com.aelitis.azureus.core.networkmanager.admin.impl;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTestScheduler;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTester;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTestListener;
import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.core3.util.SystemTime;

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

    private Boolean testRunning=Boolean.FALSE;
    private String testStatus = NOT_RUNNING;

    /** Types of speed tests **/
    public static final int BIT_TORRENT_UPLOAD_AND_DOWNLOAD = 7777;
    public static final int BIT_TORRENT_DOWNLOAD = 7778;
    public static final int TCP_DOWNLOAD = 7779;
    public static final int TCP_UPLOAD = 7780;

    private static final String NOT_RUNNING = "Not Running.";
    private static long ONE_HOUR = 60 * 60 * 1000;

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
     * Create a test of type.
     *
     * @param type -
     */
    public synchronized void start(int type) {

        testRunning=Boolean.TRUE;

        //ToDo: change the interface to a boolean incase it fails to start!!!

        //stop the downloads.
        DownloadManager dm = plugin.getDownloadManager();
        //ToDo: record any setting that need to be preservered on restart.
        dm.stopAllDownloads();

        //create the test
        currentTest = createTest(type);

        //add Scheduler's listeners here so it knows when it stops, aborts, crashes!!
        EndOfTestListener testListener = new EndOfTestListener(testRunning,lastResult,testStatus,dm);
        currentTest.addListener( testListener );

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
        return currTime <= lastResult.getTestTime() + ONE_HOUR;
    }

    /**
     * Get the most recent result for the test.
     *
     * @return - Result
     */
    public NetworkAdminSpeedTester.Result getLastResult() {
        return lastResult;
    }

    //ToDo: factory method for creating test.
    protected NetworkAdminSpeedTester createTest(int type){
        if( type == BIT_TORRENT_UPLOAD_AND_DOWNLOAD){
            return new NetworkAdminSpeedTesterImpl(plugin);
        }
        //else if(){}

        throw new IllegalArgumentException("file to create NetworkAdminSpeedTester test.");
    }

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
            stage("Not Running");

            //ToDo: restart the downloads.
            downloadManager.startAllDownloads();
        }

        /**
         * Informs listener when the test is at a new stage.
         *
         * @param update - String with stage.
         */
        public void stage(String update) {
            testStatus = update;
        }

    }//class EndOfTestListener

}
