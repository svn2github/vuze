package org.gudy.azureus2.core3.unused;

import java.nio.ByteBuffer;
/**
 * Simple class, used to queue packets to be send.
 * 
 * A packet to be send has a type and some data (the ByteBuffer to be send)
 * Type is either INFORMATION for all protocol packets
 * or DATA, for pieces.
 * 
 * @author Olivier
 *
 */

public class SendPacket
{
  public static final int INFORMATION = 1;
  public static final int DATA        = 2;
  
  private int type;
  private ByteBuffer data; 
  
  public SendPacket(int type,ByteBuffer data)
  {
    this.type = type;
    this.data = data;  
  }
  
  public int getType()
  {
    return this.type;
  }
  
  public ByteBuffer getData()
  {
    return this.data;
  }
  
}