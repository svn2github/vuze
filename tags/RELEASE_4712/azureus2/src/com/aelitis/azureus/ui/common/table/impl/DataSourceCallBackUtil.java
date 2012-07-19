package com.aelitis.azureus.ui.common.table.impl;

import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.ui.common.table.impl.TableViewImpl;

public class DataSourceCallBackUtil
{
	public static final long IMMEDIATE_ADDREMOVE_DELAY = 150;

	private static final long IMMEDIATE_ADDREMOVE_MAXDELAY = 2000;

	private static Timer timerProcessDataSources = new Timer("Process Data Sources");

	private static TimerEvent timerEventProcessDS;

	private static List processDataSourcesOutstanding = new ArrayList();
	

	public static boolean
	addDataSourceAggregated(
		addDataSourceCallback		callback )
	{
		if ( callback == null ){
			
			return( true );
		}
		
		boolean processQueueImmediately = false;
		
		List	to_do_now = null;
		
		synchronized( timerProcessDataSources ){
						
			if ( timerEventProcessDS != null && !timerEventProcessDS.hasRun()){
				
					// Push timer forward, unless we've pushed it forward for over x seconds
				
				long now = SystemTime.getCurrentTime();
				
				if (now - timerEventProcessDS.getCreatedTime() < IMMEDIATE_ADDREMOVE_MAXDELAY) {
					
					long lNextTime = now + IMMEDIATE_ADDREMOVE_DELAY;
										
					timerProcessDataSources.adjustAllBy( lNextTime - timerEventProcessDS.getWhen());
					
					if ( !processDataSourcesOutstanding.contains( callback )){
						
						processDataSourcesOutstanding.add( callback );
					}
				}else{
										
					timerEventProcessDS.cancel();
					
					timerEventProcessDS = null;

					processQueueImmediately = true;
					
					to_do_now = processDataSourcesOutstanding;
					
					processDataSourcesOutstanding = new ArrayList();
				}
			}else{
				
				if ( !processDataSourcesOutstanding.contains( callback )){
					
					processDataSourcesOutstanding.add( callback );
				}

				timerEventProcessDS = 
					timerProcessDataSources.addEvent(
						SystemTime.getCurrentTime() + IMMEDIATE_ADDREMOVE_DELAY,
						new TimerEventPerformer() 
						{
							public void 
							perform(
								TimerEvent event ) 
							{
								List	to_do;
																
								synchronized( timerProcessDataSources ){
								
									timerEventProcessDS = null;

									to_do = processDataSourcesOutstanding;
									
									processDataSourcesOutstanding = new ArrayList();
								}
								
								for (int i=0;i<to_do.size();i++){
									
									try{
										
										addDataSourceCallback this_callback = (addDataSourceCallback)to_do.get(i);
														
										if (TableViewImpl.DEBUGADDREMOVE ) {
											this_callback.debug("processDataSourceQueue after "
													+ (SystemTime.getCurrentTime() - event.getCreatedTime())
													+ "ms");
										}
										
										this_callback.process();
										
									}catch( Throwable e ){
										
										Debug.printStackTrace(e);
									}
								}
							}
						});
			}
			
			if ( to_do_now != null ){
								
					// process outside the synchronized block, otherwise we'll end up with deadlocks

				to_do_now.remove( callback );
				
				for (int i=0;i<to_do_now.size();i++){
					
					try{
						
						addDataSourceCallback this_callback = (addDataSourceCallback)to_do_now.get(i);

						if ( TableViewImpl.DEBUGADDREMOVE ){
							
							this_callback.debug("Over immediate delay limit, processing queue now");
						}
						
						this_callback.process();
						
					}catch( Throwable e ){
						
						Debug.printStackTrace(e);
					}
				}
			}
		}
		
		return( processQueueImmediately );
	}
	
	public interface
	addDataSourceCallback
	{
		public void
		process();
		
		public void
		debug(
			String		str );
	}
	
	

}
