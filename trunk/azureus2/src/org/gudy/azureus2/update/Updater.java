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
      

    File oldFile = new File(userPath, "Azureus2.jar");
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
        
          String exec = javaPath + "java -classpath \"" + classPath
                      + "\" -Djava.library.path=\"" + libraryPath
                      + "\" -Duser.dir=\"" + userPath
                      + "\" org.gudy.azureus2.ui.swt.Main";

          log.write("Updater:: executing command: " + exec + "\n");

          Runtime.getRuntime().exec(exec);
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
}
      

