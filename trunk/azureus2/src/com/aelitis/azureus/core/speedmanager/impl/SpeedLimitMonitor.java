package com.aelitis.azureus.core.speedmanager.impl;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.SystemTime;


/**
 * Created on May 23, 2007
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
 * This class is responsible for re-adjusting the limits used by AutoSpeedV2.
 *
 * This class will keep track of the "status" (i.e. seeding, downloading)of the
 * application. It will then re-adjust the MAX limits when it thinks limits
 * are being reached.
 *
 * Here are the rules it will use.
 *
 * #1) When seeding. If the upload is AT_LIMIT for a period of time it will allow
 * that to adjust upward.
 * #2) When downloading. If the download is AT_LIMIT for a period of time it will
 * allow that to adjust upward.
 *
 * #3) When downloading, if a down-tick is detected and the upload is near a limit,
 * it will drop the upload limit to 80% of MAX_UPLOAD.
 *
 * #4) Once that limit is reached it will drop both the upload and download limits together.
 *
 * #5) Seeding mode is triggered when - download bandwidth at LOW - compared to CAPACITY for 5 minutes continously.
 *
 * #6) Download mode is triggered when - download bandwidth reaches MEDIUM - compared to CURRENT_LIMIT for the first time.
 *
 * Rules #5 and #6 favor downloading over seeding.
 *
 */

public class SpeedLimitMonitor
{

    //use for home network.
    private int uploadLinespeedCapacity = 38000;
    private int uploadLimitMin = 5000;
    private int downloadLinespeedCapacity = 80000;
    private int downloadLimitMin = 8000;

    private static float upDownRatio=2.0f;

    private TransferMode transferMode = new TransferMode();

    //Upload and Download bandwidth usage modes. Compare usage to current limit.
    private SaturatedMode uploadBandwidthStatus =SaturatedMode.NONE;
    private SaturatedMode downloadBandwidthStatus =SaturatedMode.NONE;

    //Compare current limit to max limit.
    private SaturatedMode uploadLimitSettingStatus=SaturatedMode.AT_LIMIT;
    private SaturatedMode downloadLimitSettingStatus=SaturatedMode.AT_LIMIT;

    //How much confidence to we have in the current limits?
    private SpeedLimitConfidence uploadLimitConf = SpeedLimitConfidence.NONE;
    private SpeedLimitConfidence downloadLimitConf = SpeedLimitConfidence.NONE;
    private long confLimitTestStartTime=-1;
    private boolean currTestDone;
    private boolean beginLimitTest;
    private int highestUploadRate=0;
    private int highestDownloadRate=0;
    private int preTestUploadSetting;
    private int preTestDownloadSetting;
    public static final String UPLOAD_CONF_LIMIT_SETTING="SpeedLimitMonitor.setting.upload.limit.conf";
    public static final String DOWNLOAD_CONF_LIMIT_SETTING="SpeedLimitMonitor.setting.download.limit.conf";
    private static final long CONF_LIMIT_TEST_LENGTH=1000*40;//ToDo: make this configurable.


    //these methods are used to see how high limits can go.
    private boolean isUploadMaxPinned=true;
    private boolean isDownloadMaxPinned=true; 
    private long uploadAtLimitStartTime =SystemTime.getCurrentTime();
    private long downloadAtLimitStartTime = SystemTime.getCurrentTime();

    private static final long TIME_AT_LIMIT_BEFORE_UNPINNING = 5 * 60 * 1000; //five minutes.//ToDo: make this configurable.
    //private static final long TIME_AT_LIMIT_BEFORE_UNPINNING = 1 * 60 * 1000; //ToDo: REMOVE THIS IS FOR TESTING ONLY.


    public SpeedLimitMonitor(){
        //
    }

