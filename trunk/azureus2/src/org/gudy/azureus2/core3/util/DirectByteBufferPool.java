/*
 * Created on Jan 30, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Alon Rohter, All Rights Reserved.
 * 
 */
package org.gudy.azureus2.core3.util;

import java.nio.ByteBuffer;
import java.util.*;
import java.math.*;

import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.logging.LGLogger;
import com.aelitis.azureus.core.diskmanager.cache.*;


/**
 * This class handles allocation of direct ByteBuffers.
 * It always tries to find a free buffer in the buffer pool
 * before creating a new one.
 */
public class 
DirectByteBufferPool 
{

	protected static final boolean 	DEBUG_TRACK_HANDEDOUT 	= true;
	protected static final boolean 	DEBUG_PRINT_MEM 		= true;
	protected static final int		DEBUG_PRINT_TIME		= 120*1000;
	static{
		if ( DEBUG_TRACK_HANDEDOUT || DEBUG_PRINT_MEM ){
			System.out.println( "**** DirectByteBufferPool debugging on ****" );
		}
	}
	
  // There is no point in allocating buffers smaller than 4K,
  // as direct ByteBuffers are page-aligned to the underlying
  // system, which is 4096 byte pages under most OS's.
  // If we want to save memory, we can distribute smaller-than-4K
  // buffers by using the slice() method to break up a standard buffer
  // into smaller chunks, but that's more work.
  private static final int START_POWER = 12;    // 4096
  private static final int END_POWER   = 25;    // 33554432
  
  	// without an extra bucket here we get lots of wastage with the file cache as typically
  	// 16K data reads result in a buffer slightly bigger than 16K due to protocol header
  	// This means we would bump up to 32K pool entries, hence wasting 16K per 16K entry
  	
  private static final int[]	EXTRA_BUCKETS = { 128, PEPeerManager.BLOCK_SIZE + 128 };
  
  
  public static final int MAX_SIZE = BigInteger.valueOf(2).pow(END_POWER).intValue();
  
  private static final DirectByteBufferPool pool = new DirectByteBufferPool();
  

  private final Map buffersMap = new LinkedHashMap(END_POWER - START_POWER + 1);
  
  private final Object poolsLock = new Object();

  private final Timer compactionTimer;
  
  private final Map handed_out	= new IdentityHashMap();	// for debugging (ByteBuffer has .equals defined on contents
  															// hence IdentityHashMap)
  
  private static final long COMPACTION_CHECK_PERIOD = 5*60*1000; //5 min
  private static final long MAX_FREE_BYTES = 5*1024*1024; //5 MB
  
  private long bytesIn = 0;
  private long bytesOut = 0;
  
  
  private DirectByteBufferPool() {
  	
    //create the buffer pool for each buffer size
  	
  	ArrayList	list = new ArrayList();
  	
    for (int p=START_POWER; p <= END_POWER; p++) {
    	
    	list.add( new Integer(BigInteger.valueOf(2).pow(p).intValue()));
    }
    
    for (int i=0;i<EXTRA_BUCKETS.length;i++){
    	       
        list.add( new Integer(EXTRA_BUCKETS[i]));
    }
    
    Integer[]	sizes = new Integer[ list.size() ];
    list.toArray( sizes );
    Arrays.sort( sizes);
    
    for (int i=0;i<sizes.length;i++){
    	
    	ArrayList bufferPool = new ArrayList();
    	
    	buffersMap.put(sizes[i], bufferPool);
    }
    
    //initiate periodic timer to check free memory usage
    compactionTimer = new Timer("BufferPool Checker");
    compactionTimer.addPeriodicEvent(
        COMPACTION_CHECK_PERIOD,
        new TimerEventPerformer() {
          public void perform( TimerEvent ev ) {
       
            checkMemoryUsage();
          }
        }
     );
    
    if( DEBUG_PRINT_MEM ) {
      Timer printer = new Timer("printer");
      printer.addPeriodicEvent(
          DEBUG_PRINT_TIME,
          new TimerEventPerformer() {
            public void perform( TimerEvent ev ) {
              System.out.print("DIRECT: given=" +bytesOut/1024/1024+ "MB, returned=" +bytesIn/1024/1024+ "MB, ");
              
              long in_use = bytesOut - bytesIn;
              if( in_use < 1024*1024 ) System.out.print( "in use=" +in_use+ "B, " );
              else System.out.print( "in use=" +in_use/1024/1024+ "MB, " );
              
              long free = bytesFree();
              if( free < 1024*1024 ) System.out.print( "free=" +free+ "B" );
              else System.out.print( "free=" +free/1024/1024+ "MB" );

              System.out.println();
              
              printInUse( false );
              
              long free_mem = Runtime.getRuntime().freeMemory() /1024/1024;
              long max_mem = Runtime.getRuntime().maxMemory() /1024/1024;
              long total_mem = Runtime.getRuntime().totalMemory() /1024/1024;
              System.out.println("HEAP: max=" +max_mem+ "MB, total=" +total_mem+ "MB, free=" +free_mem+ "MB");
              System.out.println();
            }
          }
      );
    }
     

  }

  
  /**
   * Allocate and return a new direct ByteBuffer.
   */
  private ByteBuffer allocateNewBuffer(final int _size) {
    try {
      return ByteBuffer.allocateDirect(_size);
    }
    catch (OutOfMemoryError e) {
       Debug.out("Running garbage collector...");
       
       clearBufferPools();
       
       runGarbageCollection();

       try {
       		return ByteBuffer.allocateDirect(_size);
       	
       }catch (OutOfMemoryError ex) {
       	
         String msg = "Memory allocation failed: Out of direct memory space.\n"
                    + "To fix: Use the -XX:MaxDirectMemorySize=512m command line option,\n"
                    + "or upgrade your Java JRE to version 1.4.2_05 or 1.5 series or newer.";
       	 Debug.out( msg );
       	 
         LGLogger.logAlert( LGLogger.AT_ERROR, msg );
         
         printInUse( true );
         
         throw( ex );
       }
    }
  }

  
  /**
   * Retrieve a buffer from the buffer pool of size at least
   * <b>length</b>, and no larger than <b>DirectByteBufferPool.MAX_SIZE</b>
   */
  public static DirectByteBuffer 
  getBuffer(
  	byte	_allocator,
  	int 	_length) 
  {
    if (_length < 1) {
        Debug.out("requested length [" +_length+ "] < 1");
        return null;
    }

    if (_length > MAX_SIZE) {
        Debug.out("requested length [" +_length+ "] > MAX_SIZE [" +MAX_SIZE+ "]");
        return null;
    }

    return pool.getBufferHelper(_allocator,_length);
  }
  
  
  /**
   * Retrieve an appropriate buffer from the free pool, or
   * create a new one if the pool is empty.
   */
  private DirectByteBuffer 
  getBufferHelper(
  	byte	_allocator,
  	int 	_length) 
  {
    
    Integer reqVal = new Integer(_length);
    
    //loop through the buffer pools to find a buffer big enough
    
    Iterator it = buffersMap.keySet().iterator();
    
    while (it.hasNext()) {
    	
      Integer keyVal = (Integer)it.next();

      	//check if the buffers in this pool are big enough
      
      if (reqVal.compareTo(keyVal) <= 0) {
      	
   
        ArrayList bufferPool = (ArrayList)buffersMap.get(keyVal);
            
        ByteBuffer buff;
        
        synchronized ( poolsLock ) { 
        
        	//make sure we don't remove a buffer when running compaction
        	//if there are no free buffers in the pool, create a new one.
        	//otherwise use one from the pool
        	
          if (bufferPool.isEmpty()) {
          	
            buff = allocateNewBuffer(keyVal.intValue());
            
          }else{
          	
            synchronized ( bufferPool ) {
            	
              buff = (ByteBuffer)bufferPool.remove(bufferPool.size() - 1);
            }
          }
        }
        
        buff.clear();   //scrub the buffer
        
        buff.limit( _length );
        
        bytesOut += buff.capacity();
              
        DirectByteBuffer dbb = new DirectByteBuffer( _allocator, buff, this );
                    
        if ( DEBUG_PRINT_MEM ){
        	
        	synchronized( handed_out ){
        	        	
        		if ( handed_out.put( buff, dbb ) != null ){
          		
        			Debug.out( "buffer handed out twice!!!!");
          		
        			throw( new RuntimeException( "Buffer handed out twice" ));
        		}
        	
				//System.out.println( "[" + handed_out.size() + "] -> " + buff + ", bytesIn = " + bytesIn + ", bytesOut = " + bytesOut );
          	}
        }
        
        // addInUse( dbb.capacity() );
        
        return dbb;
      }
    }
    
    	//we should never get here
      
    Debug.out("Unable to find an appropriate buffer pool");
    
	throw( new RuntimeException( "Unable to find an appropriate buffer pool" ));	 
  }
  
  
  /**
   * Return the given buffer to the appropriate pool.
   */
  private void 
  free(ByteBuffer _buffer) 
  {
    Integer buffSize = new Integer(_buffer.capacity());
    
    ArrayList bufferPool = (ArrayList)buffersMap.get(buffSize);
    
    if (bufferPool != null) {
      //no need to sync around 'poolsLock', as adding during compaction is ok
      synchronized ( bufferPool ) {
        bufferPool.add(_buffer);
      }
    }
    else {
      Debug.out("Invalid buffer given; could not find proper buffer pool");
    }
  }
  
  
  /**
   * Clears the free buffer pools so that currently
   * unused buffers can be garbage collected.
   */
  private void clearBufferPools() {
    Iterator it = buffersMap.values().iterator();
    while (it.hasNext()) {
        ArrayList bufferPool = (ArrayList)it.next();
        bufferPool.clear();
    }
  }
  
  
  /**
   * Force system garbage collection.
   */
  private void runGarbageCollection() {
    System.out.println( "runGarbageCollection()" );
    System.runFinalization();
    System.gc();
  }
  
  
  /**
   * Checks memory usage of free buffers in buffer pools,
   * and calls the compaction method if necessary.
   */
  private void checkMemoryUsage() {
    long bytesUsed = 0;
    
    synchronized( poolsLock ) {
      
      //count up total bytes used by free buffers
      Iterator it = buffersMap.keySet().iterator();
      while (it.hasNext()) {
        Integer keyVal = (Integer)it.next();
        ArrayList bufferPool = (ArrayList)buffersMap.get(keyVal);
      
        bytesUsed += keyVal.intValue() * bufferPool.size();
      }
      
      //compact buffer pools if they use too much memory
      if (bytesUsed > MAX_FREE_BYTES) {
        compactFreeBuffers(bytesUsed);
      }
      
    }
  }
  
  
  
  
  
  
  /**
   * Fairly removes free buffers from the pools to limit memory usage.
   */
  private void compactFreeBuffers(final long bytes_used) {
    final int numPools = buffersMap.size();
    long bytesToFree = 0;
    int maxPoolSize = 0;
    
    int[] buffSizes = new int[numPools];
    int[] poolSizes = new int[numPools];
    int[] numToFree = new int[numPools];

    
    //fill size arrays
    int pos = 0;
    Iterator it = buffersMap.keySet().iterator();
    while (it.hasNext()) {
      Integer keyVal = (Integer)it.next();
      ArrayList bufferPool = (ArrayList)buffersMap.get(keyVal);
      
      buffSizes[pos] = keyVal.intValue();
      poolSizes[pos] = bufferPool.size();
      numToFree[pos] = 0;
      
      //find initial max value
      if (poolSizes[pos] > maxPoolSize) maxPoolSize = poolSizes[pos];
      
      pos++;
    }
    
    //calculate the number of buffers to free from each pool
    while( bytesToFree < (bytes_used - MAX_FREE_BYTES) ) {
      for (int i=0; i < numPools; i++) {
        //if the pool size is as large as the current max size
        if (poolSizes[i] == maxPoolSize) {
          //update counts
          numToFree[i]++;
          poolSizes[i]--;
          bytesToFree += buffSizes[i];
        }
      }
      //reduce max size for next round
      maxPoolSize--;
    }
    
    //free buffers from the pools
    pos = 0;
    it = buffersMap.values().iterator();
    while (it.hasNext()) {
      //for each pool
      ArrayList bufferPool = (ArrayList)it.next();
      synchronized( bufferPool ) {
        int size = bufferPool.size();
        //remove the buffers from the end
        for (int i=(size - 1); i >= (size - numToFree[pos]); i--) {
          bufferPool.remove(i);
        }
      }
      
      pos++;
    }
    
    runGarbageCollection();
  }
  
  protected void
  returnBuffer(
  	ByteBuffer		buffer )
  {
    bytesIn += buffer.capacity();
    
  	if ( DEBUG_TRACK_HANDEDOUT ){
  		
  		synchronized( handed_out ){

  			if ( handed_out.remove( buffer ) == null ){
  				
  				Debug.out( "buffer not handed out" );
  				
  				throw( new RuntimeException( "Buffer not handed out" ));
  			}
  			
       		// System.out.println( "[" + handed_out.size() + "] <- " + buffer + ", bytesIn = " + bytesIn + ", bytesOut = " + bytesOut );
  		}
  	}
  	
    // remInUse( buffer.capacity() );
    
    free( buffer ); 
  }
  
  
  
  
  private long bytesFree() {
    long bytesUsed = 0;
    synchronized( poolsLock ) {
      //count up total bytes used by free buffers
      Iterator it = buffersMap.keySet().iterator();
      while (it.hasNext()) {
        Integer keyVal = (Integer)it.next();
        ArrayList bufferPool = (ArrayList)buffersMap.get(keyVal);
      
        bytesUsed += keyVal.intValue() * bufferPool.size();
      }
    }
    return bytesUsed;
  }
  
  
  /*
  private final HashMap in_use_counts = new HashMap();
  
  private void addInUse( int size ) {
    Integer key = new Integer( size );
    synchronized( in_use_counts ) {
      Integer count = (Integer)in_use_counts.get( key );
      if( count == null )  count = new Integer( 1 );
      else  count = new Integer( count.intValue() + 1 );
      in_use_counts.put( key, count );
    }
  }
  
  private void remInUse( int size ) {
    Integer key = new Integer( size );
    synchronized( in_use_counts ) {
      Integer count = (Integer)in_use_counts.get( key );
      if( count == null ) System.out.println("count = null");
      if( count.intValue() == 0 ) System.out.println("count = 0");
      in_use_counts.put( key, new Integer( count.intValue() - 1 ) );
    }
  }
  
  private void printInUse() {
    synchronized( in_use_counts ) {
      for( Iterator i = in_use_counts.keySet().iterator(); i.hasNext(); ) {
        Integer key = (Integer)i.next();
        int count = ((Integer)in_use_counts.get( key )).intValue();
        int size = key.intValue();
        if( count > 0 ) {
          if( size < 1024 )  System.out.print("[" +size+ " x " +count+ "] ");
          else  System.out.print("[" +size/1024+ "K x " +count+ "] ");
        }
      }
      System.out.println();
    }
  }
  */
  
  	private void
	printInUse(
		boolean		verbose )
  	{
  		if ( DEBUG_PRINT_MEM ){
	  		CacheFileManager cm	= null;
	  		
			try{
	 			cm = CacheFileManagerFactory.getSingleton();
	 			
			}catch( Throwable e ){
					
				e.printStackTrace();
			}
	 
	  		synchronized( handed_out ){
	    	
	  			Iterator	it = handed_out.values().iterator();
	  			
	  			Map	cap_map		= new TreeMap();
	  			Map	alloc_map	= new TreeMap();
	  			
	  			while( it.hasNext()){
	  				
	  				DirectByteBuffer	db = (DirectByteBuffer)it.next();
	  				
	  				if ( verbose ){
		  				String	trace = db.getTraceString();
		  				
		  				if ( trace != null ){
		  					
		  					System.out.println( trace );
		  				}
	  				}
	  				
	  				Integer cap 	= new Integer( db.getBufferInternal().capacity());
	  				Byte	alloc 	= new Byte( db.getAllocator());
	  				
	  				myInteger	c = (myInteger)cap_map.get(cap);
	  				
	  				if ( c == null ){
	  					
	  					c	= new myInteger();
	  					
	  					cap_map.put( cap, c );
	  				}
	  				
	  				c.value++;
	  				
					myInteger	a = (myInteger)alloc_map.get(alloc);
	  				
	  				if ( a == null ){
	  					
	  					a	= new myInteger();
	  					
	  					alloc_map.put( alloc, a );
	  				}
	  				
	  				a.value++;				
	  			}
	  			
	  			it = cap_map.keySet().iterator();
	  			
	  			while( it.hasNext()){
	  				
	  				Integer		key 	= (Integer)it.next();
	  				myInteger	count 	= (myInteger)cap_map.get( key );
	  				
	  		        if( key.intValue() < 1024 ){
	  		        	
	  		        	System.out.print("[" +key.intValue()+ " x " +count.value+ "] ");
	  		        	
	  		        }else{  
	  		        	
	  		        	System.out.print("[" +key.intValue()/1024+ "K x " +count.value+ "] ");
	  		        }
	  			}
	  			
	  			System.out.print( " - " );
	  			
				it = alloc_map.keySet().iterator();
	  			
	  			while( it.hasNext()){
	  				
	  				Byte		key 	= (Byte)it.next();
	  				myInteger	count 	= (myInteger)alloc_map.get( key );
	  				
	  	        	System.out.print("[" + DirectByteBuffer.AL_DESCS[key.intValue()]+ " x " +count.value+ "] ");
	  			}
	  			
	  			if ( cm != null ){
	  				
	  				CacheFileManagerStats stats = cm.getStats();
	  				
	  				System.out.print( " - Cache: " );
	  	  			
	  				
					System.out.print( "sz=" + stats.getSize());
					System.out.print( ",us=" + stats.getUsedSize());
					System.out.print( ",cw=" + stats.getBytesWrittenToCache());
					System.out.print( ",cr=" + stats.getBytesReadFromCache());
					System.out.print( ",fw=" + stats.getBytesWrittenToFile());
					System.out.print( ",fr=" + stats.getBytesReadFromFile());
					
	  			}
	  			
	  			System.out.println();
	  		}
  		}
  	}
  	
  	protected static class
	myInteger
  	{
  		int	value;
  	}
}
