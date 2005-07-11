/*
 * File    : Logger.java
 * Created : 28-Dec-2003
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

package org.gudy.azureus2.plugins.logging;

import org.gudy.azureus2.plugins.PluginInterface;

/**
 * @author parg
 *
 */

public interface 
Logger 
{
	public LoggerChannel
	getChannel(
		String		name );
	
	public LoggerChannel
	getTimeStampedChannel(
		String		name );
	
		/**
		 * Returns a logger channel that doesn't output to the standard AZ log. Add listeners
		 * to it if output needs to be routed somewhere
		 * @param name
		 * @return
		 */
	
	public LoggerChannel
	getNullChannel(
		String		name );
	
	public LoggerChannel[]
	getChannels();
	
	public PluginInterface
	getPluginInterface();

		/**
		 * these methods give access to all alerts raised, not channel specific ones
		 * @param listener
		 */
	
	public void
	addAlertListener(
		LoggerAlertListener		listener );
	
	public void
	removeAlertListener(
		LoggerAlertListener		listener );
}