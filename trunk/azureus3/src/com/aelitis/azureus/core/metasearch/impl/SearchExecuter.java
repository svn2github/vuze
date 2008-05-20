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
