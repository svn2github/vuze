/*
 * File    : PEPeerConnection.java
 * Created : 15-Oct-2003
 * By      : Olivier
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
 
 /*
 * Created on 4 juil. 2003
 *
 */
package org.gudy.azureus2.core3.peer;

import java.util.Vector;


/**
 * @author Olivier
 * 
 */
public interface 
PEPeerConnection 
	extends PEPeer 
{
  public boolean[] getAvailable();
 
  
  public boolean isChoked();

 
  public boolean isChoking();

  public boolean isInterested();

  
  public boolean isInteresting();

  public Vector getRequested();


  public Vector getRequesting();

 
  public boolean isSeed();

 
  public boolean isSnubbed();
 
  public PEPeerStats getStats();
 
  public void setChoked(boolean b);

  public void setChoking(boolean b);

 
  public void setInterested(boolean b);

  
  public void setInteresting(boolean b);


  public void setSeed(boolean b);

  public void setSnubbed(boolean b);

}
