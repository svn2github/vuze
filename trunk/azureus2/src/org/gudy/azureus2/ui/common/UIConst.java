/*
 * Created on 10.11.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.gudy.azureus2.ui.common;

import java.util.Date;
import java.util.HashMap;
import org.gudy.azureus2.core3.global.GlobalManager;

/**
 * @author tobi
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class UIConst {
  public static Date startTime;
  public static HashMap UIS = null;
  public static GlobalManager GM = null;
  
  
  public static void shutdown() {
    Main.shutdown();
  }
  
  public static synchronized boolean startUI(String ui, String[] args) {
    if (UIS.containsKey(ui))
      return false;
    IUserInterface uif = UserInterfaceFactory.getUI(ui);
    uif.init(false, true);
    if (args!=null)
      uif.processArgs(args);
    uif.startUI();
    UIS.put(ui, uif);
    return true;
  }

}
