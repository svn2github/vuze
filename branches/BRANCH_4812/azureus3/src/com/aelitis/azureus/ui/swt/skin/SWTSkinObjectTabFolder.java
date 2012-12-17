package com.aelitis.azureus.ui.swt.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.util.Constants;

public class SWTSkinObjectTabFolder
	extends SWTSkinObjectContainer
{

	private CTabFolder tabFolder;

	public SWTSkinObjectTabFolder(SWTSkin skin, SWTSkinProperties properties,
			String sID, String sConfigID, SWTSkinObject parent) {
		super(skin, properties, null, sID, sConfigID, "tabfolder", parent);
		createTabFolder();
		
	}

	private void createTabFolder() {
		Composite createOn;
		if (parent == null) {
			createOn = skin.getShell();
		} else {
			createOn = (Composite) parent.getControl();
		}

		int style = SWT.NONE;
		if (properties.getIntValue(sConfigID + ".border", 0) == 1) {
			style = SWT.BORDER;
		}
		
		String sStyle = properties.getStringValue("style");
		if (sStyle != null && sStyle.length() > 0) {
			String[] styles = Constants.PAT_SPLIT_COMMA.split(sStyle);
			for (String aStyle : styles) {
				if (aStyle.equalsIgnoreCase("close")) {
					style |= SWT.CLOSE;
				}
			}
		}

		
		tabFolder = new CTabFolder(createOn, style);
		
		triggerListeners(SWTSkinObjectListener.EVENT_CREATED);
		setControl(tabFolder);
	}

	protected boolean setIsVisible(boolean visible, boolean walkup) {
		boolean isVisible = superSetIsVisible(visible, walkup);
		// Todo: ensure correct tabfolder child comp is visible
		return isVisible;
	}
	
	public void childAdded(SWTSkinObject soChild) {
//		super.childAdded(soChild);
//		CTabItem tabItem = new CTabItem(tabFolder, SWT.NONE);
//		tabItem.setText("WOW");
//		tabItem.setControl(soChild.getControl());
	}

	public CTabFolder getTabFolder() {
		return tabFolder;
	}
}
