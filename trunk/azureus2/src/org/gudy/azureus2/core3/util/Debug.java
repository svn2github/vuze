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
    header = header + new Date(System.currentTimeMillis()).toString() + "::";
    String className;
    String methodName;
    int lineNumber;
    
    try {
      throw new Exception();
    }
    catch (Exception e) {
      StackTraceElement st = e.getStackTrace()[1];
      className = st.getClassName() + "::";
      methodName = st.getMethodName() + "::";
      lineNumber = st.getLineNumber();
    }
    
    System.out.println(header.concat(className).concat(methodName).concat(String.valueOf(lineNumber)).concat(":"));
    if (_debug_msg.length() > 0) {
      System.out.println("  " + _debug_msg + " :");
    }
    if (_exception != null) {
      _exception.printStackTrace();
    }
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
   	new Thread("Thread Dumper")
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
}
