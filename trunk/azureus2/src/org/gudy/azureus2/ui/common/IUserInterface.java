/*
 * IUserInterface.java
 *
 * Created on 9. Oktober 2003, 00:07
 */

package org.gudy.azureus2.ui.common;

import org.gudy.azureus2.core.GlobalManager;

/**
 *
 * @author  Tobias Minich
 */
public interface IUserInterface {
  
  /** Initializes the UI. 
   * The UI should not be started at this stage.
   *
   * @param first This UI Instance is the first on the command line and should take control of singular stuff (LocaleUtil and torrents added via Command Line).
   * @param others Indicates wether other UIs run along.
   */
  public void init(boolean first, boolean others);
  /** Process UI specific command line arguments.
   * @return Unprocessed Args
   */
  public String[] processArgs(String[] args);
  /** Start the UI.
   * Now the GlobalManager is initialized.
   */
  public void startUI();
  /** Open a torrent file.
   * This is for torrents passed in the command line. Only called for the first UI.
   */
  public void openTorrent(final String fileName);
  
}
