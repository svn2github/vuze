package com.aelitis.azureus.core.networkmanager.admin;


public interface NetworkAdminSpeedTester 
{

    /**
     * Start a speed test.
     * @return true if the test seems to have stated successfully.
     */
    public boolean start();

	/**
	 * 
	 * @return true abort
	 */
	public boolean abort();
	
	/**
	 * Get the result for 
	 * @return Result of speed test
	 */
	public Result getResult();

    /**
     * To know when result is complete or at a new stage.
     * @param listener -
     */
    public void addListener(NetworkAdminSpeedTestListener listener);

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
