package com.aelitis.azureus.core.messenger.config;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.bouncycastle.util.encoders.Base64;

import java.util.*;
import java.io.File;
import java.io.IOException;

import com.aelitis.azureus.util.DCAdManager;
import com.aelitis.azureus.util.JSONUtils;
import com.aelitis.azureus.util.AzpdFileAccess;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.PlatformMessengerListener;

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

public class PlatformDCAdManager
{
	// one week timeout for unsent impressions
	private static final int UNSENT_TIMEOUT = 1000 * 3600 * 24 * 7;
	public static String RPC_LISTENER_ID = "asxads";

    public static String OP_GETADVERT = "get-advert";
    public static String OP_SAVEIMPRESSIONS = "save-impressions";
	public static List unsentImpressions = new ArrayList();
	public static AEMonitor mon_unsentImpressions = new AEMonitor(
			"unsavedImpressions");
	public static boolean DEBUG_ADS = false;
	private static int RESEND_DELAY = 1000 * 60 * 10; // 10 min
	private static TimerEvent resendEvent;


    /**
     *  Create the call to the web-servers, and handle the response.
     * @param adEnabledDownload - DownloadManager of content.
     * @param maxDelayMS - Time to wait for the response.
     * @param replyListener  - Needed by the PlatformManagerListener
     */
    public static void getAdvert(final DownloadManager adEnabledDownload,
                                 long maxDelayMS,
                                 final GetAdvertDataReplyListener replyListener)
    {
        debug("enter - PlatformDCDdManager.getAdvert");

		//prepare the parameters to send
        String contentHash="";
        try{
            contentHash = adEnabledDownload.getTorrent().getHashWrapper().toBase32String();
        }catch(TOTorrentException te){
            debug("Failed to get currentHash",te);
            te.printStackTrace();
        }

        
        List contentList = new ArrayList();
        //currently a list incase future calls will send multiple content.
        contentList.add(contentHash);

        List adList = getExistingAds();

        //send
        Map params = new HashMap();
        params.put("hashes",contentList);
        params.put("ads",adList);
        PlatformMessage message = new PlatformMessage("AZMSG",RPC_LISTENER_ID,OP_GETADVERT,params, maxDelayMS);

		//create a default azpd file.
		if( !azpdFileFound(message) ){
			File f = determineAzpdFileName(message);
			saveTempAzpdFile(f);
		}


		//deal with response.
        PlatformMessenger.queueMessage(message, new PlatformMessengerListener(){

                public void messageSent(PlatformMessage message) {
                    debug("getAdvert - messageSent");
                    if (replyListener != null) {
                        replyListener.messageSent();
                    }
                }//messageSent

			public void replyReceived(PlatformMessage message, String replyType,
					Map reply) {
				boolean success = false;
				try {
					debug("getAdvert - replyReceived");
					debug(replyType, message, reply);

					//Deserialise and then download the torrent file.
					//if( reply!=null && reply.containsKey("torrents") )
					if (reply != null) {
						List adTorrents = new ArrayList();
						List torrentsList = (List) reply.get("torrents");
						if (torrentsList != null) {
							for (int i = 0; i < torrentsList.size(); i++) {
								byte[] torrentBEncoded = Base64.decode((String) torrentsList.get(i));
								try {
									TOTorrent torrent = TOTorrentFactory.deserialiseFromBEncodedByteArray(torrentBEncoded);
									adTorrents.add(torrent);
								} catch (TOTorrentException e) {
									Debug.out("PlatformDCAdManager.getAdvert - replyRecieved: "
											+ e);
								}
							}
						}

						Map webParams = saveResponseToAzpdFile(reply, message);
						DCAdManager.addParmasToDownloadManager(adEnabledDownload, webParams);

						success = true;
						if (replyListener != null) {
							replyListener.adsReceived(adTorrents, webParams);
						}
					}
				} catch (Exception e) {
					Debug.out(e);
				}

				if (!success && replyListener != null) {
					replyListener.replyReceived(replyType, reply);
				}
			}//replyReceived
		}//class PlatformMessengerListener
		);

        debug("leave - PlatformDCDdManager.getAdvert");
    }//getAdvert

