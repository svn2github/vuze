package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinUtils;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;

/**
 * The main footer <code>SkinView</code>.  This footer may contain any number of subcomponents;
 * currently the only implemented component is a BuddiesViewer 
 * @author khai
 *
 */
public class Footer
	extends SkinView
{

	private BuddiesViewer viewer = null;

	private SWTSkin skin = null;

	public Object showSupport(SWTSkinObject skinObject, Object params) {

		skin = skinObject.getSkin();

		SWTSkinObject buddiesSkin = skin.getSkinObject(SkinConstants.VIEWID_BUDDIES_AVATARS_VIEWER);
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
		rLayout.wrap = false;
		parent.setLayout(rLayout);
		viewer = new BuddiesViewer(parent, skin);

	}

	private void hookListeners() {
		COConfigurationManager.setBooleanDefault("Detail.visible", false);

		SWTSkinObject shareButton = skin.getSkinObject("button-buddy-share");
		if (null != shareButton) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(shareButton);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				private boolean isExpanded = false;

				public void pressed(SWTSkinButtonUtility buttonUtility) {

					SWTSkinObject skinObject = skin.getSkinObject("detail-panel");
					if (skinObject != null) {

						final SWTSkinObject footerObject = skin.getSkinObject("footer");

						isExpanded = !isExpanded;

						if (null != viewer) {
							viewer.setShareMode(isExpanded);
						}
						
						final Control control = skinObject.getControl();
						final FormData fd = (FormData) control.getLayoutData();
						final Point size = new Point(control.getSize().x, isExpanded ? 600
								: 0);

						AERunnable runnable = new AERunnable() {
							boolean firstTime = true;

							public void runSupport() {
								if (control.isDisposed()) {
									return;
								}

								if (firstTime) {
									firstTime = false;
									if (control.getData("Sliding") != null) {
										return;
									}
									control.setData("Sliding", "1");
								}

								int newWidth = (int) (fd.width + (size.x - fd.width) * 0.4);
								int h = fd.height >= 0 ? fd.height : control.getSize().y;
								int newHeight = (int) (h + (size.y - h) * 0.4);
								//System.out.println(control + "] newh=" + newHeight + " to " + size.y);

								if (newWidth == fd.width && newHeight == h) {
									fd.width = size.x;
									fd.height = size.y;
									//System.out.println(control + "] side to " + size.y + " done");
									control.setSize(size);
									control.setLayoutData(fd);
									Utils.relayout(control);
									control.getParent().layout();

									control.setData("Sliding", null);
								} else {
									fd.width = newWidth;
									fd.height = newHeight;
									control.setLayoutData(fd);
									Utils.relayout(footerObject.getControl());

									final AERunnable r = this;
									SimpleTimer.addEvent("slide",
											SystemTime.getCurrentTime() + 10,
											new TimerEventPerformer() {
												public void perform(TimerEvent event) {
													control.getDisplay().asyncExec(r);
												}
											});
								}
							}
						};
						control.getDisplay().asyncExec(runnable);

						//						COConfigurationManager.setParameter("Detail.visible",
						//								!COConfigurationManager.getBooleanParameter("Detail.visible"));
						//						//						SWTSkinUtils.setVisibility(skin, "Detail.visible", "detail-panel",
						//						//								COConfigurationManager.getBooleanParameter("Detail.visible"), false, false);
						//						SWTSkinUtils.slide(control, fData, new Point(control.getSize().x,
						//								600));
						//						Control control = skinObject.getControl();
						//						FormData fData = (FormData) control.getLayoutData();
						//						if (true == skinObject.isVisible()) {
						//							fData.height = 0;
						//							control.setLayoutData(fData);
						//							skinObject.setVisible(false);
						//							if(null != viewer){
						//								viewer.setShareMode(false);
						//							}
						//
						//						} else {
						//							fData.height = 500;
						//							control.setLayoutData(fData);
						//							skinObject.setVisible(true);
						//							if(null != viewer){
						//								viewer.setShareMode(true);
						//							}
						//						}
						//
						//						skinObject = skin.getSkinObject("footer");
						//						if (skinObject != null) {
						//							Utils.relayout(skinObject.getControl());
						//						}
					}
				}
			});
		}
	}
}
