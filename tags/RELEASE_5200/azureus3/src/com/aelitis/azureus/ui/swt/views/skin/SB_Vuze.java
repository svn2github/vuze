package com.aelitis.azureus.ui.swt.views.skin;

import java.util.ArrayList;

import org.eclipse.swt.SWT;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;

import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoListener;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.swt.views.ViewTitleInfoBetaP;

public class SB_Vuze
{
	private ArrayList<MdiEntry> children = new ArrayList<MdiEntry>();

	private ViewTitleInfo titleInfo;

	public SB_Vuze(MultipleDocumentInterface mdi) {
		setup(mdi);
	}

	private void setup(final MultipleDocumentInterface mdi) {
		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_BETAPROGRAM,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {

						final ViewTitleInfoBetaP viewTitleInfo = new ViewTitleInfoBetaP();

						MdiEntry entry = mdi.createEntryFromSkinRef(
								MultipleDocumentInterface.SIDEBAR_HEADER_VUZE,
								MultipleDocumentInterface.SIDEBAR_SECTION_BETAPROGRAM,
								"main.area.beta", "{Sidebar.beta.title}", viewTitleInfo, null,
								true, MultipleDocumentInterface.SIDEBAR_POS_FIRST);

						entry.setImageLeftID("image.sidebar.beta");

						entry.addListener(new MdiCloseListener() {
							public void mdiEntryClosed(MdiEntry entry, boolean userClosed) {
								viewTitleInfo.clearIndicator();
							}
						});

						return entry;
					}
				});
		
		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_WELCOME, new MdiEntryCreationListener() {
			public MdiEntry createMDiEntry(String id) {
				MdiEntry entry = mdi.createEntryFromSkinRef(
						MultipleDocumentInterface.SIDEBAR_HEADER_VUZE,
						MultipleDocumentInterface.SIDEBAR_SECTION_WELCOME,
						"main.area.welcome",
						MessageText.getString("v3.MainWindow.menu.getting_started").replaceAll(
								"&", ""), null, null, true, "");
				entry.setImageLeftID("image.sidebar.welcome");
				addDropTest(entry);
				return entry;
			}
		});

		
		SBC_ActivityTableView.setupSidebarEntry();

		ViewTitleInfoManager.addListener(new ViewTitleInfoListener() {
			public void viewTitleInfoRefresh(ViewTitleInfo titleInfo) {
				MdiEntry childrenArray[] = children.toArray(new MdiEntry[0]);
				for (MdiEntry entry : childrenArray) {
					if (entry.getViewTitleInfo() == titleInfo) {
						if (SB_Vuze.this.titleInfo != null) {
							ViewTitleInfoManager.refreshTitleInfo(SB_Vuze.this.titleInfo);
						}
						break;
					}
				}
			}
		});

		mdi.addListener(new MdiEntryLoadedListener() {
			public void mdiEntryLoaded(MdiEntry entry) {
				if (MultipleDocumentInterface.SIDEBAR_HEADER_VUZE.equals(entry.getParentID())) {
					children.add(entry);
					entry.addListener(new MdiChildCloseListener() {
						public void mdiChildEntryClosed(MdiEntry parent, MdiEntry child,
								boolean user) {
							children.remove(child);
						}
					});
				}
				if (!entry.getId().equals(MultipleDocumentInterface.SIDEBAR_HEADER_VUZE)) {
					return;
				}
				setupHeader(entry);
			}
		});
	}

	private void setupHeader(final MdiEntry entry) {

		titleInfo = new ViewTitleInfo() {
			public Object getTitleInfoProperty(int propertyID) {
				if (propertyID == ViewTitleInfo.TITLE_INDICATOR_TEXT) {
					if (entry.isExpanded()) {
						return null;
					}
					StringBuilder sb = new StringBuilder();
					MdiEntry[] entries = entry.getMDI().getEntries();
					for (MdiEntry subEntry : entries) {
						if (entry.getId().equals(subEntry.getParentID())) {
							ViewTitleInfo titleInfo = subEntry.getViewTitleInfo();
							if (titleInfo != null) {
								Object text = titleInfo.getTitleInfoProperty(TITLE_INDICATOR_TEXT);
								if (text instanceof String) {
									if (sb.length() > 0) {
										sb.append(" | ");
									}
									sb.append(text);
								}
							}
						}
					}
					if (sb.length() > 0) {
						return sb.toString();
					}
				} else if (propertyID == ViewTitleInfo.TITLE_INDICATOR_TEXT_TOOLTIP) {
					if (entry.isExpanded()) {
						return null;
					}
					StringBuilder sb = new StringBuilder();
					MdiEntry[] entries = entry.getMDI().getEntries();
					for (MdiEntry subEntry : entries) {
						if (entry.getId().equals(subEntry.getParentID())) {
							ViewTitleInfo titleInfo = subEntry.getViewTitleInfo();
							if (titleInfo != null) {
								Object text = titleInfo.getTitleInfoProperty(TITLE_INDICATOR_TEXT);
								if (text instanceof String) {
									if (sb.length() > 0) {
										sb.append("\n");
									}
									sb.append(subEntry.getTitle() + ": " + text);
								}
							}
						}
					}
					if (sb.length() > 0) {
						return sb.toString();
					}
				}
				return null;
			}
		};
		entry.setViewTitleInfo(titleInfo);
	}

	protected void addDropTest(MdiEntry entry) {
		if (!Constants.isCVSVersion()) {
			return;
		}
		entry.addListener(new MdiEntryDropListener() {
			public boolean mdiEntryDrop(MdiEntry entry, Object droppedObject) {
				String s = "You just dropped " + droppedObject.getClass() + "\n"
						+ droppedObject + "\n\n";
				if (droppedObject.getClass().isArray()) {
					Object[] o = (Object[]) droppedObject;
					for (int i = 0; i < o.length; i++) {
						s += "" + i + ":  ";
						Object object = o[i];
						if (object == null) {
							s += "null";
						} else {
							s += object.getClass() + ";" + object;
						}
						s += "\n";
					}
				}
				new MessageBoxShell(SWT.OK, "test", s).open(null);
				return true;
			}
		});
	}

}
