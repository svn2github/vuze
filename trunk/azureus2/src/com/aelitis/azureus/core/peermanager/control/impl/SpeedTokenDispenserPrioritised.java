package com.aelitis.azureus.core.peermanager.control.impl;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.peermanager.control.SpeedTokenDispenser;
import com.aelitis.azureus.core.util.FeatureAvailability;

public class 
SpeedTokenDispenserPrioritised 
	implements SpeedTokenDispenser
{
	// crude TBF implementation
	private int		rateKiB;
	{
		COConfigurationManager.addAndFireParameterListeners(new String[] { "Max Download Speed KBs", "Use Request Limiting" }, new ParameterListener()
		{
			public void parameterChanged(String parameterName) {
				rateKiB = COConfigurationManager.getIntParameter("Max Download Speed KBs");
				if (!COConfigurationManager.getBooleanParameter("Use Request Limiting") || FeatureAvailability.isRequestLimitingEnabled())
					rateKiB = 0;
				lastTime = currentTime - 1; // shortest possible delta
				refill(); // cap buffer to threshold in case something accumulated
			}
		});
	}
	private int		bucket		= 0;
	private long	lastTime	= SystemTime.getCurrentTime();
	private long	currentTime;

	public void update(long newTime) {
		currentTime = newTime;
	}

	// allow at least 2 outstanding requests
	private static final int	BUCKET_THRESHOLD_LOWER_BOUND	= 2 * 15 * 1024;
	// 3KiB buffer per 1KiB/s speed, that should be 3 seconds max response time
	private static final int	BUCKET_THRESHOLD_FACTOR			= 3 * 1024;

	public void refill() {
		if (lastTime == currentTime || rateKiB == 0)
			return;
		long delta = currentTime - lastTime;
		lastTime = currentTime;
		// upcast to long since we might exceed int-max when rate and delta are
		// large enough; then downcast again...
		int tickDelta = (int) (((long) rateKiB * 1024 * delta) / 1000);
		int threshold = BUCKET_THRESHOLD_FACTOR * rateKiB;
		if (threshold < BUCKET_THRESHOLD_LOWER_BOUND)
			threshold = BUCKET_THRESHOLD_LOWER_BOUND;
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
		if (rateKiB != 0)
			return bucket / chunkSize;
		else
			return Integer.MAX_VALUE;
	}
}