    public void updateFromCOConfigManager(){

        uploadLinespeedCapacity = COConfigurationManager.getIntParameter(SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT);
        uploadLimitMin=COConfigurationManager.getIntParameter(SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MIN_LIMIT);
        downloadLinespeedCapacity =COConfigurationManager.getIntParameter(SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT);
        downloadLimitMin=COConfigurationManager.getIntParameter(SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MIN_LIMIT);

        //tie the upload and download ratios together.
        upDownRatio = ( (float) downloadLinespeedCapacity /(float) uploadLinespeedCapacity);
        COConfigurationManager.setParameter(
                SpeedManagerAlgorithmProviderV2.SETTING_V2_UP_DOWN_RATIO, upDownRatio);

        uploadLimitConf = SpeedLimitConfidence.parseString(
                COConfigurationManager.getStringParameter( SpeedLimitMonitor.UPLOAD_CONF_LIMIT_SETTING ));
        downloadLimitConf = SpeedLimitConfidence.parseString(
                COConfigurationManager.getStringParameter( SpeedLimitMonitor.DOWNLOAD_CONF_LIMIT_SETTING));

    }

    //SpeedLimitMonitorStatus

    public float getUpDownRatio(){
        return upDownRatio;
    }


    public void setDownloadBandwidthMode(int rate, int limit){
        downloadBandwidthStatus = SaturatedMode.getSaturatedMode(rate,limit);
    }

    public void setUploadBandwidthMode(int rate, int limit){
        uploadBandwidthStatus = SaturatedMode.getSaturatedMode(rate,limit);
    }

    public void setDownloadLimitSettingMode(int currLimit){
        downloadLimitSettingStatus = SaturatedMode.getSaturatedMode(currLimit, downloadLinespeedCapacity);
    }

    public void setUploadLimitSettingMode(int currLimit){
        uploadLimitSettingStatus = SaturatedMode.getSaturatedMode(currLimit, uploadLinespeedCapacity);
    }

    public SaturatedMode getDownloadBandwidthMode(){
        return downloadBandwidthStatus;
    }

    public SaturatedMode getUploadBandwidthMode(){
        return uploadBandwidthStatus;
    }

    public SaturatedMode getDownloadLimitSettingMode(){
        return downloadLimitSettingStatus;
    }

    public SaturatedMode getUploadLimitSettingMode(){
        return uploadLimitSettingStatus;
    }

    public void updateTransferMode(){
            
        transferMode.updateStatus( downloadBandwidthStatus );
    }

    public String getTransferModeAsString(){
        return transferMode.getString();
    }


    /**
     * Are both the upload and download bandwidths usages is low?
     * Otherwise false.
     * @return -
     */
    public boolean bandwidthUsageLow(){

        if( uploadBandwidthStatus.compareTo(SaturatedMode.LOW)<=0 &&
                downloadBandwidthStatus.compareTo(SaturatedMode.LOW)<=0){

            return true;

        }

        //Either upload or download is at MEDIUM or above.
        return false;
    }

    /**
     *
     * @return -
     */
    public boolean bandwidthUsageMedium(){
        if( uploadBandwidthStatus.compareTo(SaturatedMode.MED)<=0 &&
                downloadBandwidthStatus.compareTo(SaturatedMode.MED)<=0){
            return true;
        }

        //Either upload or download is at MEDIUM or above.
        return false;
    }

    /**
     * True if both are at limits.
     * @return - true only if both the upload and download usages are at the limits.
     */
    public boolean bandwidthUsageAtLimit(){
        if( uploadBandwidthStatus.compareTo(SaturatedMode.AT_LIMIT)==0 &&
                downloadBandwidthStatus.compareTo(SaturatedMode.AT_LIMIT)==0){
            return true;
        }
        return false;
    }

