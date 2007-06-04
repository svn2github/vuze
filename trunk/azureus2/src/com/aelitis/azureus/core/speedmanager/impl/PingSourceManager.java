package com.aelitis.azureus.core.speedmanager.impl;

import com.aelitis.azureus.core.speedmanager.SpeedManagerPingSource;
import com.aelitis.azureus.core.util.average.Average;

import java.util.Map;
import java.util.HashMap;

import org.gudy.azureus2.core3.util.SystemTime;

/**
 * Created on May 31, 2007
 * Created by Alan Snyder
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 * <p/>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * <p/>
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */


/**
 * This class manage cycling though the PingSources. It keep track of PingSource stats and
 * applies rules on if/when to cycle though a ping-source.
 *
 * #1) If the slowest ping-source is 10x the the best for a 1 min average. kick it.
 * #2) If a ping-source is slower then two combined sources (2x) for a 5 min average. then kick it.
 * #3) Every 30 minutes kick the slowest ping source and request a new one. Just to keep things fresh.
 *
 * Also maintain logic do determine if a new source is better then the previous one. (To determine
 * if these rules lead to good data.)
 *
 */
public class PingSourceManager
{
    //
    private final Map pingAverages = new HashMap(); //<Source,PingSourceStats>
    private long lastPingRemoval=0;
    private static final long TIME_BETWEEN_REMOVALS = 5 * 60000; //five minutes.


    /**
     * Determine if we should drop any ping sources.
     * Sort them, if one significantly higher then the other two. then drop it.
     * @param sources - SpeedManagerPingSource[] inputs
     */
    public void checkPingSources(SpeedManagerPingSource[] sources){

        //if the long term average of one source is 10 the lowest and twice a large as the
        //two lowest then drop the highest at the moment. Also, don't force sources to
        //drop to frequently.

        //if we just recently removed a ping source then wait.
        long currTime = SystemTime.getCurrentTime();
        if( currTime<lastPingRemoval+TIME_BETWEEN_REMOVALS ){
            return;
        }

        //no sources.
        if( sources==null ){
            return;
        }

        //if we have only two sources then don't do this test.
        if( sources.length<3 ){
            return;
        }

        double highestLongTermPing=0.0;
        SpeedManagerPingSource highestSource=null;
        double lowestLongTermPing=10000.0;

        int len = sources.length;
        for(int i=0; i<len; i++){
            PingSourceStats pss = (PingSourceStats) pingAverages.get(sources[i]);
            Average a = pss.getLongTermAve();
            double avePingTime = a.getAverage();

            //is this a new highest value?
            if( avePingTime>highestLongTermPing ){
                highestLongTermPing = avePingTime;
                highestSource = sources[i];
            }

            //is this a new lowest value?
            if( avePingTime<lowestLongTermPing ){
                lowestLongTermPing = avePingTime;
            }
        }//for

        //if the highest value is 10x the lowest then find another source.
        if( lowestLongTermPing*10 < highestLongTermPing ){
            //remove the slow source we will get a new one to replace it.
            if( highestSource!=null ){
                SpeedManagerLogger.log("dropping ping source: "+highestSource.getAddress()+" for being 10x greater then min source.");
                highestSource.destroy();
            }
        }//if

    }//checkPingSources


    public void pingSourceFound(SpeedManagerPingSource source, boolean is_replacement){
        PingSourceStats pss = new PingSourceStats(source);
        pingAverages.put(source,pss);
    }

    public void pingSourceFailed(SpeedManagerPingSource source) {
        if( pingAverages.remove(source)==null){
            SpeedManagerLogger.log("didn't find source: "+source.getAddress().getHostName());
        }
    }

    public void addPingTime(SpeedManagerPingSource source){

        PingSourceStats pss = (PingSourceStats) pingAverages.get(source);
        int pingTime = source.getPingTime();
        if(pingTime>0){
            pss.addPingTime( source.getPingTime() );
        }

    }//addPingTime

}
