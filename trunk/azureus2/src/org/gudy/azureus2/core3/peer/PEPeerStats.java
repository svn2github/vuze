package org.gudy.azureus2.core3.peer;

/**
 * Provides Statistic upon a peer.
 * It uses Average to compute its different averages. 
 * 
 * @author Olivier
 *
 */

public interface 
PEPeerStats {  
  public void discarded(int length);
 
  public void received(int length);

  public void sent(int length);
 
  public void haveNewPiece();

  public void staticticSent(int length);
 
  public String getReceptionSpeed();

  public int getReception();

  public String getSendingSpeed();
  
  public int getDownloadSpeedRaw();
  
  public int getuploadSpeedRaw();
 
  public String getOverAllDownloadSpeed();

  public String getTotalSent();

  public String getTotalReceived();
  
  public String getReallyReceived();
 
  
  public String getTotalDiscarded();
 
  public long getTotalSentRaw();
  
  
  public void setTotalSent(long sent);
 
  public long getTotalReceivedRaw();
 
  public void setTotalReceivedRaw(long received);

  
  public long getTotalDiscardedRaw();

  public String getStatisticSent();


  public int getStatisticSentRaw();

}