    /**
     * Here we need to handle several cases.
     * (a) If the download bandwidth is HIGH, then we need to back off on the upload limit to 80% of max.
     * (b) If upload bandwidth and limits are AT_LIMIT for a period of time then need to "unpin" that max limit
     *      to see how high it will go.
     * (c) If the download bandwidth and limits are AT_LIMIT for a period of time then need to "unpin" the max
     *      limit to see how high it will go.
     *
     *
     *
     * @param signalStrength -
     * @param multiple -
     * @param currUpLimit -
     * @return -
     */
    public Update createNewLimit(float signalStrength, float multiple, int currUpLimit){

        //this flag is set in a previous method.
        if( isStartLimitTestFlagSet() ){
            return startLimitTesting();
        }

        int newLimit;

        int usedUploadLimit = uploadLinespeedCapacity;
        float usedUpDownRatio = upDownRatio;
        if( transferMode.isDownloadMode() ){
            usedUploadLimit = Math.round( 0.8f * uploadLinespeedCapacity );
            //re-calculate the up-down ratio.
            usedUpDownRatio = ( (float) downloadLinespeedCapacity /(float) usedUploadLimit);
        }

        //The amount to move it against the new limit is.
        float multi = Math.abs( signalStrength * multiple * 0.3f );

        //If we are in an unpinned limits mode then consider consider
        if( !isUploadMaxPinned || !isDownloadMaxPinned ){
            //we are in a mode that is moving the limits.
            return calculateNewUnpinnedLimits(signalStrength);
        }//if

        //Force the value to the limit.
        if(multi>1.0f){
            if( signalStrength>0.0f ){
                log("forcing: max upload limit.");
                int newDownloadLimit = Math.round( usedUploadLimit*usedUpDownRatio );
                return new Update(usedUploadLimit, true,newDownloadLimit, true);
            }else{
                log("forcing: min upload limit.");
                int newDownloadLimit = Math.round( uploadLimitMin*usedUpDownRatio );
                return new Update(uploadLimitMin, true, newDownloadLimit, true);
            }
        }

        //don't move it all the way.
        int maxStep;
        int currStep;
        int minStep=1024;

        if(signalStrength>0.0f){
            maxStep = Math.round( usedUploadLimit -currUpLimit );
        }else{
            maxStep = Math.round( currUpLimit- uploadLimitMin);
        }

        currStep = Math.round(maxStep*multi);
        if(currStep<minStep){
            currStep=minStep;
        }

        if( signalStrength<0.0f ){
            currStep = -1 * currStep;
        }

        newLimit = currUpLimit+currStep;
        newLimit = (( newLimit + 1023 )/1024) * 1024;

        if(newLimit> usedUploadLimit){
            newLimit= usedUploadLimit;
        }
        if(newLimit< uploadLimitMin){
            newLimit= uploadLimitMin;
        }


        log( "new-limit:"+newLimit+":"+currStep+":"+signalStrength+":"+multiple+":"+currUpLimit+":"+maxStep+":"+ usedUploadLimit +":"+uploadLimitMin );

        int newDownloadLimit = Math.round( newLimit*usedUpDownRatio );
        return new Update(newLimit, true, newDownloadLimit, true );
    }

    /**
     * Log debug info needed during beta period.
     */
    private void logPinningInfo() {
        StringBuffer sb = new StringBuffer("pin: ");
        if(isUploadMaxPinned){
            sb.append("ul-pinned:");
        }else{
            sb.append("ul-unpinned:");
        }
        if(isDownloadMaxPinned){
            sb.append("dl-pinned:");
        }else{
            sb.append("dl-unpinned:");
        }
        long currTime = SystemTime.getCurrentTime();
        long upWait = currTime - uploadAtLimitStartTime;
        long downWait = currTime - downloadAtLimitStartTime;
        sb.append(upWait).append(":").append(downWait);
        log( sb.toString() );
    }

