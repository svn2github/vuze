/*
 * Created on Nov 19, 2003
 * Created by Alon Rohter
 *
 */
package org.gudy.azureus2.core3.util;

import java.util.*;

/**
 * Debug-assisting class.
 */
public class Debug {
  
  
  /**
   * Prints out the given debug message to System.out,
   * prefixed by the calling class name, method and
   * line number.
   */
  public static void out(final String _debug_message) {
    out( _debug_message, null );
  }
  
  /**
   * Prints out the given exception stacktrace to System.out,
   * prefixed by the calling class name, method and
   * line number.
   */
  public static void out(final Throwable _exception) {
    out( "", _exception );
  }
  
  /**
   * Prints out the given debug message to System.out,
   * prefixed by the calling class name, method and
   * line number, appending the stacktrace of the given exception.
   */
  public static void out(final String _debug_msg, final Throwable _exception) {
    String header = "DEBUG::";
    header = header + new Date(SystemTime.getCurrentTime()).toString() + "::";
    String className;
    String methodName;
    int lineNumber;
    String	trace_trace_tail = null;
    
    try {
      throw new Exception();
    }
    catch (Exception e) {
    	StackTraceElement[]	st = e.getStackTrace();
    	
      StackTraceElement first_line = st[2];
      className = first_line.getClassName() + "::";
      methodName = first_line.getMethodName() + "::";
      lineNumber = first_line.getLineNumber();
   
      for (int i=3;i<st.length;i++){
      	
      	if ( trace_trace_tail == null ){
      		trace_trace_tail = "";
      	}else{
      		trace_trace_tail += ",";
      	}
      	
      	String cn = st[i].getClassName();
      	      	  
      	cn = cn.substring( cn.lastIndexOf(".")+1);
      	
      	trace_trace_tail += cn +"::"+st[i].getMethodName()+"::"+st[i].getLineNumber();
      }
    }
    
    System.out.println(header+className+(methodName)+lineNumber+":");
    if (_debug_msg.length() > 0) {
      System.out.println("  " + _debug_msg);
    }
    if ( trace_trace_tail != null ){
    	System.out.println( "    " + trace_trace_tail );
    }
    if (_exception != null) {
      _exception.printStackTrace();
    }
  }
  
  public static String getLastCaller() {
    try {
      throw new Exception();
    }
    catch (Exception e) {
      // [0] = our throw
      // [1] = the line that called getLastCaller
      // [2] = the line that called the function that has getLastCaller
      StackTraceElement st[] = e.getStackTrace();
      if (st.length > 2)
        return st[2].toString();
      if (st.length > 1)
        return st[1].toString();
    }
    return "??";
  }

  public static void outStackTrace() {
    // skip the last, since they'll most likely be main
    outStackTrace(1);
  }

  public static void outStackTrace(int endNumToSkip) {
    try {
      throw new Exception();
    }
    catch (Exception e) {
      StackTraceElement st[] = e.getStackTrace();
      for (int i = 1; i < st.length - endNumToSkip; i++) {
        if (st[i].getMethodName() != "outStackTrace")
          System.out.println(st[i].toString());
      }
    }
  }

	public static void
	killAWTThreads()
	{
		ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
			
		killAWTThreads( threadGroup );
	}
	
	public static String
	getCompressedStackTrace(
		int	frames_to_skip )
	{
		String	trace_trace_tail	= null;
	
	   try {
	      throw new Exception();
	    }
	    catch (Exception e) {
	    	StackTraceElement[]	st = e.getStackTrace();
	    		   
	      for (int i=frames_to_skip;i<st.length;i++){
	      	
	      	if ( trace_trace_tail == null ){
	      		trace_trace_tail = "";
	      	}else{
	      		trace_trace_tail += ",";
	      	}
	      	
	      	String cn = st[i].getClassName();
	      	      	  
	      	cn = cn.substring( cn.lastIndexOf(".")+1);
	      	
	      	trace_trace_tail += cn +"::"+st[i].getMethodName()+"::"+st[i].getLineNumber();
	      }
	    }
	    
	    return( trace_trace_tail );
	}
	
	public static void
	killAWTThreads(
		   ThreadGroup	threadGroup )
	{
		 Thread[] threadList = new Thread[threadGroup.activeCount()];
			
		 threadGroup.enumerate(threadList);
			
		 for (int i = 0;	i < threadList.length;	i++){

		 	Thread t = 	threadList[i];
		 	
		 	if ( t != null ){
		 		
		 		String 	name = t.getName();
		 		
		 		if ( name.startsWith( "AWT" )){
		 			
		 			System.out.println( "Interrupting thread '".concat(t.toString()).concat("'" ));
		 			
		 			t.interrupt();
		 		}
			}
		}
		
		if ( threadGroup.getParent() != null ){
	  	
			killAWTThreads(threadGroup.getParent());
		}	
	}
		
	public static void
	dumpThreads(
		String	name )
	{
		System.out.println(name+":");
			
	  	ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
			
	  	dumpThreads( threadGroup, "\t" );
	}
   
   public static void
   dumpThreads(
   		ThreadGroup	threadGroup,
   		String		indent )
   {
	  Thread[] threadList = new Thread[threadGroup.activeCount()];
			
	  threadGroup.enumerate(threadList);
			
	  for (int i = 0;	i < threadList.length;	i++){

		Thread t = 	threadList[i];
		
		if ( t != null ){		
		
		   System.out.println( indent.concat("active thread = ").concat(t.toString()).concat(", daemon = ").concat(String.valueOf(t.isDaemon())));
		}
	  }
	  
	  if ( threadGroup.getParent() != null ){
	  	
	  	dumpThreads(threadGroup.getParent(),indent+"\t");
	  }
   }
   
   public static void
   dumpThreadsLoop(
   	final String	name )
   {
   	new AEThread("Thread Dumper")
	   {
		   public void 
		   run()
		   {	
			   while(true){
				   Debug.dumpThreads(name);
				   
				   try{
				   	Thread.sleep(5000);
				   }catch( Throwable e ){
				   	e.printStackTrace();
				   }
			   }
		   }
	   }.start();
   }
   
	public static void
	dumpSystemProperties()
	{
		System.out.println( "System Properties:");
		
 		Properties props = System.getProperties();
 		
 		Iterator it = props.keySet().iterator();
 		
 		while(it.hasNext()){
 			
 			String	name = (String)it.next();
 			
 			System.out.println( "\t".concat(name).concat(" = '").concat(props.get(name).toString()).concat("'" ));
 		}
	}
	
	public static String
	getNestedExceptionMessage(
		Throwable 		e )
	{
		String	last_message	= null;
		
		while(e != null){
			
			if ( e.getMessage() != null ){
				
				last_message	= e.getMessage() + ( last_message==null?"":(", " + last_message ));
			}
			
			e	= e.getCause();
		}
		
		if ( last_message == null ){
			
			last_message = e.getClass().getName();
			
			int	pos = last_message.lastIndexOf(".");
			
			last_message = last_message.substring( pos+1 );
		}
		
		return( last_message );
	}
}
