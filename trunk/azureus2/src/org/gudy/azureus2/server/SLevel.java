/*
 * SLevel.java
 *
 * Created on 16. August 2003, 23:16
 */

package org.gudy.azureus2.server;

import java.util.logging.Level;
import java.io.Serializable;

/**
 *
 * @author  Tobias Minich
 */
public class SLevel extends Level implements Serializable {
  
  public static final Level TORRENT_RECIEVED = new SLevel("TORRENT RECEIVED", 801);
  public static final Level TORRENT_SENT = new SLevel("TORRENT SENT", 802);
  public static final Level THREAD = new SLevel("THREAD", 803);
  public static final Level HTTP = new SLevel("HTTP", 804);
  
  SLevel(String a, int b) {super(a,b);}
}
