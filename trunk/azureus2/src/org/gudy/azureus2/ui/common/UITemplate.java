/*
 * UITemplate.java
 *
 * Created on 26. Oktober 2003, 22:40
 */

package org.gudy.azureus2.ui.common;

/**
 *
 * @author  tobi
 */
public abstract class UITemplate implements org.gudy.azureus2.core3.internat.ILocaleUtilChooser, IUserInterface {
  
  private boolean started = false;
  /** Creates a new instance of UITemplate */
  public UITemplate() {
  }
  
  public void init(boolean first, boolean others) {
    if (first)
      org.gudy.azureus2.core3.internat.LocaleUtil.setLocaleUtilChooser(this);
  }
  
  abstract public void openTorrent(String fileName);
  
  abstract public String[] processArgs(String[] args);
  
  public void startUI() {
    started = true;
  }
  
  abstract public org.gudy.azureus2.core3.internat.LocaleUtil getProperLocaleUtil(Object lastEncoding);
  
  public boolean isStarted() {
    return started;
  }
  
}
