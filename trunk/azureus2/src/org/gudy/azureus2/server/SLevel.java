/*
 * SLevel.java
 *
 * Created on 16. August 2003, 23:16
 */

package org.gudy.azureus2.server;

import org.apache.log4j.Level;
/**
 *
 * @author  Tobias Minich
 */
public class SLevel extends Level {
  
  public static final Level TORRENT_RECIEVED = new SLevel(15001, "TORRENT RECEIVED", 6);
  public static final Level TORRENT_SENT = new SLevel(15000, "TORRENT SENT", 6);
  public static final Level THREAD = new SLevel(10001, "THREAD", 6);
  public static final Level HTTP = new SLevel(12000, "HTTP", 6);
  
  SLevel(int c, String a, int b) {super(c,a,b);}
}