    /**
     *
     * @param signalStrength -
     * @return -
     */
    private Update calculateNewUnpinnedLimits(float signalStrength){

        //first verify that is this is an up signal.
        if(signalStrength<0.0f){
            //down-tick is a signal to stop moving the files up.
            isUploadMaxPinned=true;
            isDownloadMaxPinned=true;
        }//if

        //just verify settings to make sure everything is sane before updating.
        boolean updateUpload=false;
        boolean updateDownload=false;

        if( uploadBandwidthStatus.compareTo(SaturatedMode.AT_LIMIT)==0 &&
                uploadLimitSettingStatus.compareTo(SaturatedMode.AT_LIMIT)==0 ){
            updateUpload=true;
        }

        if( downloadBandwidthStatus.compareTo(SaturatedMode.AT_LIMIT)==0 &&
                downloadLimitSettingStatus.compareTo(SaturatedMode.AT_LIMIT)==0 ){
            updateDownload=true;
        }

        boolean uploadChanged=false;
        boolean downloadChanged=false;


        if(updateUpload && !transferMode.isDownloadMode() ){
            //increase limit by calculated amount, but only if not in downloading mode.
            uploadLinespeedCapacity += calculateUnpinnedStepSize(uploadLinespeedCapacity);
            uploadChanged=true;
            COConfigurationManager.setParameter(
                    SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT, uploadLinespeedCapacity);
        }
        if(updateDownload){
            //increase limit by calculated amount.
            downloadLinespeedCapacity += calculateUnpinnedStepSize(downloadLinespeedCapacity);
            downloadChanged=true;
            COConfigurationManager.setParameter(
                    SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT, downloadLinespeedCapacity);
        }

        //apply any rules that need applied.
        //The download limit can never be less then the upload limit.
        if( uploadLinespeedCapacity > downloadLinespeedCapacity){
            downloadLinespeedCapacity = uploadLinespeedCapacity;
            downloadChanged=true;
        }

        //calculate the new ratio.
        upDownRatio = ( (float) downloadLinespeedCapacity /(float) uploadLinespeedCapacity);
        COConfigurationManager.setParameter(
                SpeedManagerAlgorithmProviderV2.SETTING_V2_UP_DOWN_RATIO, upDownRatio);

        return new Update(uploadLinespeedCapacity,uploadChanged, downloadLinespeedCapacity,downloadChanged);
    }//calculateNewUnpinnedLimits

    /**
     * If setting is less then 100kBytes take 1 kByte steps.
     * If setting is less then 500kBytes take 5 kByte steps.
     * if setting is larger take 10 kBytes steps.
     * @param currLimitMax - current limit setting.
     * @return - set size for next change.
     */
    private int calculateUnpinnedStepSize(int currLimitMax){
        if(currLimitMax<102400){
            return 1024;
        }else if(currLimitMax<512000){
            return 1024*5;
        }else if(currLimitMax>=51200){
            return 1024*10;
        }
        return 1024;
    }//

    /**
     * Make a decision about unpinning either the upload or download limit. This is based on the
     * time we are saturating the limit without a down-tick signal.
     */
    public void checkForUnpinningCondition(){

        long currTime = SystemTime.getCurrentTime();


        //upload useage must be at limits for a set period of time before unpinning.
        if( !uploadBandwidthStatus.equals(SaturatedMode.AT_LIMIT) ||
                !uploadLimitSettingStatus.equals(SaturatedMode.AT_LIMIT) )
        {
            //start the clock over.
            uploadAtLimitStartTime = currTime;
        }else{
            //check to see if we have been here for the time limit.
            if( uploadAtLimitStartTime+TIME_AT_LIMIT_BEFORE_UNPINNING < currTime ){

                //if( transferMode.isDownloadConfidenceLow()  ){
                if( isDownloadConfidenceLow() ){
                    triggerLimitTestingFlag();
                }else{
                    //Don't unpin the limit is we have absolute confidence in it.
                    if( !isUploadConfidenceAbsolute() ){
                        //we have been AT_LIMIT long enough. Time to un-pin the limit see if we can go higher.
                        isUploadMaxPinned = false;
                        log("unpinning the upload max limit!!");
                    }
                }
            }
        }

        //download usage must be at limits for a set period of time before unpinning.
        if( !downloadBandwidthStatus.equals(SaturatedMode.AT_LIMIT) ||
                !downloadLimitSettingStatus.equals(SaturatedMode.AT_LIMIT) )
        {
            //start the clock over.
            downloadAtLimitStartTime = currTime;
        }else{
            //check to see if we have been here for the time limit.
            if( downloadAtLimitStartTime+TIME_AT_LIMIT_BEFORE_UNPINNING < currTime ){

                if( isUploadConfidenceLow() ){
                    triggerLimitTestingFlag();
                }else{
                    if( !isDownloadConfidenceAbsolute() ){
                        //we have been AT_LIMIT long enough. Time to un-pin the limit see if we can go higher.
                        isDownloadMaxPinned = false;
                        log("unpinning the download max limit!!");
                    }
                }
            }
        }

        logPinningInfo();
    }

