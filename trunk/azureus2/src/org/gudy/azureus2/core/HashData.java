/*
 * Created on 22 juil. 2003
 *
 */
package org.gudy.azureus2.core;

/**
 * @author Olivier
 * 
 */
public class HashData {
    public int seeds;
    public int peers;

    public HashData(int seeds, int peers) {
      this.seeds = seeds;
      this.peers = peers;
    }

}
