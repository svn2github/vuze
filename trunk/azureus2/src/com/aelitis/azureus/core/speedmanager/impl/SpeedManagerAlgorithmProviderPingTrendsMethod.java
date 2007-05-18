package com.aelitis.azureus.core.speedmanager.impl;

import com.aelitis.azureus.core.speedmanager.SpeedManagerPingSource;
import com.aelitis.azureus.core.util.average.Average;
import com.aelitis.azureus.core.util.average.AverageFactory;

import java.util.*;

import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.AEDiagnosticsLogger;

/**
 * Created on May 8, 2007
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

public class SpeedManagerAlgorithmProviderPingTrendsMethod
        implements SpeedManagerAlgorithmProvider
{
    private SpeedManagerAlgorithmProviderAdapter adapter;


    private final Map pingAverages = new HashMap();  //<SpeedManagerPingSource,PingSourceStats>

    private static final float maxUpStepPercent = 0.3f;
    private static final float maxDownStepPercent = 0.3f;

    AEDiagnosticsLogger dLog = AEDiagnostics.getLogger("v3.AutoSpeedDebug");


    //Hardcoded values during debug.
    private static final int debugMaxUp=500000;
    private static final int debugMinUp= 20000;
    
    //private static final int debugMaxUp=32000;
    //private static final int debugMinUp=5000;


    //items below are for accumulating stats to display on the UI.
    private int idleAverage;
    private Average pingAverageHistory = AverageFactory.MovingImmediateAverage(5);
    private int maxPing;
    private Average chockSpeedAverage = AverageFactory.MovingImmediateAverage(3);
    private int maxUploadAverage;


    public SpeedManagerAlgorithmProviderPingTrendsMethod(SpeedManagerAlgorithmProviderAdapter _adapter){
        adapter = _adapter;
    }

    /**
     * Reset any state to start of day values
     */
    public void reset() {
        dLog.log("reset");
    }

    /**
     * Called periodically (see period above) to allow stats to be updated.
     */
    public void updateStats() {
        dLog.log("updateStats");

        //get the stats needed for the UI, Statistics->Transfers page.
        int currentProtocolSpeed = adapter.getCurrentProtocolUploadSpeed();
        int currentDataSpeed = adapter.getCurrentDataUploadSpeed();

        int currentSpeed = currentProtocolSpeed+currentDataSpeed;



    }

    /**
     * Called when a new source of ping times has been found
     *
     * @param source -
     * @param is_replacement One of the initial sources or a replacement for a failed one
     */
    public void pingSourceFound(SpeedManagerPingSource source, boolean is_replacement) {
        //add a new ping source to the list.
        PingSourceStats pss = new PingSourceStats(source);
        pingAverages.put(source,pss);

    }

    /**
     * Ping source has failed
     *
     * @param source -
     */
    public void pingSourceFailed(SpeedManagerPingSource source) {

        if( pingAverages.remove(source)==null){
            log("didn't find source: "+source.getAddress().getHostName());
        }

    }

    /**
     * Called whenever a new set of ping values is available for processing. This
     * algorithm strategy will attempt to be similar to SpeedSence.
     *
     * @param sources -
     */
    public void calculate(SpeedManagerPingSource[] sources) {

        //we want some new ping times here.
        int trend=0;
        int len = sources.length;
        for(int i=0; i<len; i++){
            PingSourceStats pss = (PingSourceStats) pingAverages.get(sources[i]);
            int pingTime = sources[i].getPingTime();

            //exclude ping-times of -1 which mess up the averages.
            if(pingTime>0){
                pss.addPingTime( sources[i].getPingTime() );
            }
            //log the trend for debugging purposes.
            trend += pss.getTrend();
        }

        //we want to list the outputs here.
        StringBuffer sb = new StringBuffer(" trend: "+trend+" " );
        int newUploadLimit=adapter.getCurrentUploadLimit();
        if( trend>13 ){
            sb.append(":U:");
            sb.append(adapter.getCurrentUploadLimit());

            //determine where we are compared to the current limits.
            newUploadLimit = increaseUploadLimit(trend);

            sb.append(":").append(newUploadLimit);

            adapter.setCurrentUploadLimit( newUploadLimit );
        }else if( trend<-15 ){
            sb.append(":D:");
            sb.append(adapter.getCurrentUploadLimit());

            newUploadLimit = decreaseUploadLimit(trend);

            sb.append(":").append(newUploadLimit);

            adapter.setCurrentUploadLimit( newUploadLimit );
        }

        log( sb.toString() );

        //once a change is made then time is needed to determine its effect.
        //know where you are in relation to the limits to determine how much to increase or drop.

    }//calculate



    /**
     * Various getters for interesting info shown in stats view
     *
     */

    /**
     * For the UI
     * @return
     */
    public int getIdlePingMillis() {
        return( idleAverage );
    }

    /**
     * For the UI
     * @return
     */
    public int getCurrentPingMillis() {
        return( (int)pingAverageHistory.getAverage());
    }

    /**
     * For UI
     * @return
     */
    public int getMaxPingMillis() {
        return( maxPing );
    }

    /**
     * Returns the current view of when choking occurs
     *
     * @return speed in bytes/sec
     */
    public int getCurrentChokeSpeed() {
        return((int)chockSpeedAverage.getAverage());
    }

    public int getMaxUploadSpeed() {
        return( maxUploadAverage );
    }


    protected void log( String	str )
	{
		adapter.log( str );
        dLog.log(str);
    }

    /**
     * helper methods are below.
     */




    /**
     *
     * @param trend -
     * @return -
     */
    private int increaseUploadLimit(int trend)
    {
        //int maxUp = adapter.getMaxUp();
        int maxUp = debugMaxUp;
        int currUpLimit = adapter.getCurrentUploadLimit();

        int maxStep = Math.round(((float)(maxUp-currUpLimit))*maxUpStepPercent);
        int minStep = 1000;
        int currStep = 0;

        int newUpLimit = currUpLimit;

        if(trend<14){
            currStep=0;
        }
        else if(trend==14){
            currStep= (maxStep/16);
        }
        else if(trend==15){
            currStep= (maxStep/8);
        }
        else if(trend==16){
            currStep= (maxStep/4);
        }
        else if(trend==17){
            currStep= (maxStep/2);
        }
        else if(trend>=18){
            currStep= (maxStep);
        }

        if(currStep<minStep){
            currStep=minStep;
        }

        newUpLimit += currStep;

        if(newUpLimit>maxUp){
            newUpLimit = maxUp;
        }

        //To nearest K value.
        newUpLimit = (( newUpLimit + 1023 )/1024) * 1024;

        return newUpLimit;
    }

    /**
     *
     * @param trend -
     * @return -
     */
    private int decreaseUploadLimit(int trend)
    {
        //int minUp = adapter.getMinUp();
        int minUp = debugMinUp;
        int currUpLimit = adapter.getCurrentUploadLimit();

        int maxStep = Math.round(((float)(currUpLimit-minUp))* maxDownStepPercent);
        int minStep = 1000;
        int currStep=0;

        int newUpLimit = currUpLimit;
        if(trend>-16){
            newUpLimit = currUpLimit;
        }
        else if(trend==-16){
            currStep = -(maxStep/8);
        }
        else if(trend==-17){
            //newUpLimit = currUpLimit - (maxStep/4);
            currStep= - (maxStep/4);
        }
        else if(trend<=-18){
            currStep = - (maxStep);
        }

        if( Math.abs(currStep) < minStep){
            currStep = minStep;
        }

        newUpLimit+=currStep;

        if(newUpLimit<minUp){
            newUpLimit = minUp;
        }

        //To nearest K value.
        newUpLimit = (( newUpLimit + 1023 )/1024) * 1024;

        return newUpLimit;
    }//decreaseUploadLimit

}
