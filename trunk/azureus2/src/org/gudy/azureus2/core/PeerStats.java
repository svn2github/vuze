package org.gudy.azureus2.core;

/**
 * Provides Statistic upon a peer.
 * It uses Average to compute its different averages. 
 * 
 * @author Olivier
 *
 */

import org.gudy.azureus2.core3.util.*;

public class PeerStats {
//  private final int avgTime = 12;

//  private long timeCreated;

  private int pieceLength;

  private long totalReceived;
  private long totalDiscarded;
  private long totalSent;
  private long totalHave;

  private Average receptionSpeed;
  private Average chokingReceptionSpeed;
  private Average sendingSpeed;
  private Average overallSpeed;
  private Average statisticSentSpeed;



  public PeerStats(int pieceLength) {
//    timeCreated = System.currentTimeMillis() / 100;

    this.pieceLength = pieceLength;

    //average over 10s, update every 2000ms.
    receptionSpeed = Average.getInstance(2000, 10);

    //average over 5s, update every 100ms.
    sendingSpeed = Average.getInstance(1000, 5);

    //average over 20s, update every 1s.
    chokingReceptionSpeed = Average.getInstance(1000, 20);

    //average over 100s, update every 5s
    overallSpeed = Average.getInstance(5000, 100);

    //average over 60s, update every 3s
    statisticSentSpeed = Average.getInstance(3000, 60);

  }
  
  public void discarded(int length) {
    this.totalDiscarded += length;
  }

  public void received(int length) {
    totalReceived += length;
    receptionSpeed.addValue(length);
    chokingReceptionSpeed.addValue(length);
  }

  public void sent(int length) {
    totalSent += length;
    sendingSpeed.addValue(length);
  }

  public void haveNewPiece() {
    totalHave += pieceLength;
    overallSpeed.addValue(pieceLength);
  }

  public void staticticSent(int length) {
    statisticSentSpeed.addValue(length);
  }

  public String getReceptionSpeed() { 
    return DisplayFormatters.formatByteCountToKBEtc(receptionSpeed.getAverage()) + "/s";
  }

  public int getReception() {
    return chokingReceptionSpeed.getAverage();
  }

  public String getSendingSpeed() {
    return DisplayFormatters.formatByteCountToKBEtc(sendingSpeed.getAverage()) + "/s";
  }
  
  public int getDownloadSpeedRaw() {
    return receptionSpeed.getAverage();
  }
  
  public int getuploadSpeedRaw() {
      return sendingSpeed.getAverage();
    }

  public String getOverAllDownloadSpeed() {
    return DisplayFormatters.formatByteCountToKBEtc(overallSpeed.getAverage()) + "/s";
  }

  public String getTotalSent() {
    return DisplayFormatters.formatByteCountToKBEtc(totalSent);
  }

  public String getTotalReceived() {
    return DisplayFormatters.formatByteCountToKBEtc(totalReceived);
  }
  
  public String getReallyReceived() {
    if(totalDiscarded == 0)
      return DisplayFormatters.formatByteCountToKBEtc(totalReceived);
    else {
      return DisplayFormatters.formatByteCountToKBEtc(totalReceived) + " ( " + DisplayFormatters.formatByteCountToKBEtc(totalDiscarded) + " " + MessageText.getString("discarded") + " )"; 
    }
  }
  
  public String getTotalDiscarded() {
    return DisplayFormatters.formatByteCountToKBEtc(totalDiscarded);
  }  

  public long getTotalSentRaw() {
    return totalSent;
  }
  
  public void setTotalSent(long sent) {
     totalSent = sent;
   }

  public long getTotalReceivedRaw() {
    return totalReceived;
  }
  
  public void setTotalReceivedRaw(long received) {
    totalReceived = received;
  }
  
  public long getTotalDiscardedRaw() {
      return totalDiscarded;
  }

  public String getStatisticSent() {
    return DisplayFormatters.formatByteCountToKBEtc(statisticSentSpeed.getAverage()) + "/s";
  }

  public int getStatisticSentRaw() {
    return statisticSentSpeed.getAverage();
  }


}