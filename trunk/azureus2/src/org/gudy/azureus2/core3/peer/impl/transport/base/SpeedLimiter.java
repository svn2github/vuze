/*
 * Created on 9 juin 2003
 *
 */
package org.gudy.azureus2.core3.peer.impl.transport.base;

import java.util.List;
import java.util.Vector;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.peer.PEPeer;

/**
 * 
 * This class defines a singleton used to do speed limitation.
 * 
 * Speed Limitation is at the moment quite simple,
 * but really efficient in the way it won't use more bandwith than allowed.
 * On the other hand, it may not use all the allowed bandwith.
 * 
 * @author Olivier
 *
 */
public class SpeedLimiter {

  //The instance of the speed Limiter
  private static SpeedLimiter limiter;

  //The limit in bytes per second
  private int limit;

  //The List of current uploaders
  private List uploaders;

  // TODO rename the following, more descriptive fields, etc.
  static private class UploaderInfo {
    int maxUpload;
    PEPeer peerSocket;
  }

  static private class Allocation {
    int toBeAllocated;
    int peersToBeAllocated;

    public int getNumAllowed() {
      // what to do when peersToBeAllocated == 0 ?
      // Gudy : shouldn't be accessed, but we'll return toBeAllocated, in case there's an error.
      if(peersToBeAllocated == 0)
          return toBeAllocated;       
      return toBeAllocated / peersToBeAllocated;
    }
  }

  static private final int MAX_NUM_UPLOADERS = 1000;

  private UploaderInfo[] sortedUploadersHighPriority = new UploaderInfo[MAX_NUM_UPLOADERS];
  private UploaderInfo[] sortedUploadersLowPriority = new UploaderInfo[MAX_NUM_UPLOADERS];

  private int i, j, maxUpload, smallestIndex, sortedUploadersHighPriorityIndex, sortedUploadersLowPriorityIndex;

  /**
   * Private constructor for SpeedLimiter
   *
   */
  private SpeedLimiter() {
    //limit = ConfigurationManager.getInstance().getIntParameter("Max Upload Speed", 0);
    uploaders = new Vector();

    for (int i = 0; i < MAX_NUM_UPLOADERS; i++) {
      sortedUploadersHighPriority[i] = new UploaderInfo();
      sortedUploadersLowPriority[i] = new UploaderInfo();
    }
  }

  /**
   * The way to get the singleton
   * @return the SpeedLimiter instance
   */
  public synchronized static SpeedLimiter getLimiter() {
    if (limiter == null)
      limiter = new SpeedLimiter();
    return limiter;
  }

  /**
   * Public method use to change the limit
   * @param limit the upload limit in bytes per second
   */
  public void setLimit(int limit) {
    this.limit = limit;
  }

  /**
   * Uploaders will have to tell the SpeedLimiter that they upload data.
   * In order to achieve this, they call this method that increase the speedLimiter
   * count of uploads.
   *
   */
  public void addUploader(PEPeer wt) {
    synchronized (uploaders) {
      if (!uploaders.contains(wt))
        uploaders.add(wt);
    }
  }

  /**
   * Same as addUploader, but to tell that an upload is ended. 
   */
  public void removeUploader(PEPeer wt) {
    synchronized (uploaders) {
      while (uploaders.contains(wt))
        uploaders.remove(wt);
    }
  }

  /**
   * Method used to know if there is a limitation or not.
   * @return true if speed is limited
   */
  public boolean isLimited(PEPeer wt) {
    limit = COConfigurationManager.getIntParameter("Max Upload Speed", 0);
    return (this.limit != 0 && uploaders.contains(wt));
  }

