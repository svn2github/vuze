/*
 * Created on Nov 19, 2003
 * Created by nolar
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
  public static void out(String debug_message) {
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
    
    System.out.println(header + className + methodName + lineNumber + ":");
    System.out.println("  " + debug_message + "\n");
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
		 			
		 			System.out.println( "Interrupting thread '" + t + "'" );
		 			
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
		
		   System.out.println( indent + "active thread = " + t + ", daemon = " + t.isDaemon());
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
}
