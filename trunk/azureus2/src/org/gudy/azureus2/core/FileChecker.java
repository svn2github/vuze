/*
 * Created on 3 juil. 2003
 *
 */
package org.gudy.azureus2.core;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.util.Map;

/**
 * @author Olivier
 * 
 */
public class FileChecker {

  public static void main(String args[]) {
    if (args.length < 2)
      usage();
    ByteArrayOutputStream metaInfo = new ByteArrayOutputStream();
    FileInputStream fis = null;
    try {
      byte[] buf = new byte[1024];
      int nbRead;
      fis = new FileInputStream(args[0]);
      while ((nbRead = fis.read(buf)) > 0)
        metaInfo.write(buf, 0, nbRead);
      Map metaData = BDecoder.decode(metaInfo.toByteArray());
      DiskManager diskManager = new DiskManager(metaData, args[1]);
      while (diskManager.getState() != DiskManager.READY) {
        int percent = diskManager.getPercentDone();
        int percentOk = 0;
        if (diskManager.getTotalLength() != 0)
          percentOk = (int) ((1000 * (diskManager.getTotalLength() - diskManager.getRemaining())) / diskManager.getTotalLength());
        System.out.print("\rScanned : " + (percent / 10) + "." + (percent % 10) + " %,  ok : " + (percentOk / 10) + "." + (percentOk % 10) + " %   ");
        Thread.sleep(100);
      }
      System.out.println("");
      String[][] filesStatus = diskManager.getFilesStatus();
      for (int i = 0; i < filesStatus.length; i++) {
        System.out.println(filesStatus[i][0] + " : " + filesStatus[i][1]);
      }
      diskManager.stopIt();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (fis != null)
          fis.close();
      } catch (Exception e) {
      }
    }
  }

  private static void usage() {
    System.out.println("FileChecker torrent_file save_directory");
    System.exit(0);
  }
}
