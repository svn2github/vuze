/*
 * File    : DisplayFormatters.java
 * Created : 07-Oct-2003
 * By      : gardnerpar
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
 * @author gardnerpar
 *
 */
public class 
DisplayFormatters 
{
	public static String formatByteCountToKBEtc(int n) {
	  if (n < 1024)
		return n + " B";
	  if (n < 1024 * 1024)
		return (n / 1024) + "." + ((n % 1024) / 103) + " kB";
	  if (n < 1024 * 1024 * 1024)
		return (n / (1024 * 1024))
		  + "."
		  + ((n % (1024 * 1024)) / (103 * 1024))
		  + " MB";
	  if (n < 1024 * 1024 * 1024 * 1024)
		return (n / (1024 * 1024 * 1024))
		  + "."
		  + ((n % (1024 * 1024 * 1024)) / (103 * 1024 * 1024))
		  + " GB";
	  return "A lot";
	}

	public static String formatByteCountToKBEtc(long n) {
	  if (n < 1024)
		return n + " B";
	  if (n < 1024 * 1024)
		return (n / 1024) + "." + ((n % 1024) / 103) + " kB";
	  if (n < 1024 * 1024 * 1024)
		return (n / (1024 * 1024))
		  + "."
		  + ((n % (1024 * 1024)) / (103 * 1024))
		  + " MB";
	  if (n < 1024l * 1024l * 1024l * 1024l)
		return (n / (1024l * 1024l * 1024l))
		  + "."
		  + ((n % (1024l * 1024l * 1024l)) / (103l * 1024l * 1024l))
		  + " GB";
	  return "A lot !!!";
	}
}
