/*
 * Created on Feb 4, 2010
 * Created by Paul Gardner
 * 
 * Copyright 2010 Vuze, Inc.  All rights reserved.
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


package org.gudy.azureus2.plugins.utils;

import java.util.Map;


public interface 
FeatureManager 
{
	public Licence[]
	getLicences();
	
	public Licence
	addLicence(
		String		licence_key );
	
	public FeatureDetails
	getFeatureDetails(
		String					feature_id,
		Map<String,Object>		feature_properties );
	
	public void
	registerFeatureEnabler(
		FeatureEnabler	enabler );
	
	public void
	unregisterFeatureEnabler(
		FeatureEnabler	enabler );
	
	public interface
	Licence
	{
		public String
		getKey();
		
		public FeatureDetails[]
		getFeatures();
	}
	
	public interface
	FeatureEnabler
	{
		public Licence[]
       	getLicences();
       	
       	public Licence
       	addLicence(
       		String		licence_key );
		           	
		public FeatureDetails
		getFeatureDetails(
			String					requester_id,
			String					feature_id,
			Map<String,Object>		feature_properties );
	}
	
	public interface
	FeatureDetails
	{
		public String	PR_VALID_UNTIL				= "ValidUntil";				// Long
		
		public String
		getID();
		
		public boolean
		isEnabled();
		
		public byte[]
		getEncodedProperties();
		
		public byte[]
		getSignature();
		
		public Object
		getProperty(
			String		propery_name );
		
		public void
		setProperty(
			String		property_name,
			Object		property_value );
	}
}
