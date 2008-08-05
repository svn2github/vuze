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
	String[] excludeTextFilters;
	String regexFilter;
	long minSeeds = -1;
	long minSize = -1;
	long maxSize = -1;
	String categoryFilter = null;
	
	public ResultsFilterImpl(Map filters) {
		try {
			textFilters = importStrings(filters,"text_filter"," ");
			
			excludeTextFilters = importStrings(filters,"exclude_text_filter"," ");
			
			regexFilter = ImportExportUtils.importString(filters, "regex_filter");
			
			minSize = ImportExportUtils.importLong(filters,"min_size",-1l);
			
			maxSize = ImportExportUtils.importLong(filters,"max_size",-1l);
			
			minSeeds = ImportExportUtils.importLong(filters, "min_seeds",-1l);
			
			String rawCategory = ImportExportUtils.importString(filters,"category");
			if(rawCategory != null) {
				categoryFilter = rawCategory.toLowerCase();
			}
			
		} catch(Exception e) {
			//Invalid filters array
		}
	}

	private String[] importStrings(Map filters,String key,String separator) throws IOException {
		String rawStringFilter = ImportExportUtils.importString(filters,key);
		if(rawStringFilter != null) {
			StringTokenizer st = new StringTokenizer(rawStringFilter,separator);
			String[] stringFilter = new String[st.countTokens()];
			for(int i = 0 ; i < stringFilter.length ; i++) {
				stringFilter[i] = st.nextToken().toLowerCase();
			}
			return stringFilter;
		}
		return new String[0];
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
			
			for(int j = 0 ; j < excludeTextFilters.length ; j++) {
				
				//If one of the text filters do not match, let's not keep testing the others
				// and mark the result as not valid
				if(name.indexOf(excludeTextFilters[j]) != -1) {
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
			
			if(minSeeds > -1) {
				if(minSeeds < result.getNbSeeds()) {
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
		
		exportStrings(res, "text_filter", textFilters," ");
		
		exportStrings(res, "exclude_text_filter", excludeTextFilters," ");
		
		ImportExportUtils.exportString(res, "regex_filter", regexFilter);
		
		if(minSize > -1) {
			res.put("min_size",new Long(minSize));
		}
		
		if(maxSize > -1) {
			res.put("max_size",new Long(maxSize));
		}
		
		if(minSeeds > -1) {
			res.put("min_seeds",new Long(minSeeds));
		}
		
		if(categoryFilter != null) {
			ImportExportUtils.exportString(res, "category", categoryFilter);
		}
		
		return res;
	}

	private void exportStrings(Map res,String key,String[] strings,String separator) throws IOException {
		StringBuffer rawStringFilters = new StringBuffer();
		String sep = "";
		for(int i = 0 ; i < strings.length ; i++) {
			rawStringFilters.append(sep);
			rawStringFilters.append(strings[i]);
			sep = separator;
		}
		ImportExportUtils.exportString(res, key, rawStringFilters.toString());
	}
	

}