	/**
	 * create a temporary azpd file.
	 * @param azpdFile
	 */
	private static void saveTempAzpdFile(File azpdFile){

		String content = getTempAzpdTemplate();
		AzpdFileAccess.writeAzpdFile(azpdFile,content);

		debug( "temp data=_"+content+"_  to azpd file="+azpdFile.getAbsolutePath() );
	}

	/**
	 * Template for a default azpd file. 
	 * @return - template file.
	 */
	private static String getTempAzpdTemplate(){
		//ToDo: Should be updateable configuration.
		return "{\"ad_hash\":[],\"playlist\":\"<ASX version=\\\"3.0\\\">\\n<ENTRY ClientSkip=\\\"yes\\\">\\n<TITLE>" +
				"Default<\\/TITLE>\\n<PARAM NAME=\\\"Prebuffer\\\" VALUE=\\\"true\\\"\\/>\\n" +
				"<REF HREF=\\\"<##-CONTENT-PATH-##>\\\"\\/>\\n<\\/ENTRY>\\n<\\/ASX>\"}";
	}


	/**
	 * Save the replay to the azpd file.
	 * @param reply -
	 * @param message -
	 * @return - Map with params.
	 */
	private static Map saveResponseToAzpdFile(Map reply, PlatformMessage message) {
        //What we might want to do here is remove "torrents" from the Map reply and then save the
        //entire result.
        Map saveToFile = new HashMap();
        saveToFile.putAll(reply);
        saveToFile.remove("torrents");

		//Did we have an error? like when off-line. A "Thowable" param in map is an indication.
		if( saveToFile.get("Throwable")!=null ){
			return saveDefaultResponseToAzpdFile(message);
		}

		long currTime = System.currentTimeMillis();
		saveToFile.put( AzpdFileAccess.PARAM_CREATE_TIME, ""+currTime );

		//if the web doesn't specify an expire time, the make it one week.
		if( saveToFile.get( AzpdFileAccess.PARAM_EXPIRE_TIME )==null ){
			long expireTime = currTime + 1000 * 60 * 60 * 24 * 7;//one week;
			saveToFile.put( AzpdFileAccess.PARAM_EXPIRE_TIME, ""+expireTime );
		}

		String s = JSONUtils.encodeToJSON(saveToFile);
		File file = determineAzpdFileName(message);

		AzpdFileAccess.writeAzpdFile(file,s);

		debug( "data=_"+s+"_  to azpd file="+file.getAbsolutePath() );
        
        return JSONUtils.decodeJSON(s);
    }


