/*
 * File    : LocaleUtilChooser.java
 * Created : 8 oct. 2003 00:21:44
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
 
package org.gudy.azureus2.cl;

import java.io.UnsupportedEncodingException;

import org.gudy.azureus2.core3.internat.ILocaleUtilChooser;
import org.gudy.azureus2.core3.internat.LocaleUtil;

/**
 * @author Olivier
 * 
 */
public class LocaleUtilCL extends LocaleUtil implements ILocaleUtilChooser {

  /* (non-Javadoc)
   * @see org.gudy.azureus2.core3.internat.ILocaleUtilChooser#getProperLocaleUtil(java.lang.Object)
   */
  public LocaleUtil getProperLocaleUtil(Object lastEncoding) {
    return this;
  }
  
  public String getChoosableCharsetString(byte[] array) throws UnsupportedEncodingException {
    return "ISO-8859-1";
 }

}
