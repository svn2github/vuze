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

import org.eclipse.swt.program.Program;

/**
 * @author Olivier Chalouhi
 *
 */
public class UpdateSWT {
  
  static FileOutputStream fosLog;
  static String userDir;
  public static void main(String args[]) throws Exception {
    userDir = System.getProperty("user.dir") + System.getProperty("file.separator");
    File f = new File(userDir + "updateSWT.log");
    fosLog = new FileOutputStream(f,true);
    String toLog = "SWT Updater started with parameters : \n";
    for(int i = 0 ; i < args.length ; i++) {
      toLog += args[i] + "\n";
    }
    toLog += "-----------------\n";
    fosLog.write(toLog.getBytes());    
    if(args.length < 4)
      return;
    try {
      
      toLog = "user.dir="  + userDir + "\n";
      fosLog.write(toLog.getBytes());
      
      toLog = "SWT Updater is waiting 1 sec\n";
      fosLog.write(toLog.getBytes()); 
      Thread.sleep(1000);
      
      String platform = args[0];
      toLog = "SWT Updater has detected platform : " + platform + "\n";
      fosLog.write(toLog.getBytes()); 
      
      if(platform.equals("carbon"))
        updateSWT_carbon(args[1]);
      else {
        updateSWT_generic(args[1]);
      }     
      
      restart(args[2],args[3]);
      
      toLog = "SWT Updater has finished\n";
      fosLog.write(toLog.getBytes());    
      
    } catch(Exception e) {
      toLog = "SWT Updater has encountered an exception : " + e + "\n";
      fosLog.write(toLog.getBytes());
      e.printStackTrace();
    } finally {
      fosLog.close();
    }
  }
  
  public static void updateSWT_generic(String zipFileName) throws Exception {
    String toLog = "SWT Updater is doing Generic Update\n";
    fosLog.write(toLog.getBytes()); 
    
    toLog = "SWT Updater is opening zip file : " + userDir + zipFileName + "\n";
    fosLog.write(toLog.getBytes());
    
    ZipFile zipFile = new ZipFile(userDir + zipFileName);
    Enumeration enum = zipFile.entries();
    while(enum.hasMoreElements()) {
      ZipEntry zipEntry = (ZipEntry) enum.nextElement();
      
      toLog = "\tSWT Updater is processing : " + zipEntry.getName() + "\n";
      fosLog.write(toLog.getBytes());
      
      if(zipEntry.getName().equals("swt.jar")) {        
        writeFile(zipFile,zipEntry,userDir);
      }
      if(zipEntry.getName().equals("swt-pi.jar")) {        
        writeFile(zipFile,zipEntry,userDir);
      }
      if(zipEntry.getName().equals("swt-mozilla.jar")) {        
        writeFile(zipFile,zipEntry,userDir);
      }     
      if(zipEntry.getName().startsWith("libswt") && zipEntry.getName().endsWith(".so")) {        
        writeFile(zipFile,zipEntry,userDir);
      }
      if(zipEntry.getName().startsWith("swt-win32-") && zipEntry.getName().endsWith(".dll")) {
        writeFile(zipFile,zipEntry,userDir);
      }
    }    
  }
  
  public static void updateSWT_carbon(String zipFileName) throws Exception{
    String toLog = "SWT Updater is doing Carbon (OSX) Update\n";
    fosLog.write(toLog.getBytes());
    
    toLog = "SWT Updater is opening zip file : " + userDir + zipFileName + "\n";
    fosLog.write(toLog.getBytes());
    
    ZipFile zipFile = new ZipFile(userDir +  zipFileName);
    Enumeration enum = zipFile.entries();
    while(enum.hasMoreElements()) {     
      ZipEntry zipEntry = (ZipEntry) enum.nextElement();
      
      toLog = "\tSWT Updater is processing : " + zipEntry.getName() + "\n";
      fosLog.write(toLog.getBytes());
      
      if(zipEntry.getName().equals("java_swt")) {                
        writeFile(zipFile,zipEntry,userDir + "Azureus.app/Contents/MacOS/");
        File f = openFile("Azureus.app/Contents/MacOS/","java_swt");
        String path = f.getAbsolutePath();
        String chgRights = "chmod 755 " + path;
        Process p = Runtime.getRuntime().exec(chgRights);
        p.waitFor();
      }
      if(zipEntry.getName().equals("swt.jar")) {        
        writeFile(zipFile,zipEntry,userDir + "Azureus.app/Contents/Resources/Java/");
      }
      if(zipEntry.getName().startsWith("libswt-carbon-") && zipEntry.getName().endsWith(".jnilib")) {
        writeFile(zipFile,zipEntry,userDir + "Azureus.app/Contents/Resources/Java/dll/");
      }
    }    
  }
   
  public static void writeFile(ZipFile zipFile,ZipEntry zipEntry,String path) throws Exception {
    String toLog = "";
    if(path != null) {
      toLog = "\t\t Unzipping file " + zipEntry.getName() + "to path " + path + "\n";
    } else {
      toLog = "\t\t Unzipping file " + zipEntry.getName() + "\n";
    }    
    fosLog.write(toLog.getBytes());
    
    String fileName = zipEntry.getName();
        
    File f = openFile(path,fileName);
    
    
    //If file already exists, rename to .old
    if(f.exists()) {
      toLog = "\t\tFile exists, renaming to .old\n";
      fosLog.write(toLog.getBytes());
      
      String backUpName = fileName + ".old";
      File backup =  openFile(path,backUpName);
      if(backup.exists()) {backup.delete(); Thread.sleep(500); }
      if(!f.renameTo(backup)) {
        toLog = "\t\tCouldn't rename file\n";
        fosLog.write(toLog.getBytes());
        
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
  
  public static File openFile(String path,String name) throws IOException {
    String fileName = name;
    
    if(path != null) {
      fileName = path + name;            
    }
    
    String toLog = "\t\t\tOpening : " + fileName + "\n";
    fosLog.write(toLog.getBytes());
    
    return new File(fileName);
  }
  
  public static void restart(String userPath,String libPath) throws IOException{            
    
    String classPath = System.getProperty("java.class.path"); //$NON-NLS-1$   
    String javaPath = System.getProperty("java.home")
                    + System.getProperty("file.separator")
                    + "bin"
                    + System.getProperty("file.separator");
    
    String exec = javaPath + "java -classpath \"" + classPath
    + "\" -Djava.library.path=\"" + libPath
    + "\" -Duser.dir=\"" + userPath
    + "\" org.gudy.azureus2.ui.swt.Main";
             
    String toLog = "Restarting with command line : " + exec + "\n";
    fosLog.write(toLog.getBytes());
    
    Runtime.getRuntime().exec(exec);
  }
}
