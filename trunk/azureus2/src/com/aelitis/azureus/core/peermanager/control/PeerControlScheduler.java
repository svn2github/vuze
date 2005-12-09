package com.aelitis.azureus.core.peermanager.control;

public interface 
PeerControlScheduler 
{
	public static final int SCHEDULE_PERIOD_MILLIS	= 100;
	
	public void
	register(
		PeerControlInstance	instance );
	
	public void
	unregister(
		PeerControlInstance	instance );
}
