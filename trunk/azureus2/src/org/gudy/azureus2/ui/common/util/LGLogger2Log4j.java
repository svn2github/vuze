/*
 * Created on 10.11.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.gudy.azureus2.ui.common.util;

import org.apache.log4j.Logger;
import org.gudy.azureus2.core3.logging.ILoggerListener;

/**
 * @author tobi
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class LGLogger2Log4j implements ILoggerListener {
  
    public static Logger core = Logger.getLogger("azureus2.core");
    private static LGLogger2Log4j inst = null; 

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.core3.logging.ILoggerListener#log(int, int, int, java.lang.String)
	 */
	public void log(int componentId, int event, int color, String text) {
      if (event == org.gudy.azureus2.core3.logging.LGLogger.ERROR)
        core.error(text);
      else if (event == org.gudy.azureus2.core3.logging.LGLogger.RECEIVED)
        core.log(SLevel.TORRENT_RECEIVED, text);
      else if (event == org.gudy.azureus2.core3.logging.LGLogger.SENT)
        core.log(SLevel.TORRENT_SENT, text);
      else {
        if (color==0)
          core.info(text);
        else
          core.log(SLevel.CORE_INFO, text);
      }
	}
  
    public static LGLogger2Log4j getInstance() {
      if (inst == null)
        inst = new LGLogger2Log4j();
      return inst;
    }
    
    public static void set() {
      org.gudy.azureus2.core3.logging.LGLogger.setListener(getInstance());
    }

}
