/*
 * File    : Sha1AlgorithmHelper.java
 * Created : 12 mars 2004
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
package org.gudy.azureus2.core3.util;

/**
 * @author Olivier
 * 
 */
public class Sha1AlgorithmHelper {
  
  public static void main(String args[]) {
  	part2();
  }
  
  public static void part1() {
    for(int t = 16 ; t <= 79 ; t++) {
    	System.out.println( "w" + t + " = w" + (t-3) + " ^ w" + (t-8) + " ^ w" + (t-14) + " ^ w" + (t-16) + ";");
    	System.out.println( "w" + t + " = (w" + t + " << 1) | (w" + t + " >>> 31);");
    }
  }
  
  public static void part2() {
  	for(int t=0; t<= 79 ; t++) {
      int fn = t / 20;      
      System.out.print("temp = ((a << 5) | (a >>> 27)) + e + w" + t + " + ");
      if(fn == 0) {
        System.out.println("((b & c) | ((~b) & d)) + 0x5A827999 ;");
      }
      if(fn == 1) {
        System.out.println("(b ^ c ^ d) + 0x6ED9EBA1 ;");
      }
      if(fn == 2) {
        System.out.println("((b & c) | (b & d) | (c & d)) + 0x8F1BBCDC ;");
      }
      if(fn == 3) {
        System.out.println("(b ^ c ^ d) + 0xCA62C1D6 ;");
      }
      System.out.println("e = d ; d = c ; c = (b<<30) | (b >>> 2); b = a; a = temp;");
  	}
  }

}
