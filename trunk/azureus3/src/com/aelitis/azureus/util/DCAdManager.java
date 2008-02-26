package com.aelitis.azureus.util;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.download.EnhancedDownloadManager;
import com.aelitis.azureus.core.download.DownloadManagerEnhancer;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.core.messenger.config.PlatformDCAdManager;
import com.aelitis.azureus.ui.swt.views.skin.TorrentListViewsUtils;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.impl.DownloadManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;

/**
 * Created on Feb 8, 2008
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

public class DCAdManager
{

    private static DCAdManager instance=null;

    private AzureusCore core;
    private List adsDMList = new ArrayList();
	private List adSupportedDMList = new ArrayList();

    private Object lastImpressionID;
	protected boolean checkingForAds = false;

	private final static long EXPIRE_ASX = 1000L * 60 * 10; // 10 min

    public static synchronized DCAdManager getInstance()
    {
        if(instance==null){
            instance = new DCAdManager();
        }
        return instance;
    }

    private DCAdManager(){

    }


    public void initialize(final AzureusCore _core){
        core = _core;

        //start the impression tracker.
        startAdTrackerListener();

		GlobalManager gm = core.getGlobalManager();
		gm.addListener(new GlobalManagerAdapter() {

            /**
             * Delete and ASX, or azpd files when deleting the content.
             * @param dm -
             */
            public void downloadManagerRemoved(DownloadManager dm) {
				adsDMList.remove(dm);

                //remove ASX file.
                deleteAsxFile(dm);

                //remove media/azpd  file.
                deleteAzpdFile(dm);
                
            }//downloadManagerRemoved

            /**
             * Add information about the torrent.
             * @param dm -
             */
            public void downloadManagerAdded(final DownloadManager dm) {

				TOTorrent torrent = dm.getTorrent();


                if (PlatformTorrentUtils.isContentAdEnabled(torrent)) {
				    //add location of ASX file.
				    dm.setData("ASX", determineASXFileLocation(dm));
                    //add location of azpd file.
                    try{
                        dm.setData("azpd", determineAzpdFileLocation(dm));
                    }catch(TOTorrentException tote){
                        debug("Failed to set azpd location",tote);
                    }
                }

                downloadManagerAddedHook(new DownloadManager[] {
					dm
				});
			}//downloadManagerAdded

        }, false); //addListener

    }//initialize

    /**
     * Delete the ASX file. Tries transient data first, then looks up the likely directory.
     * @param dm - DownloadManager -
     */
    private void deleteAsxFile(DownloadManager dm) {
        File asxFile = (File) dm.getData("ASX");
        if (asxFile != null) {
            try {
                asxFile.delete();
            } catch (Exception e) {
            }
        }else{
            try{
                asxFile = determineASXFileLocation(dm);
                if(asxFile!=null){
                    asxFile.delete();
                }
            }catch(Exception e){
                debug("error while deleting asx file.",e);
            }
        }
    }

    /**
     * Delete the azpd file. tries the transisent data first, the looks up the
     * directory.
     * @param dm - DownloadManager - 
     */
    private void deleteAzpdFile(DownloadManager dm) {
        File azpdFile = (File) dm.getData("azpd");
        if (azpdFile != null){
            try {
                azpdFile.delete();
            } catch (Exception e){
            }
        }else{
            try{
            //the data was not persistent look in the expected directory.
                azpdFile = determineAzpdFileLocation(dm);
                if(azpdFile != null){
                    azpdFile.delete();
                }
            }catch(Exception e){
                debug("error while deleting azpd file.",e);
            }
        }
    }

    private void startAdTrackerListener() {
        ExternalStimulusHandler.addListener(new ExternalStimulusListener() {
            public boolean receive(String name, Map values) {
                if (values == null) {
                    return false;
                }

                if (name.equals("adtracker")) {
                    processImpression(values);
                    return true;
                }

                return false;
            }


            /**
             * @param name -
             * @param values -
             * @return  -
             */
            public int query(String name, Map values) {
                return 0;  //new method not likely needed.
            }
        });
    }


    /**
     * Is callback from player.
     * @param values - parameters from the AdTracking URL.
     */
    protected void processImpression(Map values) {

		try {
			String contentHash = (String) values.get("contentHash");
			if (contentHash == null) {
				debug("No Content Hash on processImpression!");
				return;
			}

			String impressionID = (String) values.get("impressionTracker");
			if (impressionID == null || impressionID.equals(lastImpressionID)) {
				return;
			}
			lastImpressionID = impressionID;

             
            String adHash = (String) values.get("srcURL");
			if (adHash == null) {
				return;
			}

			String torrentHash = (String) values.get("hash");

			DownloadManager dmContent = null;
			if (torrentHash != null) {
				dmContent = core.getGlobalManager().getDownloadManager(
						new HashWrapper(Base32.decode(torrentHash)));
			}

			if (dmContent == null) {
				dmContent = core.getGlobalManager().getDownloadManager(
						new HashWrapper(Base32.decode(contentHash)));
			}
			if (dmContent != null) {
				debug("clear last asx for "
						+ dmContent.getTorrent().getHashWrapper().toBase32String());
				dmContent.setData("LastASX", null);
			}

			debug("imp " + impressionID + " commencing on "
					+ contentHash + "/" + torrentHash);

			DownloadManager dm = core.getGlobalManager().getDownloadManager(
					new HashWrapper(Base32.decode(adHash)));
			if (dm == null) {
				debug("DM for Ad not found");
			}

			PlatformDCAdManager.saveImpresssion(impressionID,
					SystemTime.getCurrentTime(), contentHash, torrentHash, adHash,
					5000);

		} catch (Exception e) {
			Debug.out(e);
		}
	}//processImpression


    private void downloadManagerAddedHook(final DownloadManager[] dms) {

        AEThread thread = new AEThread("downloadManagerAddedHook", true) {
            public void runSupport() {
                debug("enter - downloadManagerAddedHook");
                // build adSupportedContentList of ad supported content
                List adSupportedContentList = new ArrayList();
                for (int i = 0; i < dms.length; i++) {
                    final DownloadManager dm = dms[i];

                    TOTorrent torrent = dm.getTorrent();
                    if (torrent == null) {
                        return;
                    }

                    //If this torrent is an ad do this block.
                    if (PlatformTorrentUtils.getAdId(torrent) != null) {
                        // one of us!

                        // TODO: Check expirey
                        try {
                            debug("found ad " + dm + ": "
                                    + PlatformTorrentUtils.getAdId(torrent) + ": "
                                    + dm.getTorrent().getHashWrapper().toBase32String());
                        } catch (TOTorrentException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        if (!adsDMList.contains(dm)) {
                            adsDMList.add(dm);
                        }
                        if (dm.getAssumedComplete()) {
                            dm.setForceStart(false);
                        } else {
                            dm.addListener(new DownloadManagerAdapter() {
                                public void downloadComplete(DownloadManager manager) {
                                    if (!adsDMList.contains(manager)) {
                                        adsDMList.add(manager);
                                    }
                                    manager.setForceStart(false);
                                    manager.removeListener(this);
                                }
                            });
                        }
                    }

                    //If the torrent is content that contains an ad, do this block.
                    if (   PlatformTorrentUtils.isContent(torrent, true)
                        && PlatformTorrentUtils.getContentHash(torrent) != null
                        && PlatformTorrentUtils.isContentAdEnabled(torrent))
                    {
                        adSupportedContentList.add(dm);
                        if (!adSupportedDMList.contains(dm)) {

                            //Add GetAdvert here.
                            callGetAdvert(adSupportedContentList);

                            adSupportedDMList.add(dm);
                            if (!dm.getAssumedComplete()) {
                                dm.addListener(new DownloadManagerAdapter() {

                                    /**
                                     * download complete
                                     * @param manager -
                                     */
                                    public void downloadComplete(DownloadManager manager) {
                                        // good chance we still have internet here, so get/cache
                                        // the asx
                                        debug("calling createASX from ...Hook, in torrent has AD block content="+dm.getDisplayName());

                                        createASX(dm, null);

                                    }//downloadComplete
                                });
                            }
                        }
                    }
                }//for

                if (adSupportedContentList.size() == 0) {
                    debug("none of the " + dms.length+ " new torrent(s) are ad enabled.  SKIPPING getAdvert");
                    //determine the reason torrents are not ad -enabled.
                    determineReasonAdNotEnabled(dms);
                    return;
                }//if

                debug("exit - downloadManagerAddedHook");
            }

            private void callGetAdvert(List adSupportedContentList) {
                try {
                    checkingForAds = true;
                    debug("sending ad request for " + adSupportedContentList.size()
                            + " pieces of content.  We already have " + adsDMList.size()
                            + " ads");
                    DownloadManager[] dmAdable = (DownloadManager[]) adSupportedContentList.toArray(new DownloadManager[0]);

                    //Get the advertisment.
                    PlatformDCAdManager.getAdvert(dmAdable[0], 2000,
                            new PlatformDCAdManager.GetAdvertDataReplyListener(){

                                public void messageSent() {
                                    //nothing needed here.
                                }

                                public void adsReceived(List torrents, Map webParams) {
                                    debug("enter - adsReceived has #"+torrents.size()+" torrents");
                                    for (Iterator iter = torrents.iterator(); iter.hasNext();) {
                                        TOTorrent torrent = (TOTorrent) iter.next();
                                        try {
                                            debug("Ad: "
                                                    + new String(torrent.getName()));

                                            TorrentUtils.setFlag(torrent,
                                                    TorrentUtils.TORRENT_FLAG_LOW_NOISE, true);

                                            File tempFile = File.createTempFile("AZ_", ".torrent");

                                            debug("  Writing to " + tempFile);
                                            torrent.serialiseToBEncodedFile(tempFile);

                                            String sDefDir = null;
                                            try {
                                                sDefDir = COConfigurationManager.getDirectoryParameter("Default save path");
                                            } catch (IOException e) {
                                            }

                                            if (sDefDir == null) {
                                                sDefDir = tempFile.getParent();
                                            }

                                            DownloadManager adDM = core.getGlobalManager().addDownloadManager(
                                                    tempFile.getAbsolutePath(), sDefDir);

                                            if (adDM != null) {
                                                if (adDM.getAssumedComplete()) {
                                                    adsDMList.add(adDM);
                                                    adDM.setForceStart(false);
                                                } else {
                                                    adDM.setForceStart(true);
                                                    debug("Force Start " + adDM);
                                                    adDM.addListener(new DownloadManagerAdapter() {
                                                        public void downloadComplete(DownloadManager manager) {
                                                            if (!adsDMList.contains(manager)) {
                                                                adsDMList.add(manager);
                                                            }
                                                            manager.setForceStart(false);
                                                            manager.removeListener(this);
                                                        }
                                                    });
                                                }
                                                // TODO: Add Expiry date
                                                debug("  ADDED ad "
                                                        + adDM.getDisplayName());
                                            }
                                            tempFile.deleteOnExit();
                                        } catch (Exception e) {
                                            Debug.out(e);
                                        }

                                    }

                                    checkingForAds = false;

                                    debug("exit - adsReceived");
                                }

                                public void replyReceived(String replyType, Map mapHashes) {
                                    checkingForAds = false;
                                    debug("bad reply. " + mapHashes.get("text"));
                                }
                            });

                } catch (Exception e) {
                    checkingForAds = false;
                    Debug.out(e);
                }
            }
        };
        thread.run();
    }//downloadManagerAddedHook


    /**
     * debugNotAdEnabledReason - which reason is a torrent not ad-enabled.
     * @param dms - DownloadManager[]
     */
    private void determineReasonAdNotEnabled(final DownloadManager[] dms) {
        for (int i = 0; i < dms.length; i++) {
            final DownloadManager dm = dms[i];
            TOTorrent torrent = dm.getTorrent();
            StringBuffer sb = new StringBuffer();
            sb.append( new String(torrent.getName()) ).append(" resons: ");
            if( PlatformTorrentUtils.isContent(torrent, true) ){ sb.append( "A-isContent , " ); }
            if( PlatformTorrentUtils.getContentHash(torrent) != null ){ sb.append( "B-getConentHash , " ); }
            if( PlatformTorrentUtils.isContentAdEnabled(torrent)){ sb.append( "C-isContentAdEnabled" ); }
            debug( sb.toString() );
        }//for
    }//determineReasonAdNotEnabled

    /**
     * Determine the location of the ASX file. Should be in the same directory as content if possible.
     * @param dm - download manager.
     * @return - File with name of file.
     */
    private File determineASXFileLocation(DownloadManager dm) {
        debug("determineASXFileLocation");
        EnhancedDownloadManager edm = DownloadManagerEnhancer.getSingleton().getEnhancedDownload(
				dm);
		File file;
		if (edm != null) {
			file = edm.getPrimaryFile().getFile(true);
		} else {
			file = new File(dm.getDownloadState().getPrimaryFile());
		}
		return new File(file.getAbsolutePath() + ".asx");
	}

    /**
     * For the content associated with the DownloadManager read the PlayerDataMap file from
     * the "media/azpd" directory, find the URLs for the ad and content. Then return that
     * ASX file as a String.
     *
     * @param dmContent - DownloadManager for the content.
     * @return String - completed playlist.
     */
    public String replaceASXParams(final DownloadManager dmContent)
    {

        debug("replaceASXParams");
        //Look for the PlayerDataMap file.
        Map playerDataMap = getPlayerDataMap(dmContent);
        String origPlaylist = (String) playerDataMap.get("playlist");

        if( origPlaylist==null ){
            debug("The data map is missing 'playlist' key. - playerDataMap="+playerDataMap);

            throw new IllegalStateException("The data map is missing 'playlist' key: "+playerDataMap);
        }

        
        //Replace the following params in the original playlist
        StringBuffer repBuffer = new StringBuffer(origPlaylist);

        
        String contentPath = TorrentListViewsUtils.getContentUrl(dmContent);

        debug("  contentPath: "+contentPath);
        replace(repBuffer,"<##-CONTENT-PATH-##>",contentPath);


        boolean isNullAd = determinIfNullAd(dmContent);
        if( !isNullAd ){
            //Find location of the ad(s).
            File adFile = getAdMediaFromContentDownloadManager(dmContent);
            debug("  adPath: "+adFile.getAbsolutePath());
            replace(repBuffer,"<##-AD-PATH-##>",adFile.getAbsolutePath());
        }

        //pass the params to the player via the download manager.
        addParmasToDownloadManager(dmContent,playerDataMap);

        return repBuffer.toString();
    }//replaceASXParams

    /**
     * If the map returned from the server has a lenght of zero, this assume this is
     * a NullAd type. This means that the playlist should not have a AD-PATH in it.
     * @param contentDM - DownloadManager has Map returned from web-server.
     * @return  -  boolean - true if null ad, false otherwise.
     */
    private boolean determinIfNullAd(DownloadManager contentDM){

        Map map = DCAdManager.getPlayerDataMap(contentDM);
        List adHashList = (List) map.get("ad_hash");

        if(adHashList==null){
            return true;
        }

        //is a null ad it size is zero, otherwise not a null ad.
        return (adHashList.size() <= 0);
    }//determineIfNullAd


    /**
     * The azdp data is passed to the embedded player via the DownloadManager
     * @param dmContent - download manager.
     * @param playerData - Map with player data.
     */
    public static void addParmasToDownloadManager(DownloadManager dmContent, Map playerData){

        //String add a time-stamp to the map which can be used to expire it.
        String createTime = ""+System.currentTimeMillis();
        playerData.put("create-time",createTime);

        //The key must be in synch with the embedded player.
        dmContent.setData("web-ad-params",playerData);

    }//addParamsToDownloadManager

    /**
     * a) Read the file in media/azpd
     * (b) determine the location of the advert.
     * (c) write it to the location specified.
     * @param asxFileLocation - Location of ASX file to build.
     * @param dmContent - DownloadManager
     */
    public void writeASXFile(final File asxFileLocation,
                             final DownloadManager dmContent)
    {
        String finishedASX = replaceASXParams(dmContent);
        FileUtil.writeBytesAsFile(asxFileLocation.getAbsolutePath(), finishedASX.getBytes() );
    }

    private static void replace(StringBuffer sb, String param, String value){
        replace(sb,param,value,0);
    }//replace



    private static void replace(StringBuffer sb, String param, String value, int startIndex){

        int s = sb.indexOf(param,startIndex);
        sb.replace(s, s+param.length(), value );

    }//replace

    private File getAdMediaFromContentDownloadManager(DownloadManager contentDM){

        debug("getAdMediaFromContentDownloadManager");

        GlobalManager gm = contentDM.getGlobalManager();
        Map map = DCAdManager.getPlayerDataMap(contentDM);
        List adHashList = (List) map.get("ad_hash");
        String adHash = (String) adHashList.get(0);

        if(adHash==null || adHash.equals("") ){
            throw new IllegalStateException("No adHash found map="+map);
        }

        HashWrapper adHashWrapper = new HashWrapper(Base32.decode(adHash));
        DownloadManager dmAd = gm.getDownloadManager(adHashWrapper); //ToDo: ad a check here for a null result.

        File adFile;
        if( !(dmAd==null) ){
            adFile = dmAd.getDiskManagerFileInfo()[0].getFile(true);
        }else{
            //what is an alternate method to find the adFile?
            debug("###  TODO  ### - Need to find an alternate method to get the ad media file location. ");
            throw new NullPointerException("Need to get an ad to play.");
        }

        return adFile;
    }

  /**
   * Temporary function so that EMP <= 2.0.4 can work
   * 
   * @param dm
   * @param url
   * @param createdListener
   *
   * @since 3.0.4.3
   */
	public void createASX(DownloadManager dm, String url,
			ASXCreatedListener createdListener) {
		createASX(dm, createdListener);
	}

	public void createASX(final DownloadManager dm,
			final ASXCreatedListener asxCreatedListener) {
		if (dm == null) {
			debug("createASX - null dm");
			return;
		}
		String name = dm.getDisplayName();

        debug("enter - createASX");
        try {
            TOTorrent torrent = dm.getTorrent();
            if (torrent == null || !PlatformTorrentUtils.isContent(torrent, true)) {
          		debug("createASX - " + name + " not our content");
          		return;
            }

            File asxFile = null;

            //Check the age of the current ASX file.
            Object lastASXObject = dm.getData("LastASX");
            if (lastASXObject instanceof Long) {
                long lastASX = ((Long) lastASXObject).longValue();
                if (SystemTime.getCurrentTime() - lastASX < EXPIRE_ASX) {
                    asxFile = determineASXFileLocation(dm);
                    if (asxFile.isFile()) {
                        debug("playing " + name
                                + " using existing asx: " + asxFile + "; expires in "
                                + (EXPIRE_ASX - (SystemTime.getCurrentTime() - lastASX)));
                        if (asxCreatedListener != null) {
                            asxCreatedListener.asxCreated(asxFile);
                        }
                        return;
                    }
                }
            }

            //Read the media/azpd/player data file - create an ASX file.
            File newASXFile = determineASXFileLocation(dm);
            writeASXFile(newASXFile,dm);
            
            asxFile = determineASXFileLocation(dm);
            if (asxFile.isFile()) {
                debug("playing " + name + " using existing asx: " + asxFile);
                if (asxCreatedListener != null) {
                    asxCreatedListener.asxCreated(asxFile);
                }
                return;
            }

            debug("getting asx for " + name);

        } catch (Exception e) {
            if (asxCreatedListener != null) {
                asxCreatedListener.asxFailed();
            }
            debug("createASX exception for " + name, e);
        }
        debug("exit - createASX");
    }//createASX
    

    public interface ASXCreatedListener
    {
        public void asxCreated(File asxFile);

        public void asxFailed();
    }


    /**
	 * @param nowPlaying -
     * @return true if it is an ad
	 * @since 3.0.2.3
	 */
	public boolean isAd(String nowPlaying) {
		if (nowPlaying == null) {
			return false;
		}
		File file = new File(nowPlaying);
		DownloadManager[] ads = getAds(true);
		for (int i = 0; i < ads.length; i++) {
			DownloadManager downloadManager = ads[i];
			DiskManagerFileInfo[] fileInfos = downloadManager.getDiskManagerFileInfo();
			for (int j = 0; j < fileInfos.length; j++) {
				DiskManagerFileInfo fileinfo = fileInfos[j];
				File adFile = fileinfo.getFile(true);
				if (adFile.equals(file)) {
					return true;
				}
			}
		}
		return false;
	}

    public DownloadManager[] getAds(boolean bIncludeIncomplete) {
        if (bIncludeIncomplete) {
            return (DownloadManager[]) adsDMList.toArray(new DownloadManager[0]);
        }

        ArrayList ads = new ArrayList(adsDMList);
        for (Iterator iter = ads.iterator(); iter.hasNext();) {
            DownloadManager dm = (DownloadManager) iter.next();
            if (!dm.getAssumedComplete()) {
                iter.remove();
            }
        }

        debug("There are " + ads.size() + " ads "
                + (bIncludeIncomplete ? " including incomplete" : ""));
        return (DownloadManager[]) ads.toArray(new DownloadManager[0]);
    }//getAds


    static final String EXT_AZUREUS_PLAYER_DATA = "azpd";
    public static Map getPlayerDataMap(DownloadManager dm)
    {
        try
        {
            File azureusPlayDataFile = determineAzpdFileLocation(dm);

            String data = FileUtil.readFileAsString(azureusPlayDataFile,10000000);
            return JSONUtils.decodeJSON(data);

        }catch(TOTorrentException tte){
            debug("TOTorrent Error - getPlayerDataMap(): "+tte);
            tte.printStackTrace();
            return null;
        }catch(Throwable t){

            debug("Error - getPlayerDataMap(): "+t);
            t.printStackTrace();

            return null;
        }

    }//getPlayerDataMap


    /**
     * Get the location of the azpd file.
     * @param dm - DownloadManager
     * @return - File -
     * @throws TOTorrentException - t
     */
    private static File determineAzpdFileLocation(DownloadManager dm)
        throws TOTorrentException
    {
        File azpdDir = getAzpdDir();

        String fileNamePrefix = dm.getTorrent().getHashWrapper().toBase32String();
        return new File( azpdDir ,fileNamePrefix+"."+EXT_AZUREUS_PLAYER_DATA );
    }

    public static File getAzpdDir() {
        File mediaDir = FileUtil.getUserFile("media");
        if( !mediaDir.exists() ){
            FileUtil.mkdirs(mediaDir);
        }
        File azpdDir = new File(mediaDir,"azpd");
        if( !azpdDir.exists() ){
            FileUtil.mkdirs(azpdDir);
        }
        return azpdDir;
    }

	public boolean isCheckingForNewAds() {
		return checkingForAds;
	}

    private static void debug(String msg){
        PlatformDCAdManager.debug("DCAdManager: "+msg);
    }

    private static void debug(String msg, Throwable t){
        PlatformDCAdManager.debug("DCAdManager: "+msg,t);
    }
}//class
