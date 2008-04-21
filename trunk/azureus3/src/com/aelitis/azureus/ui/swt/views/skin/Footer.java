package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.shells.LightBoxBrowserWindow;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.util.Constants;

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
		
		SWTSkinObject buddiesSkin = skin.getSkinObject(
				SkinConstants.VIEWID_BUDDIES_AVATARS_VIEWER);
		if (null != buddiesSkin) {
			createBuddiesViewer((Composite) buddiesSkin.getControl());
		}

		Utils.relayout(skinObject.getControl());
		
		hookListeners();
		return null;
	}

	private void createBuddiesViewer(Composite parent) {
//		FillLayout fLayout = new FillLayout();
//		fLayout.marginHeight =5;
//		fLayout.marginWidth =5;
//		parent.setLayout(fLayout);
		RowLayout rLayout = new RowLayout();
		rLayout.marginLeft = 5;
		rLayout.wrap=false;
		parent.setLayout(rLayout);
		new BuddiesViewer(parent, skin);
		
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
							fData.height = 500;
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
