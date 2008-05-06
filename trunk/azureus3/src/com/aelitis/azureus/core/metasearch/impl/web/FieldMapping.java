package com.aelitis.azureus.core.metasearch.impl.web;

public class FieldMapping {
	
	private String name;
	private int field;
	
	public FieldMapping(String name,int field) {
		this.name = name;
		this.field = field;
	}

	public String
	getName()
	{
		return( name );
	}
	
	public int
	getField()
	{
		return( field );
	}
}
