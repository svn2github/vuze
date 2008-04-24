package com.aelitis.azureus.ui.swt.views.skin;

import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;

public class ButtonBar
	extends SkinView
{

	public Object showSupport(SWTSkinObject skinObject, Object params) {
		/*
		 * HACK to force the footer to be loaded
		 */
		SWTSkinObject obj = skinObject.getSkin().getSkinObject(SkinConstants.VIEWID_FOOTER);
		if(false == obj.isVisible()){
			obj.setVisible(true);
			obj.setVisible(false);
		}
		return null;
	}

	
}
