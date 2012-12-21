/*
 * Created on Dec 19, 2012
 * Created by Paul Gardner
 * 
 * Copyright 2012 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.net.upnpms.impl;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import com.aelitis.net.upnpms.*;

public class 
Test 
{
	private static void
	dump(
		UPNPMSContainer		container,
		String				indent )
	
		throws Exception
	{
		System.out.println( indent + container.getTitle() + " - " + container.getID());
		
		indent += "    ";
		
		List<UPNPMSContainer>	kids = container.getContainers();
		
		for ( UPNPMSContainer kid: kids ){
			
			dump( kid, indent );
		}
		
		List<UPNPMSItem>	items = container.getItems();
		
		for ( UPNPMSItem item: items ){
			
			System.out.println( indent + item.getTitle() + " - " + item.getID());
		}
	}
	
	public static void
	main(
		String[]	args )
	{
		try{
			UPNPMSBrowser browser = 
				new UPNPMSBrowserImpl( 
						"Vuze", 
						Arrays.asList( new URL[]{ new URL( "http://192.168.1.5:2659/" )}),
						new UPNPMSBrowserListener()
						{
							public void 
							setPreferredURL(URL url) 
							{
							}
						});
			
			UPNPMSContainer root = browser.getRoot();
			
			dump( root, "" );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
