/*
 * Created on Nov 19, 2003
 * Created by Alon Rohter
 *
 */
package org.gudy.azureus2.core3.util;

import java.io.*;
import java.util.*;

/**
 * Debug-assisting class.
 */
public class Debug {
  
	private static AEDiagnosticsLogger	diag_logger	= AEDiagnostics.getLogger( "debug" );

  
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
  
  public static void
  outNoStack(
  	String		str )
  {
  	outNoStack( str, false );
  }
  
  public static void
  outNoStack(
  	String		str,
	boolean		stderr)
  {
    diag_logger.logAndOut("DEBUG::"+ new Date(SystemTime.getCurrentTime()).toString(), stderr );
    
    diag_logger.logAndOut("  " + str, stderr );
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
    
    diag_logger.logAndOut(header+className+(methodName)+lineNumber+":");
    if (_debug_msg.length() > 0) {
    	diag_logger.logAndOut("  " + _debug_msg);
    }
    if ( trace_trace_tail != null ){
    	diag_logger.logAndOut( "    " + trace_trace_tail );
    }
    if (_exception != null) {
    	diag_logger.logAndOut(_exception);
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
        	diag_logger.logAndOut(st[i].toString());
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
		 			
		 			out( "Interrupting thread '".concat(t.toString()).concat("'" ));
		 			
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
		out(name+":");
			
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
		
		   out( indent.concat("active thread = ").concat(t.toString()).concat(", daemon = ").concat(String.valueOf(t.isDaemon())));
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
				   	Debug.printStackTrace( e );
				   }
			   }
		   }
	   }.start();
   }
   
	public static void
	dumpSystemProperties()
	{
		out( "System Properties:");
		
 		Properties props = System.getProperties();
 		
 		Iterator it = props.keySet().iterator();
 		
 		while(it.hasNext()){
 			
 			String	name = (String)it.next();
 			
 			out( "\t".concat(name).concat(" = '").concat(props.get(name).toString()).concat("'" ));
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
	
	public static void
	printStackTrace(
		Throwable e )
	{
		try{
			ByteArrayOutputStream	baos = new ByteArrayOutputStream();
			
			PrintWriter	pw = new PrintWriter( new OutputStreamWriter( baos ));
			
			e.printStackTrace( pw );
			
			pw.close();
			
			outNoStack( baos.toString(), true);
			
		}catch( Throwable ignore ){
			
			e.printStackTrace();
		}
	}
}
