/*
 * LocaleUtilServer.java
 *
 * Created on 29. August 2003, 20:57
 */

package org.gudy.azureus2.server;

import java.io.UnsupportedEncodingException;

import org.gudy.azureus2.core.ILocaleUtilChooser;
import org.gudy.azureus2.core.LocaleUtil;

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
