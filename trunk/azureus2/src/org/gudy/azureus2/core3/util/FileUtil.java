/*
 * FileUtil.java
 *
 * Created on 10. Oktober 2003, 00:40
 */

package org.gudy.azureus2.core3.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;

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
  
  public static boolean isTorrentFile(String filename) throws FileNotFoundException, IOException {
    File check = new File(filename);
    if (!check.exists())
      throw new FileNotFoundException("File "+filename+" not found.");
    if (!check.canRead())
      throw new IOException("File "+filename+" cannot be read.");
    if (check.isDirectory())
      throw new FileIsADirectoryException("File "+filename+" is a directory.");
    try {
      TOTorrent test = TOTorrentFactory.deserialiseFromBEncodedFile(check);
      return true;
    } catch (TOTorrentException e) {
      return false;
    }
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
  
  public static void copyFile(File origin, File destination) throws IOException {
    OutputStream os = new FileOutputStream(destination);
    InputStream is = new FileInputStream(origin);
    byte[] buffer = new byte[32768];
    int nbRead = 0;
    while ((nbRead = is.read(buffer)) > 0) {
      os.write(buffer, 0, nbRead);
    }
    is.close();
    os.close();
  }
  
  
  /**
   * Takes a path and a file/dir name and returns the full path,
   * including file/dir name, removing 'name' duplicate if already
   * represented by 'path'
   * 
   * Example: smartFullName("c:\windows\thedir", "thedir") -> "c:\windows\thedir"
   * Example: smartFullName("c:\windows", "thedir") -> "c:\windows\thedir"
   * Example: smartFullName("c:\windows", "filename.txt") -> "c:\windows\filename.txt"
   */
  public static String smartFullName(String path, String name) {
    String fullPath = path + System.getProperty("file.separator") + name;
    //if 'name' is already represented by 'path'
    if (path.endsWith(name)) {
      File dirTest = new File(path);
      if (dirTest.isDirectory()) return path;
      else return fullPath;
    }
    else return fullPath;
  }
  
  
   /**
   * Takes a path and a file/dir name and returns the full dir path,
   * removing 'name' dir duplicate if already represented by 'path'
   * 
   * Example: smartPath("c:\windows\thedir", "thedir") -> "c:\windows\thedir"
   * Example: smartPath("c:\windows", "thedir") -> "c:\windows\thedir"
   * Example: smartPath("c:\windows\thedir", "filename.txt") -> "c:\windows\thedir"
   */
  public static String smartPath(String path, String name) {
    String fullPath = path + System.getProperty("file.separator") + name;
    //if 'name' is already represented by 'path'
    if (path.endsWith(name)) {
      File dirTest = new File(path);
      if (dirTest.isDirectory()) {
        return path;
      }
      else {
        return dirTest.getParent();
      }
    }
    //otherwise use path + name
    else {
      File dirTest = new File (fullPath);
      if (dirTest.isDirectory()) return fullPath;
      else return path;
    }
  }

}
