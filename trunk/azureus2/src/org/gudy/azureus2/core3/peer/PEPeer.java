/*
 * File    : PEPeerSocket.java
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


/**
 * @author Olivier
 *
 */

public interface 
PEPeer 
{
	public final static int CONNECTING 		= 10;
	public final static int HANDSHAKING 	= 20;
	public final static int TRANSFERING 	= 30;
	public final static int DISCONNECTED 	= 40;
	
	public int getState();	// from above set

	public byte[] getId();

	public String getIp();
 
	public int getPort();
	
	public boolean[] getAvailable();
 
	public void setSnubbed(boolean b);	// explicit un-snub
  
	public boolean isChoked();

	public boolean isChoking();

	public boolean isInterested();

	public boolean isInteresting();

	public boolean isSeed();
 
	public boolean isSnubbed();
 
	public PEPeerStats getStats();
 	
	public int getMaxUpload();

	public boolean isIncoming();

	public int getDownloadPriority();

	public int getPercentDone();

	public String getClient();

	public boolean isOptimisticUnchoke();
	
	public void hasSentABadChunk();
	
	public int getNbBadChunks();
}