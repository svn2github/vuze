/*
 * Created on 22 juil. 2003
 *
 */
package org.gudy.azureus2.core3.util;

import java.util.Arrays;

/**
 * @author Olivier
 * 
 */
public class HashWrapper {
  
  private byte[] 	hash;
  private int		hash_code;
  
  public HashWrapper(byte[] _hash) 
  {
  	this(_hash,0,_hash.length);
  }
  
  public HashWrapper(byte[] _hash, int offset,int length) 
  {
	 hash = new byte[length];
	 
	 System.arraycopy(_hash,offset,hash,0,length);

	 for (int i = 0; i < length; i++) {
	   
	 	hash_code = 31*hash_code + hash[i];
	 }
   }
  
  public boolean equals(Object o) {
    if(! (o instanceof HashWrapper))
      return false;
    
    byte[] otherHash = ((HashWrapper)o).getHash();
	return Arrays.equals(hash, otherHash);	
  }
  
  public byte[] 
  getHash() 
  {
    return( hash );
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() 
  {
  	return( hash_code );
  }
}
