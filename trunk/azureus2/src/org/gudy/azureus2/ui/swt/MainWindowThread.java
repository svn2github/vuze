/*
 * MainWindowThread.java
 *
 * Created on 10. Oktober 2003, 02:08
 */

package org.gudy.azureus2.ui.swt;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.ui.common.UIConst;

/**
 *
 * @author  tobi
 */
public class MainWindowThread extends Thread {
  
  public MainWindow mainWindow;
  
  /** Creates a new instance of MainWindowThread */
  public MainWindowThread() {
    super("Azureus Main Window");
    start();
  }
  
  public void showMainWindow() {
    if(mainWindow != null) {
      mainWindow.getDisplay().asyncExec(new Runnable() {
        public void run() {
          if (!COConfigurationManager.getBooleanParameter("Password enabled",false) || mainWindow.isVisible())          
            mainWindow.setVisible(true);
          else
            PasswordWindow.showPasswordWindow(MainWindow.getWindow().getDisplay());
        }
      });
    }
  }

  public void openTorrent(String fileName) {
    if (mainWindow!=null) {
      showMainWindow();
      mainWindow.openTorrent(fileName);
    }
  }
  
  public void run() {
    mainWindow = new MainWindow(UIConst.GM, null);
    mainWindow.waitForClose();
    UIConst.shutdown();
  }
}
