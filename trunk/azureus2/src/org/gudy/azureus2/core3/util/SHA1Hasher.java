/*
 * SHA1Hasher.java
 *
 * Created on June 4, 2003, 10:19 PM
 */

package org.gudy.azureus2.core3.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.jmule.util.SHA1;

/**
 * A SHA-1 hasher used to check pieces.
 *
 * @author  TdC_VgA
 */
public final class SHA1Hasher {
  private MessageDigest _messageDigest = null;
  private SHA1 sha1;
  private ByteBuffer resultBuffer;
  
  /** Creates a new instance of SHA1Hasher */
  public SHA1Hasher() throws NoSuchAlgorithmException {
    _messageDigest = MessageDigest.getInstance("SHA-1");
    sha1 = new SHA1();
    resultBuffer = ByteBuffer.allocate(20);
    resultBuffer.order(ByteOrder.BIG_ENDIAN);
  }

  public byte[] calculateHash(byte[] bytes) {
    return _messageDigest.digest(bytes);
  }

  public byte[] calculateHash(ByteBuffer buffer) {
    //Temp stores the current buffer's position
    int position = buffer.position();
    sha1.update(buffer);
    //Restores the buffer's position
    buffer.position(position);
    
    resultBuffer.position(0);
    sha1.finalDigest(resultBuffer);
    
    byte[] result = new byte[20];
    resultBuffer.position(0);
    resultBuffer.get(result);
//    System.out.println(result);
    
    return result;
  }
  
  public void
  update(
  	byte[]		data )
  {
  	update( data, 0, data.length );
  }
  
  public void
  update(
  	byte[]		data,
	int			pos,
	int			len )
  {
  	sha1.update( ByteBuffer.wrap( data, pos, len ));
  }
  
  public byte[]
  getDigest()
  {
  	resultBuffer.position(0);
  	sha1.finalDigest(resultBuffer);
  	
  	byte[] result = new byte[20];
  	resultBuffer.position(0);
  	resultBuffer.get(result);
//    System.out.println(result);
  	
  	return result;	
  }
}
