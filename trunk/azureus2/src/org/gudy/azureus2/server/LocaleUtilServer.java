/* Written and copyright 2001-2003 Tobias Minich.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 *
 *
 * LocaleUtilServer.java
 *
 * Created on 29. August 2003, 20:57
 */

package org.gudy.azureus2.server;

import java.io.UnsupportedEncodingException;

import org.gudy.azureus2.core3.internat.ILocaleUtilChooser;
import org.gudy.azureus2.core3.internat.LocaleUtil;

/**
 *
 * @author  tobi
 */
public class LocaleUtilServer extends LocaleUtil implements ILocaleUtilChooser {
  
  /** Creates a new instance of LocaleUtilServer */
  public LocaleUtilServer() {
    super();
  }
  
  public LocaleUtilServer(Object lastEncoding) {
    super(lastEncoding);
  }
  
  public LocaleUtil getProperLocaleUtil(Object lastEncoding) {
    return new LocaleUtilServer(lastEncoding);
  }
  
  public String getChoosableCharsetString(byte[] array) throws UnsupportedEncodingException {
    return new String(array);
  }
  
}
