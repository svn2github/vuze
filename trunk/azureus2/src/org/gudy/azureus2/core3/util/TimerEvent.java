/*
 * File    : TimerEvent.java
 * Created : 21-Nov-2003
 * By      : parg
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

package org.gudy.azureus2.core3.util;

/**
 * @author parg
 *
 */

public class 
TimerEvent
	implements Comparable
{
	protected Timer			timer;
	protected long			when;
	protected Runnable		runnable;
	
	protected
	TimerEvent(
		Timer			_timer,
		long			_when,
		Runnable		_runnable )
	{
		timer		= _timer;
		when		= _when;
		runnable	= _runnable;
	}
		
	public long
	getWhen()
	{
		return( when );
	}
	
	public Runnable
	getRunnable()
	{
		return( runnable );
	}
	
	public void
	cancel()
	{
		timer.cancelEvent( this );
	}
	
	public int
	compareTo(
		Object		other )
	{
		return((int)( when - ((TimerEvent)other).getWhen()));
	}
}
