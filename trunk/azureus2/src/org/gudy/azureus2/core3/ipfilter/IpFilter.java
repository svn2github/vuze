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

import org.gudy.azureus2.core3.ipfilter.impl.*;

public abstract class 
IpFilter 
{
	public static IpFilter
	getInstance()
	{
		return( IpFilterImpl.getInstance());
	}
	
	public abstract void save();
	
	public abstract List getIpRanges();

	public abstract boolean isInRange(String ipAddress);
	
	public abstract IpRange
	createRange();
}
