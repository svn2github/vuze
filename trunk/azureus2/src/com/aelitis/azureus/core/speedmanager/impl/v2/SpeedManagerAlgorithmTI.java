package com.aelitis.azureus.core.speedmanager.impl.v2;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.speedmanager.SpeedManagerPingSource;
import com.aelitis.azureus.core.speedmanager.impl.SpeedManagerAlgorithmProvider;
import com.aelitis.azureus.core.speedmanager.impl.SpeedManagerAlgorithmProviderAdapter;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.COConfigurationListener;
import org.gudy.azureus2.core3.util.SystemTime;



/**
 * Created on Jul 5, 2007
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

public class SpeedManagerAlgorithmTI
        implements SpeedManagerAlgorithmProvider
    {
        //private static final byte VIVALDI_VERSION = DHTNetworkPosition.POSITION_TYPE_VIVALDI_V1;

        private SpeedManagerAlgorithmProviderAdapter adapter;
        private PluginInterface dhtPlugin;


        private long timeSinceLastUpdate;

        private int consecutiveUpticks=0;
        private int consecutiveDownticks=0;

        //SpeedLimitMonitor
        private static SpeedLimitMonitor limitMonitor = new SpeedLimitMonitor();


        //for managing ping sources.
        PingSourceManager pingSourceManager = new PingSourceManager();

        private static boolean skipIntervalAfterAdjustment;
        private static boolean hadAdjustmentLastInterval;
        private static int numIntervalsBetweenCal;
        int intervalCount=0;

        float lastMetric = 0.0f;

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

                                }else{
                                    //DHT Ping is data source

                                    skipIntervalAfterAdjustment=COConfigurationManager.getBooleanParameter(
                                            SpeedManagerAlgorithmProviderV2.SETTING_WAIT_AFTER_ADJUST);
                                    numIntervalsBetweenCal=COConfigurationManager.getIntParameter(
                                            SpeedManagerAlgorithmProviderV2.SETTING_INTERVALS_BETWEEN_ADJUST);

                                }

//                                limitMonitor.initPingSpaceMap(metricGoodResult+metricGoodTolerance,metricBadResult-metricBadTolerance);

                            }catch( Throwable t ){
                                SpeedManagerLogger.log(t.getMessage());
                            }

                        }//configurationSaved
                    }
            );
        }//static

        SpeedManagerAlgorithmTI(SpeedManagerAlgorithmProviderAdapter _adapter){

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

//            limitMonitor.initPingSpaceMap(metricGoodResult+metricGoodTolerance,metricBadResult-metricBadTolerance);
        }

        /**
         * Reset any state to start of day values
         */

        public void reset() {
            log("reset");

            log("curr-data: curr-down-rate : curr-down-limit : down-capacity : down-bandwith-mode : down-limit-mode : curr-up-rate : curr-up-limit : up-capacity : upload-bandwidth-mode : upload-limit-mode : up-down-ratio : transfer-mode");

            log("new-limit:newLimit:currStep:signalStrength:multiple:currUpLimit:maxStep:uploadLimitMax:uploadLimitMin:transferMode" );

            log("consecutive:up:down");

            log("metric:value:type");

            log("user-comment:log");

            log("pin:upload-status,download-status,upload-unpin-timer,download-unpin-timer");

            log("limits:down-max:down-min:down-conf:up-max:up-min:up-conf");

            limitMonitor.resetPingSpace();
        }

        /**
         * Called periodically (see period above) to allow stats to be updated.
         */

        public void updateStats() {

            //update some stats used in the UI.

            int currUploadLimit = adapter.getCurrentUploadLimit();
            int currDataUploadSpeed = adapter.getCurrentDataUploadSpeed();
            int currProtoUploadSpeed = adapter.getCurrentProtocolUploadSpeed();
            int upRateBitsPerSec = currDataUploadSpeed + currProtoUploadSpeed;

            int currDownLimit = adapter.getCurrentDownloadLimit();
            int downDataRate = adapter.getCurrentDataDownloadSpeed();
            int downProtoRate = adapter.getCurrentProtocolDownloadSpeed();
            int downRateBitsPerSec = downDataRate+downProtoRate;

            //update the bandwidth status
            limitMonitor.setDownloadBandwidthMode(downRateBitsPerSec,currDownLimit);
            limitMonitor.setUploadBandwidthMode(upRateBitsPerSec,currUploadLimit);

            //update the limts status.  (is it near a forced max or min?)
            limitMonitor.setDownloadLimitSettingMode(currDownLimit);
            limitMonitor.setUploadLimitSettingMode(currUploadLimit);

            limitMonitor.updateTransferMode();

            if( limitMonitor.isConfTestingLimits() ){
                limitMonitor.updateLimitTestingData(downRateBitsPerSec,upRateBitsPerSec);
            }

            //update ping maps
            limitMonitor.setCurrentTransferRates(downRateBitsPerSec,upRateBitsPerSec);

            //"curr-data" ....
            logCurrentData(downRateBitsPerSec, currDownLimit, upRateBitsPerSec, currUploadLimit);
        }

        /**
         * log "curr-data" line to the AutoSpeed-Beta file.
         * @param downRate -
         * @param currDownLimit -
         * @param upRate -
         * @param currUploadLimit -
         */
        private void logCurrentData(int downRate, int currDownLimit, int upRate, int currUploadLimit) {
            StringBuffer sb = new StringBuffer("curr-data:"+downRate+":"+currDownLimit+":");
            sb.append( limitMonitor.getDownloadLineCapacity() ).append(":");
            sb.append(limitMonitor.getDownloadBandwidthMode()).append(":");
            sb.append(limitMonitor.getDownloadLimitSettingMode()).append(":");
            sb.append(upRate).append(":").append(currUploadLimit).append(":");
            sb.append( limitMonitor.getUploadLineCapacity() ).append(":");
            sb.append(limitMonitor.getUploadBandwidthMode()).append(":");
            sb.append(limitMonitor.getUploadLimitSettingMode()).append(":");
            sb.append( limitMonitor.getUpDownRatio() ).append(":");
            sb.append(limitMonitor.getTransferModeAsString());

            SpeedManagerLogger.log( sb.toString() );
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
            pingSourceManager.pingSourceFound(source, is_replacement);
        }

        /**
         * Ping source has failed
         *
         * @param source -
         */

        public void pingSourceFailed(SpeedManagerPingSource source) {
            //Where does the vivaldi data for the chart come from.
            log("pingSourceFailed");

            pingSourceManager.pingSourceFailed(source);
        }

        public void calculate( TestInterface tf ){

            long currTime = SystemTime.getCurrentTime();

            if( timeSinceLastUpdate==0 ){
                timeSinceLastUpdate=currTime;
            }


            if ( calculateMediaDHTPingTime(tf) ){
                return;
            }


            logLimitStatus();

            float signalStrength = determineSignalStrength(lastMetric);

            //if are are NOT looking for limits and we have a signal then make an adjustment.
            if( signalStrength!=0.0f ){//&& !limitMonitor.isConfTestingLimits() ){
                //hadAdjustmentLastInterval=true;

                float multiple = consectiveMultiplier();
                int currUpLimit = adapter.getCurrentUploadLimit();
                int currDownLimit = adapter.getCurrentDownloadLimit();

                //NOTE: int[2] with only max  upload/download   limits.
                int[] limits = tf.getLimits();

                SpeedLimitMonitor.Update update = modifyLimits(signalStrength,multiple,currUpLimit,currDownLimit, limits);

                //log
                logNewLimits(update);

                //setting new
                setNewLimits( update );

            }else{
                //hadAdjustmentLastInterval=false;

                //verify the limits. It is possible for the user to adjust the capacity down below the current limit, so check that condition here.
                int currUploadLimit = adapter.getCurrentUploadLimit();
                int currDownloadLimit = adapter.getCurrentDownloadLimit();
                if( !limitMonitor.areSettingsInSpec(currUploadLimit, currDownloadLimit) ){
                    SpeedLimitMonitor.Update update = limitMonitor.adjustLimitsToSpec(currUploadLimit, currDownloadLimit);
                    logNewLimits( update );
                    setNewLimits( update );
                }

            }


        }//

        //NOTE:  we need to move the LimitSlider to here!!
        static LimitSlider slider;
        static{
            slider = new LimitSlider();
        }

        SpeedLimitMonitor.Update modifyLimits(float signalStrength, float multiple, int currUpLimit, int currDownLimit, int[] limits){

            int uploadLimitMax = limits[TestInterface.UPLOAD_MAX_INDEX];
            int downloadLimitMax = limits[TestInterface.DOWNLOAD_MAX_INDEX];

            SaturatedMode uploadLimitSettingStatus = limitMonitor.getUploadLimitSettingMode();
            SaturatedMode downloadLimitSettingStatus = limitMonitor.getDownloadLimitSettingMode();

            //Mapper is now trying to determine the limits.
            limitMonitor.setRefLimits(uploadLimitMax,downloadLimitMax);

            int downloadLimitMin = limitMonitor.getDownloadMinLimit();
            int uploadLimitMin = limitMonitor.getUploadMinLimit();

            slider.updateLimits(uploadLimitMax,uploadLimitMin,
                    downloadLimitMax,downloadLimitMin);
            slider.updateStatus(currUpLimit,uploadLimitSettingStatus,
                    currDownLimit, downloadLimitSettingStatus);

            return slider.adjust( signalStrength*multiple );

        }//modifyLimits


        /**
         * Called whenever a new set of ping values is available for processing
         *
         * @param sources -
         */
        static TestInterface testIfc = new TestInterfaceImpl();
        
        public void calculate(SpeedManagerPingSource[] sources) {

            //turn the source values into -1 to +1
            calculate( testIfc );

        }



        /**
         * Log the limit status. Max, Min and Conf.
         * log("limits:down-max:down-min:down-conf:up-max:up-min:up-conf");
         */
        private void logLimitStatus(){

            StringBuffer msg = new StringBuffer();
            msg.append("limits:");
            msg.append(limitMonitor.getUploadLineCapacity()).append(":");
            msg.append(limitMonitor.getUploadMinLimit()).append(":");
            msg.append(limitMonitor.getUploadConfidence()).append(":");
            msg.append(limitMonitor.getDownloadLineCapacity()).append(":");
            msg.append(limitMonitor.getDownloadMinLimit()).append(":");
            msg.append(limitMonitor.getDownloadConfidence());

            SpeedManagerLogger.log( msg.toString() );
        }//logLimitStatus


        /**
         * DHT Ping data is one of the metrics used. Calculate it here.
         * @param tf
         * @return - true if should exit early from the caluculate method.
         */
        private boolean calculateMediaDHTPingTime( TestInterface tf ) {
            //Don't count this data point, if we skip the next ping times after an adjustment.
            if(skipIntervalAfterAdjustment && hadAdjustmentLastInterval){
                hadAdjustmentLastInterval=false;
                intervalCount=0;
                return true;
            }

            //have we accululated enough data to make an adjustment?
            if( intervalCount < numIntervalsBetweenCal ){
                //get more data before making another calculation.
                intervalCount++;
                return true;
            }

            //we have enough data. find the median ping time.
            lastMetric = tf.getCurrentMetric();


            //we have now consumed this data. reset the counters.
            intervalCount=0;
            return false;
        }

        private void logNewLimits(SpeedLimitMonitor.Update update) {
            if( update.hasNewUploadLimit ){
                int kbpsUpoadLimit = update.newUploadLimit/1024;
                log(" new up limit  : "+ kbpsUpoadLimit +" kb/s");
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


        /**
         *
         * @param testMetric - float -1.0f   to   +1.0f
         * @return  signal as float with 0.0 meaning don't make an adjustment.
         */
        private float convertTestMetricToSignal( float testMetric ){

            if( testMetric>=1.0f ){
                return 1.0f;
            }
            if(testMetric<=-1.0f){
                return -1.0f;
            }

            //here we will map the neutral region to -0.5f to +0.5f   to singal = 0;
            if( testMetric>-0.5f && testMetric<0.5f ){
                return 0.0f;
            }

            //map weak up signal.
            if( testMetric > 0.0f ){
                return ( (testMetric - 0.5f) * 2.0f );
            }

            //map weak down signal
            return ( ( testMetric+0.5f ) * 2.0f );
        }

        private float determineSignalStrength(float lastMetric){
            //determine if this is an up-tick (+1), down-tick (-1) or neutral (0).

            //  range should be -1.0 (bad) to +1.0 (good)
            float signal = convertTestMetricToSignal( lastMetric );

            if( signal > 0.0f){
                consecutiveUpticks++;
                consecutiveDownticks=0;
            }else if(signal < 0.0f ){
                consecutiveUpticks=0;
                consecutiveDownticks++;
            }else{
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

            //return lastMetricValue;
            return 43;

        }//getIdlePingMillis

        public int getCurrentPingMillis() {
            return 0;
        }

        public int getMaxPingMillis() {
            return 913;  //Currently a fixed number to be sure of algorightm.
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

        public boolean getAdjustsDownloadLimits() {
            // TODO Auto-generated method stub
            return true;
        }
        protected void log(String str){

            SpeedManagerLogger.log(str);
        }//log


 /**********  static classes below here  ****************/

        /**
         * Imported from SpeedLimitMonitor
         */
    static class LimitSlider{
        private float valueUp=0.5f;//number between 0.0 - 1.0
        int upMax;
        int upCurr;
        int upMin;
        SaturatedMode upUsage;

        private float valueDown=1.0f;
        int downMax;
        int downCurr;
        int downMin;
        SaturatedMode downUsage;


        public void updateStatus(int currUpLimit, SaturatedMode uploadUsage, int currDownLimit, SaturatedMode downloadUsage){
            upCurr = currUpLimit;
            upUsage = uploadUsage;
            downCurr = currDownLimit;
            downUsage = downloadUsage;
        }


        public void updateLimits(int _upMax, int _upMin, int _downMax, int _downMin){
            upMax = _upMax;
            upMin = _upMin;
            downMax = _downMax;
            downMin = _downMin;
        }


        public SpeedLimitMonitor.Update adjust( float amount ){

            boolean increase = true;
            if( amount<0.0f ){
                increase = false;
            }

            float factor = amount/10.0f;

            if( increase ){
                //increase download first
                if( valueDown<0.99f ){
                    valueDown = calculateNewValue(valueDown,factor);
                }else{
                    valueUp = calculateNewValue(valueUp,factor);
                }
            }else{
                //decrease upload first
                if( valueUp > 0.01f){
                    valueUp = calculateNewValue(valueUp,factor);
                }else{
                    valueDown = calculateNewValue(valueDown,factor);
                }
            }

            return update();

        }//adjust

        private SpeedLimitMonitor.Update update(){
            int upLimit;
            int downLimit;

            upLimit = Math.round( ((upMax-upMin)*valueUp)+upMin );
            downLimit = Math.round( ((downMax-downMin)*valueDown)+downMin );

            //log this change.
            String msg = " create-update: valueUp="+valueUp+",upLimit="+upLimit+",valueDown="+valueDown
                    +",downLimit="+downLimit+",upMax="+upMax+",upMin="+upMin+",downMax="+downMax
                    +",downMin="+downMin;
            SpeedManagerLogger.log( msg );

            return new SpeedLimitMonitor.Update(upLimit,true,downLimit,true);
        }

        private float calculateNewValue(float curr, float amount){
            curr += amount;
            if( curr > 1.0f){
                curr = 1.0f;
            }
            if( curr < 0.0f ){
                curr = 0.0f;
            }
            return curr;
        }

    }//LimitSlider

}
