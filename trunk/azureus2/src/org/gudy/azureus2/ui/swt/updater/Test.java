/*
 * File    : Test.java
 * Created : 4 avr. 2004
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;


/**
 * @author Olivier Chalouhi
 *
 */
public class Test {
  
  public static void main(String args[]) throws Exception{
    String classPath = System.getProperty("java.class.path"); //$NON-NLS-1$
    String libraryPath = System.getProperty("java.library.path"); //$NON-NLS-1$
    String userPath = System.getProperty("user.dir"); //$NON-NLS-1$
    String javaPath = System.getProperty("java.home")
                    + System.getProperty("file.separator")
                    + "bin"
                    + System.getProperty("file.separator");
    /*
    String[] exec = {
        javaPath + "java" ,
        	"-classpath" ,
        	"\"" + classPath + "\"" ,
        	"-Duser.dir=\"" + userPath + "\"" ,
        	"org.gudy.azureus2.ui.swt.updater.UpdateSWT" ,
        	"\"" + "carbon" + "\"" ,
        	"\"swtTemp.zip\"",
        	"\"" + userPath + "\"",
        	"\"" + libraryPath + "\"" };
    */
    String exec = javaPath + "java -classpath \"" + classPath
    + "\" -Duser.dir=\"" + userPath + "\" org.gudy.azureus2.ui.swt.updater.UpdateSWT \"" + "carbon" + "\" \"swtTemp.zip\" \""
    + userPath + "\" \"" + libraryPath + "\"";
    
    //String[] exec =  {"java","-version"};
    
    System.out.println("About to exec : " + exec );
    
    Process p = Runtime.getRuntime().exec(exec);
    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
    
    int exitCode = p.waitFor();
    String line = null;
    while((line = br.readLine()) != null ) {
      System.out.println(line);
    }
    br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
    line = null;
    while((line = br.readLine()) != null ) {
      System.out.println(line);
    }
    
//  p.waitFor();
    System.out.println("Exited with code : " + exitCode);
  }
  
}
