/*
 * File    : TimerEventPeriodic.java
 * Created : 07-Dec-2003
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
TimerEventPeriodic
	implements TimerEventPerformer
{
	protected Timer					timer;
	protected long					frequency;
	protected TimerEventPerformer	performer;
	
	protected TimerEvent			current_event;
	protected boolean				cancelled;
	
	protected
	TimerEventPeriodic(
		Timer				_timer,
		long				_frequency,
		TimerEventPerformer	_performer )
	{
		timer		= _timer;
		frequency	= _frequency;
		performer	= _performer;
		
		current_event = timer.addEvent( 	SystemTime.getCurrentTime()+ frequency,
											this );
		
	}
	
	public void
	perform(
		TimerEvent	event )
	{
		if ( !cancelled ){
			
			try{
				performer.perform( event );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		
			synchronized( this ){
				
				if ( !cancelled ){
				
					current_event = timer.addEvent( 	SystemTime.getCurrentTime()+ frequency,
														this );
				}
			}
		}
	}
	
	public synchronized void
	cancel()
	{
		if ( current_event != null ){
			
			current_event.cancel();
			
			cancelled	= true;
		}
	}
}
