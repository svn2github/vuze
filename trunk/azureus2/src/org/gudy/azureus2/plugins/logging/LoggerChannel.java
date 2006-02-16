/*
 * File    : LoggerChannel.java
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

/**
 * @author parg
 *
 */
public interface 
LoggerChannel 
{
	public static final int	LT_INFORMATION	= 1;
	public static final int LT_WARNING		= 2;
	public static final int LT_ERROR		= 3;
		
	public String
	getName();
	
		/**
		 * Indicates whether or not logging is enabled - use to optimise calls to the log methods that
		 * require resources to construct the message to be logged
		 * Note that this doesn't apply to alerts - these will always be handled
		 * @return
		 */
		
	public boolean
	isEnabled();
	
		/**
		 * This causes the channel to also write to logs/<name> files in a cyclic fashion (c.f. the debug_1/2._log
		 * files)
		 */
	
	public void
	setDiagnostic();
	
	public void
	log(
		int		log_type,
		String	data );
	
		/**
		 * log string with implicit type "information"
		 * @param data
		 */
	
	public void
	log(
		String	data );
	
	public void
	log(
		Throwable 	error );
	
	public void
	log(
		String		data,
		Throwable 	error );
	
	/**
	 * Log a string against a list of objects
	 * 
	 * @param relatedTo a list of what this log is related to (ex. Peer, Torrent,
	 *                   Download, Object)
	 * @param log_type LT_* constant
	 * @param data text to log
	 * 
	 * @since 2.3.0.7
	 */
	public void log(Object[] relatedTo, int log_type, String data);

	/**
	 * Log an error against an object.
	 * 
	 * @param relatedTo What this log is related to (ex. Peer, Torrent,
	 *         Download, Object, etc)
	 * @param log_type LT_* constant
	 * @param data text to log
	 * 
	 * @since 2.3.0.7
	 */
	public void log(Object relatedTo, int log_type, String data);
	
	/**
	 * Log an error against an object.
	 * 
	 * @param relatedTo What this log is related to (ex. Peer, Torrent,
	 *         Download, Object, etc)
	 * @param data text to log
	 * @param error Error that will be appended to the log entry
	 * 
	 * @since 2.3.0.7
	 */
	
	public void log(Object relatedTo, String data, Throwable error);
	
	/**
	 * Log an error against a list of objects
	 * 
	 * @param relatedTo a list of what this log is related to (ex. Peer, Torrent,
	 *                   Download, Object)
	 * @param data text to log
	 * @param error Error that will be appended to the log entry
	 * 
	 * @since 2.3.0.7
	 */
	public void log(Object[] relatedTo, String data, Throwable error);

	/**
	 * raise an alert to the user, if UI present
	 * Note that messages shown to the user are filtered on unique message content
	 * So if you raise an identical alert the second + subsequent messages will not be
	 * shown. Thus, if you want "identical" messages to be shown, prefix them with something
	 * unique like a timestamp.
	 * @param alert_type
	 * @param message
	 */
	
	public void
	logAlert(
		int			alert_type,
		String		message );
	
	public void
	logAlert(
		String		message,
		Throwable 	e );
	
		/**
		 * These variants will always generate an alert (i.e. duplicates won't be filtered)
		 * @param alert_type
		 * @param message
		 */
	public void
	logAlertRepeatable(
		int			alert_type,
		String		message );
	
	public void
	logAlertRepeatable(
		String		message,
		Throwable 	e );
	
	public void
	addListener(
		LoggerChannelListener	l );
	
	public void
	removeListener(
		LoggerChannelListener	l );
	
	public Logger
	getLogger();
	
}
