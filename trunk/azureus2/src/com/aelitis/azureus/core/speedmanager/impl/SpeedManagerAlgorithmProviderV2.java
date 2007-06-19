/*
 * Created on May 7, 2007
 * Created by Paul Gardner
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.speedmanager.impl;

import com.aelitis.azureus.core.speedmanager.SpeedManagerPingSource;

public class 
SpeedManagerAlgorithmProviderV2 
	implements SpeedManagerAlgorithmProvider
{

    private SpeedManagerAlgorithmProviderAdapter		adapter;

    //Test algorithms below.
    private SpeedManagerAlgorithmProvider strategy;


    //line-speed limits.
    //ToDo: these are teh MAX_LIMIT
    //public static final String SETTING_DOWNLOAD_LINESPEED_CAPACITY = "SpeedManagerAlgorithmProviderV2.setting.download.linespeed.capacity";
    //public static final String SETTING_UPLOAD_LINESPEED_CAPACITY = "SpeedManagerAlgorithmProviderV2.setting.upload.linespeed.capacity";
    
    //key names are below.
    public static final String SETTING_DOWNLOAD_MAX_LIMIT = "SpeedManagerAlgorithmProviderV2.setting.download.max.limit";
    public static final String SETTING_DOWNLOAD_MIN_LIMIT = "SpeedManagerAlgorithmProviderV2.setting.download.min.limit";
    public static final String SETTING_UPLOAD_MAX_LIMIT = "SpeedManagerAlgorithmProviderV2.setting.upload.max.limit";
    public static final String SETTING_UPLOAD_MIN_LIMIT = "SpeedManagerAlgorithmProviderV2.setting.upload.min.limit";

    //sets the input source,  vivaldi, DHT ping, ICMP pint, etc ....
    public static final String SETTING_DATA_SOURCE_INPUT = "SpeedManagerAlgorithmProviderV2.source.data.input";
    //modes for the data source.
    public static final String VALUE_SOURCE_VIVALDI = "vivaldi";
    public static final String VALUE_SOURCE_DHT = "dht";

    //Vivaldi settings.
    public static final String SETTING_VIVALDI_GOOD_SET_POINT = "SpeedManagerAlgorithmProviderV2.setting.vivaldi.good.setpoint";
    public static final String SETTING_VIVALDI_GOOD_TOLERANCE = "SpeedManagerAlgorithmProviderV2.setting.vivaldi.good.tolerance";
    public static final String SETTING_VIVALDI_BAD_SET_POINT = "SpeedManagerAlgorithmProviderV2.setting.vivaldi.bad.setpoint";
    public static final String SETTING_VIVALDI_BAD_TOLERANCE = "SpeedManagerAlgorithmProviderV2.setting.vivaldi.good.tolerance";

    //adjustments up should be smaller then down.
    public static final String SETTING_UPTICK_DAMPING_FACTOR = "SpeedManagerAlgorithmProviderV2.uptick.adjust.factor";

    //DHT ping settings.
    public static final String SETTING_DHT_GOOD_SET_POINT = "SpeedManagerAlgorithmProviderV2.setting.dht.good.setpoint";
    public static final String SETTING_DHT_GOOD_TOLERANCE = "SpeedManagerAlgorithmProviderV2.setting.dht.good.tolerance";
    public static final String SETTING_DHT_BAD_SET_POINT = "SpeedManagerAlgorithmProviderV2.setting.dht.bad.setpoint";
    public static final String SETTING_DHT_BAD_TOLERANCE = "SpeedManagerAlgorithmProviderV2.setting.dht.bad.tolerance";

    //ping factors settings.
    public static final String SETTING_WAIT_AFTER_ADJUST = "SpeedManagerAlgorithmProviderV2.setting.wait.after.adjust";
    public static final String SETTING_INTERVALS_BETWEEN_ADJUST = "SpeedManagerAlgorithmProviderV2.intervals.between.adjust";

    //enable this mode.
    public static final String SETTING_V2_BETA_ENABLED = "SpeedManagerAlgorithmProviderV2.setting.beta.enabled";


    //the ratio of upload/download speed when making adjustments.
    public static final String SETTING_V2_UP_DOWN_RATIO = "SpeedManagerAlgorithmProviderV2.setting.updown.ratio";


    protected
	SpeedManagerAlgorithmProviderV2(
		SpeedManagerAlgorithmProviderAdapter	_adapter )
	{
		adapter	= _adapter;
		adapter.setLoggingEnabled( true );

        strategy = new SpeedManagerAlgorithmProviderVivaldi(_adapter);
    }
	
	public void
	reset()
	{
        strategy.reset();
    }
	
	public void
	updateStats()
	{
        strategy.updateStats();
    }
	
	public void
	pingSourceFound(
		SpeedManagerPingSource		source,
		boolean						is_replacement )
	{
		log( "Found ping source: " + source.getAddress());

        strategy.pingSourceFound(source,is_replacement);
    }
	
	public void
	pingSourceFailed(
		SpeedManagerPingSource		source )
	{
		log( "Lost ping source: " + source.getAddress());

        strategy.pingSourceFailed(source);
    }
	
	public void
	calculate(
		SpeedManagerPingSource[]	sources )
	{
		String	str = "";
		
		for (int i=0;i<sources.length;i++){
			
			str += (i==0?"":",") + sources[i].getAddress() + " -> " + sources[i].getPingTime();
		}
		
		log( "ping-data: " + str );


        strategy.calculate(sources);
    }
	
	public int
	getIdlePingMillis()
	{
        return strategy.getIdlePingMillis();
	}
	
	public int
	getCurrentPingMillis()
	{
        return strategy.getCurrentPingMillis();
	}
	
	public int
	getMaxPingMillis()
	{
        return strategy.getMaxPingMillis();
	}
	
	public int
	getCurrentChokeSpeed()
	{
        return strategy.getCurrentChokeSpeed();
	}
	
	public int
	getMaxUploadSpeed()
	{        
        return strategy.getMaxUploadSpeed();
	}
	
	public boolean getAdjustsDownloadLimits() {
		// TODO Auto-generated method stub
		return false;
	}
	   
	protected void
	log(
		String	str )
	{
        SpeedManagerLogger.log( str );
    }

}
