/*
 * Created on Apr 16, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.core3.util;

import java.util.*;

/**
 * Utility class to retrieve current system time,
 * and catch clock backward time changes.
 */
public class SystemTime {
  
  public static final long TIME_GRANULARITY_MILLIS = 25;   //internal update time ms
  
  private static final int STEPS_PER_SECOND = (int)(1000/TIME_GRANULARITY_MILLIS);
  
  private static final SystemTime instance = new SystemTime();
  
  
  private final Thread updater;
  
  private volatile long 	stepped_time;
  private volatile long		last_approximate_time;
  
  private volatile int		access_count;
  private volatile int 		slice_access_count;
  private volatile int		access_average_per_slice;
  private volatile int		drift_adjusted_granularity;
 
  private volatile List		consumer_list		= new ArrayList();
  private volatile List		clock_change_list	= new ArrayList();
  
  private 
  SystemTime() 
  {
    stepped_time = System.currentTimeMillis();

    updater = 
    	new Thread("SystemTime") 
    	{
    		public void 
    		run() 
    		{
    			Average access_average	= null;
    			Average drift_average	= null;

    			long	last_second	= 0;
    			
    			int	tick_count = 0;
    			
    			while( true ) {
             				
    				stepped_time = System.currentTimeMillis();  
          
    				List	consumer_list_ref = consumer_list;
    				
    				if ( last_second == 0 ){
    					
    					last_second	= stepped_time - 1000;
    				}else{
    					
    					long	offset = stepped_time - last_second;
    					
    					if ( offset < 0 || offset > 5000 ){
    						
    							// clock's changed
    						
    						last_approximate_time	= 0;
    						
    						last_second	= stepped_time - 1000;
    						
    						access_average 	= null;
    						drift_average	= null;
    						
    						Iterator	it = clock_change_list.iterator();
    						
    						while( it.hasNext()){
    							
    							((consumer)it.next()).consume( offset );
    						}
    					}
    				}
    				
    				tick_count++;
    				    				
  					if ( tick_count == STEPS_PER_SECOND ){

  						if ( access_average == null ){
  							
  				   			access_average 	= Average.getInstance( 1000, 10 );
  				   			
  			    			drift_average 	= Average.getInstance( 1000, 10 );

  						}
  		   				long drift = stepped_time - last_second -1000;
  		   			 
  		   				last_second	= stepped_time;
  		   				
  		   				drift_average.addValue( drift );
  		   				
  		   				drift_adjusted_granularity	= (int)( TIME_GRANULARITY_MILLIS + ( drift_average.getAverage() / STEPS_PER_SECOND ));
  		   				
     					access_average.addValue( access_count );
    	   						
    					access_average_per_slice	= (int)( access_average.getAverage() / STEPS_PER_SECOND );
    						
    					// System.out.println( "access count = " + access_count + ", average = " + access_average.getAverage() + ", per slice = " + access_average_per_slice + ", drift = " + drift +", average = " + drift_average.getAverage() + ", dag =" + drift_adjusted_granularity );
    						
    					access_count = 0;
    						
    	    			tick_count = 0;
  					}
  					
					slice_access_count	= 0; 

					for (int i=0;i<consumer_list_ref.size();i++){
						
  						consumer	cons = (consumer)consumer_list_ref.get(i);
  						
  						try{
  							cons.consume( stepped_time );
  							
  						}catch( Throwable e ){
  							
  							Debug.printStackTrace(e);
  						}
					}
					
    				try{  
    					Thread.sleep( TIME_GRANULARITY_MILLIS );
    					
    				}catch(Exception e){
    					
    					Debug.printStackTrace( e );
    				}
    			}
    		}
    	};
    	
    updater.setDaemon( true );
    
    // we don't want this thread to lag much as it'll stuff up the upload/download rate mechanisms (for example)
    updater.setPriority(Thread.MAX_PRIORITY);
    
    updater.start();
  }

  private long
  getApproximateTime()
  {
	  long	adjusted_time = stepped_time;

	  long	temp = access_average_per_slice;
	  
	  if ( temp > 0 ){
	  
		  long	x = (drift_adjusted_granularity*slice_access_count)/temp;
		  
		  if ( x >= drift_adjusted_granularity ){
			  
			  x = drift_adjusted_granularity-1;
		  }
				  
		  adjusted_time += x;
	  }
	  
	  access_count++;
	  
	  slice_access_count++;
	  
	  	// make sure we don't go backwards
	  
	  if ( adjusted_time < last_approximate_time ){
		  
		  adjusted_time	= last_approximate_time;
		  
	  }else{
		  
		  last_approximate_time = adjusted_time;
	  }
	  	  
	  return( adjusted_time );
  }
  
  /**
   * Get the current system time.
   * @return time like System.currentTimeMillis()
   */
  public static long 
  getCurrentTime() 
  {
    return( instance.getApproximateTime());
  }
  
  public static void
  registerConsumer(
	consumer	c )
  {
	synchronized( instance ){
		
		List	new_list = new ArrayList( instance.consumer_list );
		
		new_list.add( c );
		
		instance.consumer_list	= new_list;
	}
  }
		
  public static void
  unregisterConsumer(
	consumer	c )
  {
	synchronized( instance ){
			
		List	new_list = new ArrayList( instance.consumer_list );
			
		new_list.remove( c );
			
		instance.consumer_list	= new_list;
	}  
  }
  
  public static void
  registerClockChangeListener(
	consumer	c )
  {
	synchronized( instance ){
		
		List	new_list = new ArrayList( instance.clock_change_list );
		
		new_list.add( c );
		
		instance.clock_change_list	= new_list;
	}
  }
		
  public static void
  unregisterClockChangeListener(
	consumer	c )
  {
	synchronized( instance ){
			
		List	new_list = new ArrayList( instance.clock_change_list );
			
		new_list.remove( c );
			
		instance.clock_change_list	= new_list;
	}  
  }
  
  public interface
  consumer
  {
	  	// for consumers this is the current time, for clock change listeners this is the delta
	  
	  public void
	  consume(
		long	time );
  }
  
  public static void
  main(
	String[]	args )
  {
	  for (int i=0;i<1;i++){
		  
		  final int f_i = i;
		  
		  new Thread()
		  {
			  public void
			  run()
			  {
				  /*
				  Average access_average 	= Average.getInstance( 1000, 10 );
						 
				  long	last = SystemTime.getCurrentTime();
				  
				  int	count = 0;
				  
				  while( true ){
					  
					  long	now = SystemTime.getCurrentTime();
					  
					  long	diff = now - last;
					  
					  System.out.println( "diff=" + diff );
					  
					  last	= now;
					  
					  access_average.addValue( diff );
					  
					  count++;
					  
					  if ( count == 33 ){
						  
						  System.out.println( "AVERAGE " + f_i + " = " + access_average.getAverage());
						  
						  count = 0;
					  }
					  
					  try{
						  Thread.sleep( 3 );
						  
					  }catch( Throwable e ){
						  
					  }
				  }
				  */
				  
				  long start = SystemTime.getCurrentTime();
				  
				  while( true ){
					  
					  long now = SystemTime.getCurrentTime();
					  
					  System.out.println( now - start );
					  
					  try{
						  Thread.sleep(1000);
					  }catch( Throwable e ){
					  
					  }
				  }
			  }
		  }.start();
	  }
  }
}
