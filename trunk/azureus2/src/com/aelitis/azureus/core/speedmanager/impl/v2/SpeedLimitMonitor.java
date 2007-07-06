package com.aelitis.azureus.core.speedmanager.impl.v2;

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
    private int uploadLimitMax = 40000;
    private int uploadLimitMin = 5000;
    private int downloadLimitMax = 80000;
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
    private int preTestUploadCapacity=5042;
    private int preTestUploadLimit=5142;
    private int preTestDownloadCapacity=5042;
    private int preTestDownloadLimit=5142;

    public static final String UPLOAD_CONF_LIMIT_SETTING="SpeedLimitMonitor.setting.upload.limit.conf";
    public static final String DOWNLOAD_CONF_LIMIT_SETTING="SpeedLimitMonitor.setting.download.limit.conf";
    private static final long CONF_LIMIT_TEST_LENGTH=1000*60;

    //these methods are used to see how high limits can go.
    private boolean isUploadMaxPinned=true;
    private boolean isDownloadMaxPinned=true; 
    private long uploadAtLimitStartTime =SystemTime.getCurrentTime();
    private long downloadAtLimitStartTime = SystemTime.getCurrentTime();

    private static final long TIME_AT_LIMIT_BEFORE_UNPINNING = 30 * 1000; //30 seconds.

    //which percent of the measured upload capacity to use in download and seeding mode.
    public static final String USED_UPLOAD_CAPACITY_DOWNLOAD_MODE = "SpeedLimitMonitor.setting.upload.used.download.mode";
    public static final String USED_UPLOAD_CAPACITY_SEEDING_MODE = "SpeedLimitMonitor.setting.upload.used.seeding.mode";
    private float percentUploadCapacityDownloadMode = 0.6f;
    private float percentUploadCapacitySeedingMode = 0.9f;

    //PingSpaceMaps for the entire session.
    PingSpaceMapper pingMapOfDownloadMode;
    PingSpaceMapper pingMapOfSeedingMode;

    //Short-term PingSpaceMap to decide if a limit is too high.
    PingSpaceMonitor pingMonitor;

    //Testing
    LimitSlider slider = new LimitSlider();

    public SpeedLimitMonitor(){
        //
    }

    public void updateFromCOConfigManager(){

        uploadLimitMax = COConfigurationManager.getIntParameter(SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT);
        uploadLimitMin=COConfigurationManager.getIntParameter(SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MIN_LIMIT);
        downloadLimitMax =COConfigurationManager.getIntParameter(SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT);
        downloadLimitMin=COConfigurationManager.getIntParameter(SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MIN_LIMIT);

        //tie the upload and download ratios together.
        upDownRatio = ( (float) downloadLimitMax /(float) uploadLimitMax);
        COConfigurationManager.setParameter(
                SpeedManagerAlgorithmProviderV2.SETTING_V2_UP_DOWN_RATIO, upDownRatio);

        uploadLimitConf = SpeedLimitConfidence.parseString(
                COConfigurationManager.getStringParameter( SpeedLimitMonitor.UPLOAD_CONF_LIMIT_SETTING ));
        downloadLimitConf = SpeedLimitConfidence.parseString(
                COConfigurationManager.getStringParameter( SpeedLimitMonitor.DOWNLOAD_CONF_LIMIT_SETTING));

        percentUploadCapacityDownloadMode = (float)
                COConfigurationManager.getIntParameter(SpeedLimitMonitor.USED_UPLOAD_CAPACITY_DOWNLOAD_MODE, 60)/100.0f;

        percentUploadCapacitySeedingMode = (float)
                COConfigurationManager.getIntParameter(SpeedLimitMonitor.USED_UPLOAD_CAPACITY_SEEDING_MODE, 90)/100.0f;

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
        downloadLimitSettingStatus = SaturatedMode.getSaturatedMode(currLimit, downloadLimitMax);
    }

    public void setUploadLimitSettingMode(int currLimit){
        if( !transferMode.isDownloadMode() ){
            uploadLimitSettingStatus = SaturatedMode.getSaturatedMode(currLimit,
                    Math.round(uploadLimitMax * percentUploadCapacitySeedingMode));
        }else{
            uploadLimitSettingStatus = SaturatedMode.getSaturatedMode(currLimit,
                    uploadLimitMax);
        }
    }

    public int getUploadLineCapacity(){
        return uploadLimitMax;
    }

    public int getDownloadLineCapacity(){
        return downloadLimitMax;
    }

    public int getUploadMinLimit(){
        return uploadLimitMin;
    }

    public int getDownloadMinLimit(){
        return downloadLimitMin;
    }

    public String getUploadConfidence(){
        return uploadLimitConf.getString();
    }

    public String getDownloadConfidence(){
        return downloadLimitConf.getString();
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
     * True if the upload bandwidth usage is HIGH or AT_LIMIT.
     * @return -
     */
    public boolean isUploadBandwidthUsageHigh(){
        if( uploadBandwidthStatus.compareTo(SaturatedMode.AT_LIMIT)==0 ||
                uploadBandwidthStatus.compareTo(SaturatedMode.HIGH)==0){
            return true;
        }
        return false;
    }

    /**
     * Does the same as createNewLimit except it drops the upload rate first when in download mode.
     * @param signalStrength -
     * @param multiple -
     * @param currUpLimit -
     * @param currDownLimit -
     * @return  -
     */
    public Update modifyLimits(float signalStrength, float multiple, int currUpLimit, int currDownLimit){

        //this flag is set in a previous method.
        if( isStartLimitTestFlagSet() ){
            return startLimitTesting(currUpLimit, currDownLimit);
        }

        if( !isUploadMaxPinned || !isDownloadMaxPinned ){
            return calculateNewUnpinnedLimits(signalStrength);
        }


        slider.updateLimits(uploadLimitMax,uploadLimitMin,
                downloadLimitMax,downloadLimitMin);
        slider.updateStatus(currUpLimit,uploadLimitSettingStatus,
                currDownLimit, downloadLimitSettingStatus);

        return slider.adjust( signalStrength*multiple );
    }//modifyLimits

    
//ToDo: remove if method is no longer needed.
    /**
     * Here we need to handle several cases.
     * (a) If the download bandwidth is HIGH, then we need to back off on the upload limit to 80% of max.
     * (b) If upload bandwidth and limits are AT_LIMIT for a period of time then need to "unpin" that max limit
     *      to see how high it will go.
     * (c) If the download bandwidth and limits are AT_LIMIT for a period of time then need to "unpin" the max
     *      limit to see how high it will go.
     *
     *
     * @param signalStrength -
     * @param multiple -
     * @param currUpLimit -
     * @param currDownLimit -
     * @return -
     */
    public Update createNewLimit(float signalStrength, float multiple, int currUpLimit, int currDownLimit){

        //this flag is set in a previous method.
        if( isStartLimitTestFlagSet() ){
            return startLimitTesting(currUpLimit,currDownLimit);
        }

        int newLimit;

        int usedUploadLimit = uploadLimitMax;
        float usedUpDownRatio = upDownRatio;
        if( transferMode.isDownloadMode() ){
            usedUploadLimit = Math.round( percentUploadCapacityDownloadMode * uploadLimitMax);
            SpeedManagerLogger.trace("download mode usedUploadLimit="+usedUploadLimit
                    +" % used="+percentUploadCapacityDownloadMode);
        }else{
            usedUploadLimit = Math.round( percentUploadCapacitySeedingMode * uploadLimitMax);
            SpeedManagerLogger.trace("seeding mode usedUploadLimit="+usedUploadLimit
                    +" % used="+percentUploadCapacitySeedingMode);
        }

        if(usedUploadLimit<5120){
            usedUploadLimit=5120;
        }


        //re-calculate the up-down ratio.
        usedUpDownRatio = ( (float) downloadLimitMax /(float) usedUploadLimit);

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
            uploadLimitMax += calculateUnpinnedStepSize(uploadLimitMax);
            uploadChanged=true;
            COConfigurationManager.setParameter(
                    SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT, uploadLimitMax);
        }
        if(updateDownload){
            //increase limit by calculated amount.
            downloadLimitMax += calculateUnpinnedStepSize(downloadLimitMax);
            downloadChanged=true;
            COConfigurationManager.setParameter(
                    SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT, downloadLimitMax);
        }

        //apply any rules that need applied.
        //The download limit can never be less then the upload limit.
        if( uploadLimitMax > downloadLimitMax){
            downloadLimitMax = uploadLimitMax;
            downloadChanged=true;
            COConfigurationManager.setParameter(
                    SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT, downloadLimitMax);
        }

        //The min rate is alway 10% of the max rate.
        if(  uploadLimitMin*10 < uploadLimitMax){
            //increase the upload limit min.
            uploadLimitMin = uploadLimitMax /10;
            COConfigurationManager.setParameter(
                    SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MIN_LIMIT, uploadLimitMin);
        }

        if( downloadLimitMin*10 < downloadLimitMax){
            //increase the download limit min.
            downloadLimitMin = downloadLimitMax /10;
            COConfigurationManager.setParameter(
                    SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MIN_LIMIT, downloadLimitMin);
        }
        
        //calculate the new ratio.
        upDownRatio = ( (float) downloadLimitMax /(float) uploadLimitMax);
        COConfigurationManager.setParameter(
                SpeedManagerAlgorithmProviderV2.SETTING_V2_UP_DOWN_RATIO, upDownRatio);

        return new Update(uploadLimitMax,uploadChanged, downloadLimitMax,downloadChanged);
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

                if( isUploadConfidenceLow() ){
                    if( !transferMode.isDownloadMode() ){
                        //triggerLimitTestingFlag();//ToDo: restore after test.
                        isUploadMaxPinned = false; //slower search to prevent over-estimates on cable modem.
                    }
                }else{
                    //Don't unpin the limit is we have absolute confidence in it.
                    if( !isUploadConfidenceAbsolute() ){
                        //we have been AT_LIMIT long enough. Time to un-pin the limit see if we can go higher.
                        isUploadMaxPinned = false;
                        SpeedManagerLogger.trace("unpinning the upload max limit!!");
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

                if( isDownloadConfidenceLow() ){
                    if( transferMode.isDownloadMode() ){
                        triggerLimitTestingFlag();
                    }
                }else{
                    if( !isDownloadConfidenceAbsolute() ){
                        //we have been AT_LIMIT long enough. Time to un-pin the limit see if we can go higher.
                        isDownloadMaxPinned = false;
                        SpeedManagerLogger.trace("unpinning the download max limit!!");
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
            SpeedManagerLogger.trace("pinning the upload max limit, due to downtick signal.");
        }

        if( !isDownloadMaxPinned ){
            SpeedManagerLogger.trace("pinning the download max limit, due to downtick signal.");
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
            SpeedManagerLogger.trace("finished limit search test.");
            currTestDone=true;
        }
    }

    /**
     * Call this method to start the limit testing.
     * @param currUploadLimit -
     * @param currDownloadLimit -
     * @return - Update
     */
    public Update startLimitTesting(int currUploadLimit, int currDownloadLimit){

        confLimitTestStartTime=SystemTime.getCurrentTime();
        highestUploadRate=0;
        highestDownloadRate=0;
        currTestDone=false;

        //reset the flag.
        beginLimitTest=false;

        //get the limits before the test, we are restoring them after the test.
        preTestUploadLimit = currUploadLimit;
        preTestDownloadLimit = currDownloadLimit;

        //configure the limits for this test. One will be at min and the other unlimited.
        Update retVal;
        if( transferMode.isDownloadMode() ){
            //test the download limit.
            retVal = new Update(uploadLimitMin,true,
                        Math.round(downloadLimitMax *1.2f),true);
            preTestDownloadCapacity = downloadLimitMax;
            transferMode.setMode( TransferMode.State.DOWNLOAD_LIMIT_SEARCH );
        }else{
            //test the upload limit.
            retVal = new Update( Math.round(uploadLimitMax *1.2f),true,
                        downloadLimitMin,true);
            preTestUploadCapacity = uploadLimitMax;
            transferMode.setMode( TransferMode.State.UPLOAD_LIMIT_SEARCH );
        }

        return retVal;
    }

    /**
     * Ramp the upload and download rates higher, so ping-times are relevant.
     * @param uploadLimit -
     * @param downloadLimit -
     * @return -
     */
    public Update rampTestingLimit(int uploadLimit, int downloadLimit){
        Update retVal;
        if( transferMode.getMode() == TransferMode.State.DOWNLOAD_LIMIT_SEARCH ){
            downloadLimit *= 1.15f;
            retVal = new Update(uploadLimit,false,downloadLimit,true);
        //}else{
        }else if( transferMode.getMode() == TransferMode.State.UPLOAD_LIMIT_SEARCH ){
            uploadLimit *= 1.15f;
            retVal = new Update(uploadLimit,true,downloadLimit,false);
        }else{
            retVal = new Update(uploadLimit,false,downloadLimit,false);
            SpeedManagerLogger.trace("ERROR: rampTestLimit should only be called during limit testing. ");
        }

        return retVal;
    }//rampTestingLimit

    public void triggerLimitTestingFlag(){
        beginLimitTest=true;
    }

    public synchronized boolean isStartLimitTestFlagSet(){
        return beginLimitTest;
    }

    public synchronized boolean isConfLimitTestFinished(){
        return currTestDone;
    }

    public synchronized Update endLimitTesting(int downloadCapacityGuess, int uploadCapacityGuess){

        SpeedManagerLogger.trace(" repalce highestDownloadRate: "+highestDownloadRate+" with "+downloadCapacityGuess);
        SpeedManagerLogger.trace(" replace highestUploadRate: "+highestUploadRate+" with "+uploadCapacityGuess);

        highestDownloadRate = downloadCapacityGuess;
        highestUploadRate = uploadCapacityGuess;

        return endLimitTesting();
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
            SpeedManagerLogger.trace("pre-upload-setting="+ preTestUploadCapacity +" up-capacity"+ uploadLimitMax
                    +" pre-download-setting="+ preTestDownloadCapacity +" down-capacity="+ downloadLimitMax);

            retVal = new Update(preTestUploadLimit,true, downloadLimitMax,true);
            //change back to original mode.
            transferMode.setMode( TransferMode.State.DOWNLOADING );

        }else if( transferMode.getMode()==TransferMode.State.UPLOAD_LIMIT_SEARCH){

            uploadLimitConf = determineConfidenceLevel();

            //set that value.
            retVal = new Update(uploadLimitMax,true, downloadLimitMax,true);
            //change back to original mode.
            transferMode.setMode( TransferMode.State.SEEDING );

        }else{
            //This is an "illegal state" make it in the logs, but try to recover by setting back to original state.
            SpeedManagerLogger.log("SpeedLimitMonitor had IllegalState during endLimitTesting.");
            retVal = new Update(preTestUploadLimit,true, preTestDownloadLimit,true);
        }

        currTestDone=true;

        //reset the counter
        uploadAtLimitStartTime = SystemTime.getCurrentTime();
        downloadAtLimitStartTime = SystemTime.getCurrentTime();

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
        String settingMaxLimitName;
        String settingMinLimitName;
        String settingConfidenceName;
        int preTestValue;
        int highestValue;
        if(transferMode.getMode()==TransferMode.State.DOWNLOAD_LIMIT_SEARCH){

            settingConfidenceName = DOWNLOAD_CONF_LIMIT_SETTING;
            settingMaxLimitName = SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT;
            settingMinLimitName = SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MIN_LIMIT;
            preTestValue = preTestDownloadCapacity;
            highestValue = highestDownloadRate;
        }else if(transferMode.getMode()==TransferMode.State.UPLOAD_LIMIT_SEARCH){

            settingConfidenceName = UPLOAD_CONF_LIMIT_SETTING;
            settingMaxLimitName = SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT;
            settingMinLimitName = SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MIN_LIMIT;
            preTestValue = preTestUploadCapacity;
            highestValue = highestUploadRate;
        }else{
            //
            SpeedManagerLogger.log("IllegalState in determineConfidenceLevel(). Setting level to NONE.");
            return SpeedLimitConfidence.NONE;
        }

        float percentDiff = (float)Math.abs( highestValue-preTestValue )/(float)(Math.max(highestValue,preTestValue));
        if( percentDiff<0.15f){
            retVal = SpeedLimitConfidence.MED;
        }else{
            retVal = SpeedLimitConfidence.LOW;
        }

        //update the values.
        COConfigurationManager.setParameter(settingConfidenceName, retVal.getString() );
        int newMaxLimitSetting = highestValue;
        COConfigurationManager.setParameter(settingMaxLimitName, newMaxLimitSetting);
        int newMinLimitSetting = Math.max( Math.round( newMaxLimitSetting * 0.1f ), 5120 );
        COConfigurationManager.setParameter(settingMinLimitName, newMinLimitSetting );

        //temp fix.  //Need a param listener above and all rules need to be one method.
        StringBuffer sb = new StringBuffer();
        if( transferMode.getMode()==TransferMode.State.UPLOAD_LIMIT_SEARCH ){
            sb.append("new upload limits: ");
            uploadLimitMax =newMaxLimitSetting;
            uploadLimitMin=newMinLimitSetting;
            //downloadCapacity can never be less then upload capacity.
            if( downloadLimitMax < uploadLimitMax){
                downloadLimitMax = uploadLimitMax;
                COConfigurationManager.setParameter(
                        SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT, downloadLimitMax);
                
            }
        }else{
            sb.append("new download limits: ");
            downloadLimitMax =newMaxLimitSetting;
            downloadLimitMin=newMinLimitSetting;
            //upload capacity should never be 20x less then download.
            if( uploadLimitMax *20< downloadLimitMax){
                uploadLimitMax = downloadLimitMax /20;
                COConfigurationManager.setParameter(
                         SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT, uploadLimitMax);

                uploadLimitMin = Math.max( uploadLimitMax /20, 5120 );
                COConfigurationManager.setParameter(
                        SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MIN_LIMIT,uploadLimitMin);
            }//if
            
        }
        upDownRatio = ((float) downloadLimitMax /(float) uploadLimitMax);
        COConfigurationManager.setParameter(
                SpeedManagerAlgorithmProviderV2.SETTING_V2_UP_DOWN_RATIO, upDownRatio);


        sb.append(newMaxLimitSetting).append(":").append(newMinLimitSetting).append(":").append(upDownRatio);

        SpeedManagerLogger.trace( sb.toString() );

        return retVal;
    }

    /**
     * If the user changes the line capacity settings on the configuration panel and adjustment
     * needs to occur even if the signal is NO-CHANGE-NEEDED. Test for that condition here.
     * @param currUploadLimit  - reported upload capacity from the adapter
     * @param currDownloadLimit - reported download capacity from the adapter.
     * @return - true if the "capacity" is lower then the current limit.
     */
    public boolean areSettingsInSpec(int currUploadLimit, int currDownloadLimit){

        //during a confidence level test, anything goes.
        if( isConfTestingLimits() ){
            return true;
        }

        boolean retVal = true;
        if( currUploadLimit> uploadLimitMax){
            retVal = false;
        }
        if(  currDownloadLimit> downloadLimitMax){
            retVal = false;
        }
        return retVal;
    }

    public void setRefLimits(int uploadMax, int downloadMax){

        if( uploadLimitMax != uploadMax ){
            uploadLimitMax=uploadMax;
            COConfigurationManager.setParameter(
                    SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT, uploadLimitMax);
        }

        int uploadMin = Math.max( uploadMax/10, 5120 );
        if( uploadLimitMin != uploadMin ){
            uploadLimitMin = uploadMin;
            COConfigurationManager.setParameter(
                    SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MIN_LIMIT, uploadLimitMin);
        }

        if( downloadLimitMax != downloadMax){
            downloadLimitMax = downloadMax;
            COConfigurationManager.setParameter(
                    SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT, downloadLimitMax);

        }

        int downloadMin = Math.max( downloadMax/10, 20480 );
        if( downloadLimitMin != downloadMin ){
            downloadLimitMin = downloadMin;
            COConfigurationManager.setParameter(
                    SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MIN_LIMIT, downloadLimitMin);
        }

    }

    /**
     * It is likely the user adjusted the "line speed capacity" on the configuration panel.
     * We need to adjust the current limits down to adjust.
     * @param currUploadLimit -
     * @param currDownloadLimit -
     * @return - Updates as needed.
     */
    public Update adjustLimitsToSpec(int currUploadLimit, int currDownloadLimit){

        int newUploadLimit = currUploadLimit;
        boolean uploadChanged = false;
        int newDownloadLimit = currDownloadLimit;
        boolean downloadChanged = false;

        //check for the case when the line-speed capacity is below the current limit.
        if( currUploadLimit> uploadLimitMax){
            newUploadLimit = uploadLimitMax;
            uploadChanged = true;
        }

        //check for the case when the min setting has been moved above the current limit.
        if( currDownloadLimit> downloadLimitMax){
            newDownloadLimit = downloadLimitMax;
            downloadChanged = true;
        }

        //Another possibility is the min limits have been raised.
        if( currUploadLimit<uploadLimitMin ){
            newUploadLimit = uploadLimitMin;
            uploadChanged = true;
        }

        if( currDownloadLimit<downloadLimitMin ){
            newDownloadLimit = downloadLimitMin;
            downloadChanged = true;
        }

        SpeedManagerLogger.trace("Adjusting limits due to out of spec: new-up="+newUploadLimit
                +" new-down="+newDownloadLimit);

        return new Update(newUploadLimit,uploadChanged,newDownloadLimit,downloadChanged);
    }


    protected void log(String str){

        SpeedManagerLogger.log(str);
    }//log



    public void initPingSpaceMap(int maxGoodPing, int minBadPing){
        pingMapOfDownloadMode = new PingSpaceMapper(maxGoodPing,minBadPing);
        pingMapOfSeedingMode = new PingSpaceMapper(maxGoodPing,minBadPing);

        pingMonitor = new PingSpaceMonitor(maxGoodPing,minBadPing,transferMode);
    }

    public int guessDownloadLimit(){
        return pingMapOfDownloadMode.guessDownloadLimit();
    }

    public int guessUploadLimit(){
        int dmUpLimitGuess = pingMapOfDownloadMode.guessUploadLimit();
        int smUpLimitGuess = pingMapOfSeedingMode.guessUploadLimit();

        return Math.max(dmUpLimitGuess,smUpLimitGuess);
    }


    /**
     * Just log this data until we decide if it is useful.
     */
    public void logPingMapData() {

        int downLimGuess = pingMapOfDownloadMode.guessDownloadLimit();
        int upLimGuess = pingMapOfDownloadMode.guessUploadLimit();
        int seedingUpLimGuess = pingMapOfSeedingMode.guessUploadLimit();

        StringBuffer sb = new StringBuffer("ping-map: ");
        sb.append(":down=").append(downLimGuess);
        sb.append(":up=").append(upLimGuess);
        sb.append(":(seed)up=").append(seedingUpLimGuess);

        SpeedManagerLogger.log( sb.toString()  );
    }

    public void setCurrentTransferRates(int downRate, int upRate){

        if( pingMapOfDownloadMode!=null && pingMapOfSeedingMode!=null){
            pingMapOfDownloadMode.setCurrentTransferRates(downRate,upRate);
            pingMapOfSeedingMode.setCurrentTransferRates(downRate,upRate);
        }
    }

    public void resetPingSpace(){

        if( pingMapOfDownloadMode!=null && pingMapOfSeedingMode!=null){
            pingMapOfDownloadMode.reset();
            pingMapOfSeedingMode.reset();
        }
    }

    public void addToPingMapData(int lastMetricValue){
        String modeStr = getTransferModeAsString();

        if(    modeStr.equalsIgnoreCase(TransferMode.State.DOWNLOADING.getString())
            || modeStr.equalsIgnoreCase(TransferMode.State.DOWNLOAD_LIMIT_SEARCH.getString())  )
        {
            //add point to map for download mode
            pingMapOfDownloadMode.addMetricToMap(lastMetricValue);

        }
        else if(     modeStr.equalsIgnoreCase(TransferMode.State.SEEDING.getString())
                  || modeStr.equalsIgnoreCase(TransferMode.State.UPLOAD_LIMIT_SEARCH.getString()) )
        {
            //add point to map for seeding mode.
            pingMapOfSeedingMode.addMetricToMap(lastMetricValue);

        }

        boolean dropLimits = pingMonitor.addToPingMapData(lastMetricValue,transferMode);
        if( dropLimits ){

            handleDropLimitRequest();

        }

    }//addToPingMapData

    /**
     * PingSpaceMonitor is requesting the limits be dropped. Handle this
     * request.
     * Rules:
     * #1) IGNORE if confidence limit is HIGH or ABSOLUTE.
     * #2)
     */
    private void handleDropLimitRequest(){

        int newLimit = pingMonitor.getNewLimit();
        int type = pingMonitor.limitType();

        if( type == PingSpaceMonitor.DOWNLOAD ){

            if( SpeedLimitConfidence.HIGH.isGreater( downloadLimitConf ) ){
                //We have a MED,LOW,NONE setting move it lower.
                SpeedManagerLogger.log("PingSpaceMonitor lower download limit="+newLimit);

                downloadLimitMax = newLimit;
                COConfigurationManager.setParameter(
                        SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT,
                        newLimit);

                //Automatically set the confidence limit back to LOW
                COConfigurationManager.setParameter(
                        SpeedLimitMonitor.DOWNLOAD_CONF_LIMIT_SETTING,
                        SpeedLimitConfidence.LOW.getString() );


            }else{
                //we have high confidence in these limits, don't change them.
                SpeedManagerLogger.trace(" PingSpaceMonitor - keeping same limits, since conf interval is high.");
            }

        }else if( type == PingSpaceMonitor.UPLOAD ){

            if( SpeedLimitConfidence.HIGH.isGreater( uploadLimitConf ) ){
                SpeedManagerLogger.log("PingSpaceMonitor lower upload-limit="+newLimit);

                uploadLimitMax = newLimit;
                COConfigurationManager.setParameter(
                        SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT,
                        newLimit);

                //Automatically set the confidence limit back to LOW
                COConfigurationManager.setParameter(
                        SpeedLimitMonitor.UPLOAD_CONF_LIMIT_SETTING,
                        SpeedLimitConfidence.LOW.getString() );

            }else{
                SpeedManagerLogger.trace(" PingSpaceMonitor - keeping same limits, since conf interval is high.");
            }

        }

        //we have consumed this request, now reset it.
        pingMonitor.resetNewLimit();

    }//handleDropLimitRequest

    /** Internal classes here **/

    /**
     * Class for sending update data.
     */
    static class Update{

        public int newUploadLimit;
        public int newDownloadLimit;

        public boolean hasNewUploadLimit;
        public boolean hasNewDownloadLimit;

        public Update(int upLimit, boolean newUpLimit, int downLimit, boolean newDownLimit)
        {
            newUploadLimit = upLimit;
            newDownloadLimit = downLimit;

            hasNewUploadLimit = newUpLimit;
            hasNewDownloadLimit = newDownLimit;
        }

    }//class Update


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


        public Update adjust( float amount ){

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

        private Update update(){
            int upLimit;
            int downLimit;

            upLimit = Math.round( ((upMax-upMin)*valueUp)+upMin );
            downLimit = Math.round( ((downMax-downMin)*valueDown)+downMin );

            //log this change.
            String msg = " create-update: valueUp="+valueUp+",upLimit="+upLimit+",valueDown="+valueDown
                    +",downLimit="+downLimit+",upMax="+upMax+",upMin="+upMin+",downMax="+downMax
                    +",downMin="+downMin;
            SpeedManagerLogger.log( msg );

            return new Update(upLimit,true,downLimit,true);
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


}//SpeedLimitMonitor
