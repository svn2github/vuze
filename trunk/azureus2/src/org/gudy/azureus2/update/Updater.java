/*
 * Created on 24.07.2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.update;

import java.io.File;

/**
 * @author Arbeiten
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Updater {

  public static final String VERSION = "1.0";

  public static final char NOT_FOUND = '0';
  public static final char READY = '1';
  public static final char FOUND = '2';
  
  public static void main(String[] args) {
    if (args.length < 3) {
      System.out.println("Usage: Updater full_classpath full_librarypath full_userpath");
      System.exit(-1);
    }
    
    File targetFile = new File(args[2], "Azureus2.jar"); //$NON-NLS-1$
    System.out.println("Old JAR: " + targetFile.getAbsolutePath());
    if(targetFile.isFile()) {
      File updateFile = new File(targetFile.getParentFile(), "Azureus2-new.jar");
      System.out.println("New JAR: " + updateFile.getAbsolutePath());
      if(updateFile.isFile()) {
//        System.out.println(FOUND);
        while(!targetFile.delete()) {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
          }
        }
        while(!updateFile.renameTo(targetFile)) {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
          }
        }
        String classPath = args[0]; // targetFile.getAbsolutePath()
        String libraryPath = args[1];
        String userPath = args[2];
//        classPath.replaceAll("Azureus\\.jar", "Azureus-new.jar");

        String exec = "java -classpath \"" + classPath + "\" -Djava.library.path=\"" + libraryPath + "\" -Duser.dir=\"" + userPath + "\" org.gudy.azureus2.ui.swt.Main";
//        System.out.println("executing: " + exec);

        try {
          Runtime.getRuntime().exec(exec);
        } catch (Exception e1) {
          e1.printStackTrace();
        }
      }
    }
//    System.out.println(NOT_FOUND);
  }
}
