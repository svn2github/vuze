/*
 * FileUtil.java
 *
 * Created on 10. Oktober 2003, 00:40
 */

package org.gudy.azureus2.core3.util;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author  Tobias Minich
 */
public class FileUtil {
  
  public static String getCanonicalFileName(String filename) {
    // Sometimes Windows use filename in 8.3 form and cannot
    // match .torrent extension. To solve this, canonical path
    // is used to get back the long form

    String canonicalFileName = filename;
    try {
      canonicalFileName = new File(filename).getCanonicalPath();
    }
    catch (IOException ignore) {}
    return canonicalFileName;
  }

  public static String getApplicationPath() {
    if (System.getProperty("os.name").equals("Linux")) {
      return System.getProperty("user.home") + System.getProperty("file.separator") + ".azureus" + System.getProperty("file.separator");
    } else {
      return System.getProperty("user.dir") + System.getProperty("file.separator");
    }
  }
  
  public static File getApplicationFile(String filename) {
    return new File(FileUtil.getApplicationPath(), filename);
  }

  
  /**
   * Deletes the given dir and all files/dirs underneath
   */
  public static void recursiveDelete(File f) {
    if (f.isDirectory()) {
      File[] files = f.listFiles();
      for (int i = 0; i < files.length; i++) {
        recursiveDelete(files[i]);
      }
      f.delete();
    }
    else {
      f.delete();
    }
  }
  

}
