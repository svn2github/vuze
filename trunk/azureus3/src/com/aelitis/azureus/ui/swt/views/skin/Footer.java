package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

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

	public Object showSupport(SWTSkinObject skinObject, Object params) {

		SWTSkinObject buddiesSkin = skinObject.getSkin().getSkinObject(
				SkinConstants.VIEWID_BUDDIES_VIEWER);
		if (null != buddiesSkin) {
			createBuddiesViewer((Composite) buddiesSkin.getControl());
		}

//		skinObject.getSkin().layout();
		return null;
	}

	private void createBuddiesViewer(Composite parent) {
		FillLayout fLayout = new FillLayout();
		fLayout.marginHeight =5;
		fLayout.marginWidth =5;
		parent.setLayout(fLayout);
		new BuddiesViewer(parent);
	}

}
