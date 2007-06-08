/**
 * 
 */
package com.aelitis.azureus.ui.swt.skin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * @author TuxPaper
 * @created Jun 8, 2006
 */
public class SWTSkinTabSet
{
	private Listener tabMouseListener;

	private final SWTSkin skin;

	private final SWTSkinProperties skinProperties;

	private final String sID;

	private SWTSkinObjectTab activeTab;

	/** List of SWTSKinObjectTab */
	private List tabs;

	private ArrayList listeners = new ArrayList();

	// XXX Do we need to pass in SkinProperties in case of cloning?
	public SWTSkinTabSet(SWTSkin skin, String sID) {
		this.sID = sID;
		this.skin = skin;
		skinProperties = skin.getSkinProperties();
		tabs = new ArrayList();
	}

	public void addTab(SWTSkinObjectTab tab) {
		tabs.add(tab);
		tab.setTabset(this);

		//System.out.println("AddTab for " + sID + ": " + tab.getSkinObjectID());
		addMouseListener(tab, tab.getControl());
		if (tabs.size() == 1) {
			//			activeTab = tab;
			//			setTabVisible(tab, true, null);
			setTabVisible(tab, false, tab.getActiveWidgets());
		} else {
			setTabVisible(tab, false, tab.getActiveWidgets());
		}
	}

	public SWTSkinObjectTab getActiveTab() {
		return activeTab;
	}

	public SWTSkinObjectTab[] getTabs() {
		return (SWTSkinObjectTab[]) tabs.toArray(new SWTSkinObjectTab[0]);
	}

	public void clean() {
		if (tabs.size() > 0) {
			for (int i = tabs.size() - 1; i >= 0; i--) {
				SWTSkinObjectTab tab = (SWTSkinObjectTab) tabs.get(i);
				setTabVisible(tab, false, null);
				if (i == 0) {
					setActiveTab(tab);
				}
			}
		}
	}

	public SWTSkinObjectTab getTab(String sID) {
		for (int i = 0; i < tabs.size(); i++) {
			SWTSkinObjectTab tab = (SWTSkinObjectTab) tabs.get(i);
			String sTabID = tab.getSkinObjectID();

			if (sTabID.equals(sID)) {
				return tab;
			}
		}

		return null;
	}

	public boolean setActiveTab(String sID) {
		for (int i = 0; i < tabs.size(); i++) {
			SWTSkinObject tab = (SWTSkinObject) tabs.get(i);
			if (tab instanceof SWTSkinObjectTab) {
				String sTabID = tab.getSkinObjectID();

				if (sTabID.equals(sID)) {
					setActiveTab((SWTSkinObjectTab) tab);
					return true;
				}
			}
		}

		return false;
	}
	
	public void setActiveTab(final SWTSkinObjectTab newTab) {
		setActiveTab(newTab, false);
	}

	private void setActiveTab(final SWTSkinObjectTab newTab, final boolean bEvenIfSame) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				// Don't exit early if we are already on tab.  We want to be notified if
				// the user clicks on the tab again (for example, for page refreshing)
				if (!tabs.contains(newTab)) {
					System.err.println("No contain in " + sID + ": " + newTab);
					return;
				}

				String sOldID = activeTab == null ? "" : activeTab.getSkinObjectID();

				SWTSkinObject[] objects = setTabVisible(newTab, true, null);
				if (newTab != activeTab) {
					if (activeTab != null) {
						setTabVisible(activeTab, false, objects);
					}

					activeTab = newTab;
				} else if (!bEvenIfSame) {
					return;
				}

				String sConfigID = activeTab.getConfigID();
				String sNewID = activeTab.getSkinObjectID();

				SWTSkinObject parent = skin.getSkinObject(skinProperties.getStringValue(sConfigID
						+ ".activate"));
				if (parent != null) {
					parent.getControl().setFocus();
				}

				if (org.gudy.azureus2.core3.util.Constants.isOSX) {
					boolean bHasSkinBrowser = false;
					SWTSkinObject[] activeWidgets = activeTab.getActiveWidgets();
					for (int i = 0; i < activeWidgets.length; i++) {
						SWTSkinObject skinObject = activeWidgets[i];
						if (hasSkinBrowser(skinObject)) {
							bHasSkinBrowser = true;
							break;
						}
					}

					if (bHasSkinBrowser) {
						Shell shell = activeTab.getControl().getShell();
						Point size = shell.getSize();
						size.x -= 1;
						shell.setSize(size);
						size.x += 1;
						shell.setSize(size);
					}
				}

