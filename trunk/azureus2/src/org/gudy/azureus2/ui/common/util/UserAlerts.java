/*
 * Created on 28.11.2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.ui.common.util;

import java.applet.Applet;
import java.applet.AudioClip;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.ui.swt.config.ParameterListener;

/**
 * Contains methods to alert the user of certain events.
 * @author Rene Leonhardt
 */
public class UserAlerts implements ParameterListener {
  private static final UserAlerts parameterListener = new UserAlerts();

  private static AudioClip audioDownloadFinished = null;

  /**
   * 
   */
  private UserAlerts() {
    ConfigurationManager.getInstance().addParameterListener("Play Download Finished", this);
    initialize();
  }

  private void initialize() {
    if(COConfigurationManager.getBooleanParameter("Play Download Finished", true))
      activatePlayDownloadFinished();
  }

  /**
   * Sets the Download Finished sound, if not already set 
   *
   * @author Rene Leonhardt
   */
  private static void activatePlayDownloadFinished() {
    if(null == audioDownloadFinished) {
      try {
        audioDownloadFinished = Applet.newAudioClip(ClassLoader.getSystemResource("org/gudy/azureus2/ui/icons/downloadFinished.wav"));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
  
  /**
   * Plays the Download Finished sound, if it is available.
   * Could alert user by mail for example, too.
   *
   * @author Rene Leonhardt
   */
  public static void downloadFinished() {
    if(null != audioDownloadFinished) {
      try {
        audioDownloadFinished.play();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * @param parameterName the name of the parameter that has changed
   * @see org.gudy.azureus2.ui.swt.config.ParameterListener#parameterChanged(java.lang.String)
   */
  public void parameterChanged(String parameterName) {
    if(COConfigurationManager.getBooleanParameter("Play Download Finished", true))
      activatePlayDownloadFinished();
    else
      audioDownloadFinished = null;
  }
  
 }
