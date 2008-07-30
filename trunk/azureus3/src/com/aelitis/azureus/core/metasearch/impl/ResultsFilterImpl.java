package com.aelitis.azureus.core.metasearch.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.aelitis.azureus.core.metasearch.Result;
import com.aelitis.azureus.core.metasearch.ResultsFilter;

public class ResultsFilterImpl implements ResultsFilter {
	
	String[] textFilters;
	long minSize = -1;
	long maxSize = -1;
	String categoryFilter = null;
	
	public ResultsFilterImpl(Map filters) {
		try {
			String rawTextFilter = ImportExportUtils.importString(filters,"textFilter");
			if(rawTextFilter != null) {
				StringTokenizer st = new StringTokenizer(rawTextFilter," ");
				textFilters = new String[st.countTokens()];
				for(int i = 0 ; i < textFilters.length ; i++) {
					textFilters[i] = st.nextToken().toLowerCase();
				}
			}
			
			minSize = ImportExportUtils.importLong(filters,"minSize",-1l);
			
			maxSize = ImportExportUtils.importLong(filters,"maxSize",-1l);
			
			String rawCategory = ImportExportUtils.importString(filters,"category");
			if(rawCategory != null) {
				categoryFilter = rawCategory.toLowerCase();
			}
			
		} catch(Exception e) {
			//Invalid filters array
		}
	}
	
	public Result[] filter(Result[] results) {
		List filteredResults = new ArrayList(results.length);
		for(int i = 0 ; i < results.length ; i++) {
			Result result = results[i];
			
			String name = result.getName();
			//Results need a name, or they are by default invalid
			if(name == null) {
				continue;
			}
			name = name.toLowerCase();
			
			boolean valid = true;
			for(int j = 0 ; j < textFilters.length ; j++) {
				
				//If one of the text filters do not match, let's not keep testing the others
				// and mark the result as not valid
				if(name.indexOf(textFilters[j]) == -1) {
					valid = false;
					break;
				}
			}
			
			//if invalid after name check, let's get to the next result
			if(!valid) {
				continue;
			}
			
			long size = result.getSize();
			
			if(minSize > -1) {
				if(minSize > size) {
					continue;
				}
			}
			
			if(maxSize > -1) {
				if(maxSize < size) {
					continue;
				}
			}
			
			if(categoryFilter != null) {
				String category = result.getCategory();
				if(category == null || !category.equals(categoryFilter)) {
					continue;
				}
			}
			
			
			//All filters are ok, let's add the results to the filtered results
			filteredResults.add(result);
			
		}
		
		Result[] fResults = (Result[]) filteredResults.toArray(new Result[filteredResults.size()]);
		
		return fResults;
	}
	
	public Map exportToBencodedMap() throws IOException {
		
		Map res = new HashMap();
		
		StringBuffer rawTextFilters = new StringBuffer();
		String separator = "";
		for(int i = 0 ; i < textFilters.length ; i++) {
			rawTextFilters.append(separator);
			rawTextFilters.append(textFilters[i]);
			separator = " ";
		}
		ImportExportUtils.exportString(res, "textFilter", rawTextFilters.toString());
		
		if(minSize > -1) {
			res.put("minSize",new Long(minSize));
		}
		
		if(maxSize > -1) {
			res.put("maxSize",new Long(maxSize));
		}
		
		if(categoryFilter != null) {
			ImportExportUtils.exportString(res, "category", categoryFilter);
		}
		
		return res;
	}
	

}
