package com.aelitis.azureus.ui.swt.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ExpandBar;

/**
 * Container that hold ExpandItems
 * 
 */
public class SWTSkinObjectExpandBar
	extends SWTSkinObjectContainer
{

	private ExpandBar expandBar;

	public SWTSkinObjectExpandBar(SWTSkin skin, SWTSkinProperties properties,
			String sID, String sConfigID, SWTSkinObject parent) {
		super(skin, properties, null, sID, sConfigID, "expandbar", parent);
		createExpandBar();
	}

	private void createExpandBar() {
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
		
		expandBar = new ExpandBar(createOn, style | SWT.V_SCROLL);
		// ensure no layout for expandbar (children don't setlayoutdata because they are expanditems)
		expandBar.setLayout(null);
		
		triggerListeners(SWTSkinObjectListener.EVENT_CREATED);
		setControl(expandBar);
		
	}

	public ExpandBar getExpandbar() {
		return expandBar;
	}
}
