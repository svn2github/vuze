/*
 * Created on 8 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import org.gudy.azureus2.core3.internat.ILocaleUtilChooser;
import org.gudy.azureus2.core3.internat.LocaleUtil;
import org.gudy.azureus2.ui.common.IUserInterface;

/**
 * @author Olivier
 * 
 */
public class UI implements ILocaleUtilChooser,IUserInterface {  
  
  MainWindowThread mainWindow = null;
  
  public UI() {
  }
  
  public LocaleUtil getProperLocaleUtil(Object lastEncoding) {
    return new LocaleUtilSWT(lastEncoding);
  }
  
  public void init(boolean first, boolean others) {
    if (first)
      LocaleUtil.setLocaleUtilChooser(this);
  }
  
  public void openTorrent(String fileName) {
    if (mainWindow!=null) {
      mainWindow.openTorrent(fileName);
    }
  }
  
  public String[] processArgs(String[] args) {
    return args;
  }
  
  public void startUI() {
    mainWindow = new MainWindowThread();
  }
  
}
