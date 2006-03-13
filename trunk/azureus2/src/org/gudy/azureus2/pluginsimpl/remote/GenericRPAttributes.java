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
        }
        else if (obj_class == Download.class) {
            Download dload = (Download)object;
            map.put("torrent",         dload.getTorrent());
            map.put("stats",           dload.getStats());
            map.put("announce_result", dload.getLastAnnounceResult());
            map.put("scrape_result",   dload.getLastScrapeResult());
            map.put("position",        dload.getPosition());
            map.put("force_start",     dload.isForceStart());
        }

        else if (obj_class == DownloadAnnounceResult.class) {
            DownloadAnnounceResult dsr = (DownloadAnnounceResult)object;
            map.put("seed_count",     dsr.getSeedCount());
            map.put("non_seed_count", dsr.getNonSeedCount());
        }
        else if (obj_class == DownloadScrapeResult.class) {
            DownloadScrapeResult dsr = (DownloadScrapeResult)object;
            map.put("seed_count",     dsr.getSeedCount());
            map.put("non_seed_count", dsr.getNonSeedCount());
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
            map.put("name", torrent.getName());
            map.put("size", torrent.getSize());
            map.put("hash", torrent.getHash());
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
        class_definitions.put(plugin_class, attributes);

        attributes = new HashMap();
        plugin_class = Download.class;
        attributes.put("torrent",         Torrent.class);
        attributes.put("stats",           DownloadStats.class);
        attributes.put("announce_result", DownloadAnnounceResult.class);
        attributes.put("scrape_result",   DownloadScrapeResult.class);
        attributes.put("position",        int.class);
        attributes.put("force_start",     boolean.class);
        class_definitions.put(plugin_class, attributes);

        attributes = new HashMap();
        plugin_class = DownloadAnnounceResult.class;
        attributes.put("seed_count",      int.class);
        attributes.put("non_seed_count",  int.class);
        class_definitions.put(plugin_class, attributes);

        /**
         * DownloadScrapeResult has the same attributes as
         * DownloadAnnounceResult, so we'll just reuse the mapping.
         */
        class_definitions.put(DownloadScrapeResult.class, attributes);

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
        attributes.put("name", String.class);
        attributes.put("size", long.class);
        attributes.put("hash", new byte[0].getClass());
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