				triggerChangeListener(sOldID, sNewID);
			}
		});
	}

	private boolean hasSkinBrowser(SWTSkinObject skinObject) {
		if (skinObject instanceof SWTSkinObjectContainer) {
			SWTSkinObject[] children = ((SWTSkinObjectContainer) skinObject).getChildren();
			for (int i = 0; i < children.length; i++) {
				SWTSkinObject object = children[i];
				if (hasSkinBrowser(object)) {
					return true;
				}
			}
		}
		return (skinObject instanceof SWTSkinObjectBrowser);
	}

	/**
	 * @param oldID
	 * @param newID
	 */
	private void triggerChangeListener(String oldID, String newID) {
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			try {
				SWTSkinTabSetListener l = (SWTSkinTabSetListener) iter.next();
				l.tabChanged(this, oldID, newID);
			} catch (Exception e) {
				Debug.printStackTrace(e);
			}
		}
	}

	private void addMouseListener(SWTSkinObject tab, Control control) {
		if (tabMouseListener == null) {
			tabMouseListener = new Listener() {
				boolean bDownPressed = false;

				public void handleEvent(Event event) {
					if (event.type == SWT.MouseDown) {
						bDownPressed = true;
						return;
					} else if (!bDownPressed) {
						return;
					}

					bDownPressed = false;

					Control control = (Control) event.widget;
					setActiveTab((SWTSkinObjectTab) control.getData("Tab"), true);
				}
			};
		}

		control.setData("Tab", tab);
		control.addListener(SWT.MouseUp, tabMouseListener);
		control.addListener(SWT.MouseDown, tabMouseListener);

		if (control instanceof Composite) {
			Control[] children = ((Composite) control).getChildren();
			for (int i = 0; i < children.length; i++) {
				addMouseListener(tab, children[i]);
			}
		}
	}

	private SWTSkinObject[] setTabVisible(SWTSkinObjectTab tab, boolean visible,
			SWTSkinObject[] skipObjects) {
		String sSkinID = tab.getSkinObjectID();

		SWTSkinObject swtObject = skin.getSkinObjectByID(sSkinID);
		if (swtObject == null) {
			return null;
		}

		String suffix = visible ? "-selected" : "";

		swtObject.switchSuffix(suffix, 1, true);

		tab.triggerListeners(SWTSkinObjectListener.EVENT_SELECT);

		SWTSkinObject[] activeWidgets = tab.getActiveWidgets();
		for (int i = 0; i < activeWidgets.length; i++) {
			SWTSkinObject skinObject = activeWidgets[i];
			boolean ok = true;
			if (skipObjects != null) {
				for (int j = 0; j < skipObjects.length; j++) {
					if (skinObject.equals(skipObjects[j])) {
						ok = false;
						break;
					}
				}
			}

			if (ok) {
				if (visible) {
  				skinObject.setDefaultVisibility();
				} else {
					skinObject.setVisible(visible);
				}
				//System.out.println(((visible ? "show" : "hide") + " " + skinObject) + Debug.getCompressedStackTrace());
			}
		}

		return activeWidgets;
	}

	public void addListener(SWTSkinTabSetListener listener) {
		listeners.add(listener);
	}

	public String getID() {
		return sID;
	}

	protected static String[] getTemplateInfo(SWTSkin skin,
			SWTSkinObject skinObject, String sTemplateKey) {
		SWTSkinProperties skinProperties = skin.getSkinProperties();
		String sID = skinObject.getConfigID() + ".view.template." + sTemplateKey;
		return skinProperties.getStringArray(sID);
	}

	protected static String getTemplateID(SWTSkin skin, SWTSkinObject skinObject,
			String sTemplateKey) {
		String[] templateInfo = getTemplateInfo(skin, skinObject, sTemplateKey);
		if (templateInfo != null) {
			return templateInfo[0];
		}
		return null;
	}

	public static String getTabSetID(SWTSkin skin, SWTSkinObject skinObject,
			String sTemplateKey) {
		String[] templateInfo = getTemplateInfo(skin, skinObject, sTemplateKey);
		if (templateInfo != null && templateInfo.length > 1) {
			return templateInfo[1];
		}
		return null;
	}
}
