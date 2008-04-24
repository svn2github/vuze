package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;

/**
 * The main footer <code>SkinView</code>.  This footer may contain any number of subcomponents;
 * currently the only implemented component is a BuddiesViewer 
 * @author khai
 *
 */
public class Footer
	extends SkinView
{

	private SWTSkin skin = null;

	public Object showSupport(SWTSkinObject skinObject, Object params) {

		skin = skinObject.getSkin();

		SWTSkinObject buddiesSkin = skin.getSkinObject(SkinConstants.VIEWID_BUDDIES_AVATARS_VIEWER);
		if (null != buddiesSkin) {
			createBuddiesViewer((Composite) buddiesSkin.getControl());
		}

		Utils.relayout(skinObject.getControl());
		return null;
	}

	private void createBuddiesViewer(Composite parent) {
		RowLayout rLayout = new RowLayout();
		rLayout.marginLeft = 5;
		rLayout.wrap = false;
		parent.setLayout(rLayout);
		new BuddiesViewer(parent, skin);

	}
}