	/**
	 * When offline of another error occurs. Save the default template to play only the content.
	 * It should expire immediately, to replace it on restart.
	 * @param message -
	 * @return -
	 */
	private static Map saveDefaultResponseToAzpdFile(PlatformMessage message){
		File azpdFile = determineAzpdFileName(message);
		saveTempAzpdFile(azpdFile);
		debug("using content-only template due to error.");
		try{
			Map webParams = AzpdFileAccess.readAzpdFileToMap(azpdFile);
			webParams.put( AzpdFileAccess.PARAM_IS_OFFLINE, "true" );
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
		return new HashMap();
	}

	/**
	 * Based on the hash determine the Azpd file name.
	 * @param message -
	 * @return File - azpd file.
	 */
	private static File determineAzpdFileName(PlatformMessage message) {
		File azpdFile = AzpdFileAccess.getAzpdDir();

		//Get the content hash from the message.
		String azpdFileNameBase = "no_file";
		Map requestParamMap = message.getParameters();
		if( requestParamMap!=null ){
			List contentList = (List) requestParamMap.get("hashes");
			if( contentList!=null ){
				azpdFileNameBase = (String) contentList.get(0);
			}
		}

		File file = new File(azpdFile, azpdFileNameBase+".azpd" );
		return file;
	}

	/**
	 * True if the azpd file for content exists.
	 * @param message - to get the hash
	 * @return boolean - true if it exists.
	 */
	private static boolean azpdFileFound(PlatformMessage message){
		File f = determineAzpdFileName(message);
		return f.exists();
	}


	private static List getExistingAds() {
        DownloadManager[] existingAds = DCAdManager.getInstance().getAds(true);
        List adList = new ArrayList();
        for (int i = 0; i < existingAds.length; i++) {
            DownloadManager dm = existingAds[i];

            try {
                TOTorrent torrent = dm.getTorrent();
                String hash = torrent.getHashWrapper().toBase32String();
                String adid = PlatformTorrentUtils.getAdId(torrent);//ToDo: Is AdId in torrent file no longer needed?

                Map mapAd = new HashMap();
                mapAd.put("hash", hash);

                adList.add(mapAd);
            } catch (TOTorrentException te) {
                debug("Failed while reading existing ads",te);
                te.printStackTrace();
            }
        }
        return adList;
    }


    public static interface GetAdvertDataReplyListener{
        public void messageSent();

        public void adsReceived(List torrents, Map webParams);

        public void replyReceived(String replyType, Map mapHashes);
    }


    /**
     * Send the impression information.
     * @param trackingID -
     * @param viewedOn -
     * @param contentHash -
     * @param torrentHash -
     * @param adHash      -
     * @param maxDelayMS  -
     */
    public static void saveImpresssion(String trackingID, long viewedOn,
            String contentHash, String torrentHash, String adHash,
            long maxDelayMS) {
        // pass in contentHash instead of DownloadManager in case the user removed
        // the DM (and we are retrying)
        try {
            Map ad = new HashMap();

            ad.put("tracking-id", trackingID);
            ad.put("viewed-on", new Long(viewedOn));
            ad.put("content-hash", contentHash);
            if (torrentHash != null) {
                ad.put("torrent-hash", torrentHash);
            }
            if (adHash != null) {
                ad.put("hash", adHash);
            }


            try {
                mon_unsentImpressions.enter();
                unsentImpressions.add(ad);
            } finally {
                mon_unsentImpressions.exit();
            }
            saveUnsentImpressions();
            sendUnsentImpressions(maxDelayMS);
        } catch (Exception e) {
            Debug.out(e);
        }
    }



    //ToDo: rename to sendUnsavedImpressions
    public static void sendUnsentImpressions(long maxDelayMS) {
        // clear unsentImpressions.  If storing fails, we'll add them back in
        List sendingImpressions;
        try {
            mon_unsentImpressions.enter();

            sendingImpressions = unsentImpressions;
            unsentImpressions = new ArrayList();
            saveUnsentImpressions();
        } finally {
            mon_unsentImpressions.exit();
        }

        if (sendingImpressions.size() == 0) {
            return;
        }

        final List fSendingImpressions = sendingImpressions;

        Map ads = new HashMap();
        ads.put("ads", fSendingImpressions);
        ads.put("rpc-version", new Long(2));

        try {
            debug("sending " + fSendingImpressions.size() + " impressions");
            PlatformMessage message = new PlatformMessage("AZMSG", RPC_LISTENER_ID,
                    OP_SAVEIMPRESSIONS, ads, maxDelayMS);

            PlatformMessenger.queueMessage(message, new PlatformMessengerListener() {
                public void replyReceived(PlatformMessage message, String replyType,
                        Map reply) {
                    if (!replyType.equals(PlatformMessenger.REPLY_RESULT)) {
                        debug("sending " + fSendingImpressions + " impressions failed. "
                                + reply);
                        try {
                            mon_unsentImpressions.enter();

                            unsentImpressions.addAll(fSendingImpressions);

                            _setupResendTimer();
                        } finally {
                            mon_unsentImpressions.exit();
                        }
                        saveUnsentImpressions();
                        return;
                    }
                    // TODO: check result to see which ones succeeded
                    debug("sending " + fSendingImpressions.size()
                            + " impressions completed");
                }

                public void messageSent(PlatformMessage message) {
                }

            });
        } catch (Exception e) {
            Debug.out(e);
        }
    }

    /**
     *
     *
     * @since 3.0.1.5
     */
    protected static void _setupResendTimer() {
        if (resendEvent != null) {
            resendEvent.cancel();
            resendEvent = null;
        }
        resendEvent = SimpleTimer.addEvent("resender",
                SystemTime.getOffsetTime(RESEND_DELAY), new TimerEventPerformer() {
                    public void perform(TimerEvent event) {
                        debug("resend impressions triggered");
                        sendUnsentImpressions(5000);
                    }
                });
    }

    /**
     *
     *
     * @since 3.0.1.5
     */
    private static void saveUnsentImpressions() {
        try {
            mon_unsentImpressions.enter();

            Map map = new HashMap();
            map.put("unsent", unsentImpressions);
            FileUtil.writeResilientConfigFile("unsentdata.config", map);
        } finally {
            mon_unsentImpressions.exit();
        }
    }

    public static void loadUnsentImpressions() {
        try {
            mon_unsentImpressions.enter();

            Map map = BDecoder.decodeStrings(FileUtil.readResilientConfigFile("unsentdata.config"));
            Object value = map.get("unsent");
            if (value instanceof List) {
                unsentImpressions = (List) value;
                for (Iterator iter = unsentImpressions.iterator(); iter.hasNext();) {
                    long viewedOn = 0;
                    Map ad = (Map) iter.next();
                    try {
                        if (ad.containsKey("viewed-on")) {
                            viewedOn = ((Long) ad.get("viewed-on")).longValue();
                        }
                    } catch (Exception e) {
                    }

                    if (SystemTime.getCurrentTime() - viewedOn > UNSENT_TIMEOUT) {
                        iter.remove();
                        debug("timing out impression " + ad.get("tracking-id"));
                    }
                }
            } else {
                unsentImpressions.clear();
            }
        } finally {
            mon_unsentImpressions.exit();
        }
    }

    public static void debug(String string) {
        debug(string, null);
    }

    /**
     * @param string -
     * @param e -
     *
     * @since 3.0.1.7
     */
    public static void debug(String string, Throwable e) {
        try {
            AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger("v3.ads");
            diag_logger.log(string);

            System.out.println(string);//ToDo: remove.

            if (e != null) {
                diag_logger.log(e);
                Debug.out(string, e);
            }
            if (com.aelitis.azureus.util.Constants.DIAG_TO_STDOUT || DEBUG_ADS) {
                System.out.println(Thread.currentThread().getName() + "|ADS|"
                        + System.currentTimeMillis() + "] " + string);
                if (e != null) {
                    e.printStackTrace();
                }
            }
        } catch (Throwable t) {
            // ignore
        }
    }

    /**
     * Delete this problem some day.
     * @param replyType -
     * @param message -
     * @param reply -
     */
    private static void debug(String replyType, PlatformMessage message, Map reply) {
        debug( "------ PlatformDCAdManager.getAdvert()  PlatformMessageListener.reply()  ------" );
        debug( replyType );
        debug( message.toString() );

        if(reply!=null){
            String wOrd = (String) reply.get("web_ord");
            String wAdId = (String) reply.get("web_ad_id");
            String playlist = (String) reply.get("playlist");
            debug("web_ord: "+wOrd);
            debug("web_ad_id: "+wAdId);
            debug("playlist: "+playlist);
        }
        debug( "-------------------------------------------------------------------------------" );
    }

}