    /**
     * If we have a down-tick signal then reset all the counters for increasing the limits.
     */
    public void notifyOfDownSingal(){

        if( !isUploadMaxPinned ){
            log("pinning the upload max limit, due to downtick signal.");
        }

        if( !isDownloadMaxPinned ){
            log("pinning the download max limit, due to downtick signal.");
        }

        long currTime = SystemTime.getCurrentTime();

        uploadAtLimitStartTime = currTime;
        downloadAtLimitStartTime = currTime;
        isUploadMaxPinned = true;
        isDownloadMaxPinned = true;
    }


    /**
     * Return true if we are confidence testing the limits.
     * @return - Update
     */
    public boolean isConfTestingLimits(){
        return transferMode.isConfTestingLimits();
    }//

    /**
     * Determine if we have low confidence in this limit.
     * @return - true if the confidence setting is LOW or NONE. Otherwise return true.
     */
    public boolean isDownloadConfidenceLow(){
        return ( downloadLimitConf.compareTo(SpeedLimitConfidence.MED) < 0 );
    }

    public boolean isUploadConfidenceLow(){
        return ( uploadLimitConf.compareTo(SpeedLimitConfidence.MED) < 0 );
    }

    public boolean isDownloadConfidenceAbsolute(){
        return ( downloadLimitConf.compareTo(SpeedLimitConfidence.ABSOLUTE)==0 );
    }

    public boolean isUploadConfidenceAbsolute(){
        return ( uploadLimitConf.compareTo(SpeedLimitConfidence.ABSOLUTE)==0 );
    }

    /**
     * Give the status of confidence testing as a String.
     * @return -
     */
    public String confTestStatus(){
        //ToDo: figure out what information is relevant here.
        //current download speed. highest current download speed.
        //current upload speed. hight current upload speed.
        //Is this a download or an upload?
        //current limit confidence.
        //Time to complete.
        return "";//ToDo: complete this.
    }


    /**
     *
     * @param downloadRate - currentUploadRate in bytes/sec
     * @param uploadRate - currentUploadRate in bytes/sec
     */
    public synchronized void updateLimitTestingData( int downloadRate, int uploadRate ){
        if( downloadRate>highestDownloadRate ){
            highestDownloadRate=downloadRate;
        }
        if( uploadRate>highestUploadRate){
            highestUploadRate=uploadRate;
        }

        long currTime = SystemTime.getCurrentTime();
        if(currTime>  confLimitTestStartTime+CONF_LIMIT_TEST_LENGTH){
            //set the test done flag.
            SpeedManagerLogger.log("finished limit search test.");
            currTestDone=true;
        }
    }

    /**
     * Call this method to start the limit testing.
     * @return - Update
     */
    public Update startLimitTesting(){

        confLimitTestStartTime=SystemTime.getCurrentTime();
        highestUploadRate=0;
        highestDownloadRate=0;
        currTestDone=false;

        //reset the flag.
        beginLimitTest=false;

        //configure the limits for this test. One will be at min and the other unlimited.
        Update retVal;
        if( transferMode.isDownloadMode() ){
            //test the download limit.
            retVal = new Update(uploadLimitMin,true,0,true);
            preTestDownloadSetting = downloadLinespeedCapacity;
            transferMode.setMode( TransferMode.State.DOWNLOAD_LIMIT_SEARCH );
        }else{
            //test the upload limit.
            retVal = new Update(0,true,downloadLimitMin,true);
            preTestUploadSetting = uploadLinespeedCapacity;
            transferMode.setMode( TransferMode.State.UPLOAD_LIMIT_SEARCH );
        }

        return retVal;
    }

    public void triggerLimitTestingFlag(){
        beginLimitTest=true;
    }

    public synchronized boolean isStartLimitTestFlagSet(){
        return beginLimitTest;
    }

    public synchronized boolean isConfLimitTestFinished(){
        return currTestDone;
    }

