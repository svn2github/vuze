// XXX [TuxPaper] I need to comment this
// @deprecated Use {link DownloadManagerImpl.getDownload(DownloadManager)}
// XXX DeleteMe 

package org.gudy.azureus2.pluginsimpl.local.download;

import org.gudy.azureus2.plugins.download.*;

public class PluginDownloadGrabber {
  public static Download getDownload(org.gudy.azureus2.core3.download.DownloadManager coreDM) 
  {
    try {
      org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl pluginDM = 
        org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl.singleton;
      if (pluginDM != null)
        return pluginDM.getDownload(coreDM);
      return null;
    } catch (Throwable e) { return null; }
  }
}