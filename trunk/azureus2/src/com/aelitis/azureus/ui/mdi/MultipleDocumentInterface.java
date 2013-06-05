package com.aelitis.azureus.ui.mdi;

import java.util.List;
import java.util.Map;

import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;

public interface MultipleDocumentInterface
{
	public static final String SIDEBAR_POS_FIRST = "";

	public static final String SIDEBAR_HEADER_VUZE = "header.vuze";

	public static final String SIDEBAR_HEADER_TRANSFERS = "header.transfers";

	public static final String SIDEBAR_HEADER_DISCOVERY = "header.discovery";

	public static final String SIDEBAR_HEADER_DEVICES = "header.devices";

	public static final String SIDEBAR_HEADER_DVD = "header.dvd";

	public static final String SIDEBAR_HEADER_SUBSCRIPTIONS = "header.subscriptions";

	public static final String SIDEBAR_HEADER_PLUGINS = "header.plugins";

	public static final String SIDEBAR_SECTION_PLUGINS = "Plugins";

	public static final String SIDEBAR_SECTION_ABOUTPLUGINS = "About.Plugins";

	public static final String SIDEBAR_SECTION_LIBRARY = "Library";

	public static final String SIDEBAR_SECTION_GAMES = "Games";

	public static final String SIDEBAR_SECTION_BETAPROGRAM = "BetaProgramme";

	public static final String SIDEBAR_SECTION_LIBRARY_DL = "LibraryDL";

	public static final String SIDEBAR_SECTION_LIBRARY_CD = "LibraryCD";

	public static final String SIDEBAR_SECTION_TAGS = "TagsOverview";

	public static final String SIDEBAR_SECTION_LIBRARY_UNOPENED = "LibraryUnopened";
	
	public static final String SIDEBAR_TORRENT_DETAILS_PREFIX = "DMDetails_";

	public static final String SIDEBAR_SECTION_WELCOME = "Welcome";

	public static final String SIDEBAR_SECTION_PLUS = "Plus";

	public static final String SIDEBAR_SECTION_SUBSCRIPTIONS = "Subscriptions";

	public static final String SIDEBAR_SECTION_DEVICES = "Devices";

	public static final String SIDEBAR_SECTION_BURN_INFO = "BurnInfo";

	public static final String SIDEBAR_SECTION_ACTIVITIES = "Activity";

	public static final String SIDEBAR_SECTION_SEARCH = "Search";
	
	public boolean showEntryByID(String id);

		/**
		 * If you prefix the 'preferedAfterID' string with '~' then the operation will actually
		 * switch to 'preferedBeforeID'
		 * @param parentID
		 * @param id
		 * @param configID
		 * @param title
		 * @param titleInfo
		 * @param params
		 * @param closeable
		 * @param preferedAfterID
		 * @return
		 */
	public MdiEntry createEntryFromSkinRef(String parentID, String id,
			String configID, String title, ViewTitleInfo titleInfo, Object params,
			boolean closeable, String preferedAfterID);

	public MdiEntry getCurrentEntry();

	public MdiEntry getEntry(String id);

	public void addListener(MdiListener l);

	public void removeListener(MdiListener l);

	public void addListener(MdiEntryLoadedListener l);

	public void removeListener(MdiEntryLoadedListener l);

	public boolean isVisible();

	public void closeEntry(String id);

	public MdiEntry[] getEntries();

	public void registerEntry(String id, MdiEntryCreationListener l);
	
	public void registerEntry(String id,
			MdiEntryCreationListener2 mdiEntryCreationListener2);

	public boolean entryExists(String id);

	public void removeItem(MdiEntry entry);

	public void setEntryAutoOpen(String id, Object datasource);
	
	public void removeEntryAutoOpen(String id);

	public void showEntry(MdiEntry newEntry);

	public void informAutoOpenSet(MdiEntry entry, Map<String, Object> autoOpenInfo);

	public boolean loadEntryByID(String id, boolean activate);

	public void setPreferredOrder(String[] preferredOrder);

	public String[] getPreferredOrder();

	public MdiEntry createHeader(String id, String title, String preferredAfterID);

	public List<MdiEntry> getChildrenOf(String id);

	public boolean loadEntryByID(String id, boolean activate,
			boolean onlyLoadOnce, Object datasource);

}
