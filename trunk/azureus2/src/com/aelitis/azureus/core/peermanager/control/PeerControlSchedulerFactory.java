package com.aelitis.azureus.core.peermanager.control;

import com.aelitis.azureus.core.peermanager.control.impl.PeerControlSchedulerImpl;

public class 
PeerControlSchedulerFactory 
{
	public static PeerControlScheduler
	getSingleton()
	{
		return( PeerControlSchedulerImpl.getSingleton());
	}
}
