package com.aelitis.azureus.ui.swt.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class SWTSkinObjectSeparator
	extends SWTSkinObjectBasic
{

	private Label separator;

	public SWTSkinObjectSeparator(SWTSkin skin, SWTSkinProperties properties,
			String sid, String configID, SWTSkinObject parent) {
		super(skin, properties, sid, configID, "separator", parent);

		Composite createOn;
		if (parent == null) {
			createOn = skin.getShell();
		} else {
			createOn = (Composite) parent.getControl();
		}

		separator = new Label(createOn, SWT.NONE);

		setControl(separator);

	}

}