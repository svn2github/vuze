/*
 * File    : GCStringPrinter.java
 * Created : 16 mars 2004
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
package org.gudy.azureus2.ui.swt.shells;

import java.util.StringTokenizer;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

/**
 * @author Olivier Chalouhi
 *
 */
public class GCStringPrinter {
  
  public static void printString(GC gc,String string,Rectangle printArea) {
    int x0 = printArea.x;
    int y0 = printArea.y;
    int width = printArea.width;
    int height = printArea.height;
    Rectangle oldClipping = gc.getClipping();

    //Protect the GC from drawing outside the drawing area
    gc.setClipping(printArea);    
    
    //We need to add some cariage return ...
    StringTokenizer st = new StringTokenizer(string," ");
    StringBuffer outputLine = new StringBuffer();
    String space = "";  
    int length = 0;
    while(st.hasMoreElements()) {      
      String word = st.nextToken();
      length += getAdvanceWith(gc,word + " ");
      if(length > width) {
        length = 0;
        space = "\n";
      }
      outputLine.append(space + word);            
      space = " ";
    }
    
    gc.drawText(outputLine.toString(),x0,y0,true);        
    gc.setClipping(oldClipping);
  }
  
  private static int getAdvanceWith(GC gc,String s) {
    int result = 0;
    for(int i = 0 ; i < s.length() ; i++) {
      result += gc.getAdvanceWidth(s.charAt(i));
    }
    return result;
  }
}
