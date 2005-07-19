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

import org.gudy.azureus2.core3.disk.DiskManager;
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

	protected static final boolean 	DEBUG_TRACK_HANDEDOUT 	= AEDiagnostics.TRACE_DBB_POOL_USAGE;
	protected static final boolean 	DEBUG_PRINT_MEM 		= AEDiagnostics.PRINT_DBB_POOL_USAGE;
	protected static final int		DEBUG_PRINT_TIME		= 120*1000;
	
	protected static final boolean 	DEBUG_HANDOUT_SIZES 	= false;

	
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
  	
	private static final int[]	EXTRA_BUCKETS = { DiskManager.BLOCK_SIZE + 128 };
  
  
	public static final int MAX_SIZE = BigInteger.valueOf(2).pow(END_POWER).intValue();
  
	private static final DirectByteBufferPool pool = new DirectByteBufferPool();
  

	private final Map buffersMap = new LinkedHashMap(END_POWER - START_POWER + 1);
  
	private final Object poolsLock = new Object();

	private static final int	SLICE_END_SIZE				= 2048;	
	private static final int    SLICE_ALLOC_CHUNK_SIZE		= 4096;
  

	private static final short[]		SLICE_ENTRY_SIZES		= { 8, 16, 32, 64, 128, 256, 512, 1024, SLICE_END_SIZE };
	private static final short[]		SLICE_ALLOC_MAXS		= { 256, 256, 128, 64, 64,  64,  64,  64,   64 };
	
	private static final short[]		SLICE_ENTRY_ALLOC_SIZES = new short[SLICE_ENTRY_SIZES.length];
	private static final List[]			slice_entries 			= new List[SLICE_ENTRY_SIZES.length];
	private static final boolean[][]	slice_allocs 			= new boolean[SLICE_ENTRY_SIZES.length][];
	private static final boolean[]		slice_alloc_fails		= new boolean[SLICE_ENTRY_SIZES.length];
	
	static{
		for (int i=0;i<SLICE_ENTRY_SIZES.length;i++){
			
			SLICE_ENTRY_ALLOC_SIZES[i] = (short)(SLICE_ALLOC_CHUNK_SIZE/SLICE_ENTRY_SIZES[i]);
					
			slice_allocs[i] = new boolean[SLICE_ALLOC_MAXS[i]];
			
			slice_entries[i] = new LinkedList();
		}
	}
	
	private static final long[]			slice_use_count 	= new long[SLICE_ENTRY_SIZES.length];
	  
	private final Timer compactionTimer;
  
	private final Map handed_out	= new IdentityHashMap();	// for debugging (ByteBuffer has .equals defined on contents, hence IdentityHashMap)
	
	private final Map	size_counts	= new TreeMap();
 
	private static final long COMPACTION_CHECK_PERIOD = 2*60*1000; //2 min
	private static final long MAX_FREE_BYTES = 10*1024*1024; //10 MB
  
	private long bytesIn = 0;
	private long bytesOut = 0;
  
  
	private 
	DirectByteBufferPool() 
	{
	  	
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
	       
	            compactBuffers();
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
       //Debug.out("Running garbage collector...");
       
       clearBufferPools();
       
       runGarbageCollection();

       try {
       		return ByteBuffer.allocateDirect(_size);
       	
       }catch (OutOfMemoryError ex) {
       	
         String msg = "Memory allocation failed: Out of direct memory space.\n"
                    + "To fix: Use the -XX:MaxDirectMemorySize=512m command line option,\n"
                    + "or upgrade your Java JRE to version 1.4.2_05 or 1.5 series or newer.";
       	 Debug.out( msg );
       	 
         LGLogger.logUnrepeatableAlert( LGLogger.AT_ERROR, msg );
         
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
		DirectByteBuffer	res;
		
		if ( _length <= SLICE_END_SIZE ){

			res = getSliceBuffer( _allocator, _length );
			
		}else{
				
			ByteBuffer	buff = null;
			
			Integer reqVal = new Integer(_length);
	    
				//loop through the buffer pools to find a buffer big enough
	    
			Iterator it = buffersMap.keySet().iterator();
	    
			while (it.hasNext()) {
	    	
				Integer keyVal = (Integer)it.next();
	
					//	check if the buffers in this pool are big enough
	      
				if (reqVal.compareTo(keyVal) <= 0) {
	      	
	   
					ArrayList bufferPool = (ArrayList)buffersMap.get(keyVal);
	            
					synchronized ( poolsLock ) { 
	        
						//	make sure we don't remove a buffer when running compaction
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
			
					break;
				}
			}
		
			if ( buff == null ){
						      
			    Debug.out("Unable to find an appropriate buffer pool");
			    
			    throw( new RuntimeException( "Unable to find an appropriate buffer pool" ));
			}
			
			res = new DirectByteBuffer( _allocator, buff, this );		   
		}
			
        	// clear doesn't actually zero the data, it just sets pos to 0 etc.
        
		ByteBuffer buff = res.getBufferInternal();
		
        buff.clear();   //scrub the buffer
        
		buff.limit( _length );
		
        bytesOut += buff.capacity();
                                
        if ( DEBUG_PRINT_MEM || DEBUG_TRACK_HANDEDOUT ){
        	
        	synchronized( handed_out ){
        	        	
				if ( DEBUG_HANDOUT_SIZES ){
					
					int	trim_size;
					
					if ( _length < 32 ){
						
						trim_size = 4;
					}else{
						
						trim_size = 16;
					}
					
					int	trim = ((_length+trim_size-1)/trim_size)*trim_size;
					
					Long count = (Long)size_counts.get(new Integer(trim));
					
					if ( count == null ){
						
						size_counts.put( new Integer( trim ), new Long(1));
						
					}else{
						
						size_counts.put( new Integer( trim), new Long( count.longValue() + 1 ));
					}
				}
				
        		if ( handed_out.put( buff, res ) != null ){
          		
        			Debug.out( "buffer handed out twice!!!!");
          		
        			throw( new RuntimeException( "Buffer handed out twice" ));
        		}
        	
				//System.out.println( "[" + handed_out.size() + "] -> " + buff + ", bytesIn = " + bytesIn + ", bytesOut = " + bytesOut );
          	}
        }
        
        // addInUse( dbb.capacity() );
        
        return( res );
    }
  
  
	  /**
	   * Return the given buffer to the appropriate pool.
	   */
	
	protected void 
	returnBuffer(
		DirectByteBuffer ddb ) 
	{
		ByteBuffer	buff = ddb.getBufferInternal();
		
		int	capacity = buff.capacity();

		bytesIn += capacity;
		    
	  	if ( DEBUG_TRACK_HANDEDOUT ){
	  		
	  		synchronized( handed_out ){

	  			if ( handed_out.remove( buff ) == null ){
	  				
	  				Debug.out( "buffer not handed out" );
	  				
	  				throw( new RuntimeException( "Buffer not handed out" ));
	  			}
	  			
	       		// System.out.println( "[" + handed_out.size() + "] <- " + buffer + ", bytesIn = " + bytesIn + ", bytesOut = " + bytesOut );
	  		}
	  	}
	  	
	    // remInUse( buffer.capacity() );
		
		if ( capacity <= SLICE_END_SIZE ){
			
			freeSliceBuffer( ddb );
			
		}else{
		    Integer buffSize = new Integer(capacity);
		    
		    ArrayList bufferPool = (ArrayList)buffersMap.get(buffSize);
		    
		    if (bufferPool != null) {
				
				//no need to sync around 'poolsLock', as adding during compaction is ok
				
		      synchronized ( bufferPool ){
				  
		        bufferPool.add(buff);
		      }
		    }else{
				
		      Debug.out("Invalid buffer given; could not find proper buffer pool");
		    }
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
    if( DEBUG_PRINT_MEM ) {
      System.out.println( "runGarbageCollection()" );
    }
    System.runFinalization();
    System.gc();
  }
  
  
  /**
   * Checks memory usage of free buffers in buffer pools,
   * and calls the compaction method if necessary.
   */
  private void compactBuffers() {
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
	
	compactSlices();
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
					
				Debug.printStackTrace( e );
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
	  			
	  			System.out.println();
	  			
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
				
				if ( DEBUG_HANDOUT_SIZES ){
					it = size_counts.entrySet().iterator();
					
					String	str = "";
						
					while( it.hasNext()){
						
						Map.Entry	entry = (Map.Entry)it.next();
						
						str += (str.length()==0?"":",") + entry.getKey() + "=" + entry.getValue();
					}
					
					System.out.println( str );
				}
				
				String str = "";
				
				for (int i=0;i<slice_entries.length;i++){
				
					boolean[]	allocs = slice_allocs[i];
					int	alloc_count = 0;
					for (int j=0;j<allocs.length;j++){
						if( allocs[j]){
							alloc_count++;
						}
					}
					str += (i==0?"":",") + "["+SLICE_ENTRY_SIZES[i]+"]f=" +slice_entries[i].size()+",a=" + (alloc_count*SLICE_ENTRY_ALLOC_SIZES[i]) + ",u=" +slice_use_count[i];
				}
				
				System.out.println( "slices: " + str );

	  		}
  		}
  	}
  	
	
		// Slice buffer management
	
	protected DirectByteBuffer
	getSliceBuffer(
		byte		_allocator,
		int			_length )
	{
		int	slice_index = getSliceIndex( _length );
		
		List		my_slice_entries 	= slice_entries[slice_index];

		synchronized( my_slice_entries ){
	
			boolean[]	my_allocs			= slice_allocs[slice_index];
			
			sliceBuffer	sb = null;
			
			if ( my_slice_entries.size() > 0 ){
				
				sb = (sliceBuffer)my_slice_entries.remove(0);
				
				slice_use_count[slice_index]++;
				
			}else{
				
					// find a free slot
				
				short	slot = -1;
				
				for (short i=0;i<my_allocs.length;i++){
					
					if( !my_allocs[i]){
						
						slot	= i;
			
						break;
					}
				}
				
				if ( slot != -1 ){
					
					short	slice_entry_size 	= SLICE_ENTRY_SIZES[slice_index];
					short	slice_entry_count	= SLICE_ENTRY_ALLOC_SIZES[slice_index];
					
					ByteBuffer	chunk = ByteBuffer.allocateDirect(  slice_entry_size*slice_entry_count  );
					
					my_allocs[slot] = true;
					
					for (short i=0;i<slice_entry_count;i++){
						
						chunk.limit((i+1)*slice_entry_size);
						chunk.position(i*slice_entry_size);
						
						ByteBuffer	slice = chunk.slice();
						
						sliceBuffer new_buffer = new sliceBuffer( slice, slot, i );
						
						if ( i == 0 ){
							
							sb = new_buffer;
							
							slice_use_count[slice_index]++;
							
						}else{
							
							my_slice_entries.add( new_buffer );
						}
					}
				}else{
					
					if ( !slice_alloc_fails[slice_index] ){
						
						slice_alloc_fails[slice_index]	= true;
						
						Debug.out( "Run out of slice space for '" + SLICE_ENTRY_SIZES[slice_index] + ", reverting to normal allocation" );
					}
					
					ByteBuffer buff = ByteBuffer.allocate( _length );
					
				    return( new DirectByteBuffer( _allocator, buff, this ));

				}
			}
			
			sliceDBB dbb = new sliceDBB( _allocator, sb );

			return( dbb );
		}
	}
	
	protected void
	freeSliceBuffer(
		DirectByteBuffer	ddb )
	{		
		if ( ddb instanceof sliceDBB ){
			
			int	slice_index = getSliceIndex( ddb.getBufferInternal().capacity());

			List		my_slice_entries 	= slice_entries[slice_index];

			synchronized( my_slice_entries ){
			
				my_slice_entries.add( 0, new sliceBuffer((sliceDBB)ddb ));
			}
		}
	}
	
	protected void
	compactSlices()
	{
			// we don't maintain the buffers in sorted order as this is too costly. however, we
			// always allocate and free from the start of the free list, so unused buffer space
			// will be at the end of the list. we periodically sort this list into allocate block
			// order so that if an entire block isn't used for one compaction cycle all of
			// its elements will end up together, if you see what I mean :P
		
			// when we find an entire block is unused then we just drop them from the list to
			// permit them (and the underlying block) to be garbage collected
		
		for (int i=0;i<slice_entries.length;i++){
			
			int			entries_per_alloc 	= SLICE_ENTRY_ALLOC_SIZES[i];
	
			List	l = slice_entries[i];
	
				// no point in trying gc if not enough entries
			
			if ( l.size() >= entries_per_alloc ){
			
				synchronized( l ){
					
					Collections.sort( l,
						new Comparator()
						{
							public int
							compare(
								Object	o1,
								Object	o2 )
							{
								sliceBuffer	sb1 = (sliceBuffer)o1;
								sliceBuffer	sb2 = (sliceBuffer)o2;
								
								int	res = sb1.getAllocID() - sb2.getAllocID();
								
								if ( res == 0 ){
									
									res = sb1.getSliceID() - sb2.getSliceID();
								}
								
								return( res );
							}
						});
			
					boolean[]	allocs				= slice_allocs[i];
			
					Iterator	it = l.iterator();
					
					int	current_alloc 	= -1;
					int entry_count		= 0;
			
					boolean	freed_one	= false;
					
					while( it.hasNext()){
						
						sliceBuffer	sb = (sliceBuffer)it.next();
						
						int	aid = sb.getAllocID();
						
						if ( aid != current_alloc ){
							
							if ( entry_count == entries_per_alloc ){
								
								// System.out.println( "CompactSlices[" + SLICE_ENTRY_SIZES[i]+"] freeing " + aid );
								
								freed_one	= true;
								
								allocs[aid]	= false;					
							}
							
							current_alloc	= aid;
							
							entry_count		= 1;
							
						}else{
							
							entry_count++;
						}
					}
					
					if ( entry_count == entries_per_alloc ){
						
						// System.out.println( "CompactSlices[" + SLICE_ENTRY_SIZES[i]+"] freeing " + current_alloc );
						
						freed_one	= true;
						
						allocs[current_alloc]	= false;					
					}
					
					if ( freed_one ){
						
						it = l.iterator();
						
						while( it.hasNext()){
							
							sliceBuffer	sb = (sliceBuffer)it.next();
							
							if ( !allocs[ sb.getAllocID()]){
								
								it.remove();
							}
						}
					}
				}
			}
		}
	}
	
	protected int
	getSliceIndex( 
		int	_length )
	{
		for (int i=0;i<SLICE_ENTRY_SIZES.length;i++){
			
			if ( _length <= SLICE_ENTRY_SIZES[i] ){
				
				return( i );
			}
		}
		
		Debug.out( "eh?");
		
		return( 0 );
	}
	
 	protected static class
	sliceBuffer
	{
		private ByteBuffer	buffer;
		private short		alloc_id;
		private short		slice_id;
		
		protected
		sliceBuffer(
			ByteBuffer	_buffer,
			short		_alloc_id,
			short		_slice_id )
		{
			buffer		= _buffer;
			alloc_id	= _alloc_id;
			slice_id	= _slice_id;
		}
		
		protected
		sliceBuffer(
			sliceDBB	dbb  )
		{
			buffer		= dbb.getBufferInternal();
			alloc_id	= dbb.getAllocID();
			slice_id	= dbb.getSliceID();		
		}
		
		protected ByteBuffer
		getBuffer()
		{
			return( buffer );
		}
		
		protected short
		getAllocID()
		{
			return( alloc_id );
		}
		
		protected short
		getSliceID()
		{
			return( slice_id );
		}
	}
	
	protected static class
	sliceDBB
		extends DirectByteBuffer
	{
		private short		alloc_id;
		private short		slice_id;

		protected
		sliceDBB(
			byte		_allocator,
			sliceBuffer	_sb )
		{	
			super( _allocator, _sb.getBuffer(), pool );
			
			alloc_id		= _sb.getAllocID();
			slice_id		= _sb.getSliceID();
		}
		
		protected short
		getAllocID()
		{
			return( alloc_id );
		}
		
		protected short
		getSliceID()
		{
			return( slice_id );
		}
	}
	
 	protected static class
	myInteger
  	{
  		int	value;
  	}
}
