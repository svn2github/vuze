package org.gudy.azureus2.core;

/**
 * 
 * This class represents a Bittorrent Request.
 * and a time stamp to know when it was created.
 * 
 * Request may expire after some time, which is used to determine who is snubbed.
 * 
 * @author Olivier
 *
 * 
 */
public class Request
{
  //60 secs of expiration for any request.
  private static final int EXPIRATION_TIME = 1000 * 60;
  
  private int pieceNumber;
  private int offset;
  private int length;
  private long timeCreated;
  
  /**
   * Parameters correspond to bittorrent parameters
   * @param pieceNumber
   * @param offset
   * @param length
   */
  public Request(int pieceNumber,int offset,int length)
  {
    this.pieceNumber = pieceNumber;
    this.offset = offset;
    this.length = length;
    timeCreated = System.currentTimeMillis();
  }
  
  /**
   * Method to determine if a Request has expired
   * @return true is the request is expired
   */
  public boolean isExpired()
  {
    return ((System.currentTimeMillis() - timeCreated) > EXPIRATION_TIME);    
  }
  
  /**
   * Allow some more time to the request.
   * Typically used on peers that have just sent some data, we reset all
   * other requests to give them extra time.
   */
  public void reSetTime()
  {
      timeCreated = System.currentTimeMillis();
  }
  
  //Getters  
  public int getPieceNumber()
  {
    return this.pieceNumber;
  }
  
  public int getOffset()
  {
    return this.offset;
  }
  
  public int getLength()
  {
    return this.length;
  }  
  
  /**
   * We override the equals method
   * 2 requests are equals if
   * all their bt fields (piece number, offset, length) are equal
   */
  public boolean equals(Object o)
  {
    if(! (o instanceof Request))
      return false;    
    Request otherRequest = (Request) o;
    if(otherRequest.pieceNumber != this.pieceNumber)
      return false;
    if(otherRequest.offset != this.offset)
      return false;
    if(otherRequest.length != this.length)
      return false;
      
    return true;
  }
}