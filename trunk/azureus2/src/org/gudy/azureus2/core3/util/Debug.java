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

}
