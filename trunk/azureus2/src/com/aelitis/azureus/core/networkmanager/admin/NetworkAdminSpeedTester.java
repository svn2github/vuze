package com.aelitis.azureus.core.networkmanager.admin;


public interface NetworkAdminSpeedTester 
{
	/**
	 * 
	 * @return true is a test is already running.
	 */
	public boolean isRunning();
	
	/**
	 * 
	 * @return true if the test seems to have stated successfully.
	 */
	public boolean startSpeedTest();
	
	/**
	 * 
	 * @return true if 
	 */
	public boolean isComplete();
	
	/**
	 * 
	 * @return true abortIsSuccessful.
	 */
	public boolean abortSpeedTest();
	
	/**
	 * Get the result for 
	 * @return Result of speed test
	 */
	public Result getResult();
	
	/**
	 * Interface to report last test result.
	 * @author asnyder
	 */
	public interface Result{
		
		public long getTestTime();
		public int getDownloadSpeed();
		public int getUploadSpeed();
		public boolean hadError();
		public String getLastError();
		
	}//interface
	
}//interface
