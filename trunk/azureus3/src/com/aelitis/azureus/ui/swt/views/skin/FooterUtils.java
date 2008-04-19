package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Control;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;

public class FooterUtils
{
	private SWTSkin skin;

	private UIFunctionsSWT uiFunctions = null;

	public FooterUtils(SWTSkin skin, UIFunctionsSWT uiFunctions) {
		this.skin = skin;
		this.uiFunctions = uiFunctions;

		hookListeners();

	}

	private void hookListeners() {

		SWTSkinObject shareButton = skin.getSkinObject("button-buddy-share");
		if (null != shareButton) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(shareButton);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {

					SWTSkinObject skinObject = skin.getSkinObject("detail-panel");
					if (skinObject != null) {
						Control control = skinObject.getControl();
						FormData fData = (FormData) control.getLayoutData();
						if (true == skinObject.isVisible()) {
							fData.height = 0;
							control.setLayoutData(fData);
							skinObject.setVisible(false);
						} else {
							fData.height = 200;
							control.setLayoutData(fData);
							skinObject.setVisible(true);
						}

						skinObject = skin.getSkinObject("footer");
						if (skinObject != null) {
							Utils.relayout(skinObject.getControl());
						}
					}
				}
			});
		}
	}
}
