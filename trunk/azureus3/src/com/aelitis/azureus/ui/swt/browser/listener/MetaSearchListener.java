package com.aelitis.azureus.ui.swt.browser.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.MetaSearch;
import com.aelitis.azureus.core.metasearch.MetaSearchManager;
import com.aelitis.azureus.core.metasearch.MetaSearchManagerFactory;
import com.aelitis.azureus.core.metasearch.Result;
import com.aelitis.azureus.core.metasearch.ResultListener;
import com.aelitis.azureus.core.metasearch.SearchParameter;
import com.aelitis.azureus.ui.swt.browser.msg.AbstractMessageListener;
import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;


public class MetaSearchListener extends AbstractMessageListener {
	
	public static final String LISTENER_ID = "metasearch";

	public static final String OP_SEARCH = "search";
	
	public static final String OP_ADD_ENGINE = "add-engine";
	public static final String OP_REMOVE_ENGINE = "remove-engine";
	
	public static final String OP_GET_ENGINES = "get-engines";

	public static final String OP_SET_MODE = "set-mode";
	
	public MetaSearchListener() {
		super(LISTENER_ID);
	}
	
	public void handleMessage(BrowserMessage message) {
		
		//MetaSearchManagerFactory.getSingleton().getMostPopularTemplates();
		
		System.out.println("Got message : " + message);
		
		String opid = message.getOperationId();

		MetaSearchManager metaSearchManager = MetaSearchManagerFactory.getSingleton();
		
		if (OP_SEARCH.equals(opid)) {
			Map decodedMap = message.getDecodedMap();
			ResultListener listener = new ResultListener() {
				
				public void engineFailed(Engine engine) {
					Map params = new HashMap();
					params.put("id", new Long(engine.getId()));
					params.put("name", engine.getName());
					params.put("favicon", engine.getIcon());
					context.sendBrowserMessage("metasearch", "engineFailed",params);
				}
				
				public void resultsComplete(Engine engine) {
					Map params = new HashMap();
					params.put("id", new Long(engine.getId()));
					params.put("name", engine.getName());
					params.put("favicon", engine.getIcon());
					context.sendBrowserMessage("metasearch", "engineCompleted",params);
				}
				
				public void resultsReceived(Engine engine,Result[] results) {
					Map params = new HashMap();
					params.put("id", new Long(engine.getId()));
					params.put("name", engine.getName());
					params.put("favicon", engine.getIcon());
					List resultsList = new ArrayList(results.length);
					for(int i = 0 ; i < results.length ; i++) {
						Result result = results[i];
						System.out.println(result);
						resultsList.add(result.toMap());
					}
					params.put("results", resultsList);
					context.sendBrowserMessage("metasearch", "resultsReceived",params);
				}
			};
			
			String searchText = (String) decodedMap.get("searchText");
			SearchParameter parameter = new com.aelitis.azureus.core.metasearch.SearchParameter("s",searchText);
			SearchParameter[] parameters = new com.aelitis.azureus.core.metasearch.SearchParameter[] {parameter};
			metaSearchManager.getMetaSearch().search(listener, parameters);

		} else if(OP_GET_ENGINES.equals(opid)) {

			Engine[] engines = metaSearchManager.getMetaSearch().getEngines();
			List params = new ArrayList();
			for(int i = 0 ; i < engines.length ; i++) {
				Engine engine = engines[i];
				Map engineMap = new HashMap();
				engineMap.put("id", new Long(engine.getId()));
				engineMap.put("name", engine.getName());
				engineMap.put("favicon", engine.getIcon());
				params.add(engineMap);
			}
			context.sendBrowserMessage("metasearch", "enginesUsed",params);
		} else if(OP_SET_MODE.equals(opid)) {
			//TODO : set the mode
			
			//metaSearchManager.aaaaa
		} else if(OP_ADD_ENGINE.equals(opid)) {
			//TODO : add an engine
			
			//metaSearchManager.getMetaSearch().addEngine( engine );
		} else if(OP_REMOVE_ENGINE.equals(opid)) {
			//TODO: remove an engine
			
			//metaSearchManager.getMetaSearch().removeEngine( engine );
		}
	}

}
