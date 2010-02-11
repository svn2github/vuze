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


public interface 
FeatureManager 
{
	public Licence[]
	getLicences();
	
	public Licence
	addLicence(
		String		licence_key );
	
	public FeatureDetails[]
	getFeatureDetails(
		String					feature_id );
	
	public void
	refreshLicences();
	
	public void
	registerFeatureEnabler(
		FeatureEnabler	enabler );
	
	public void
	unregisterFeatureEnabler(
		FeatureEnabler	enabler );
	
	public void
	addListener(
		FeatureManagerListener		listener );
	
	public void
	removeListener(
		FeatureManagerListener		listener );

	
	public interface
	Licence
	{
		public final int LS_PENDING_AUTHENTICATION	= 1;
		public final int LS_AUTHENTICATED			= 2;
		public final int LS_INVAID_KEY				= 3;
		public final int LS_CANCELLED				= 4;
		public final int LS_REVOKED					= 5;
		
		public int
		getState();
		
		public String
		getKey();
		
		public FeatureDetails[]
		getFeatures();
		
		public void
		remove();
	}
	
	public interface
	FeatureEnabler
	{
		public Licence[]
       	getLicences();
       	
       	public Licence
       	addLicence(
       		String		licence_key );
       	
       	public void
       	refreshLicences();
       	
    	public void
    	addListener(
    		FeatureManagerListener		listener );
    	
    	public void
    	removeListener(
    		FeatureManagerListener		listener );
	}
	
	public interface
	FeatureDetails
	{
		public String	PR_PUBLIC_KEY				= "PublicKey";				// String
		public String	PR_VALID_UNTIL				= "ValidUntil";				// Long
		public String	PR_TRIAL_USES_REMAINING		= "TrialUsesRemaining";		// Long
		
		public Licence
		getLicence();
		
		public String
		getID();
				
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
	
	public interface
	FeatureManagerListener
	{
		public void
		licenceAdded(
			Licence	licence );
		
		public void
		licenceChanged(
			Licence	licence );
		
		public void
		licenceRemoved(
			Licence	licence );
	}
}
