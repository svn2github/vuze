/*
 * File    : TRTrackerServerFactory.java
 * Created : 5 Oct. 2003
 * By      : Parg 
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
public class ByteFormatter {
  public static String nicePrint(byte[] data) {
	if(data == null)
	  return "";      
	String out = "";    
	for (int i = 0; i < data.length; i++) {
	  out = out + nicePrint(data[i]);
	  if (i % 4 == 3)
		out = out + " ";
	}
	return out;
  }
  

  public static String nicePrint(byte[] data, boolean tight) {
	if(data == null)
	  return "";      
	String out = "";    
	for (int i = 0; i < data.length; i++) {
	  out = out + nicePrint(data[i]);
	  if (!tight && (i % 4 == 3))
		out = out + " ";
	}
	return out;
  }


  public static String nicePrint(byte b) {
	byte b1 = (byte) ((b >> 4) & 0x0000000F);
	byte b2 = (byte) (b & 0x0000000F);
	return nicePrint2(b1) + nicePrint2(b2);
  }


  public static String nicePrint2(byte b) {
	String out = "";
	switch (b) {
	  case 0 :
		out = "0";
		break;
	  case 1 :
		out = "1";
		break;
	  case 2 :
		out = "2";
		break;
	  case 3 :
		out = "3";
		break;
	  case 4 :
		out = "4";
		break;
	  case 5 :
		out = "5";
		break;
	  case 6 :
		out = "6";
		break;
	  case 7 :
		out = "7";
		break;
	  case 8 :
		out = "8";
		break;
	  case 9 :
		out = "9";
		break;
	  case 10 :
		out = "A";
		break;
	  case 11 :
		out = "B";
		break;
	  case 12 :
		out = "C";
		break;
	  case 13 :
		out = "D";
		break;
	  case 14 :
		out = "E";
		break;
	  case 15 :
		out = "F";
		break;
	}
	return out;
  }
}