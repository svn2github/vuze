/*
 * Created on 22 juil. 2003
 *
 */
package org.gudy.azureus2.core;

import java.util.Arrays;

/**
 * @author Olivier
 * 
 */
public class Hash {
  
  private byte[] hash;
  
  public Hash(byte[] hash) {
    this.hash = new byte[hash.length];
    System.arraycopy(hash,0,this.hash,0,this.hash.length);
  }
  
  public boolean equals(Object o) {
    if(! (o instanceof Hash))
      return false;
    
    byte[] otherHash = ((Hash)o).getHash();
	return Arrays.equals(hash, otherHash);	
  }
  
  public byte[] getHash() {
    return this.hash;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    String str = null;
    try {    
      str = new String(hash);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return str.hashCode();
  }

}
