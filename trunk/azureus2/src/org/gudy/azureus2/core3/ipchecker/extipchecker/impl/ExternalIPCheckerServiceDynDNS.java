/*
 * File    : ExternalIPCheckerServiceDynDNS.java
 * Created : 09-Nov-2003
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

package org.gudy.azureus2.core3.ipchecker.extipchecker.impl;

/**
 * @author parg
 *
 */
public class 
ExternalIPCheckerServiceDynDNS
	extends ExternalIPCheckerServiceImpl 
{
	protected static final String	CHECKER_URL	= "http://checkip.dyndns.org/";
	
	protected
	ExternalIPCheckerServiceDynDNS()
	{
		super( "IPChecker.external.service.dyndns" );
	}
	
	public void
	initiateCheck(
		long		timeout )
	{
		super.initiateCheck( timeout );
	}
	
	protected void
	initiateCheckSupport()
	{
		reportProgress( "Loading web page '" + CHECKER_URL + "'");
		
		String	page = loadPage( CHECKER_URL );

		reportProgress( "Analysing Response" );		
				
		String	IP = extractIPAddress( page );
				
		reportProgress( "Extracted IP address '" + IP + "'" );
		
		informSuccess( IP==null?"":IP );
	}
}
