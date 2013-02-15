/*
 * Created on Jan 17, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
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


package com.aelitis.azureus.util;

import java.util.Collections;
import java.util.Set;
import java.util.TreeMap;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.FeatureManager;
import org.gudy.azureus2.plugins.utils.FeatureManager.FeatureDetails;
import org.gudy.azureus2.plugins.utils.FeatureManager.Licence;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.pluginsimpl.local.utils.UtilitiesImpl;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;

public class 
FeatureUtils 
{
	private static FeatureManager featman;

	static{
		AzureusCoreFactory.addCoreRunningListener(
			new AzureusCoreRunningListener() 
			{
				public void 
				azureusCoreRunning(
					AzureusCore core) 
				{
					PluginInterface pi = core.getPluginManager().getDefaultPluginInterface();
					featman = pi.getUtilities().getFeatureManager();
				}
			});
	}
			
	public static String getMode() {
		boolean isFull = hasFullLicence();
		boolean isTrial = hasFullBurn() && !isFull;
		return isFull ? "plus" : isTrial ? "trial" : "free";
	}
	
	public static boolean hasFullLicence() {
		if (featman == null) {
			//Debug.out("featman null");
			Set<String> featuresInstalled = UtilitiesImpl.getFeaturesInstalled();
			return featuresInstalled.contains("dvdburn");
		}
		licenceDetails fullFeatureDetails = getFullFeatureDetails();
		long now = SystemTime.getCurrentTime();
		return fullFeatureDetails != null && fullFeatureDetails.expiry > now
				&& fullFeatureDetails.displayedExpiry > now;
	}

	public static class licenceDetails {
		public final Licence licence;
		public long expiry;
		public long displayedExpiry;

		public licenceDetails(long expiry, long displayedExpiry, Licence licence) {
			this.expiry = expiry;
			this.displayedExpiry = displayedExpiry;
			this.licence = licence;
		}
		
		public String getRenewalKey() {
			FeatureDetails[] features = licence.getFeatures();
			if (features == null) {
				return null;
			}
			for (FeatureDetails fd : features) {
				Object property = fd.getProperty(FeatureDetails.PR_RENEWAL_KEY);
				if (property instanceof String) {
					return (String) property;
				}
			}
			return null;
		}
	}

	public static licenceDetails getFullFeatureDetails() {
		if (featman == null) {
			Debug.out("featman null");
			return null;
		}

		TreeMap<Long, Object[]> mapOrder = new TreeMap<Long, Object[]>(
				Collections.reverseOrder());
		FeatureDetails[] featureDetails = featman.getFeatureDetails("dvdburn");
		// if any of the feature details are still valid, we have a full
		for (FeatureDetails fd : featureDetails) {
			Licence licence = fd.getLicence();
			int state = licence.getState();
			if (state == Licence.LS_ACTIVATION_DENIED) {
				mapOrder.put(-1L, new Object[] { licence, Long.valueOf(0) });
				continue;
			} else if (state == Licence.LS_CANCELLED) {
				mapOrder.put(-2L, new Object[] { licence, Long.valueOf(0) });
				continue;
			} else if (state == Licence.LS_INVALID_KEY) {
				mapOrder.put(-3L, new Object[] { licence, Long.valueOf(0) });
				continue;
			} else if (state == Licence.LS_REVOKED) {
				mapOrder.put(-4L, new Object[] { licence, Long.valueOf(0) });
				continue;
			} else if (state == Licence.LS_PENDING_AUTHENTICATION) {
				mapOrder.put(-6L, new Object[] { licence, Long.valueOf(0) });
				continue;
			}

			long now = SystemTime.getCurrentTime();
			Long lValidUntil = (Long) fd.getProperty(FeatureDetails.PR_VALID_UNTIL);
			Long lValidOfflineUntil = (Long) fd.getProperty(FeatureDetails.PR_OFFLINE_VALID_UNTIL);

			if (lValidUntil == null && lValidOfflineUntil == null) {
				continue;
			}

			long minValidUntil = -1;
			long maxValidUntil = -1;
			if (lValidUntil != null) {
				minValidUntil = maxValidUntil = lValidUntil.longValue();
				if (minValidUntil < now) {
					mapOrder.put(minValidUntil, new Object[] { licence, Long.valueOf(minValidUntil) });
					continue;
				}
			}
			if (lValidOfflineUntil != null) {
				long validOfflineUntil = lValidOfflineUntil.longValue();
				if (validOfflineUntil < now) {
					mapOrder.put(validOfflineUntil, new Object[] { licence, Long.valueOf(maxValidUntil) });
					continue;
				}
				if (maxValidUntil == -1 || validOfflineUntil > maxValidUntil) {
					maxValidUntil = validOfflineUntil;
				}
			}

			mapOrder.put(maxValidUntil, new Object[] { licence, minValidUntil });
		}

		if (mapOrder.size() == 0) {
			return null;
		}

		Long firstKey = mapOrder.firstKey();
		Object[] objects = mapOrder.get(firstKey);
		Licence licence = (Licence) objects[0];
		return new licenceDetails(firstKey.longValue(), ((Long) objects[1]).longValue(), licence);
	}
	
	public static boolean isTrialLicence(Licence licence) {
		if (featman == null) {
			return false;
		}

		// if any of the FeatureDetails is a trial, return true

		boolean trial = false;
		FeatureDetails[] featureDetails = licence.getFeatures();
		for (FeatureDetails fd : featureDetails) {
			trial = isTrial(fd);
			if (trial) {
				break;
			}
		}

		return trial;
	}

	public static boolean isTrial(FeatureDetails fd) {
		Long lIsTrial = (Long) fd.getProperty(FeatureDetails.PR_IS_TRIAL);
		return lIsTrial == null ? false : lIsTrial.longValue() != 0;
	}
	
	public static long getRemaining() {
		FeatureDetails[] featureDetails = featman.getFeatureDetails("dvdburn_trial");
		if (featureDetails == null) {
			return 0;
		}
		for (FeatureDetails fd : featureDetails) {
			long remainingUses = getRemainingUses(fd);
			if (remainingUses >= 0) {
				return remainingUses;
			}
		}
		return 0;
	}

	private static long getRemainingUses(FeatureDetails fd) {
		if (fd == null) {
			return 0;
		}
		Long lRemainingUses = (Long) fd.getProperty(FeatureDetails.PR_TRIAL_USES_REMAINING);
		long remainingUses = lRemainingUses == null ? -1
				: lRemainingUses.longValue();
		return remainingUses;
	}

	/**
	 * @return
	 */
	public static boolean hasFullBurn() {
		
		PluginInterface pi = PluginInitializer.getDefaultInterface().getPluginState().isInitialisationComplete()
				? AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID(
						"azburn_v") : null;
		if (pi == null) {
			// maybe not added yet.. use featman
			Set<String> featuresInstalled = UtilitiesImpl.getFeaturesInstalled();
			return featuresInstalled.contains("dvdburn_trial") && !featuresInstalled.contains("dvdburn");
		}
		return pi.getPluginState().isOperational();
	}
	
	
	public static long getPlusExpiryTimeStamp() {
		licenceDetails fullFeatureDetails = getFullFeatureDetails();
		if (fullFeatureDetails == null || fullFeatureDetails.expiry == 0) {
			return 0;
		}
		return fullFeatureDetails.expiry;
	}

	public static long getPlusExpiryDisplayTimeStamp() {
		licenceDetails fullFeatureDetails = getFullFeatureDetails();
		if (fullFeatureDetails == null || fullFeatureDetails.expiry == 0) {
			return 0;
		}
		return fullFeatureDetails.displayedExpiry;
	}

	public static String getPlusRenewalCode() {
		licenceDetails fullFeatureDetails = getFullFeatureDetails();
		if (fullFeatureDetails == null || fullFeatureDetails.expiry == 0) {
			return null;
		}

		return fullFeatureDetails.getRenewalKey();
	}
}
