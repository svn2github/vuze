package com.aelitis.azureus.core.metasearch.impl;

import java.util.regex.Matcher;

import com.aelitis.azureus.core.metasearch.Result;
import com.aelitis.azureus.core.metasearch.Engine;

public class FieldRemapper {
	
	private int inField;
	private int outField;
	
	private FieldRemapping[] fieldRemappings;

	
	public 
	FieldRemapper(
		int 				inField, 
		int 				outField,
		FieldRemapping[] 	fieldRemappings) 
	{
		super();
		this.inField = inField;
		this.outField = outField;
		this.fieldRemappings = fieldRemappings;
	}

	public int
	getInField()
	{
		return( inField );
	}
	
	public int
	getOutField()
	{
		return( outField );
	}
	
	public FieldRemapping[]
	getMappings()
	{
		return( fieldRemappings );
	}

	public void 
	remap(
		Result r )
	{
		String input = null;
		switch(inField) {
			case Engine.FIELD_CATEGORY :
				input = r.getCategory();
				break;
		}
		
		String output = null;
		if(input != null) {
			for(int i = 0 ; i < fieldRemappings.length ; i++) {
				Matcher matcher = fieldRemappings[i].getMatch().matcher(input);
				if(matcher.matches()) {
					output = matcher.replaceAll(fieldRemappings[i].getReplacement());
					i = fieldRemappings.length;
				}
			}
		}
		
		if(output != null) {
			switch(outField) {
			case Engine.FIELD_CATEGORY :
				r.setCategory(output);
				break;
			case Engine.FIELD_CONTENT_TYPE :
				r.setContentType(output);
				break;
			}
		}
		
	}

}
