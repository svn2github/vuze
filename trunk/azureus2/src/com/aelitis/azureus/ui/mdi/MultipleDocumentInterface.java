package com.aelitis.azureus.ui.mdi;


import java.util.Map;


import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;

public interface MultipleDocumentInterface
{
	public static final String SIDEBAR_SECTION_PLUGINS = "Plugins";

	public static final String SIDEBAR_SECTION_LIBRARY = "Library";

	public static final String SIDEBAR_SECTION_GAMES = "Games";

	public static final String SIDEBAR_SECTION_LIBRARY_DL = "LibraryDL";

	public static final String SIDEBAR_SECTION_LIBRARY_CD = "LibraryCD";

	public static final String SIDEBAR_SECTION_LIBRARY_UNOPENED = "LibraryUnopened";

	public static final String SIDEBAR_SECTION_TOOLS = "Tools";

	public static final String SIDEBAR_SECTION_WELCOME = "Welcome";

	public static final String SIDEBAR_SECTION_PLUS = "Plus";

	public static final String SIDEBAR_SECTION_SUBSCRIPTIONS = "Subscriptions";

	public static final String SIDEBAR_SECTION_DEVICES = "Devices";

	public static final String SIDEBAR_SECTION_RELATED_CONTENT = "RelatedContent";
	
	public static final String SIDEBAR_SECTION_BURN_INFO = "BurnInfo";
	
	public MdiEntry createEntryFromSkinRef(String parentID, String id,
			String configID, String title, ViewTitleInfo titleInfo, Object datasource,
			boolean closeable, int index);

	public boolean showEntryByID(String id);

	public MdiEntry createEntryFromIViewClass(String parent, String id,
			String title, Class<?> iviewClass, Class<?>[] iviewClassArgs,
			Object[] iviewClassVals, Object datasource, ViewTitleInfo titleInfo,
			boolean closeable);

	public MdiEntry getCurrentEntry();

	public MdiEntry getEntry(String id);

	public void addListener(MdiListener sideBarListener);

	public boolean isVisible();

	public void closeEntry(String id);

	public MdiEntry[] getEntries();

	public void registerEntry(String id, MdiEntryCreationListener l);

	public void removeListener(MdiListener l);

	boolean entryExists(String id);

	public void removeItem(MdiEntry entry);

	public void setEntryAutoOpen(String id, boolean autoOpen);

	public void showEntry(MdiEntry newEntry);

	public void informAutoOpenSet(MdiEntry entry, Map<String, Object> autoOpenInfo);
}
