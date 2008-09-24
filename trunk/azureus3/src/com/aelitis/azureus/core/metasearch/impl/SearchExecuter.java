/*
 * Created on May 6, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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

package com.aelitis.azureus.core.metasearch.impl;

import java.util.HashMap;

import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.ResultListener;
import com.aelitis.azureus.core.metasearch.SearchException;
import com.aelitis.azureus.core.metasearch.SearchParameter;


public class SearchExecuter {
	
	ResultListener listener;
	
	public SearchExecuter(ResultListener listener ) {
		this.listener = listener;
	}
	
	public void 
	search(
		final Engine 			engine,
		final SearchParameter[] searchParameters, 
		final String 			headers,
		final int				desired_max_matches )
	{
		Thread t = new Thread(engine.getName() + " runner") {
			public void 
			run() 
			{
				try{
					engine.search(searchParameters, new HashMap(), desired_max_matches, -1, headers, listener );
					
				}catch( SearchException e ){
				}
			}
		};
		t.setDaemon(false);
		t.start();
	}

}
