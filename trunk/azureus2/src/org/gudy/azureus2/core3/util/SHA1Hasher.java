/*
 * File    : TRTrackerServerFactory.java
 * Created : 5 Oct. 2003
 * By      : Parg 
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
//	  System.out.println(result);
    
	return result;
  }
}