package com.aelitis.azureus.core.speedmanager.impl;

import com.aelitis.azureus.core.speedmanager.SpeedManagerPingSource;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.util.average.Average;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.control.DHTControlContact;
import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.COConfigurationListener;

import java.util.*;

/**
 * Created on May 14, 2007
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
 * This class implements an Vivaldi strategy for adjusting the upload limit.
 * It will every 30 seconds calculate the distance to the center of the
 * Vivaldi structure and if it determines that it is going away from it will
 * lower the limit. If it getting closer it will increase the upload limit.
 */

public class SpeedManagerAlgorithmProviderVivaldi
    implements SpeedManagerAlgorithmProvider
{
	private static final byte VIVALDI_VERSION = DHTNetworkPosition.POSITION_TYPE_VIVALDI_V1;
	
    private SpeedManagerAlgorithmProviderAdapter adapter;
    private PluginInterface dhtPlugin;

    //private AEDiagnosticsLogger dLog = AEDiagnostics.getLogger("v3.AutoSpeed_Beta_Debug");

    private long timeSinceLastUpdate;
    private static final long VIVALDI_TIME_BETWEEN_UPDATES = 15000;

    //metric values for DHT Ping times and Vivaldi
    private static int metricGoodResult = 100;
    private static int metricGoodTolerance = 300;
    private static int metricBadResult = 1300;
    private static int metricBadTolerance = 300;
    private static boolean useVivaldi = false;


    private int consecutiveUpticks=0;
    private int consecutiveDownticks=0;

    //SpeedLimitMonitor
    private static SpeedLimitMonitor limitMonitor = new SpeedLimitMonitor();

    //variables for display and vivaldi.
    private int lastMetricValue;

    //use for DHT ping.
    private static int numIntervalsBetweenCal = 2;
    private static boolean skipIntervalAfterAdjustment = true;

    private List pingTimeList = new ArrayList(); //<Integer>
    private boolean hadAdjustmentLastInterval = false;
    private int intervalCount = 0;

    //for managing ping sources.
    private final Map pingAverages = new HashMap(); //<Source,PingSourceStats>
    private long lastPingRemoval=0;
    private static final long TIME_BETWEEN_REMOVALS = 5 * 60000; //five minutes.


    static{
        COConfigurationManager.addListener(
                new COConfigurationListener(){
                    public void configurationSaved(){

                        try{

                            limitMonitor.updateFromCOConfigManager();

                            String mode=COConfigurationManager.getStringParameter( SpeedManagerAlgorithmProviderV2.SETTING_DATA_SOURCE_INPUT );
                            //what mode are we?
                            if( SpeedManagerAlgorithmProviderV2.VALUE_SOURCE_VIVALDI.equals(mode) )
                            {
                                //Vivadi is data source
                                useVivaldi = true;
                                metricGoodResult =COConfigurationManager.getIntParameter(
                                        SpeedManagerAlgorithmProviderV2.SETTING_VIVALDI_GOOD_SET_POINT);
                                metricGoodTolerance =COConfigurationManager.getIntParameter(
                                        SpeedManagerAlgorithmProviderV2.SETTING_VIVALDI_GOOD_TOLERANCE);
                                metricBadResult =COConfigurationManager.getIntParameter(
                                        SpeedManagerAlgorithmProviderV2.SETTING_VIVALDI_BAD_SET_POINT);
                                metricBadTolerance =COConfigurationManager.getIntParameter(
                                        SpeedManagerAlgorithmProviderV2.SETTING_VIVALDI_BAD_TOLERANCE);
                            }else{
                                //DHT Ping is data source
                                useVivaldi = false;
                                metricGoodResult =COConfigurationManager.getIntParameter(
                                        SpeedManagerAlgorithmProviderV2.SETTING_DHT_GOOD_SET_POINT);
                                metricGoodTolerance =COConfigurationManager.getIntParameter(
                                        SpeedManagerAlgorithmProviderV2.SETTING_DHT_GOOD_TOLERANCE);
                                metricBadResult =COConfigurationManager.getIntParameter(
                                        SpeedManagerAlgorithmProviderV2.SETTING_DHT_BAD_SET_POINT);
                                metricBadTolerance =COConfigurationManager.getIntParameter(
                                        SpeedManagerAlgorithmProviderV2.SETTING_DHT_BAD_TOLERANCE);

                                skipIntervalAfterAdjustment=COConfigurationManager.getBooleanParameter(
                                        SpeedManagerAlgorithmProviderV2.SETTING_WAIT_AFTER_ADJUST);
                                numIntervalsBetweenCal=COConfigurationManager.getIntParameter(
                                        SpeedManagerAlgorithmProviderV2.SETTING_INTERVALS_BETWEEN_ADJUST);

                            }

                        }catch( Throwable t ){
                            SpeedManagerLogger.log(t.getMessage());
                        }

                    }//configurationSaved
                }
        );
    }//static

    SpeedManagerAlgorithmProviderVivaldi(SpeedManagerAlgorithmProviderAdapter _adapter){

        adapter = _adapter;

        try{
            dhtPlugin = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByClass( DHTPlugin.class );
        }catch(AzureusCoreException ace){
            log("Warning: AzureusCore was not initialized on startup.");
        }

        if( dhtPlugin==null ){
            //Need to log this condition if it ever happens!!
            log(" Error: failed to get DHT Plugin ");
        }//if

    }

    /**
     * Reset any state to start of day values
     */

    public void reset() {
        log("reset");

        log("curr-data: curr-down-rate : curr-down-limit : down-bandwith-mode : down-limit-mode : curr-up-rate : curr-up-limit : upload-bandwidth-mode : upload-limit-mode");

        log( "new-limit:newLimit:currStep:signalStrength:multiple:currUpLimit:maxStep:uploadLimitMax:uploadLimitMin" );

        log("consecutive:up:down");

        log("metric:value:type");

        log("user-comment:log");

        //log("pin:")
    }

    /**
     * Called periodically (see period above) to allow stats to be updated.
     */

    public void updateStats() {

        //update some stats used in the UI.

        int currUploadLimit = adapter.getCurrentUploadLimit();
        int currDataUploadSpeed = adapter.getCurrentDataUploadSpeed();
        int currProtoUploadSpeed = adapter.getCurrentProtocolUploadSpeed();
        int upRate = currDataUploadSpeed + currProtoUploadSpeed;

        int currDownLimit = adapter.getCurrentDownloadLimit();
        int downDataRate = adapter.getCurrentDataDownloadSpeed();
        int downProtoRate = adapter.getCurrentProtocolDownloadSpeed();
        int downRate = downDataRate+downProtoRate;

        //update the bandwidth status
        limitMonitor.setDownloadBandwidthMode(downRate,currDownLimit);
        limitMonitor.setUploadBandwidthMode(currDataUploadSpeed,currUploadLimit);


        //update the limts status.  (is it near a forced max or min?)
        limitMonitor.setDownloadLimitSettingMode(currDownLimit);
        limitMonitor.setUploadLimitSettingMode(currUploadLimit);

        StringBuffer sb = new StringBuffer("curr-data:"+downRate+":"+currDownLimit+":");
        sb.append(limitMonitor.getDownloadBandwidthMode()).append(":");
        sb.append(limitMonitor.getDownloadLimitSettingMode()).append(":");
        sb.append(upRate).append(":").append(currUploadLimit).append(":");
        sb.append(limitMonitor.getUploadBandwidthMode()).append(":");
        sb.append(limitMonitor.getUploadLimitSettingMode()).append(":");

        log( sb.toString() );
    }

    /**
     * Called when a new source of ping times has been found
     *
     * @param source -
     * @param is_replacement One of the initial sources or a replacement for a failed one
     */

    public void pingSourceFound(SpeedManagerPingSource source, boolean is_replacement) {

        //We might not use ping source if the vivaldi data is available.
        log("pingSourceFound");

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
        //Where does the vivaldi data for the chart come from.
        log("pingSourceFailed");

        if( pingAverages.remove(source)==null){
            log("didn't find source: "+source.getAddress().getHostName());
        }
    }

    /**
     * Called whenever a new set of ping values is available for processing
     *
     * @param sources -
     */

    public void calculate(SpeedManagerPingSource[] sources) {
        //Get the vivaldi chart data and calculate a center of mass.
        //Look at

        int len = sources.length;
        for(int i=0; i<len; i++){
            PingSourceStats pss = (PingSourceStats) pingAverages.get(sources[i]);
            int pingTime = sources[i].getPingTime();

            //exclude ping-times of -1 which mess up the averages.
            if(pingTime>0){
                pss.addPingTime( sources[i].getPingTime() );

                pingTimeList.add( new Integer( sources[i].getPingTime() ) );
                intervalCount++;
            }//if
        }//for

        long currTime = SystemTime.getCurrentTime();

        if( timeSinceLastUpdate==0 ){
            timeSinceLastUpdate=currTime;
        }

        //If Vivaldi calculate results on a time interval.
        if( timeSinceLastUpdate+ VIVALDI_TIME_BETWEEN_UPDATES > currTime && useVivaldi){
            //still waiting for the next time to update the value.
            log("calculate-deferred");
            return;
        }


        log("calculate");


        if(!useVivaldi){
            //use the DHT ping times instead.

            //Don't count this data point, if we skip the next ping times after an adjustment.
            if(skipIntervalAfterAdjustment && hadAdjustmentLastInterval){
                hadAdjustmentLastInterval=false;
                pingTimeList = new ArrayList();
                intervalCount=0;
                return;
            }

            //have we accululated enough data to make an adjustment?
            if( intervalCount < numIntervalsBetweenCal ){
                //get more data before making another calculation.
                return;
            }

            //we have enough data. find the median ping time.
            Collections.sort( pingTimeList );

            //if we don't have any pings, then either the connection is lost or very bad network congestion.
            //force an adjustment down.
            if( pingTimeList.size()==0 ){
                lastMetricValue =10000;  //ToDo: This is a high value to force an adjusment down.
            }else{
                int medianIndex = pingTimeList.size()/2;

                Integer medianPingTime = (Integer) pingTimeList.get(medianIndex);
                lastMetricValue = medianPingTime.intValue();
            }

            //we have now consumed this data. reset the counters.
            intervalCount=0;
            pingTimeList = new ArrayList();

        }else{
            //if we have not initialized the core yet, then try now.
            if(dhtPlugin==null){
                try{
                    dhtPlugin = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByClass( DHTPlugin.class );
                }catch(AzureusCoreException ace){
                    log("Warning: AzureusCore was not initialized on startup.");
                    return;
                }
            }//if

            DHT[] dhts = ((DHTPlugin)dhtPlugin.getPlugin()).getDHTs();

            if(dhts==null){
                log("No DHTs to process try later");
                return;
            }

            if(dhts.length<2){
                log("Not enough DHT nodes. length="+dhts.length);
                return;
            }

            int currVivaldiDistance = calculateMedianVivaldiDistance(dhts);

            if(lastMetricValue == currVivaldiDistance){
                //don't use this result.
                log("vivaldi not updated don't use this data.");
                return;
            }
            timeSinceLastUpdate = currTime;

            lastMetricValue = currVivaldiDistance;
        }
        log("metric:"+ lastMetricValue);

        float signalStrength = determineSignalStrength(lastMetricValue);
        if( signalStrength!=0.0f ){
            hadAdjustmentLastInterval=true;

            float multiple = consectiveMultiplier();
            int currUpLimit = adapter.getCurrentUploadLimit();

            limitMonitor.checkForUnpinningCondition();
            SpeedLimitMonitor.Update update = limitMonitor.createNewLimitEx(signalStrength,multiple,currUpLimit);

            //log
            logNewLimits(update);

            //setting new
            setNewLimits( update );

        }else{
            hadAdjustmentLastInterval=false;
        }

        //determine if we need to drop a ping source.
        checkPingSources(sources);
    }

    private void logNewLimits(SpeedLimitMonitor.Update update) {
        if( update.hasNewUploadLimit ){
            int kbpsUpoadLimit = update.newUploadLimit/1024;
            log(" setting new limit to: "+ kbpsUpoadLimit +" kb/s");
        }

        if( update.hasNewDownloadLimit ){
            int kpbsDownloadLimit = update.newDownloadLimit/1024;
            log(" new down limit: "+kpbsDownloadLimit+" kb/s");
        }
    }

    /**
     * Just update the limits.
     * @param update - SpeedLimitMonitor.Update
     */
    private void setNewLimits( SpeedLimitMonitor.Update update ){

        adapter.setCurrentUploadLimit( update.newUploadLimit );
        adapter.setCurrentDownloadLimit( update.newDownloadLimit );

    }


    private static int calculateMedianVivaldiDistance(DHT[] dhts) {
        DHT forSelf = dhts[dhts.length-1];
        DHTTransportContact c = forSelf.getControl().getTransport().getLocalContact();

        DHTNetworkPosition ownLocation = c.getNetworkPosition( VIVALDI_VERSION );
 
        List l = forSelf.getControl().getContacts();
        Iterator itr = l.iterator();
        List forMedian = new ArrayList( l.size() );//List<Float>
        
        while( itr.hasNext() ){
        	
            DHTControlContact contact = (DHTControlContact) itr.next();
            
            DHTNetworkPosition _pos = contact.getTransportContact().getNetworkPosition( VIVALDI_VERSION );
            
            if( _pos==null ){
                continue;
            }
            
            float rtt = ownLocation.estimateRTT( _pos );
            
            if( !Float.isNaN( rtt )){
               
                forMedian.add( new Float( rtt ));
            }
        }

        Collections.sort(forMedian);

        //The mean value should be in middle value. (Is this any more meaningful the ave. ping time?)
        int size = forMedian.size();
        Float meanDistance = (Float) forMedian.get( size/2 );

        //We now have meanDistance!!! use it to set the upload limit!!!
        return Math.round( meanDistance.floatValue() );
    }


    /**
     * Determine if we should drop any ping sources.
     * Sort them, if one significantly higher then the other two. then drop it.
     * @param sources - SpeedManagerPingSource[] inputs
     */
    private void checkPingSources(SpeedManagerPingSource[] sources){

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
                log("dropping ping source: "+highestSource.getAddress()+" for being 10x greater then min source.");
                highestSource.destroy();
            }
        }//if

    }//checkPingSources

    /**
     * Determined by the vivaldi value and the number of consecutive calculations
     * with the same result.
     * @param currMetricValue -
     * @return -
     */
    private float determineSignalStrength(int currMetricValue){

        //determine if this is an up-tick (+1), down-tick (-1) or neutral (0).
        float signal=0.0f;
        if( currMetricValue< metricGoodResult){
            //strong up signal.
            signal=1.0f;
            consecutiveUpticks++;
            consecutiveDownticks=0;
        }
        else if( currMetricValue < (metricGoodResult + metricGoodTolerance)){
            //weak up signal.
            signal = (float)(currMetricValue- metricGoodResult)/ metricGoodTolerance;

            consecutiveUpticks++;
            consecutiveDownticks=0;
        }
        else if( currMetricValue > metricBadResult){
            //strong down signal
            signal = -1.0f;
            consecutiveUpticks=0;
            consecutiveDownticks++;
        }
        else if( currMetricValue > (metricBadResult - metricBadTolerance) ){
            //weak down signal
            consecutiveUpticks=0;
            consecutiveDownticks++;

            int lowerBound= metricBadResult - metricBadTolerance;
            signal = (currMetricValue-lowerBound) / metricBadTolerance;
            signal -= 1.0f;
        }
        else{
            //This is a neutral signal. 
        }

        log("consecutive:"+consecutiveUpticks+":"+consecutiveDownticks);

        return signal;
    }

    /**
     * The longer were get the same signal the stronger it is. On upticks however we only increase the
     * rates when if the upload or download is saturated.
     *
     * @return -
     */
    private float consectiveMultiplier(){

        float multiple;

        if( consecutiveUpticks > consecutiveDownticks ){

            //Set the consecutive upticks back to zero if the bandwidth is not being used.
            if( limitMonitor.bandwidthUsageLow() ){
                consecutiveUpticks=0;
            }

            multiple = calculateUpTickMultiple(consecutiveUpticks);
        }else{
            multiple = calculateDownTickMultiple(consecutiveDownticks);
            limitMonitor.notifyOfDownSingal();
        }

        return multiple;
    }



    /**
     * Want to rise much slower then drop.
     * @param c - number of upsignals recieved in a row
     * @return - multiple factor.
     */
    private float calculateUpTickMultiple(int c) {

        float multiple=0.0f;
        
        if(c<0){
            return multiple;
        }

        switch(c){
            case 0:
            case 1:
                multiple=0.25f;
                break;
            case 2:
                multiple=0.5f;
                break;
            case 3:
                multiple=1.0f;
                break;
            case 4:
                multiple=1.25f;
                break;
            case 5:
                multiple=1.5f;
                break;
            case 6:
                multiple=1.75f;
                break;
            case 7:
                multiple=2.0f;
                break;
            case 8:
                multiple=2.25f;
                break;
            case 9:
                multiple=2.5f;
                break;
            default:
                multiple=3.0f;
        }//switch

        //decrease the signal strength if bandwith usage is only in MED use.
        if( limitMonitor.bandwidthUsageMedium() ){
            multiple /= 2.0f;
        }

        return multiple;
    }

    /**
     * Want to drop rate faster then increase.
     * @param c -
     * @return -
     */
    private float calculateDownTickMultiple(int c) {

        float multiple=0.0f;
        if(c<0){
            return multiple;
        }

        switch(c){
            case 0:
            case 1:
                multiple=0.25f;
                break;
            case 2:
                multiple=0.5f;
                break;
            case 3:
                multiple=1.0f;
                break;
            case 4:
                multiple=2.0f;
                break;
            case 5:
                multiple=3.0f;
                break;
            case 6:
                multiple=4.0f;
                break;
            case 7:
                multiple=6.0f;
                break;
            case 8:
                multiple=9.0f;
                break;
            case 9:
                multiple=15.0f;
                break;
            default:
                multiple=20.0f;
        }//switch
        return multiple;
    }


    /**
     * Various getters for interesting info shown in stats view
     *
     * @return -
     */
    public int getIdlePingMillis() {

        //return the vivaldi time.
        return lastMetricValue;
        
    }//getIdlePingMillis

    public int getCurrentPingMillis() {
        return 0;
    }

    public int getMaxPingMillis() {
        return 912;  //Currently a fixed number to be sure of algorightm.
    }

    /**
     * Returns the current view of when choking occurs
     *
     * @return speed in bytes/sec
     */

    public int getCurrentChokeSpeed() {
        return 0;
    }

    public int getMaxUploadSpeed() {
        return 3;
    }

    protected void log(String str){

        SpeedManagerLogger.log(str);
    }//log

}
