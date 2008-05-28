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

import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.ResultListener;
import com.aelitis.azureus.core.metasearch.SearchParameter;


public class SearchExecuter {
	
	ResultListener listener;
	
	public SearchExecuter(ResultListener listener ) {
		this.listener = listener;
	}
	
	public void search(final Engine engine,final SearchParameter[] searchParameters, final String headers ) {
		Thread t = new Thread(engine.getName() + " runner") {
			public void run() {
				engine.search(searchParameters, -1, headers, listener );
			}
		};
		t.setDaemon(false);
		t.start();
	}

}
