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

import java.util.*;


import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.peer.impl.PEPeerStatsImpl;
import org.gudy.azureus2.core3.peer.impl.PEPeerControl;
import org.gudy.azureus2.core3.disk.DiskManagerRequest;
import org.gudy.azureus2.core3.logging.*;

public class 
PEPeerConnectionImpl
	extends PEPeerImpl
{
	protected PEPeerStatsImpl stats;

	protected boolean choked;
	protected boolean interested;
	private List requested;

	protected boolean choking;
	protected boolean interesting;

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
	  requested = new ArrayList();

	  choking = true;
	  interesting = false;

	  available = new boolean[manager.getPiecesNumber()];
	  Arrays.fill(available, false);

	  stats = manager.createPeerStats();
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
	
	protected void cancelRequests() {
		if (requested == null)
			return;
		synchronized (requested) {
			for (int i = requested.size() - 1; i >= 0; i--) {
				DiskManagerRequest request = (DiskManagerRequest) requested.remove(i);
				manager.requestCanceled(request);
			}
		}
	}

	public int 
	getNbRequests() {
		return requested.size();
	}

	/**
	 * 
	 * @return	may be null for performance purposes
	 */
	
	public List 
	getExpiredRequests() {
		List result = null;
    synchronized (requested) {
    	for (int i = 0; i < requested.size(); i++) {
    		try {
    			DiskManagerRequest request = (DiskManagerRequest) requested.get(i);
    			if (request.isExpired()) {
    				if ( result == null ){
    					result = new ArrayList();
    				}
    				result.add(request);
    			}
    		}
    		catch (ArrayIndexOutOfBoundsException e) {
    			//Keep going, most probably, piece removed...
    			//Hopefully we'll find it later :p
    		}
    	}
    }
		return result;
	}
	
	protected boolean
	alreadyRequested(
		DiskManagerRequest	request )
	{
    synchronized (requested) {
      return requested.contains( request );
    }
	}
	
	protected void
	addRequest(
		DiskManagerRequest	request )
	{
    synchronized (requested) {
    	requested.add(request);
    }
	}
	
	protected void
	removeRequest(
		DiskManagerRequest	request )
	{
    synchronized (requested) {
    	requested.remove(request);
    }
	}
	
	protected void 
	reSetRequestsTime() {
    synchronized (requested) {
		  for (int i = 0; i < requested.size(); i++) {
		  	DiskManagerRequest request = null;
		  	try {
		  		request = (DiskManagerRequest) requested.get(i);
		  	}
		  	catch (Exception e) {}
        
		  	if (request != null)
		  		request.reSetTime();
		  }
    }
	}
	
}
