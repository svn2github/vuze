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
	extends		AERunnable
	implements 	Comparable
{
	protected Timer					timer;
	protected long					created;
	protected long					when;
	protected TimerEventPerformer	performer;
	
	protected boolean		cancelled;
	protected boolean		has_run;
	
	protected long			unique_id	= 1;
	
	protected
	TimerEvent(
		Timer					_timer,
		long					_unique_id,
		long					_created,
		long					_when,
		TimerEventPerformer		_performer )
	{
		timer		= _timer;
		unique_id	= _unique_id;
		when		= _when;
		performer	= _performer;
		
		created 	= _created;
	}
		
	public long
	getCreatedTime()
	{
		return( created );
	}
	
	public long
	getWhen()
	{
		return( when );
	}
	
	protected AERunnable
	getRunnable()
	{
		return( this );
	}
	
	public void
	runSupport()
	{
		performer.perform( this );
	}
	
	public synchronized void
	cancel()
	{
		cancelled	= true;
		
		timer.cancelEvent( this );
	}
	
	public synchronized boolean
	isCancelled()
	{
		return( cancelled );
	}
	
	protected void
	setHasRun()
	{
		has_run	= true;
	}
	
	public boolean
	hasRun()
	{
		return( has_run );
	}
	
	protected long
	getUniqueId()
	{
		return( unique_id );
	}
	
	public int
	compareTo(
		Object		other )
	{
		long	res =  when - ((TimerEvent)other).getWhen();

		if ( res == 0 ){
			
			return((int)( unique_id - ((TimerEvent)other).getUniqueId()));
			
		}else{
			
			return((int)res);
		}
	}
}