  /**
   * This method returns the amount of data a thread may upload
   * over a period of 100ms.
   * @return number of bytes allowed for 100 ms
   */
  public synchronized int getLimitPer100ms(PEPeer wt) {

    if (this.uploaders.size() == 0)
      return 0;

    sortedUploadersHighPriorityIndex = 0;
    sortedUploadersLowPriorityIndex = 0;

    assignUploaderInfos();

    sortUploaderInfos(sortedUploadersHighPriority, sortedUploadersHighPriorityIndex);
    sortUploaderInfos(sortedUploadersLowPriority, sortedUploadersLowPriorityIndex);

    //System.out.println(sortedUploadersHighPriority.size() + " : " + sortedUploadersLowPriority.size());
    Allocation allocation = new Allocation();
    allocation.toBeAllocated = this.limit / 10;

    if (!findPeerSocket(sortedUploadersHighPriority, sortedUploadersHighPriorityIndex, allocation, wt)) {
      findPeerSocket(sortedUploadersLowPriority, sortedUploadersLowPriorityIndex, allocation, wt);
    }

    int result = getLimit(wt, allocation);
    //Logger.getLogger().log(0,0,Logger.ERROR,"Allocated for 100ms :" + result);
    return result;
  }

  // refactored out of getLimitPer100ms() - Moti
  private boolean findPeerSocket(UploaderInfo[] uploaderInfos, int numPeers, Allocation allocation, PEPeer wt) {
    allocation.peersToBeAllocated = numPeers;

    for (i = 0; i < numPeers; i++) {
      if (uploaderInfos[i].peerSocket == wt) {
        return true;
      }
      allocation.toBeAllocated -= getLimit(uploaderInfos[i].peerSocket, allocation);
      allocation.peersToBeAllocated--;
    }
    return false;
  }

  // refactored out of getLimitPer100ms() - Moti
  private int getLimit(PEPeer wt, Allocation allocation) {
    maxUpload = wt.getMaxUpload();
    return min(allocation.getNumAllowed(), maxUpload);
  }

  static private int min(int a, int b) {
    return a < b ? a : b;
  }

  // refactored out of getLimitPer100ms() - Moti
  private void sortUploaderInfos(UploaderInfo[] uploaderInfos, int numToSort) {
    for (j = 0; j < numToSort - 1; j++) {
      smallestIndex = j;
      for (i = j + 1; i < numToSort; i++) {
        if (uploaderInfos[i].maxUpload < uploaderInfos[smallestIndex].maxUpload)
          smallestIndex = i;
      }
      if (j != smallestIndex) {
        uploaderInfos[numToSort].maxUpload = uploaderInfos[j].maxUpload;
        uploaderInfos[numToSort].peerSocket = uploaderInfos[j].peerSocket;
        uploaderInfos[j].maxUpload = uploaderInfos[smallestIndex].maxUpload;
        uploaderInfos[j].peerSocket = uploaderInfos[smallestIndex].peerSocket;
        uploaderInfos[smallestIndex].maxUpload = uploaderInfos[numToSort].maxUpload;
        uploaderInfos[smallestIndex].peerSocket = uploaderInfos[numToSort].peerSocket;
      }
    }
  }

  // refactored out of getLimitPer100ms() - Moti
  private void assignUploaderInfos() {
    //We construct a TreeMap to sort all writeThread according to their up speed.
    synchronized (uploaders) {
      for (i = 0; i < uploaders.size(); i++) {
        PEPeer wti = (PEPeer) uploaders.get(i);
        maxUpload = wti.getMaxUpload();
        if (wti.getDownloadPriority() == DownloadManager.HIGH_PRIORITY) {
          assignUploaderInfo(sortedUploadersHighPriority, sortedUploadersHighPriorityIndex, wti);
          sortedUploadersHighPriorityIndex++;
        }
        else {
          assignUploaderInfo(sortedUploadersLowPriority, sortedUploadersLowPriorityIndex, wti);
          sortedUploadersLowPriorityIndex++;
        }
      }
    }
  }

  // refactored out of getLimitPer100ms() - Moti
  private void assignUploaderInfo(UploaderInfo[] uploaderInfos, int index, PEPeer wti) {
    for (j = 0; j < index; j++)
      if (uploaderInfos[j].maxUpload == maxUpload) {
        maxUpload++;
        j = -1;
      }
    uploaderInfos[index].maxUpload = maxUpload;
    uploaderInfos[index].peerSocket = wti;
  }

}
