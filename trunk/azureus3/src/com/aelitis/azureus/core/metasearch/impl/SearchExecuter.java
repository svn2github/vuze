package com.aelitis.azureus.core.metasearch.impl;

import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.Result;
import com.aelitis.azureus.core.metasearch.ResultListener;
import com.aelitis.azureus.core.metasearch.SearchParameter;


public class SearchExecuter {
	
	ResultListener listener;
	
	public SearchExecuter(ResultListener listener) {
		this.listener = listener;
	}
	
	public void search(final Engine engine,final SearchParameter[] searchParameters) {
		Thread t = new Thread(engine.getName() + " runner") {
			public void run() {
				Result[] results = engine.search(searchParameters);
				listener.resultsReceived(engine,results);
				listener.resultsComplete(engine);
			}
		};
		t.setDaemon(false);
		t.start();
	}

}