    /**
     * Call this method to end the limit testing.
     * @return - Update
     */
    public synchronized Update endLimitTesting(){

        Update retVal;
        //determine if the new setting is different then the old setting.
        if( transferMode.getMode()==TransferMode.State.DOWNLOAD_LIMIT_SEARCH ){

            downloadLimitConf = determineConfidenceLevel();

            //set that value.
            retVal = new Update(preTestUploadSetting,true,preTestDownloadSetting,true);
            //change back to original mode.
            transferMode.setMode( TransferMode.State.DOWNLOADING );

        }else if( transferMode.getMode()==TransferMode.State.UPLOAD_LIMIT_SEARCH){

            uploadLimitConf = determineConfidenceLevel();

            //set that value.
            retVal = new Update(preTestUploadSetting,true,preTestDownloadSetting,true);
            //change back to original mode.
            transferMode.setMode( TransferMode.State.SEEDING );

        }else{
            //This is an "illegal state" make it in the logs, but try to recover by setting back to original state.
            SpeedManagerLogger.log("SpeedLimitMonitor had IllegalState during endLimitTesting.");
            retVal = new Update(preTestUploadSetting,true,preTestDownloadSetting,true);
        }

        currTestDone=true;

        return retVal;
    }

    /**
     * After a test is complete determine how condifent the client should be in it
     * based on how different it is from the previous result.  If the new result is within
     * 20% of the old result then give it a MED. If it is great then give it a LOW. 
     * @return - what the new confidence interval should be.
     */
    public SpeedLimitConfidence determineConfidenceLevel(){
        SpeedLimitConfidence retVal=SpeedLimitConfidence.NONE;
        String configLimitParamName;
        String configConfParamName;
        int preTestValue;
        int highestValue;
        if(transferMode.getMode()==TransferMode.State.DOWNLOAD_LIMIT_SEARCH){

            configConfParamName = DOWNLOAD_CONF_LIMIT_SETTING;
            configLimitParamName = SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT;
            preTestValue = preTestDownloadSetting;
            highestValue = highestDownloadRate;
        }else if(transferMode.getMode()==TransferMode.State.UPLOAD_LIMIT_SEARCH){

            configConfParamName = UPLOAD_CONF_LIMIT_SETTING;
            configLimitParamName = SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT;
            preTestValue = preTestUploadSetting;
            highestValue = highestUploadRate;
        }else{
            //
            SpeedManagerLogger.log("IllegalState in determineConfidenceLevel(). Setting level to NONE.");
            return SpeedLimitConfidence.NONE;
        }

        float percentDiff = (float)Math.abs( highestValue-preTestValue )/(float)(Math.max(highestValue,preTestValue));
        if( percentDiff>0.2f){
            retVal = SpeedLimitConfidence.LOW;
        }else if(percentDiff>0.1f){
            retVal = SpeedLimitConfidence.MED;
        }

        //update the values.
        COConfigurationManager.setParameter(configConfParamName, retVal.getString() );
        COConfigurationManager.setParameter(configLimitParamName, Math.max(highestValue,preTestValue)); 

        //temp fix.  //Need a param listener above.
        if( transferMode.getMode()==TransferMode.State.UPLOAD_LIMIT_SEARCH ){
            uploadLinespeedCapacity=Math.max(highestValue,preTestValue);
        }else{
            uploadLinespeedCapacity=Math.max(highestValue,preTestValue);
        }

        return retVal;
    }

    protected void log(String str){

        SpeedManagerLogger.log(str);
    }//log


    /**
     * Class for sending update data.
     */
    static class Update{

        public int newUploadLimit;
        public int newDownloadLimit;

        public boolean hasNewUploadLimit;
        public boolean hasNewDownloadLimit;

        public Update(int upLimit, boolean newUpLimit, int downLimit, boolean newDownLimit){
            newUploadLimit = upLimit;
            newDownloadLimit = downLimit;

            hasNewUploadLimit = newUpLimit;
            hasNewDownloadLimit = newDownLimit;
        }

    }



}//SpeedLimitMonitor
