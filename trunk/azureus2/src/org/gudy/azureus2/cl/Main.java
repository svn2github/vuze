/*
 * Created on 11 juil. 2003
 *
 */
package org.gudy.azureus2.cl;

import org.gudy.azureus2.core.ConfigurationManager;
import org.gudy.azureus2.core.DownloadManager;
import org.gudy.azureus2.core.GlobalManager;
import org.gudy.azureus2.ui.swt.Messages;

/**
 * @author Olivier
 * 
 */
public class Main {
  public static void main(String args[]) {
    if (!parseParameters(args))
      usage();
      
    String torrentFile = args[args.length - 2];
    String path = args[args.length - 1];
    GlobalManager gm = new GlobalManager();
    DownloadManager manager = new DownloadManager(gm, torrentFile, path);
    while (true) {
      if (manager.getState() == DownloadManager.STATE_WAITING)
        manager.initialize();
      if (manager.getState() == DownloadManager.STATE_READY)
        manager.startDownload();

      StringBuffer buf = new StringBuffer();
      int state = manager.getState();
      switch (state) {
        case DownloadManager.STATE_WAITING :
          buf.append(Messages.getString("Main.download.state.waiting")); //$NON-NLS-1$
          break;
        case DownloadManager.STATE_ALLOCATING :
          buf.append(Messages.getString("Main.download.state.allocating")); //$NON-NLS-1$
          break;
        case DownloadManager.STATE_CHECKING :
          buf.append(Messages.getString("Main.download.state.checking")); //$NON-NLS-1$
          break;
        case DownloadManager.STATE_READY :
          buf.append(Messages.getString("Main.download.state.ready")); //$NON-NLS-1$
          break;
        case DownloadManager.STATE_DOWNLOADING :
          buf.append(Messages.getString("Main.download.state.downloading")); //$NON-NLS-1$
          break;
        case DownloadManager.STATE_SEEDING :
          buf.append(Messages.getString("Main.download.state.seeding")); //$NON-NLS-1$
          break;
        case DownloadManager.STATE_STOPPED :
          buf.append(Messages.getString("Main.download.state.stopped")); //$NON-NLS-1$
          break;
        case DownloadManager.STATE_ERROR :
          buf.append(Messages.getString("Main.download.state.error") + " : " + manager.getErrorDetails()); //$NON-NLS-1$
          break;
      }
      
      buf.append(" C:");
      int completed = manager.getCompleted();
      buf.append(completed/10);
      buf.append('.'); //$NON-NLS-1$
      buf.append(completed%10);
      buf.append('%'); //$NON-NLS-1$
      buf.append(" S:");
      buf.append(manager.getNbSeeds());
      buf.append(" P:");
      buf.append(manager.getNbPeers());
      buf.append(" D:");
      buf.append(manager.getDownloaded());
      buf.append(" U:");
      buf.append(manager.getUploaded());
      buf.append(" DS:");
      buf.append(manager.getDownloadSpeed());
      buf.append(" US:");
      buf.append(manager.getUploadSpeed());
      buf.append(" T:");
      buf.append(manager.getTrackerStatus());
      while(buf.length() < 80) {
       buf.append(' '); //$NON-NLS-1$
      }
      System.out.print("\r" + buf.toString()); //$NON-NLS-1$
      
      try {
        Thread.sleep(500);
      } catch (Exception e) {
        //Do nothing
      }
    }
  }

  private static boolean parseParameters(String args[]) {
    if (args.length < 2)
      return false;
    if (args.length == 2)
      return true;
    if ((args.length % 2) != 0)
      return false;
    try {
      ConfigurationManager config = ConfigurationManager.getInstance();
      for (int i = 0; i < args.length - 2; i += 2) {
        String param = args[i];
        String value = args[i + 1];
        if (param.equals("--maxUploads")) //$NON-NLS-1$
          config.setParameter("Max Uploads", Integer.parseInt(value)); //$NON-NLS-1$
        else if (param.equals("--maxSpeed")) //$NON-NLS-1$
          config.setParameter("Max Upload Speed", Integer.parseInt(value)); //$NON-NLS-1$
        else
          return false;
      }
      return true;
    }
    catch (Exception e) {
      return false;
    }
  }

  private static void usage() {
    System.out.println(Messages.getString("Main.parameter.usage")); //$NON-NLS-1$
    System.out.println("--maxUploads :\t\t " + Messages.getString("Main.parameter.maxUploads")); //$NON-NLS-1$ //$NON-NLS-2$
    System.out.println("--maxSpeed :\t\t " + Messages.getString("Main.parameter.maxSpeed")); //$NON-NLS-1$ //$NON-NLS-2$
    System.exit(0);
  }
}
