package com.aelitis.azureus.core.networkmanager.admin.impl;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTester;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTestListener;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.PluginInterface;

import java.util.List;
import java.util.ArrayList;


public class NetworkAdminSpeedTesterImpl 
	implements NetworkAdminSpeedTester
{
    private PluginInterface plugin;

    private static Result lastResult=null;
    private static long lastResultTime;

    private static long ONE_HOUR = 60 * 60 * 1000;

    private List listenerList = new ArrayList();


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



        }catch(Exception e){
            //ToDo: Create a result with just an error.

            return false;
        }
        return true;
    }
	
	/**
	 * If a test has been completed in the last hour, return true.
	 * @return true if a new result is detected in the past hour.
	 */
	public synchronized boolean isComplete(){

        //check for a result.
        if(lastResult==null)
            return false;

        //has it been longer then an hour?
        long currTime = SystemTime.getCurrentTime();
        return currTime <= lastResultTime + ONE_HOUR;
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
	public NetworkAdminSpeedTester.Result getResult(){ return null;}

    /**
     * To know when result is complete or at a new stage.
     *
     * @param listener -
     */
    public void addListener(NetworkAdminSpeedTestListener listener) {
        listenerList.add( listener );
    }

    public class ResultImpl implements NetworkAdminSpeedTester.Result{

        public long getTestTime(){return 0L;}
		public int getDownloadSpeed(){return 0;}
		public int getUploadSpeed(){return 0;}
		public boolean hadError(){return true;}
		public String getLastError(){return "not implemented";}

	}//class Result

}
