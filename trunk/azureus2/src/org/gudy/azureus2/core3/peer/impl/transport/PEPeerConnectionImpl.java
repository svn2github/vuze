/*
 * File    : PEPeerConnectionImpl.java
 * Created : 16-Oct-2003
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

package org.gudy.azureus2.core3.peer.impl.transport;

/**
 * @author parg
 *
 */

import java.util.Arrays;
import java.util.Vector;

import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.peer.impl.PEPeerStatsImpl;
import org.gudy.azureus2.core3.peer.impl.PEPeerControl;

public class 
PEPeerConnectionImpl
	extends PEPeerImpl
{
	protected PEPeerStatsImpl stats;

	protected boolean choked;
	protected boolean interested;
	protected Vector requested;

	protected boolean choking;
	protected boolean interesting;
	protected Vector requesting;

	protected boolean snubbed;

	protected boolean[] available;
	protected boolean seed;

	/**
	 * @param manager
	 * @param id
	 * @param ip
	 * @param port
	 */
	public PEPeerConnectionImpl(PEPeerControl manager, byte[] id, String ip, int port) {
	  super(manager, id, ip, port);
	}

	protected void allocateAll() {
	  seed = false;

	  choked = true;
	  interested = false;
	  requested = new Vector();

	  choking = true;
	  interesting = false;
	  requesting = new Vector();

	  available = new boolean[manager.getPiecesNumber()];
	  Arrays.fill(available, false);

	  stats = (PEPeerStatsImpl)manager.createPeerStats();
	}


	/**
	 * @return
	 */
	public boolean[] getAvailable() {
	  return available;
	}

	/**
	 * @return
	 */
	public boolean isChoked() {
	  return choked;
	}

	/**
	 * @return
	 */
	public boolean isChoking() {
	  return choking;
	}

	/**
	 * @return
	 */
	public boolean isInterested() {
	  return interested;
	}

	/**
	 * @return
	 */
	public boolean isInteresting() {
	  return interesting;
	}

	/**
	 * @return
	 */
	public Vector getRequested() {
	  return requested;
	}

	/**
	 * @return
	 */
	public Vector getRequesting() {
	  return requesting;
	}

	/**
	 * @return
	 */
	public boolean isSeed() {
	  return seed;
	}

	/**
	 * @return
	 */
	public boolean isSnubbed() {
	  return snubbed;
	}

	/**
	 * @return
	 */
	public PEPeerStats getStats() {
	  return stats;
	}

	/**
	 * @param b
	 */
	public void setChoked(boolean b) {
	  choked = b;
	}

	/**
	 * @param b
	 */
	public void setChoking(boolean b) {
	  choking = b;
	}

	/**
	 * @param b
	 */
	public void setInterested(boolean b) {
	  interested = b;
	}

	/**
	 * @param b
	 */
	public void setInteresting(boolean b) {
	  interesting = b;
	}

	/**
	 * @param b
	 */
	public void setSeed(boolean b) {
	  seed = b;
	}

	/**
	 * @param b
	 */
	public void setSnubbed(boolean b) {
	  snubbed = b;
	}
}
