package com.aelitis.azureus.core.peermanager.control;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.SystemTime;

public class SpeedTokenDispenser {
	// crude TBF implementation
	private int	rateKiB;

	{
		COConfigurationManager.addAndFireParameterListeners(
			new String[] {
				"Max Download Speed KBs",
				"Use Request Limiting"
			},
			new ParameterListener()
			{
				public void parameterChanged(String parameterName) {
					rateKiB = COConfigurationManager.getIntParameter("Max Download Speed KBs");
					if(!COConfigurationManager.getBooleanParameter("Use Request Limiting"))
						rateKiB = 0;
					lastTime = currentTime;
					bucket = 0;
				}
			}
		);
	}
	private int			bucket		= 0;
	private long		lastTime	= SystemTime.getCurrentTime();
	private long		currentTime;

	public void update(long newTime) {
		currentTime = newTime;
	}

	public void refill() {
		if (lastTime == currentTime || rateKiB == 0)
			return;
		long delta = currentTime - lastTime;
		lastTime = currentTime;
		// upcast to long since we might exceed int-max when rate and delta are
		// large enough; then downcast again...
		int tickDelta = (int) (((long) rateKiB * 1024 * delta) / 1000);
		int threshold = 3 * rateKiB * 1024;
		//System.out.println("threshold:" + threshold + " update: " + bucket + " time delta:" + delta);
		bucket += tickDelta;
		if (bucket > threshold)
			bucket = threshold;
	}

	public int dispense(int numberOfChunks, int chunkSize) {
		if (rateKiB == 0)
			return numberOfChunks;
		if (chunkSize > bucket)
			return 0;
		if (chunkSize * numberOfChunks <= bucket)
		{
			bucket -= chunkSize * numberOfChunks;
			return numberOfChunks;
		}
		int availableChunks = bucket / chunkSize;
		bucket -= chunkSize * availableChunks;
		return availableChunks;
	}

	public void returnUnusedChunks(int unused, int chunkSize) {
		bucket += unused * chunkSize;
	}

	public int peek(int chunkSize) {
		if (rateKiB > 0)
			return bucket / chunkSize;
		else
			return Integer.MAX_VALUE;
	}
}
