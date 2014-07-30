package com.aelitis.azureus.ui.swt.views;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;

import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.util.JSONUtils;
import com.aelitis.azureus.util.MapUtils;

public class ViewTitleInfoBetaP
	implements ViewTitleInfo
{
	private static final String PARAM_LASTPOSTCOUNT = "betablog.numPosts";

	long numNew = 0;

	private long postCount = 0;

	@SuppressWarnings("rawtypes")
	public ViewTitleInfoBetaP() {
		SimpleTimer.addEvent("devblog", SystemTime.getCurrentTime(),
				new TimerEventPerformer() {
					public void perform(TimerEvent event) {
						long lastPostCount = COConfigurationManager.getLongParameter(
								PARAM_LASTPOSTCOUNT, 0);
						PluginInterface pi = PluginInitializer.getDefaultInterface();
						try {
							ResourceDownloader rd = pi.getUtilities().getResourceDownloaderFactory().create(
									new URL(
											"http://api.tumblr.com/v2/blog/devblog.vuze.com/info?api_key=C5a8UGiSwPflOrVecjcvwGiOWVsLFF22pC9SgUIKSuQfjAvDAY"));
							InputStream download = rd.download();
							Map json = JSONUtils.decodeJSON(FileUtil.readInputStreamAsString(
									download, 65535));
							Map mapResponse = MapUtils.getMapMap(json, "response", null);
							if (mapResponse != null) {
								Map mapBlog = MapUtils.getMapMap(mapResponse, "blog", null);
								if (mapBlog != null) {
									postCount = MapUtils.getMapLong(mapBlog, "posts", 0);
									numNew = postCount - lastPostCount;
									ViewTitleInfoManager.refreshTitleInfo(ViewTitleInfoBetaP.this);
								}
							}

						} catch (Exception e) {
						}
					}
				});
	}

	public Object getTitleInfoProperty(int propertyID) {
		if (propertyID == TITLE_INDICATOR_TEXT && numNew > 0) {
			return "" + numNew;
		}
		return null;
	}

	public void clearIndicator() {
		COConfigurationManager.setParameter(PARAM_LASTPOSTCOUNT, postCount);
		numNew = 0;
	}
}
