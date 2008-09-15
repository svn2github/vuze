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
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * <p>
 * Parameters:
 * <dl>
 * <dt>.startpos</dt>
 * <dd>Position in % or pixels of where to start the sash by default</dd>
 * <dt>.resize.container.min</dt>
 * <dd></dd>
 * <dt>.dblclick</dt>
 * <dd></dd>
 * <dt></dt>
 * <dd></dd>
 * </dl>
 * 
 * @author TuxPaper
 * @created Oct 18, 2006
 *
 */
public class SWTSkinObjectSash
	extends SWTSkinObjectBasic
{
	/**
	 * Fast Drag disables resizing left and right sides on each mouse move (when
	 * mouse is down)
	 * 
	 * Two problems with disabling FASTDRAG:
	 * 1) The places we use the sash currently have very slow re-rendering
	 * 2) when the user drags out of bounds (minsize, etc), and we set doit
	 *    to false.  When the user lifts up the mouse button, we get one
	 *    selection event at the old position (because we cancelled)
	 *    
	 * #2 can be fixed... #1 not so much..
	 */
	private static final boolean FASTDRAG = true;

	protected String sControlBefore;

	protected String sControlAfter;

	private int resizeContainerAboveMin = -1;

	boolean ignoreContainerAboveMin = false;

	private Composite createOn;

	private final boolean isVertical;

	private Sash sash;

	Point lastSize = new Point(0, 0);

	private Composite parentComposite;

	private Composite above = null;

	private int aboveMin = 0;

	private Composite below = null;

	private int belowMin = 0;

	private double sashPct;

	private boolean noresize = false;

	private String sBorder;

	public SWTSkinObjectSash(final SWTSkin skin,
			final SWTSkinProperties properties, final String sID,
			final String sConfigID, String[] typeParams, SWTSkinObject parent,
			final boolean bVertical) {
		super(skin, properties, sID, sConfigID, "sash", parent);
		isVertical = bVertical;

		int style = bVertical ? SWT.VERTICAL : SWT.HORIZONTAL;

		if (typeParams.length > 2) {
			sControlBefore = typeParams[1];
			sControlAfter = typeParams[2];
		}

		if (parent == null) {
			createOn = skin.getShell();
		} else {
			createOn = (Composite) parent.getControl();
		}

		if (createOn == null || createOn.isDisposed()) {
			Debug.out("Can not create " + sID + " because parent is null or disposed");
			return;
		}

		sash = new Sash(createOn, style);

		int splitAt = COConfigurationManager.getIntParameter("v3." + sID
				+ ".SplitAt", -1);
		if (splitAt >= 0) {
			sashPct = splitAt / 10000.0;
			if (sashPct > 1) {
				sashPct = 1;
			} else if (sashPct < 0) {
				sashPct = 0;
			}
			sash.setData("PCT", new Double(sashPct));
		} else {
			String sPos = properties.getStringValue(sConfigID + ".startpos");
			if (sPos != null) {
				try {
					long l = NumberFormat.getInstance().parse(sPos).longValue();
					if (sPos.endsWith("%")) {
						sashPct = (double) (100 - l) / 100;
						sash.setData("PCT", new Double(sashPct));
					} else {
						sash.setData("PX", new Long(l));
					}
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		}

		parentComposite = createOn;

		String sMinContainerPos = properties.getStringValue(sConfigID
				+ ".resize.container.min");
		if (sMinContainerPos != null) {
			try {
				resizeContainerAboveMin = NumberFormat.getInstance().parse(
						sMinContainerPos).intValue();
			} catch (Exception e) {
				Debug.out(e);
			}
		}
		
		noresize = properties.getBooleanValue(sConfigID + ".noresize", false);

		SWTSkinObject soInitializeSashAfterCreated = parent == null ? this : parent;
		soInitializeSashAfterCreated.addListener(new SWTSkinObjectListener() {
			public Object eventOccured(SWTSkinObject skinObject, int eventType,
					Object params) {
				if (eventType == SWTSkinObjectListener.EVENT_CREATED) {
					initialize();
				}
				return null;
			}
		});

		String sDblClick = properties.getStringValue(sConfigID + ".dblclick");
		if (sDblClick != null) {
			sDblClick = sDblClick.toLowerCase();

			final int dir = (sDblClick.equals("left")) ? SWT.LEFT : SWT.RIGHT;
			sash.addListener(SWT.MouseDoubleClick, new Listener() {
				// @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)

				public void handleEvent(Event e) {
					if (below == null || above == null) {
						return;
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

					setPercent(pct, sash, above, below, bVertical, parentComposite,
							aboveMin, belowMin);
					below.getParent().layout();
				}

			});
		} // dblclick
		
		sBorder = properties.getStringValue(sConfigID + ".border", (String) null);
		if (sBorder != null) {
			sash.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent e) {
					e.gc.setForeground(e.gc.getDevice().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
					Point size = sash.getSize();
					if (bVertical) {
						e.gc.drawLine(0, 0, 0, size.y);
						if (!sBorder.startsWith("thin-top")) {
							int x = size.x - 1;
							e.gc.drawLine(x, 0, x, 0 + size.y);
						}
					} else {
						e.gc.drawLine(0, 0, 0 + size.x, 0);
						if (!sBorder.startsWith("thin-top")) {
							int y = size.y - 1;
							e.gc.drawLine(0, y, 0 + size.x, y);
						}
					}
				}
			});
		}

		setControl(sash);
	}

	/**
	 * 
	 *
	 * @since 3.1.0.1
	 */
	protected void initialize() {
		SWTSkinObject skinObject;

		skinObject = skin.getSkinObjectByID(sControlBefore);

		if (skinObject != null) {
			above = (Composite) skinObject.getControl();
			aboveMin = skinObject.getProperties().getIntValue(
					skinObject.getConfigID() + (isVertical ? ".minwidth" : ".minheight"),
					0);
		}

		skinObject = skin.getSkinObjectByID(sControlAfter);

		if (skinObject != null) {
			below = (Composite) skinObject.getControl();
		}
		if (below == null) {
			return;
		}

		belowMin = skinObject.getProperties().getIntValue(
				skinObject.getConfigID() + (isVertical ? ".minwidth" : ".minheight"), 0);

		Listener l = new Listener() {
			private boolean skipResize = false;

			public void handleEvent(Event e) {
				if (e.type == SWT.MouseUp) {
					if (e.button == 2 || (e.button == 3 && (e.stateMask & SWT.MOD1) > 0)) {
						String sPos = properties.getStringValue(sConfigID + ".startpos");
						if (sPos == null) {
							return;
						}
						try {
							long l = NumberFormat.getInstance().parse(sPos).longValue();
							if (sPos.endsWith("%")) {
								sashPct = (double) (100 - l) / 100;
								sash.setData("PCT", new Double(sashPct));
							} else {
								sash.setData("PX", new Long(l));
								sash.setData("PCT", null);
							}
							// FALL THROUGH
							e.type = SWT.Show;
						} catch (Exception ex) {
							Debug.out(ex);
							return;
						}
					} else {
						return;
					}
				}

				if (e.type == SWT.Resize && skipResize) {
					return;
				}
				if (e.type == SWT.Resize || e.type == SWT.Show) {
					handleShowResize(e);
				} else if (e.type == SWT.Selection) {
					skipResize = true;

					if (FASTDRAG && e.detail == SWT.DRAG) {
						return;
					}

					Rectangle area = parentComposite.getBounds();
					FormData belowData = (FormData) below.getLayoutData();
					if (isVertical) {
						// Need to figure out if we have to use border width elsewhere
						// in calculations (probably)
						area.width -= below.getBorderWidth() * 2;
						belowData.width = area.width - (e.x + e.width);
						if (area.width - belowData.width - sash.getSize().x < aboveMin) {
							belowData.width = area.width - aboveMin - sash.getSize().x;
						} else if (belowData.width < belowMin) {
							belowData.width = belowMin;
							e.doit = false;
						}
						int aboveWidth = area.width - belowData.width - sash.getSize().x;
						ignoreContainerAboveMin = aboveWidth < resizeContainerAboveMin;
					} else {
						area.height -= below.getBorderWidth() * 2;
						belowData.height = area.height - (e.y + e.height);
						if (area.height - belowData.height < aboveMin) {
							belowData.height = area.height - aboveMin;
						} else if (belowData.height < belowMin) {
							belowData.height = belowMin;
							e.doit = false;
						}
					}

					parentComposite.layout(true);

					double d;
					double nw;
					if (isVertical) {
						nw = below.getBounds().width + (sash.getSize().x / 2.0);
						d = nw / parentComposite.getBounds().width;
						nw = parentComposite.getBounds().width - nw;
					} else {
						nw = below.getBounds().height + (sash.getSize().y / 2.0);
						d = nw / parentComposite.getBounds().height;
						nw = parentComposite.getBounds().height - nw;
					}
					Double l = new Double(d);
					l = ensureVisibilityStates(l, above, below, isVertical);
					sashPct = l.doubleValue();
					sash.setData("PCT", l);
					if (noresize) {
						sash.setData("PX", new Long((long)nw));
					}

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
		sash.addListener(SWT.MouseUp, l);
		sash.getShell().addListener(SWT.Show, l);

		Event event = new Event();
		event.type = SWT.Show;

		handleShowResize(event);
	}

	/**
	 * @param e
	 *
	 * @since 3.1.0.1
	 */
	protected void handleShowResize(Event e) {
		if (!createOn.isVisible() && e.type != SWT.Show) {
			return;
		}

		Double l = (Double) sash.getData("PCT");
		Long px = (Long) sash.getData("PX");
		if (noresize && px == null &&  e.type != SWT.Show) {
			Point size = createOn.getSize();
			px = new Long((long) (size.x * (1 - l.doubleValue())));
			sash.setData("PX", px);
		}
		if (l != null && (!noresize || e.type == SWT.Show)) {
			Point size = createOn.getSize();
			if (isVertical && size.x == lastSize.x) {
				return;
			} else if (!isVertical && size.y == lastSize.y) {
				return;
			}

			lastSize = size;

			setPercent(l, sash, above, below, isVertical, parentComposite, aboveMin,
					belowMin);
		} else if (px != null) {
			int i = (isVertical) ? parentComposite.getSize().x
					: parentComposite.getSize().y;
			if (i == 0) {
				return;
			}
			double pctBelow = 1 - (px.doubleValue() / i);

			FormData belowData = (FormData) below.getLayoutData();
			if (belowData == null) {
				belowData = Utils.getFilledFormData();
				below.setLayoutData(belowData);
			}
			if (isVertical) {
				int parentWidth = parentComposite.getBounds().width;
				belowData.width = (int) (parentWidth * pctBelow);
				if (parentWidth - belowData.width < aboveMin) {
					belowData.width = parentWidth - aboveMin;
				}
			} else {
				int parentHeight = parentComposite.getBounds().height;
				belowData.height = (int) (parentHeight * pctBelow);
				if (parentHeight - belowData.width < aboveMin) {
					belowData.height = parentHeight - aboveMin;
				}
			}
			if (pctBelow >= 0 && pctBelow <= 1.0) {
				sashPct = pctBelow;
				sash.setData("PCT", new Double(pctBelow));
			}
			ignoreContainerAboveMin = px.longValue() < resizeContainerAboveMin;
			// layout in resize is not needed (and causes browser widget to blink)
		}
		if (e.type == SWT.Show) {
			parentComposite.layout(true);
		}

	}

	public void setPercent(double pct) {
		setPercent(new Double(pct), sash, above, below, isVertical,
				parentComposite, aboveMin, belowMin);
	}

	public double getPercent() {
		return sashPct;
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
			Composite below, boolean bVertical, Control parentComposite,
			int minAbove, int belowMin) {
		FormData belowData = (FormData) below.getLayoutData();
		double d = l.doubleValue();
		boolean layoutNeeded = false;
		if (bVertical) {
			int parentWidth = parentComposite.getBounds().width;
			int newWidth = (int) ((parentWidth - (sash.getSize().x / 2)) * d);
			if (newWidth != belowData.width) {
				belowData.width = newWidth;
				layoutNeeded = true;
			}

			int aboveWidth = parentWidth - belowData.width - sash.getSize().x;

			//System.out.println("ignore=" + ignoreContainerAboveMin + ";above=" + aboveWidth + ";d=" + d);
			if (!ignoreContainerAboveMin && resizeContainerAboveMin > 0 && d != 0.0
					&& d != 1.0) {
				minAbove = Math.max(resizeContainerAboveMin, minAbove);
			}
			if (l.doubleValue() != 1.0
					&& parentWidth - belowData.width - sash.getSize().x < minAbove) {
				belowData.width = parentWidth - minAbove - sash.getSize().x;
				layoutNeeded = true;

				//d = (double) (belowData.width + (sash.getSize().x / 2)) / parentWidth;
			} else if (belowData.width < belowMin) {
				layoutNeeded = true;
				belowData.width = belowMin;
			} else {
				ignoreContainerAboveMin = aboveWidth <= resizeContainerAboveMin;
			}

		} else {
			int parentHeight = parentComposite.getBounds().height;
			int newHeight = (int) ((parentHeight - (sash.getSize().y / 2)) * d);
			if (belowData.height != newHeight) {
				belowData.height = newHeight;
				layoutNeeded = true;
			}

			if (l.doubleValue() != 1.0 && parentHeight - belowData.height < minAbove
					&& parentHeight >= minAbove) {
				belowData.height = parentHeight - minAbove;
				layoutNeeded = true;
			} else if (belowData.height < belowMin) {
				layoutNeeded = true;
				belowData.height = belowMin;
			}
		}
		if (layoutNeeded) {
			below.getParent().layout();
		}

		l = ensureVisibilityStates(l, above, below, bVertical);
		sash.setData("PCT", l);
		sashPct = l.doubleValue();
		COConfigurationManager.setParameter("v3." + sID + ".SplitAt",
				(int) (l.doubleValue() * 10000));
	}

	private Double ensureVisibilityStates(Double l, Composite above,
			Composite below, boolean bVertical) {
		double pct = l.doubleValue();
		if (pct > 1) {
			pct = 1;
		} else if (pct < 0) {
			pct = 0;
		}

		Double ret = new Double(pct);
		int sizeBelow = bVertical ? below.getSize().x : below.getSize().y;
		int sizeAbove = bVertical ? above.getSize().x : above.getSize().y;

		if ((pct == 0.0 || sizeBelow <= 1) && below != null && below.getVisible()) {
			below.setVisible(false);
			below.setData("SashSetVisibility", new Boolean(true));
		} else if (below != null && !below.isVisible()
				&& (below.getData("SashSetVisibility") != null)) {
			below.setVisible(true);
			below.setData("SashSetVisibility", null);
		}

		if ((pct == 1.0 || sizeAbove <= 1) && above != null && above.getVisible()) {
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
