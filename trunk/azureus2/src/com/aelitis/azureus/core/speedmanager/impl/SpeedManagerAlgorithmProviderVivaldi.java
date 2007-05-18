package com.aelitis.azureus.core.speedmanager.impl;

import com.aelitis.azureus.core.speedmanager.SpeedManagerPingSource;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.util.average.Average;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.control.DHTControlContact;
import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;
import com.aelitis.azureus.core.dht.netcoords.vivaldi.ver1.VivaldiPosition;
import com.aelitis.azureus.core.dht.netcoords.vivaldi.ver1.impl.HeightCoordinatesImpl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.core3.util.AEDiagnosticsLogger;
import org.gudy.azureus2.core3.util.AEDiagnostics;
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
    private SpeedManagerAlgorithmProviderAdapter adapter;
    private PluginInterface dhtPlugin;

    AEDiagnosticsLogger dLog = AEDiagnostics.getLogger("v3.AutoSpeed_Beta_Debug");


    long timeSinceLastUpdate;
    private static final long TIME_BETWEEN_UPDATES = 15000;


    //use for home network.
    private static int uploadLimitMax = 38000;
    private static int uploadLimitMin = 5000;
    private static int downloadLimitMax = 80000;
    private static int downloadLimitMin = 8000;

    private static int vivaldiGoodResult = 100;
    private static int vivaldiGoodTolerance = 300;
    private static int vivaldiBadResult = 1300;
    private static int vivaldiBadTolerance = 300;


    int consecutiveUpticks=0;
    int consecutiveDownticks=0;

    //variables for display.
    int lastVivaldiDistance;

    //for managing ping sources.
    private final Map pingAverages = new HashMap(); //<Source,PingSourceStats>
    private long lastPingRemoval=0;
    private static final long TIME_BETWEEN_REMOVALS = 5 * 60000; //five minutes.


    static{
        COConfigurationManager.addListener(
                new COConfigurationListener(){
                    public void configurationSaved(){

                        try{
                            uploadLimitMax=COConfigurationManager.getIntParameter(SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT);
                            uploadLimitMin=COConfigurationManager.getIntParameter(SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MIN_LIMIT);
                            downloadLimitMax=COConfigurationManager.getIntParameter(SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT);
                            downloadLimitMin=COConfigurationManager.getIntParameter(SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MIN_LIMIT);
                            vivaldiGoodResult=COConfigurationManager.getIntParameter(SpeedManagerAlgorithmProviderV2.SETTING_VIVALDI_GOOD_SET_POINT);
                            vivaldiGoodTolerance=COConfigurationManager.getIntParameter(SpeedManagerAlgorithmProviderV2.SETTING_VIVALDI_GOOD_TOLERANCE);
                            vivaldiBadResult=COConfigurationManager.getIntParameter(SpeedManagerAlgorithmProviderV2.SETTING_VIVALDI_BAD_SET_POINT);
                            vivaldiBadTolerance=COConfigurationManager.getIntParameter(SpeedManagerAlgorithmProviderV2.SETTING_VIVALDI_BAD_TOLERANCE); 

                            //NOTE: need to be careful about changing parameters used by changing parameters used by SpeedManagerAlgorithmV1.
                            String algorithmUsed = System.getProperty("azureus.autospeed.alg.provider.version");
                            if( !(algorithmUsed==null) && !("1".equals(algorithmUsed)) ){

                                //we are using V2, so lets change the settings we need.

                                //set the Upload to Download ratio.
                                float downloadRatio = ( (float)downloadLimitMax/(float)uploadLimitMax );
                                COConfigurationManager.setParameter("AutoSpeed Download Adj Ratio",downloadRatio);

                                //enable upload to download ratio.
                                COConfigurationManager.setParameter("AutoSpeed Download Adj Enable",true);

                            }//if

                        }catch( Throwable t ){

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

        log("curr-data: curr-download : curr-upload-limit : curr-upload-data-speed : curr-proto-upload-speed");

        log( "new-limit:newLimit:currStep:signalStrength:multiple:currUpLimit:maxStep:uploadLimitMax:uploadLimitMin" );

        log("consecutive:up:down");
    }

    /**
     * Called periodically (see period above) to allow stats to be updated.
     */

    public void updateStats() {

        //update some stats used in the UI.
        log("updateStats");

        int currDownload = adapter.getCurrentDownloadLimit();

        int currUploadLimit = adapter.getCurrentUploadLimit();
        int currDataUploadSpeed = adapter.getCurrentDataUploadSpeed();
        int currProtoUploadSpeed = adapter.getCurrentProtocolUploadSpeed();

        log("curr-data:"+currDownload+":"+currUploadLimit+":"+currDataUploadSpeed+":"+currProtoUploadSpeed);

        //current upload limit setting
        //current upload average
        //current upload data rate

        //current download limit setting
        //current download average
        //current download data rate

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
            }//if
        }//for

        long currTime = SystemTime.getCurrentTime();

        if( timeSinceLastUpdate==0 ){
            timeSinceLastUpdate=currTime;
        }

        if( timeSinceLastUpdate+TIME_BETWEEN_UPDATES > currTime ){
            //still waiting for the next time to update the value.
            log("calculate-deferred");
            return;
        }
        timeSinceLastUpdate = currTime;

        log("calculate");

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

        DHT forSelf = dhts[dhts.length-1];
        DHTTransportContact c = forSelf.getControl().getTransport().getLocalContact();

        DHTNetworkPosition ownLocation = c.getNetworkPosition(DHTNetworkPosition.POSITION_TYPE_VIVALDI_V1);
        VivaldiPosition loc = (VivaldiPosition) ownLocation;
        float locErrorEstimate = loc.getErrorEstimate();
        HeightCoordinatesImpl ownCoords = (HeightCoordinatesImpl) loc.getCoordinates();

        List l = forSelf.getControl().getContacts();
        Iterator itr = l.iterator();
        List forMedian = new ArrayList( l.size() );//List<Float>
        while( itr.hasNext() ){
            DHTControlContact contact = (DHTControlContact) itr.next();
            DHTNetworkPosition _pos = contact.getTransportContact().getNetworkPosition(DHTNetworkPosition.POSITION_TYPE_VIVALDI_V1);
            if( _pos==null ){
                continue;
            }
            VivaldiPosition pos = (VivaldiPosition)_pos;
            HeightCoordinatesImpl coord = (HeightCoordinatesImpl) pos.getCoordinates();
            if(coord.isValid()){
                ownCoords.distance(coord);
                forMedian.add( new Float(ownCoords.distance(coord) ) );
            }
        }//while

        Collections.sort(forMedian);

        //The mean value should be in middle value. (Is this any more meaningful the ave. ping time?)
        int size = forMedian.size();
        Float meanDistance = (Float) forMedian.get( size/2 );

        //We now have meanDistance!!! use it to set the upload limit!!!
        lastVivaldiDistance = Math.round( meanDistance.floatValue() );

        log("vivaldi:"+lastVivaldiDistance+":"+locErrorEstimate);

        float signalStrength = determineSignalStrength( lastVivaldiDistance );
        if( signalStrength!=0.0f ){
            int newLimit = createNewLimit(signalStrength);

            int kbpsLimit = newLimit/1024;
            log(" setting new limit to: "+kbpsLimit+" kb/s");

            //based on the value need to set a limit.
            adapter.setCurrentUploadLimit( newLimit );
        }//if

        //determine if we need to drop a ping source.
        checkPingSources(sources);
    }


    /**
     * We need to move the upload limit. Calculate what it should be.
     * @param signalStrength -
     * @return -
     */
    private int createNewLimit(float signalStrength){

        int newLimit;

        float multiple = consectiveMultiplier();

        //The amount to move it against the new limit is.
        float multi = Math.abs( signalStrength * multiple * 0.3f );

        //Force the value to the limit.
        if(multi>1.0f){
            if( signalStrength>0.0f ){
                log("forcing max upload limit.");
                return uploadLimitMax;
            }else{
                log("forcing min upload limit.");
                return uploadLimitMin;
            }
        }

        //don't move it all the way.
        int maxStep;
        int currStep;
        int minStep=1024;
        int currUpLimit = adapter.getCurrentUploadLimit();

        if(signalStrength>0.0f){
            maxStep = Math.round( uploadLimitMax -currUpLimit );
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

        if(newLimit> uploadLimitMax){
            newLimit= uploadLimitMax;
        }
        if(newLimit< uploadLimitMin){
            newLimit= uploadLimitMin;
        }

        log( "new-limit:"+newLimit+":"+currStep+":"+signalStrength+":"+multiple+":"+currUpLimit+":"+maxStep+":"+uploadLimitMax+":"+uploadLimitMin );

        return newLimit;
    }//createNewLimit

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
     * @param vivaldiValue -
     * @return -
     */
    private float determineSignalStrength(int vivaldiValue){

        //determine if this is an up-tick (+1), down-tick (-1) or neutral (0).
        float signal=0.0f;
        if( vivaldiValue< vivaldiGoodResult){
            //strong up signal.
            signal=1.0f;
            consecutiveUpticks++;
            consecutiveDownticks=0;
        }
        else if( vivaldiValue < (vivaldiGoodResult + vivaldiGoodTolerance)){
            //weak up signal.
            signal = (float)(vivaldiValue- vivaldiGoodResult)/vivaldiGoodTolerance;

            consecutiveUpticks++;
            consecutiveDownticks=0;
        }
        else if( vivaldiValue > vivaldiBadResult){
            //strong down signal
            signal = -1.0f;
            consecutiveUpticks=0;
            consecutiveDownticks++;
        }
        else if( vivaldiValue > (vivaldiBadResult - vivaldiBadTolerance) ){
            //weak down signal
            consecutiveUpticks=0;
            consecutiveDownticks++;

            int lowerBound= vivaldiBadResult - vivaldiBadTolerance;
            signal = (vivaldiValue-lowerBound) / vivaldiBadTolerance;
            signal -= 1.0f;
        }
        else{
            //This is a neutral signal. 
        }

        log("consecutive:"+consecutiveUpticks+":"+consecutiveDownticks);

        return signal;
    }

    /**
     * The longer were get the same signal the stronger it is.
     * @return -
     */
    private float consectiveMultiplier(){

        int c;

        if( consecutiveUpticks > consecutiveDownticks ){
            c=consecutiveUpticks;
        }else{
            c=consecutiveDownticks;
        }

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
        return lastVivaldiDistance;
        
    }//getIdlePingMillis

    public int getCurrentPingMillis() {
        return 0;
    }

    public int getMaxPingMillis() {
        return 42;  //To change body of implemented methods use File | Settings | File Templates.
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
        if(adapter!=null){
            adapter.log( str );
        }
        if(dLog!=null){
            dLog.log(str);
        }
    }//log

}
