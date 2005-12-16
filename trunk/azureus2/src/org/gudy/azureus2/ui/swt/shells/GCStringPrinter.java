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
import org.eclipse.swt.graphics.Point;

/**
 * @author Olivier Chalouhi
 *
 */
public class GCStringPrinter {
  
  public static boolean printString(GC gc,String string,Rectangle printArea) {
    int x0 = printArea.x;
    int y0 = printArea.y;
    int height = 0;
    Rectangle oldClipping = gc.getClipping();

    //Protect the GC from drawing outside the drawing area
    gc.setClipping(printArea);    
    
    //We need to add some cariage return ...
    String sTabsReplaced = string.replaceAll("\t", "  ");

    StringBuffer outputLine = new StringBuffer();

    // Process string line by line
    StringTokenizer stLine = new StringTokenizer(sTabsReplaced,"\n");
    while(stLine.hasMoreElements()) {      
      int iLineHeight = 0;
      String sLine = stLine.nextToken();
      if (gc.stringExtent(sLine).x > printArea.width) {
        //System.out.println("Line: "+ sLine);
        StringTokenizer stWord = new StringTokenizer(sLine, " ");
        String space = "";  
        int iLineLength = 0;
        iLineHeight = gc.stringExtent(" ").y;
  
        // Process line word by word
        while(stWord.hasMoreElements()) {      
          String word = stWord.nextToken();
          
          // check if word is longer than our print area, and split it
          Point ptWordSize = gc.stringExtent(word + " ");
          while (ptWordSize.x > printArea.width) {
          	int endIndex = word.length() - 1;
          	do {
          		endIndex--;
          		ptWordSize = gc.stringExtent(word.substring(0, endIndex) + " ");
          	} while (endIndex > 3 && ptWordSize.x + iLineLength > printArea.width);
          	// append part that will fit
          	outputLine.append(space)
          			  .append(word.substring(0, endIndex))
          			  .append("\n");
            height += ptWordSize.y;

            // setup word as the remaining part that didn't fit
          	word = word.substring(endIndex);
          	ptWordSize = gc.stringExtent(word + " ");
          	iLineLength = 0;
          }
          iLineLength += ptWordSize.x;
          //System.out.println(outputLine + " : " + word + " : " + iLineLength);
          if(iLineLength > printArea.width) {
            iLineLength = ptWordSize.x;
            height += iLineHeight;
            iLineHeight = ptWordSize.y;
            space = "\n";
          }
          if (iLineHeight < ptWordSize.y)
            iLineHeight = ptWordSize.y;
           
          outputLine.append(space).append(word);            
          space = " ";
        }
      } else {
        outputLine.append(sLine);
        iLineHeight = gc.stringExtent(sLine).y;
      }
      outputLine.append("\n");
      height += iLineHeight;
    }
    
    String sOutputLine = outputLine.toString();
    gc.drawText(sOutputLine,x0,y0,true);        
    gc.setClipping(oldClipping);
    return height <= printArea.height;
  }
  
  private static int getAdvanceWidth(GC gc,String s) {
    int result = 0;
    for(int i = 0 ; i < s.length() ; i++) {
      result += gc.getAdvanceWidth(s.charAt(i)) - 1;
    }
    return result;
 }
}
