/*
 * File    : IpFilter.java
 * Created : 1 oct. 2003 12:27:26
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
 
package org.gudy.azureus2.core3.ipfilter;



/**
 * @author Olivier
 * 
 */

import java.util.List;
import java.io.File;

import org.gudy.azureus2.core3.ipfilter.impl.*;

public abstract class 
IpFilter 
{
	public static IpFilter
	getInstance()
	{
		return( IpFilterImpl.getInstance());
	}
	
	public abstract File getFile();
	
	public abstract void save() throws Exception;
	
	public abstract void 
	reload()
	
		throws Exception;
	
	/**
	 * deprecated and to be removed after 2.0.8.0. Left in to support old SafePeer plugin
	 * version that uses this stuff directly... 
	 * @deprecated
	 * @return
	 */
	public abstract List
	getIpRanges();
	
	public abstract IpRange[] getRanges();

	public abstract boolean isInRange(String ipAddress);
  
  public abstract boolean isInRange(String ipAddress, String torrent_name);
	
	public abstract IpRange
	createRange(boolean sessionOnly);
	
	public abstract void
	addRange(
		IpRange	range );
	
	public abstract void
	removeRange(
		IpRange	range );
	
	public abstract int getNbRanges();
	
	public abstract int getNbIpsBlocked();
	
	public abstract BlockedIp[] getBlockedIps();
	
	public abstract void clearBlockedIPs();
	
	public abstract void ban(String ipAddress);
	
	public abstract boolean
	isEnabled();

	public abstract void
	setEnabled(
		boolean	enabled );
	
	public abstract boolean
	getInRangeAddressesAreAllowed();
	
	public abstract void
	setInRangeAddressesAreAllowed(
		boolean	b );

	public abstract void
	markAsUpToDate();
	
	public abstract long
	getLastUpdateTime();
}
