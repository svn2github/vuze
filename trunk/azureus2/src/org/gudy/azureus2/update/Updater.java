/*
 * Created on 24.07.2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.update;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

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
    
    URL origin = getClassLocation(Updater.class);
//    System.out.println("Update Location: " + origin.toString());
    File originDirectory = getFileFromURL(origin).getParentFile();
    if(originDirectory.isDirectory()) {
      File updateFile = new File(originDirectory, "Azureus2-new.jar");
      File targetFile = new File(originDirectory, "Azureus2.jar");
      if(updateFile.isFile()) {
//        System.out.println(FOUND);
        while(!updateFile.isFile())
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
          }
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

        String exec = "java -classpath \"" + classPath + "\" -Djava.library.path=\"" + libraryPath + "\" -Duser.dir=\"" + userPath + "\" org.gudy.azureus2.ui.swt.Main";
//        System.out.println("executing: " + exec);

        try {
          Process process = Runtime.getRuntime().exec(exec);
        } catch (Exception e1) {
          e1.printStackTrace();
        }
      }
    }
//    System.out.println(NOT_FOUND);
  }

  public static File getFileFromClassJar(Class cls, String fileName) {
    URL origin = getClassLocation(cls);
    File originDirectory = getFileFromURL(origin).getParentFile();
    File jarFile = null;
    if (originDirectory.isDirectory()) {
      jarFile = new File(originDirectory, fileName);
    }
    return jarFile;
  }

  /**
   * Given a Class object, attempts to find its .class location [returns null
   * if no such definition can be found]. Use for testing/debugging only.
   * 
   * @return URL that points to the class definition [null if not found].
   */
  public static URL getClassLocation (final Class cls)
  {
      if (cls == null) throw new IllegalArgumentException ("null input: cls");
        
      URL result = null;
      final String clsAsResource = cls.getName ().replace ('.', '/').concat (".class");
        
      final ProtectionDomain pd = cls.getProtectionDomain ();
      // java.lang.Class contract does not specify if 'pd' can ever be null;
      // it is not the case for Sun's implementations, but guard against null
      // just in case:
      if (pd != null) 
      {
          final CodeSource cs = pd.getCodeSource ();
          // 'cs' can be null depending on the classloader behavior:
          if (cs != null) result = cs.getLocation ();
            
          if (result != null)
          {
              // Convert a code source location into a full class file location
              // for some common cases:
              if ("file".equals (result.getProtocol ()))
              {
                  try
                  {
                      if (result.toExternalForm ().endsWith (".jar") ||
                          result.toExternalForm ().endsWith (".zip")) 
                          result = new URL ("jar:".concat (result.toExternalForm ())
                              .concat("!/").concat (clsAsResource));
                      else if (new File (result.getFile ()).isDirectory ())
                          result = new URL (result, clsAsResource);
                  }
                  catch (MalformedURLException ignore) {}
              }
          }
      }
        
      if (result == null)
      {
          // Try to find 'cls' definition as a resource; this is not
          // documented to be legal, but Sun's implementations seem to allow this:
          final ClassLoader clsLoader = cls.getClassLoader ();
            
          result = clsLoader != null ?
              clsLoader.getResource (clsAsResource) :
              ClassLoader.getSystemResource (clsAsResource);
      }
        
      return result;
  }

  public static File getFileFromURL(URL url) {
    File file = null;
    String urlString = url.toString();
//    System.out.println("urlString: " + urlString);
    if(urlString.startsWith("jar:file:/")) {
      int posDirectory = urlString.indexOf(".jar!", 11);
      String jarName = urlString.substring(4, posDirectory+4);
//        System.out.println("jarName: " + jarName);
      URI uri = URI.create(jarName);
      file = new File(uri);
    } else {
      file = new File(System.getProperty("user.dir"), "1.tst");
    }
    return file;
  }
}
