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

import java.io.BufferedReader;
import java.io.InputStreamReader;


/**
 * @author Olivier Chalouhi
 *
 */
public class TestSnippet {
  
  public static void main(String args[]) throws Exception{
    try {
    String[] exec =  {"java","-version"};
    //String exec = "java -version";
    
    System.out.println("About to exec : " + exec );
    
    Process p = Runtime.getRuntime().exec(exec);
    
    int exitCode = p.waitFor();
    
    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
    String line = null;
    while((line = br.readLine()) != null ) {
      System.out.println(line);
    }
    
    br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
    line = null;
    while((line = br.readLine()) != null ) {
      System.out.println(line);
    }
    
    System.out.println("Exited with code : " + exitCode);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
  
}
