/*
 * Created on May 7, 2007
 * Created by Paul Gardner
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.speedmanager.impl;


import com.aelitis.azureus.core.speedmanager.SpeedManagerPingSource;

public class 
SpeedManagerAlgorithmProviderV2 
	implements SpeedManagerAlgorithmProvider
{
	private SpeedManagerAlgorithmProviderAdapter		adapter;
	
	protected
	SpeedManagerAlgorithmProviderV2(
		SpeedManagerAlgorithmProviderAdapter	_adapter )
	{
		adapter	= _adapter;
		
		adapter.setLoggingEnabled( true );
	}
	
	public void
	reset()
	{	
			// TODO - reset everything to start-of-day values
	}
	
	public void
	updateStats()
	{
			// TODO - calculate upload speed!
		
		adapter.setCurrentUploadLimit( 1024*16 );
	}
	
	public void
	pingSourceFound(
		SpeedManagerPingSource		source,
		boolean						is_replacement )
	{
		log( "Found ping source: " + source.getAddress());
	}
	
	public void
	pingSourceFailed(
		SpeedManagerPingSource		source )
	{
		log( "Lost ping source: " + source.getAddress());
	}
	
	public void
	calculate(
		SpeedManagerPingSource[]	sources )
	{
		String	str = "";
		
		for (int i=0;i<sources.length;i++){
			
			str += (i==0?"":",") + sources[i].getAddress() + " -> " + sources[i].getPingTime();
		}
		
		log( "Calculate: " + str );
	}
	
	public int
	getIdlePingMillis()
	{
		return( 0 );	// TODO
	}
	
	public int
	getCurrentPingMillis()
	{
		return( 0 );	// TODO
	}
	
	public int
	getMaxPingMillis()
	{
		return( 0 );	// TODO
	}
	
	public int
	getCurrentChokeSpeed()
	{
		return( 0 );	// TODO
	}
	
	public int
	getMaxUploadSpeed()
	{
		return( 0 );	// TODO
	}
	
	protected void
	log(
		String	str )
	{
		adapter.log( str );
	}
}
