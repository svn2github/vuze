/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.ui.swt.skin;

import java.text.NumberFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Debug;

/**
 * @author TuxPaper
 * @created Oct 18, 2006
 *
 */
public class SWTSkinObjectSash extends SWTSkinObjectBasic
{
	private static final boolean FASTDRAG = false;

	protected String sControlBefore;

	protected String sControlAfter;

	public SWTSkinObjectSash(final SWTSkin skin, SWTSkinProperties properties,
			final String sID, String sConfigID, String[] typeParams,
			SWTSkinObject parent, final boolean bVertical) {
		super(skin, properties, sID, sConfigID, "sash", parent);

		int style = bVertical ? SWT.VERTICAL : SWT.HORIZONTAL;

		if (typeParams.length > 2) {
			sControlBefore = typeParams[1];
			sControlAfter = typeParams[2];
		}

		final Composite createOn;
		if (parent == null) {
			createOn = skin.getShell();
		} else {
			createOn = (Composite) parent.getControl();
		}

		if (createOn == null || createOn.isDisposed()) {
			Debug.out("Can not create " + sID + " because parent is null or disposed");
			return;
		}

		final Sash sash = new Sash(createOn, style);

		int splitAt = COConfigurationManager.getIntParameter("v3." + sID
				+ ".SplitAt", -1);
		if (splitAt != -1) {
			double pct = (double) splitAt / 10000.0;
			sash.setData("PCT", new Double(pct));
		} else {
			String sPos = properties.getStringValue(sConfigID + ".startpos");
			if (sPos != null) {
				try {
					long l = NumberFormat.getInstance().parse(sPos).longValue();
					if (sPos.endsWith("%")) {
						double pct = (double) (100 - l) / 100;
						sash.setData("PCT", new Double(pct));
					} else {
						sash.setData("PX", new Long(l));
					}
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		}

		final Composite parentComposite = createOn;

		Listener l = new Listener() {
			private boolean skipResize = false;

			public void handleEvent(Event e) {
				Composite below = null;
				SWTSkinObject skinObject = skin.getSkinObjectByID(sControlAfter);

				if (skinObject != null) {
					below = (Composite) skinObject.getControl();
				}
				if (below == null) {
					return;
				}

				Composite above = null;
				skinObject = skin.getSkinObjectByID(sControlBefore);

				if (skinObject != null) {
					above = (Composite) skinObject.getControl();
				}

				if (e.type == SWT.Resize) {
					if (skipResize) {
						return;
					}
					Double l = (Double) sash.getData("PCT");
					if (l != null) {
						setPercent(l, sash, above, below, bVertical, parentComposite);
					}

					Long px = (Long) sash.getData("PX");
					if (px != null) {
						int i = (bVertical) ? createOn.getSize().x : createOn.getSize().y;
						double pct = 1 - (px.doubleValue() / i);

						FormData belowData = (FormData) below.getLayoutData();
						if (bVertical) {
							belowData.width = (int) (parentComposite.getBounds().width * pct);
						} else {
							belowData.height = (int) (parentComposite.getBounds().height * pct);
						}
						sash.setData("PCT", new Double(pct));
						// layout in resize is not needed (and causes browser widget to blink)
					}

				} else if (e.type == SWT.Selection) {
					skipResize = true;

					if (FASTDRAG && e.detail == SWT.DRAG)
						return;

					Rectangle area = parentComposite.getClientArea();
					FormData belowData = (FormData) below.getLayoutData();
					if (bVertical) {
						belowData.width = area.width - (e.x + e.width);
						if (belowData.width < 0) {
							belowData.width = 0;
							e.doit = false;
						}
					} else {
						belowData.height = area.height - (e.y + e.height);
						if (belowData.height < 0) {
							belowData.height = 0;
							e.doit = false;
						}
					}
					parentComposite.layout(true);

					double d;
					if (bVertical) {
						d = (double) below.getBounds().width
								/ parentComposite.getBounds().width;
					} else {
						d = (double) below.getBounds().height
								/ parentComposite.getBounds().height;
					}
					Double l = new Double(d);
					l = ensureVisibilityStates(l, above, below, bVertical);
					sash.setData("PCT", l);

					if (e.detail != SWT.DRAG) {
						COConfigurationManager.setParameter("v3." + sID + ".SplitAt",
								(int) (l.doubleValue() * 10000));
					}

					skipResize = false;
				}
			}
		};
		createOn.addListener(SWT.Resize, l);
		sash.addListener(SWT.Selection, l);

		String sDblClick = properties.getStringValue(sConfigID + ".dblclick");
		if (sDblClick != null) {
			sDblClick = sDblClick.toLowerCase();

			final int dir = (sDblClick.equals("left")) ? SWT.LEFT : SWT.RIGHT;
			sash.addListener(SWT.MouseDoubleClick, new Listener() {
				// @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)

				public void handleEvent(Event e) {
					Composite below = null;
					SWTSkinObject skinObject = skin.getSkinObjectByID(sControlAfter);

					if (skinObject != null) {
						below = (Composite) skinObject.getControl();
					}
					if (below == null) {
						return;
					}

					Composite above = null;
					skinObject = skin.getSkinObjectByID(sControlBefore);

					if (skinObject != null) {
						above = (Composite) skinObject.getControl();
					}

					Double oldPCT = (Double) sash.getData("PCT");
					if (oldPCT == null) {
						oldPCT = new Double(-1);
					}
					Double pct;
					if (dir == SWT.LEFT) {
						if (oldPCT.doubleValue() == 1) {
							pct = new Double(0);
						} else {
							pct = new Double(1);
						}
					} else {
						if (oldPCT.doubleValue() == 0) {
							pct = new Double(1);
						} else {
							pct = new Double(0);
						}
					}

					setPercent(pct, sash, above, below, bVertical, parentComposite);
					below.getParent().layout();
				}

			});
		}

		setControl(sash);
	}

	/**
	 * @param below 
	 * @param bVertical 
	 * @param parentComposite 
	 * @param sash 
	 * @param above 
	 * 
	 */
	protected void setPercent(Double l, Control sash, Composite above,
			Composite below, boolean bVertical, Control parentComposite) {
		FormData belowData = (FormData) below.getLayoutData();
		if (bVertical) {
			belowData.width = (int) ((parentComposite.getBounds().width - (sash.getSize().x / 2)) * l.doubleValue());
			if (belowData.width < 0) {
				belowData.width = 0;
			}
		} else {
			belowData.height = (int) ((parentComposite.getBounds().height - (sash.getSize().y / 2)) * l.doubleValue());
			if (belowData.height < 0) {
				belowData.height = 0;
			}
		}
		below.getParent().layout();

		l = ensureVisibilityStates(l, above, below, bVertical);
		sash.setData("PCT", l);
		COConfigurationManager.setParameter("v3." + sID + ".SplitAt",
				(int) (l.doubleValue() * 10000));
	}

	private Double ensureVisibilityStates(Double l, Composite above,
			Composite below, boolean bVertical) {
		Double ret = l;
		int sizeBelow = bVertical ? below.getSize().x : below.getSize().y;
		int sizeAbove = bVertical ? above.getSize().x : above.getSize().y;

		if ((l.doubleValue() == 0.0 || sizeBelow <= 1) && below != null
				&& below.getVisible()) {
			ret = new Double(0);
			below.setVisible(false);
			below.setData("SashSetVisibility", new Boolean(true));
		} else if (below != null && !below.isVisible()
				&& (below.getData("SashSetVisibility") != null)) {
			below.setVisible(true);
			below.setData("SashSetVisibility", null);
		}

		if ((l.doubleValue() == 1.0 || sizeAbove <= 1) && above != null
				&& above.getVisible()) {
			ret = new Double(1);
			above.setVisible(false);
			above.setData("SashSetVisibility", new Boolean(true));
		} else if (above != null && !above.isVisible()
				&& (above.getData("SashSetVisibility") != null)) {
			above.setVisible(true);
			above.setData("SashSetVisibility", null);
		}

		return ret;
	}
}
