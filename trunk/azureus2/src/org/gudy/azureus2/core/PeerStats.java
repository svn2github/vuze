package org.gudy.azureus2.core;

/**
 * Provides Statistic upon a peer.
 * It uses Average to compute its different averages. 
 * 
 * @author Olivier
 *
 */
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
    return format(receptionSpeed.getAverage()) + "/s";
  }

  public int getReception() {
    return chokingReceptionSpeed.getAverage();
  }

  public String getSendingSpeed() {
    return format(sendingSpeed.getAverage()) + "/s";
  }
  
  public int getDownloadSpeedRaw() {
    return receptionSpeed.getAverage();
  }
  
  public int getuploadSpeedRaw() {
      return sendingSpeed.getAverage();
    }

  public String getOverAllDownloadSpeed() {
    return format(overallSpeed.getAverage()) + "/s";
  }

  public String getTotalSent() {
    return format(totalSent);
  }

  public String getTotalReceived() {
    return format(totalReceived);
  }
  
  public String getReallyReceived() {
    if(totalDiscarded == 0)
      return format(totalReceived);
    else {
      return format(totalReceived) + " ( " + format(totalDiscarded) + " " + MessageText.getString("discarded") + ")"; 
    }
  }
  
  public String getTotalDiscarded() {
    return format(totalDiscarded);
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
    return format(statisticSentSpeed.getAverage()) + "/s";
  }

  public int getStatisticSentRaw() {
    return statisticSentSpeed.getAverage();
  }

  public static String format(int n) {
    if (n < 1024)
      return n + " B";
    if (n < 1024 * 1024)
      return (n / 1024) + "." + ((n % 1024) / 103) + " kB";
    if (n < 1024 * 1024 * 1024)
      return (n / (1024 * 1024))
        + "."
        + ((n % (1024 * 1024)) / (103 * 1024))
        + " MB";
    if (n < 1024 * 1024 * 1024 * 1024)
      return (n / (1024 * 1024 * 1024))
        + "."
        + ((n % (1024 * 1024 * 1024)) / (103 * 1024 * 1024))
        + " GB";
    return "A lot";
  }

  public static String format(long n) {
    if (n < 1024)
      return n + " B";
    if (n < 1024 * 1024)
      return (n / 1024) + "." + ((n % 1024) / 103) + " kB";
    if (n < 1024 * 1024 * 1024)
      return (n / (1024 * 1024))
        + "."
        + ((n % (1024 * 1024)) / (103 * 1024))
        + " MB";
    if (n < 1024l * 1024l * 1024l * 1024l)
      return (n / (1024l * 1024l * 1024l))
        + "."
        + ((n % (1024l * 1024l * 1024l)) / (103l * 1024l * 1024l))
        + " GB";
    return "A lot !!!";
  }
}