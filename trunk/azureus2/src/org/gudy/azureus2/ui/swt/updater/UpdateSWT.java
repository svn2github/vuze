/*
 * File    : UpdateSWT.java
 * Created : 3 avr. 2004
 * By      : Olivier
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.gudy.azureus2.ui.swt.updater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Olivier Chalouhi
 *
 */
public class UpdateSWT {
  
  public static void main(String args[]) {
    if(args.length < 4)
      return;
    try {
      Thread.sleep(3000);
      String platform = args[0];
      
      if(platform.equals("carbon"))
        updateSWT_carbon(args[1]);
      else {
        updateSWT_generic(args[1]);
      }     
      
      restart(args[2],args[3]);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
  
  public static void updateSWT_generic(String zipFileName) throws IOException {
    ZipFile zipFile = new ZipFile(zipFileName);
    Enumeration enum = zipFile.entries();
    while(enum.hasMoreElements()) {
      ZipEntry zipEntry = (ZipEntry) enum.nextElement();
      if(zipEntry.getName().equals("swt.jar")) {        
        writeFile(zipFile,zipEntry,null);
      }
      if(zipEntry.getName().startsWith("swt-win32-") && zipEntry.getName().endsWith(".dll")) {
        writeFile(zipFile,zipEntry,null);
      }
    }    
  }
  
  public static void updateSWT_carbon(String zipFileName) throws IOException{
    ZipFile zipFile = new ZipFile(zipFileName);
    Enumeration enum = zipFile.entries();
    while(enum.hasMoreElements()) {
      ZipEntry zipEntry = (ZipEntry) enum.nextElement();
      if(zipEntry.getName().equals("swt.jar")) {        
        writeFile(zipFile,zipEntry,null);
      }
      if(zipEntry.getName().startsWith("libswt-carbon-") && zipEntry.getName().endsWith(".jnilib")) {
        writeFile(zipFile,zipEntry,null);
      }
    }    
  }
   
  public static void writeFile(ZipFile zipFile,ZipEntry zipEntry,String path) throws IOException {
    String fileName = zipEntry.getName();
    File f = openFile(path,fileName);
    
    //If file already exists, rename to .old
    if(f.exists()) {
      String backUpName = fileName + ".old";
      File backup =  openFile(path,backUpName);
      if(backup.exists()) backup.delete();
      if(!f.renameTo(backup)) {
        throw new IOException("File " + fileName + " cannot be renamed into " + backUpName);
      }
    }
    
    f = openFile(path,fileName);
    FileOutputStream fos = new FileOutputStream(f);
    InputStream is = zipFile.getInputStream(zipEntry);
    byte[] buffer = new byte[4096];
    int read = 0;
    while((read = is.read(buffer)) > 0) {
      fos.write(buffer,0,read);
    }
    fos.close();
  }
  
  public static File openFile(String path,String name) {
    if(path != null)
      return new File(path,name);
    return new File(name);
  }
  
  public static void restart(String userPath,String libPath) throws IOException{
    String classPath = System.getProperty("java.class.path"); //$NON-NLS-1$   
    String javaPath = System.getProperty("java.home")
                    + System.getProperty("file.separator")
                    + "bin"
                    + System.getProperty("file.separator");
    
    String exec = javaPath + "javaw -classpath \"" + classPath
    + "\" -Djava.library.path=\"" + libPath
    + "\" -Duser.dir=\"" + userPath
    + "\" org.gudy.azureus2.ui.swt.Main";
             
    
    //File f = new File("updateSWT.log");
    //FileOutputStream fos = new FileOutputStream(f);
    //fos.write("Command Line : ".getBytes());
    //fos.write(exec.getBytes());
    //fos.write("\n".getBytes());
    //fos.close();
    Runtime.getRuntime().exec(exec);
  }
}
