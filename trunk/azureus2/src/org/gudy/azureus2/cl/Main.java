/*
 * Created on 11 juil. 2003
 *
 */
package org.gudy.azureus2.cl;

import org.gudy.azureus2.core.ConfigurationManager;
import org.gudy.azureus2.core.DownloadManager;

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
    DownloadManager manager = new DownloadManager(null, torrentFile, path);
    final boolean initStoppedDownloads = false;
    while (true) {
      manager.startDownloadInitialized(initStoppedDownloads);

      StringBuffer buf = new StringBuffer();
      int state = manager.getState();
      switch (state) {
        case DownloadManager.STATE_WAITING :
          buf.append("Waiting");
          break;
        case DownloadManager.STATE_ALLOCATING :
          buf.append("Allocating");
          break;
        case DownloadManager.STATE_CHECKING :
          buf.append("Checking");
          break;
        case DownloadManager.STATE_READY :
          buf.append("Ready");
          break;
        case DownloadManager.STATE_DOWNLOADING :
          buf.append("Downloading");
          break;
        case DownloadManager.STATE_SEEDING :
          buf.append("Seeding");
          break;
        case DownloadManager.STATE_STOPPED :
          buf.append("Stopped");
          break;
        case DownloadManager.STATE_ERROR :
          buf.append("Error : " + manager.getErrorDetails());
          break;
      }
      
      buf.append(" C:");
      int completed = manager.getCompleted();
      buf.append(completed/10);
      buf.append(".");
      buf.append(completed%10);
      buf.append("%");
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
       buf.append(" ");
      }
      System.out.print("\r" + buf.toString());
      if(state == DownloadManager.STATE_ERROR)
        return;
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
        if (param.equals("--maxUploads"))
          config.setParameter("Max Uploads", Integer.parseInt(value));
        else if (param.equals("--maxSpeed"))
          config.setParameter("Max Upload Speed", Integer.parseInt(value));
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
    System.out.println("Usage : java org.gudy.azureus2.cl.Main [parameters] \"file.torrent\" \"save path\"");
    System.out.println("--maxUploads :\t\t Max number of simultaneous uploads");
    System.out.println("--maxSpeed :\t\t Max upload speed in bytes/sec");
    System.exit(0);
  }
}
