/*
 * Created on 22 juil. 2003
 *
 */
package org.gudy.azureus2.core;

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
  
  public byte[] getHash() {
    return this.hash;
  }
  
  public boolean equals(Object o) {
    if(! (o instanceof Hash))
      return false;
    byte[] otherHash = ((Hash)o).getHash();
    for(int i = 0 ; i < otherHash.length ; i++) {
      if(otherHash[i] != this.hash[i])
        return false;
    }
    return true;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    String str = "";
    try {    
      str = new String(hash,"ISO-8859-1");
    } catch (Exception e) {
      e.printStackTrace();
    }
    return str.hashCode();
  }

}
