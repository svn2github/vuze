package com.aelitis.azureus.core.networkmanager.admin.impl;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTester;


public class NetworkAdminSpeedTesterImpl 
	implements NetworkAdminSpeedTester
{
	/**
	 * 
	 * @return true is a test is already running.
	 */
	public boolean isRunning(){return false;}
	
	/**
	 * 
	 * @return true if the test seems to have stated successfully.
	 */
	public boolean startSpeedTest(){return false;}
	
	/**
	 * 
	 * @return true if 
	 */
	public boolean isComplete(){return false;}
	
	/**
	 * 
	 * @return true abort is successful.
	 */
	public boolean abortSpeedTest(){return false;}
	
	/**
	 * Get the result for 
	 * @return Result object of speed test.
	 */
	public Result getResult(){ return null;}
	
	public class ResultImpl implements NetworkAdminSpeedTester.Result{
	
		public long getTestTime(){
			return 0L;
		}
		
		public int getDownloadSpeed(){
			return 0;
		}
		
		public int getUploadSpeed(){
			return 0;
		}
		
		public boolean hadError(){
			return true;
		}
		
		public String getLastError(){
			return "Error: not implemented.";
		}
		
	}//class Result
}
