/*
 * Created on July 24 2003
 * Created by Alon Rohter
 */
package org.gudy.azureus2.update;

import java.io.*;

public class Updater {

  public static final String VERSION  = "1.1";
  
  public static final char NOT_FOUND  = '0';
  public static final char READY      = '1';
  public static final char FOUND      = '2';
    
  
  public static void main(String[] args) {
    FileWriter log = null;
    
    if (args.length < 3) {
      System.out.println("Usage: Updater full_classpath full_librarypath full_userpath");
      System.exit(-1);
    }
    
    String classPath    = args[0];
    String libraryPath  = args[1];
    String userPath     = args[2];
    String javaPath = System.getProperty("java.home")
                    + System.getProperty("file.separator")
                    + "bin"
                    + System.getProperty("file.separator");
      
    String relativePath = "";
    if(System.getProperty("os.name").equalsIgnoreCase("Mac OS X")) {
      relativePath = "Azureus.app/Contents/Resources/Java/";
    }
    
    File oldFile = new File(userPath, relativePath + "Azureus2.jar");
    File updateFile = new File(oldFile.getParentFile(), "Azureus2-new.jar");
    File logFile = new File( userPath, "update.log" );
    
    try {
      log = new FileWriter( logFile, true );
      
      log.write("Updater:: classPath=" + classPath
                         + " libraryPath=" + libraryPath
                         + " userPath=" + userPath + "\n");
      
      log.write("Updater:: oldFile=" + oldFile.getAbsolutePath()
                    + " updateFile=" + updateFile.getAbsolutePath() + "\n");
    
      log.write("Updater:: testing for " + oldFile.getAbsolutePath() + " .....");
      if(oldFile.isFile()) {
        log.write("exists\n");
        
        log.write("Updater:: testing for " + updateFile.getAbsolutePath() + " .....");
        if(updateFile.isFile()) {
          log.write("exists\n");
          
          log.write("Updater:: attempting to delete " + oldFile.getAbsolutePath() + " ...");
          while(!oldFile.delete()) {
            log.write(" x ");
            Thread.sleep(1000);
          }
          log.write("deleted\n");

          
          log.write("Updater:: attempting to rename " + updateFile.getAbsolutePath() + " ...");
          while(!updateFile.renameTo(oldFile)) {
            log.write(" x ");
            Thread.sleep(1000);
          }
          log.write("renamed\n");
        
          log.write("Updater:: is restarting ");
          restartAzureus("org.gudy.azureus2.ui.swt.Main",new String[0]);
        }
        else log.write("not found\n");
      }
      else log.write("not found\n");
    }
    catch (Exception e) {
      try {
        log.write("\nUpdater:: exception:\n" + e.toString());
      } catch (IOException ignore) {}
    }
    finally {
      try {
        if (log != null) log.close();
      } catch (IOException ignore) {}
    }
  }
  
  
  /*
   * Follows a complete copy of RestartUtil, with logging options disabled. 
   *
   */
  private static final String restartScriptName = "restartScript";
  
  public static void restartAzureus(String mainClass,String[] parameters) {
    String osName = System.getProperty("os.name");
    if(osName.equalsIgnoreCase("Mac OS X")) {
      restartAzureus_OSX(mainClass,parameters);
    } else if(osName.equalsIgnoreCase("Linux")) {
      restartAzureus_Linux(mainClass,parameters);
    } else {
      restartAzureus_win32(mainClass,parameters);
    }
  }
  
  private static void restartAzureus_win32(String mainClass,String[] parameters) {
    //Classic restart way using Runtime.exec directly on java(w)
    String classPath = System.getProperty("java.class.path"); //$NON-NLS-1$
    String javaPath = System.getProperty("java.home")
                    + System.getProperty("file.separator")
                    + "bin"
                    + System.getProperty("file.separator");
    
    String exec = "\"" + javaPath + "javaw\" -classpath \"" + classPath
    + "\" " + mainClass;
    for(int i = 0 ; i < parameters.length ; i++) {
      exec += " \"" + parameters[i] + "\"";
    }
    
    try {                
      Runtime.getRuntime().exec(exec);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
  
  private static void restartAzureus_OSX(String mainClass,String[] parameters) {
    String classPath = System.getProperty("java.class.path"); //$NON-NLS-1$
    String libraryPath = System.getProperty("java.library.path"); //$NON-NLS-1$
    String userPath = System.getProperty("user.dir"); //$NON-NLS-1$
    String javaPath = System.getProperty("java.home")
                    + System.getProperty("file.separator")
                    + "bin"
                    + System.getProperty("file.separator");
    
    String exec = "#!/bin/bash\n\"" + userPath + "/Azureus.app/Contents/MacOS/java_swt\" -classpath \"" + classPath
    + "\" -Duser.dir=\"" + userPath + "\" -Djava.library.path=\"" + libraryPath + "\" " + mainClass ;
    for(int i = 0 ; i < parameters.length ; i++) {
      exec += " \"" + parameters[i] + "\"";
    }
    
    String fileName = userPath + "/" + restartScriptName;
    
    File fUpdate = new File(fileName);
    try {
	    FileOutputStream fosUpdate = new FileOutputStream(fUpdate,false);
	    fosUpdate.write(exec.getBytes());
	    fosUpdate.close();
	    Process pChMod = Runtime.getRuntime().exec("chmod 755 " + fileName);
	    pChMod.waitFor();
	    Process p = Runtime.getRuntime().exec("./" + restartScriptName);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
  
  private static void restartAzureus_Linux(String mainClass,String[] parameters) {
    String classPath = System.getProperty("java.class.path"); //$NON-NLS-1$
    String libraryPath = System.getProperty("java.library.path"); //$NON-NLS-1$
    String userPath = System.getProperty("user.dir"); //$NON-NLS-1$
    String javaPath = System.getProperty("java.home")
                    + System.getProperty("file.separator")
                    + "bin"
                    + System.getProperty("file.separator");
    
    String exec = "#!/bin/bash\n\"" + javaPath + "java\" -classpath \"" + classPath
    + "\" -Duser.dir=\"" + userPath + "\" -Djava.library.path=\"" + libraryPath + "\" " + mainClass ;
    for(int i = 0 ; i < parameters.length ; i++) {
      exec += " \"" + parameters[i] + "\"";
    }
    
    String fileName = userPath + "/" + restartScriptName;
    
    File fUpdate = new File(fileName);
    try {
	    FileOutputStream fosUpdate = new FileOutputStream(fUpdate,false);
	    fosUpdate.write(exec.getBytes());
	    fosUpdate.close();
	    Process pChMod = Runtime.getRuntime().exec("chmod 755 " + fileName);
	    pChMod.waitFor();
	    Process p = Runtime.getRuntime().exec("./" + restartScriptName);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
  
}
      

