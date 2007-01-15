/**
 * Created on 10-Jan-2006
 * Created by Allan Crooks
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
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
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.pluginsimpl.remote;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResult;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;
import org.gudy.azureus2.plugins.download.DownloadStats;
import org.gudy.azureus2.plugins.ipfilter.IPFilter;
import org.gudy.azureus2.plugins.ipfilter.IPRange;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.peers.PeerStats;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.tracker.TrackerTorrent;

public class GenericRPAttributes {

    public static Map getAttributes(Object object, Class obj_class, Map attribute_types) {
        RemoteClassMap map = new RemoteClassMap(attribute_types);
        if (obj_class == DiskManagerFileInfo.class) {
            DiskManagerFileInfo dmfi = (DiskManagerFileInfo)object;
            map.put("access_mode",        dmfi.getAccessMode());
            map.put("downloaded",         dmfi.getDownloaded());
            map.put("file",               dmfi.getFile());
            map.put("first_piece_number", dmfi.getFirstPieceNumber());
            map.put("num_pieces",         dmfi.getNumPieces());
            map.put("is_priority",        dmfi.isPriority());
            map.put("is_skipped",         dmfi.isSkipped());
            map.put("length",             dmfi.getLength());
            map.put("link",               dmfi.getLink());            
            map.put("is_deleted",         dmfi.isDeleted());
        }
        else if (obj_class == Download.class) {
            Download dload = (Download)object;
            map.put("torrent",             dload.getTorrent());
            map.put("stats",               dload.getStats());
            map.put("announce_result",     dload.getLastAnnounceResult());
            map.put("scrape_result",       dload.getLastScrapeResult());
            map.put("position",            dload.getPosition());
            map.put("force_start",         dload.isForceStart());
            map.put("name",                dload.getName());
            map.put("creation_time",       dload.getCreationTime());
            map.put("download_peer_id",    dload.getDownloadPeerId());
            map.put("error_state_details", dload.getErrorStateDetails());
            map.put("max_download_rate",   dload.getMaximumDownloadKBPerSecond());
            map.put("max_upload_rate",     dload.getUploadRateLimitBytesPerSecond());
            map.put("position",            dload.getPosition());
            map.put("save_path",           dload.getSavePath());
            map.put("seeding_rank",        dload.getSeedingRank());
            map.put("state",               dload.getState());
            map.put("sub_state",           dload.getSubState());
            map.put("torrent_file",        dload.getTorrentFileName());
            map.put("checking",            dload.isChecking());
            map.put("complete",            dload.isComplete());
            map.put("messaging_enabled",   dload.isMessagingEnabled());
            map.put("paused",              dload.isPaused());
            map.put("persistent",          dload.isPersistent());
    	}
        else if (obj_class == DownloadAnnounceResult.class) {
            DownloadAnnounceResult dar = (DownloadAnnounceResult)object;
            map.put("seed_count",          dar.getSeedCount());
            map.put("non_seed_count",      dar.getNonSeedCount());
            map.put("error",               dar.getError());
            map.put("reported_peer_count", dar.getReportedPeerCount());
            map.put("response_type",       dar.getResponseType());
            map.put("time_to_wait",        dar.getTimeToWait());
            map.put("url",                 dar.getURL());
        }
        else if (obj_class == DownloadScrapeResult.class) {
            DownloadScrapeResult dsr = (DownloadScrapeResult)object;
            map.put("seed_count",      dsr.getSeedCount());
            map.put("non_seed_count",  dsr.getNonSeedCount());
            map.put("next_start_time", dsr.getNextScrapeStartTime());
            map.put("response_type",   dsr.getResponseType());
            map.put("start_time",      dsr.getScrapeStartTime());
            map.put("status",          dsr.getStatus());
            map.put("url",             dsr.getURL());
        }
        else if (obj_class == DownloadStats.class) {
            DownloadStats stats = (DownloadStats)object;
            map.put("downloaded",              stats.getDownloaded());
            map.put("uploaded",                stats.getUploaded());
            map.put("completed",               stats.getCompleted());
            map.put("downloadCompletedLive",   stats.getDownloadCompleted(true));
            map.put("downloadCompletedStored", stats.getDownloadCompleted(false));
            map.put("status",                  stats.getStatus());
            map.put("status_localised",        stats.getStatus(true));
            map.put("upload_average",          stats.getUploadAverage());
            map.put("download_average",        stats.getDownloadAverage());
            map.put("eta",                     stats.getETA());
            map.put("share_ratio",             stats.getShareRatio());
            map.put("availability",            stats.getAvailability());
            map.put("health",                  stats.getHealth());
            map.put("discarded",               stats.getDiscarded());
            map.put("elapsed_time",            stats.getElapsedTime());
            map.put("hash_fails",              stats.getHashFails());
            map.put("seconds_downloading",     stats.getSecondsDownloading());
            map.put("seconds_only_seeding",    stats.getSecondsOnlySeeding());
            map.put("download_directory",      stats.getDownloadDirectory());
            map.put("target_file_or_dir",      stats.getTargetFileOrDir());
            map.put("time_started",            stats.getTimeStarted());
            map.put("time_started_seeding",    stats.getTimeStartedSeeding());
            map.put("total_average",           stats.getTotalAverage());
            map.put("tracker_status",          stats.getTrackerStatus());
            map.put("remaining",               stats.getRemaining());
        }
        else if (obj_class == IPFilter.class) {
            IPFilter filter = (IPFilter)object;
            map.put("last_update_time",      filter.getLastUpdateTime());
            map.put("number_of_ranges",      filter.getNumberOfRanges());
            map.put("number_of_blocked_ips", filter.getNumberOfBlockedIPs());
        }
        else if (obj_class == IPRange.class) {
            IPRange range = (IPRange)object;
            map.put("description", range.getDescription());
            map.put("start_ip",    range.getStartIP());
            map.put("end_ip",      range.getEndIP());
        }
        else if (obj_class == LoggerChannel.class) {
        	LoggerChannel lc = (LoggerChannel)object;
        	map.put("name",    lc.getName());
        	map.put("enabled", lc.isEnabled());
        }
        else if (obj_class == Peer.class) {
            Peer peer = (Peer)object;
            map.put("stats",              peer.getStats());
            map.put("ip",                 peer.getIp());
            map.put("port",               peer.getPort());
            map.put("client",             peer.getClient());
            map.put("id",                 peer.getId());
            map.put("percent_done",       peer.getPercentDoneInThousandNotation());
            map.put("snubbed_time",       peer.getSnubbedTime());
            map.put("state",              peer.getState());
            map.put("choked",             peer.isChoked());
            map.put("choking",            peer.isChoking());
            map.put("download_possible",  peer.isDownloadPossible());
            map.put("incoming",           peer.isIncoming());
            map.put("interested",         peer.isInterested());
            map.put("interesting",        peer.isInteresting());
            map.put("optimistic_unchoke", peer.isOptimisticUnchoke());
            map.put("seed",               peer.isSeed());
            map.put("snubbed",            peer.isSnubbed());
            map.put("transfer_available", peer.isTransferAvailable());
        }
        else if (obj_class == PeerStats.class) {
            PeerStats stats = (PeerStats)object;
            map.put("download_average",                  stats.getDownloadAverage());
            map.put("reception",                         stats.getReception());
            map.put("statistic_sent_average",            stats.getStatisticSentAverage());
            map.put("time_since_connection_established", stats.getTimeSinceConnectionEstablished());
            map.put("total_average",                     stats.getTotalAverage());
            map.put("total_discarded",                   stats.getTotalDiscarded());
            map.put("total_received",                    stats.getTotalReceived());
            map.put("total_sent",                        stats.getTotalSent());
            map.put("upload_average",                    stats.getUploadAverage());
         }
        else if (obj_class == PluginConfig.class) {
            PluginConfig pconfig = (PluginConfig)object;
            String[] property_names = new String[] {
                PluginConfig.CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC,
                PluginConfig.CORE_PARAM_INT_MAX_UPLOAD_SPEED_SEEDING_KBYTES_PER_SEC,
                PluginConfig.CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC,
                PluginConfig.CORE_PARAM_INT_MAX_CONNECTIONS_PER_TORRENT,
                PluginConfig.CORE_PARAM_INT_MAX_CONNECTIONS_GLOBAL,
                PluginConfig.CORE_PARAM_INT_MAX_DOWNLOADS,
                PluginConfig.CORE_PARAM_INT_MAX_ACTIVE,
                PluginConfig.CORE_PARAM_INT_MAX_ACTIVE_SEEDING,
                PluginConfig.CORE_PARAM_INT_MAX_UPLOADS,
                PluginConfig.CORE_PARAM_INT_MAX_UPLOADS_SEEDING
            };
            // All integers at the moment.
            int[] property_values = new int[property_names.length];
            for (int i=0; i<property_values.length; i++) {
                property_values[i] = pconfig.getIntParameter(property_names[i]);
            }
            map.put("cached_property_names",  property_names);
            map.put("cached_property_values", property_values);
        }
        else if (obj_class == PluginInterface.class) {
            PluginInterface pi = (PluginInterface)object;
            map.put("azureus_name",    Constants.AZUREUS_NAME);
            map.put("azureus_version", Constants.AZUREUS_VERSION);
            map.put("plugin_id",       pi.getPluginID());
            map.put("plugin_name",     pi.getPluginName());
        }
        else if (obj_class == Torrent.class) {
            Torrent torrent = (Torrent)object;
            map.put("name",          torrent.getName());
            map.put("size",          torrent.getSize());
            map.put("hash",          torrent.getHash());
            map.put("comment",       torrent.getComment());
            map.put("created_by",    torrent.getCreatedBy());
            map.put("creation_date", torrent.getCreationDate());
            map.put("encoding",      torrent.getEncoding());
            map.put("piece_count",   torrent.getPieceCount());
            map.put("piece_size",    torrent.getPieceSize());
            map.put("private",       torrent.isPrivate());
            map.put("announce_url",  torrent.getAnnounceURL());
        }
        else if (obj_class == TrackerTorrent.class) {
            TrackerTorrent ttobject = (TrackerTorrent)object;
            map.put("torrent",                ttobject.getTorrent());
            map.put("status",                 ttobject.getStatus());
            map.put("total_uploaded",         ttobject.getTotalUploaded());
            map.put("total_downloaded",       ttobject.getTotalDownloaded());
            map.put("average_uploaded",       ttobject.getAverageUploaded());
            map.put("average_downloaded",     ttobject.getAverageDownloaded());
            map.put("total_left",             ttobject.getTotalLeft());
            map.put("completed_count",        ttobject.getCompletedCount());
            map.put("total_bytes_in",         ttobject.getTotalBytesIn());
            map.put("average_bytes_in",       ttobject.getAverageBytesIn());
            map.put("total_bytes_out",        ttobject.getTotalBytesOut());
            map.put("average_bytes_out",      ttobject.getAverageBytesOut());
            map.put("scrape_count",           ttobject.getScrapeCount());
            map.put("average_scrape_count",   ttobject.getAverageScrapeCount());
            map.put("announce_count",         ttobject.getAnnounceCount());
            map.put("average_announce_count", ttobject.getAverageAnnounceCount());
            map.put("seed_count",             ttobject.getSeedCount());
            map.put("leecher_count",          ttobject.getLeecherCount());
            map.put("bad_NAT_count",          ttobject.getBadNATCount());
        }
        return map;
    }

    private static Map class_definitions = null;
    static {
        class_definitions = new HashMap();
        Map attributes = null;
        Class plugin_class = null;

        attributes = new HashMap();
        plugin_class = DiskManagerFileInfo.class;
        attributes.put("access_mode",        int.class);
        attributes.put("downloaded",         long.class);
        attributes.put("file",               File.class);
        attributes.put("first_piece_number", int.class);
        attributes.put("num_pieces",         int.class);
        attributes.put("is_priority",        boolean.class);
        attributes.put("is_skipped",         boolean.class);
        attributes.put("length",             long.class);
        attributes.put("link",               File.class);        
        attributes.put("is_deleted",         boolean.class);
        class_definitions.put(plugin_class, attributes);

        attributes = new HashMap();
        plugin_class = Download.class;
        attributes.put("torrent",             Torrent.class);
        attributes.put("stats",               DownloadStats.class);
        attributes.put("announce_result",     DownloadAnnounceResult.class);
        attributes.put("scrape_result",       DownloadScrapeResult.class);
        attributes.put("position",            int.class);
        attributes.put("force_start",         boolean.class);
        attributes.put("name",                String.class);
        attributes.put("creation_time",       long.class);
        attributes.put("download_peer_id",    byte[].class);
        attributes.put("error_state_details", String.class);
        attributes.put("max_download_rate",   int.class);
        attributes.put("max_upload_rate",     int.class);
        attributes.put("position",            int.class);
        attributes.put("save_path",           String.class);
        attributes.put("seeding_rank",        int.class);
        attributes.put("state",               int.class);
        attributes.put("sub_state",           int.class);
        attributes.put("torrent_file",        String.class);
        attributes.put("checking",            boolean.class);
        attributes.put("complete",            boolean.class);
        attributes.put("messaging_enabled",   boolean.class);
        attributes.put("paused",              boolean.class);
        attributes.put("persistent",          boolean.class);
        class_definitions.put(plugin_class, attributes);

        attributes = new HashMap();
        plugin_class = DownloadAnnounceResult.class;
        attributes.put("seed_count",          int.class);
        attributes.put("non_seed_count",      int.class);
        attributes.put("error",               String.class);
        attributes.put("reported_peer_count", int.class);
        attributes.put("response_type",       int.class);
        attributes.put("time_to_wait",        long.class);
        attributes.put("url",                 URL.class);
        class_definitions.put(plugin_class, attributes);

        attributes = new HashMap();
        plugin_class = DownloadScrapeResult.class;
        attributes.put("seed_count",      int.class);
        attributes.put("non_seed_count",  int.class);
        attributes.put("next_start_time", long.class);
        attributes.put("response_type",   int.class);
        attributes.put("start_time",      long.class);
        attributes.put("status",          String.class);
        attributes.put("url",             URL.class);
        class_definitions.put(plugin_class, attributes);

        attributes = new HashMap();
        plugin_class = DownloadStats.class;
        attributes.put("downloaded",              long.class);
        attributes.put("uploaded",                long.class);
        attributes.put("completed",               int.class);
        attributes.put("downloadCompletedLive",   int.class);
        attributes.put("downloadCompletedStored", int.class);
        attributes.put("status",                  String.class);
        attributes.put("status_localised",        String.class);
        attributes.put("upload_average",          long.class);
        attributes.put("download_average",        long.class);
        attributes.put("eta",                     String.class);
        attributes.put("share_ratio",             int.class);
        attributes.put("availability",            float.class);
        attributes.put("health",                  int.class);
        attributes.put("discarded",               long.class);
        attributes.put("elapsed_time",            String.class);
        attributes.put("hash_fails",              long.class);
        attributes.put("seconds_downloading",     long.class);
        attributes.put("seconds_only_seeding",    long.class);
        attributes.put("download_directory",      String.class);
        attributes.put("target_file_or_dir",      String.class);
        attributes.put("time_started",            long.class);
        attributes.put("time_started_seeding",    long.class);
        attributes.put("total_average",           long.class);
        attributes.put("tracker_status",          String.class);
        attributes.put("remaining",               long.class);
        class_definitions.put(plugin_class, attributes);

        attributes = new HashMap();
        plugin_class = IPFilter.class;
        attributes.put("last_update_time",      long.class);
        attributes.put("number_of_ranges",      int.class);
        attributes.put("number_of_blocked_ips", int.class);
        class_definitions.put(plugin_class, attributes);

        attributes = new HashMap();
        plugin_class = IPRange.class;
        attributes.put("description", String.class);
        attributes.put("start_ip",    String.class);
        attributes.put("end_ip",      String.class);
        class_definitions.put(plugin_class, attributes);
        
        attributes = new HashMap();
        plugin_class = LoggerChannel.class;
        attributes.put("enabled", boolean.class);
        attributes.put("name",    String.class);
        class_definitions.put(plugin_class, attributes);
        
        attributes = new HashMap();
        plugin_class = Peer.class;
        attributes.put("stats",              PeerStats.class);
        attributes.put("ip"  ,               String.class);
        attributes.put("port",               int.class);
        attributes.put("client",             String.class);
        attributes.put("id",                 byte[].class);
        attributes.put("percent_done",       int.class);
        attributes.put("snubbed_time",       long.class);
        attributes.put("state",              int.class);
        attributes.put("choked",             boolean.class);
        attributes.put("choking",            boolean.class);
        attributes.put("download_possible",  boolean.class);
        attributes.put("incoming",           boolean.class);
        attributes.put("interested",         boolean.class);
        attributes.put("interesting",        boolean.class);
        attributes.put("optimistic_unchoke", boolean.class);
        attributes.put("seed",               boolean.class);
        attributes.put("snubbed",            boolean.class);
        attributes.put("transfer_available", boolean.class);
        class_definitions.put(plugin_class, attributes);

        attributes = new HashMap();
        plugin_class = PeerStats.class;
        attributes.put("download_average",                  int.class);
        attributes.put("reception",                         int.class);
        attributes.put("statistic_sent_average",            int.class);
        attributes.put("time_since_connection_established", long.class);
        attributes.put("total_average",                     int.class);
        attributes.put("total_discarded",                   long.class);
        attributes.put("total_received",                    long.class);
        attributes.put("total_sent",                        long.class);
        attributes.put("upload_average",                    int.class);
        class_definitions.put(plugin_class, attributes);
        
        attributes = new HashMap();
        plugin_class = PluginConfig.class;
        attributes.put("cached_property_names",  new String[0].getClass());
        attributes.put("cached_property_values", new Object[0].getClass());
        class_definitions.put(plugin_class, attributes);

        attributes = new HashMap();
        plugin_class = PluginInterface.class;
        attributes.put("azureus_name",    String.class);
        attributes.put("azureus_version", String.class);
        attributes.put("plugin_id",       String.class);
        attributes.put("plugin_name",     String.class);
        class_definitions.put(plugin_class, attributes);

        attributes = new HashMap();
        plugin_class = TrackerTorrent.class;
        attributes.put("torrent",                Torrent.class);
        attributes.put("status",                 int.class);
        attributes.put("total_uploaded",         long.class);
        attributes.put("total_downloaded",       long.class);
        attributes.put("average_uploaded",       long.class);
        attributes.put("average_downloaded",     long.class);
        attributes.put("total_left",             long.class);
        attributes.put("completed_count",        long.class);
        attributes.put("total_bytes_in",         long.class);
        attributes.put("average_bytes_in",       long.class);
        attributes.put("total_bytes_out",        long.class);
        attributes.put("average_bytes_out",      long.class);
        attributes.put("scrape_count",           long.class);
        attributes.put("average_scrape_count",   long.class);
        attributes.put("announce_count",         long.class);
        attributes.put("average_announce_count", long.class);
        attributes.put("seed_count",             int.class);
        attributes.put("leecher_count",          int.class);
        attributes.put("bad_NAT_count",          int.class);
        class_definitions.put(plugin_class, attributes);

        attributes = new HashMap();
        plugin_class = Torrent.class;
        attributes.put("name",          String.class);
        attributes.put("size",          long.class);
        attributes.put("hash",          new byte[0].getClass());
        attributes.put("comment",       String.class);
        attributes.put("created_by",    String.class);
        attributes.put("creation_date", long.class);
        attributes.put("encoding",      String.class);
        attributes.put("piece_count",   long.class);
        attributes.put("piece_size",    long.class);
        attributes.put("private",       boolean.class);
        attributes.put("announce_url",  URL.class);
        class_definitions.put(plugin_class, attributes);

        attributes = new HashMap();
        plugin_class = RPObject.class;
        attributes.put("_object_id", int.class);
        class_definitions.put(plugin_class, attributes);

        attributes = new HashMap();
        plugin_class = RPPluginInterface.class;
        attributes.put("_object_id",     long.class);
        attributes.put("_connection_id", long.class);
        class_definitions.put(plugin_class, attributes);
    }

    public static Map getAttributeTypes(Class c) {
        Map result = (Map)class_definitions.get(c);
        if (result == null) {
            result = Collections.EMPTY_MAP;
        }
        return result;
    }

    public static Map getRPAttributeTypes(Class c) {
        Map result = null;
        if (RPPluginInterface.class.isAssignableFrom(c)) {
            result = (Map)class_definitions.get(RPPluginInterface.class);
        }
        else /* (RPObject.class.isAssignableFrom(c)) */ {
            result = (Map)class_definitions.get(RPObject.class);
        }
        return result;
    }

    public static Map getRPAttributes(RPObject object, Class obj_class, Map attribute_types) {
        RemoteClassMap map = new RemoteClassMap(attribute_types);
        if (RPPluginInterface.class.isAssignableFrom(obj_class)) {
            map.put("_connection_id", ((RPPluginInterface)object)._connection_id);
        }
        map.put("_object_id", object._getOID());
        return map;
    }


    private static class RemoteClassMap extends HashMap {

        private static RemoteMethodInvoker invoker = RemoteMethodInvoker.create(null, true);

        private Map attribute_types;

        public RemoteClassMap(Map attribute_types) {
            super();
            this.attribute_types = attribute_types;
        }

        public Object put(Object key, Object value) {
            try {
                Class c_type = (Class)attribute_types.get(key);
                if (c_type == null) {
                    throw new RuntimeException("error - missing type definition for " + key + " in RemoteClassMap");
                }
                return super.put(key, invoker.prepareRemoteResult(value, c_type));
            }
            catch (java.lang.reflect.InvocationTargetException ite) {
                throw new RuntimeException(ite);
            }
            catch (NoSuchMethodException nsme) {
                throw new RuntimeException(nsme);
            }
        }

        public Object put(String key, byte[] value) {
            return super.put(key, value);
        }

        public Object put(String key, boolean value) {
            return super.put(key, Boolean.valueOf(value));
        }

        public Object put(String key, float value) {
            return super.put(key, new Float(value));
        }

        public Object put(String key, int value) {
            return super.put(key, new Integer(value));
        }

        public Object put(String key, long value) {
            return super.put(key, new Long(value));
        }

        public Object put(String key, File value) {
            return super.put(key, value);
        }

        public Object put(String key, URL value) {
            return super.put(key, value);
        }


        public Object put(String key, String value) {
            return super.put(key, value);
        }

        public Object put(String key, String[] value) {
            return super.put(key, value);
        }

        public Object put(String key, int[] value) {
            return super.put(key, value);
        }

    }

}