/*
 * Created on 3 juil. 2003
 *
 */
package org.gudy.azureus2.core3.unused;

import java.io.ByteArrayOutputStream;
import java.io.File;

import org.gudy.azureus2.core.DiskManager;
import org.gudy.azureus2.core3.torrent.*;

/**
 * @author Olivier
 * 
 */
public class FileChecker {

  public static void main(String args[]) {
    if (args.length < 2){
    	
      	usage();
    }
    
    ByteArrayOutputStream metaInfo = new ByteArrayOutputStream();
 
    try {
   		TOTorrent	torrent = TOTorrentFactory.deserialiseFromBEncodedFile( new File(args[0]));
   		
      DiskManager diskManager = new DiskManager(torrent, args[1]);
      
      while ( true ){
      	
      	int	state = diskManager.getState();
      	
      	if ( state == DiskManager.READY ){
      		
      		break;
      		
      	}else if ( state == DiskManager.FAULTY ){
      		
      		System.out.println( "DiskManager reports FAULTY state ( " + diskManager.getErrorMessage() + ")");
      		
      		System.exit(1);
      	}
      	
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
    }
  }

  private static void usage() {
    System.out.println("FileChecker torrent_file save_directory");
    System.exit(0);
  }
}
