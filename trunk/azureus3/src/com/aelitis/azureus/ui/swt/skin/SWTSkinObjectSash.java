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
import org.gudy.azureus2.core3.util.AERunnable;
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

		noresize = properties.getBooleanValue(sConfigID + ".noresize", false);

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


		int splitAt = COConfigurationManager.getIntParameter("v3." + sID
				+ ".splitAt", -1);
		int splitAtPX = COConfigurationManager.getIntParameter("v3." + sID
				+ ".splitAtPX", -1);
		if (noresize && splitAtPX >= 0) {
			if (splitAtPX < resizeContainerAboveMin) {
				splitAtPX = resizeContainerAboveMin;
			}
			sash.setData("PX", new Long(splitAtPX));
		} else if (!noresize && splitAt >= 0) {
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
						sashPct = (double) l / 100;
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
					double pct;
					if (dir == SWT.LEFT) {
						if (oldPCT.doubleValue() == 1) {
							pct = 0;
						} else {
							pct = 1;
						}
					} else {
						if (oldPCT.doubleValue() == 0) {
							pct = 1;
						} else {
							pct = 0;
						}
					}

					setPercent(pct, sash, above, below, bVertical, parentComposite,
							aboveMin, belowMin);
					above.getParent().layout();
				}

			});
		} // dblclick

		sBorder = properties.getStringValue(sConfigID + ".border", (String) null);
		if (sBorder != null) {
			sash.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent e) {
					e.gc.setForeground(e.gc.getDevice().getSystemColor(
							SWT.COLOR_WIDGET_NORMAL_SHADOW));
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
					getConfigID() + ".above"
							+ (isVertical ? ".minwidth" : ".minheight"), 0);
		}
		
		skinObject = skin.getSkinObjectByID(sControlAfter);

		if (skinObject != null) {
			below = (Composite) skinObject.getControl();
		}
		if (below == null) {
			return;
		}

		belowMin = skinObject.getProperties().getIntValue(
				getConfigID() + ".below"
						+ (isVertical ? ".minwidth" : ".minheight"), 0);

		Listener l = new Listener() {
			private boolean skipResize = false;

			public void handleEvent(Event e) {
				if (e.type == SWT.MouseUp) {
					if (e.button == 3 || (e.button == 1 && (e.stateMask & SWT.MOD1) > 0)) {
						String sPos = properties.getStringValue(sConfigID + ".startpos");
						if (sPos == null) {
							return;
						}
						try {
							long l = NumberFormat.getInstance().parse(sPos).longValue();
							if (sPos.endsWith("%")) {
								sashPct = (double) l / 100;
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
					FormData aboveData = (FormData) above.getLayoutData();
					FormData belowData = (FormData) below.getLayoutData();
					if (isVertical) {
						// Need to figure out if we have to use border width elsewhere
						// in calculations (probably)
						aboveData.width = e.x - above.getBorderWidth();
						if (aboveData.width < aboveMin) {
							aboveData.width = aboveMin;
							e.x = aboveMin;
						} else {
							int excess = area.width - (above.getBorderWidth() * 2)
									- sash.getSize().x;
							if (excess - aboveData.width < belowMin) {
								aboveData.width = excess - belowMin;
								e.doit = false;
							}
						}
						ignoreContainerAboveMin = aboveData.width < resizeContainerAboveMin;
					} else {
						aboveData.height = e.y - above.getBorderWidth();
						if (aboveData.height < aboveMin) {
							aboveData.height = aboveMin;
							e.y = aboveMin;
						} else {
							int excess = area.height - (above.getBorderWidth() * 2)
									- sash.getSize().y;
							if (excess - aboveData.height < belowMin) {
								aboveData.height = excess - belowMin;
								e.doit = false;
							}
						}
					}

					parentComposite.layout(true);

					double d;
					double aboveNewSize;
					if (isVertical) {
						aboveNewSize = above.getBounds().width + (sash.getSize().x / 2.0);
						d = aboveNewSize / parentComposite.getBounds().width;
					} else {
						aboveNewSize = above.getBounds().height + (sash.getSize().y / 2.0);
						d = aboveNewSize / parentComposite.getBounds().height;
					}
					sashPct = ensureVisibilityStates(d, above, below, isVertical);
					sash.setData("PCT", new Double(sashPct));
					if (noresize) {
						sash.setData("PX", new Long((long) aboveNewSize));
					}

					if (e.detail != SWT.DRAG) {
						COConfigurationManager.setParameter("v3." + sID + ".splitAt",
								(int) (sashPct * 10000));
					}

					skipResize = false;
				}
			}
		};
		if (!noresize) {
			createOn.addListener(SWT.Resize, l);
		}
		sash.addListener(SWT.Selection, l);
		sash.addListener(SWT.MouseUp, l);
		sash.getShell().addListener(SWT.Show, l);

		Event event = new Event();
		event.type = SWT.Show;

		handleShowResize(event);
	}
	
	public void dispose() {
		if (noresize) {
			Long px = (Long) sash.getData("PX");
			if (px != null && px.longValue() != 0) {
				COConfigurationManager.setParameter("v3." + sID + ".splitAtPX", px.longValue());
			}
		}
		super.dispose();
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
		if (noresize && px == null && e.type != SWT.Show) {
			Point size = createOn.getSize();
			size.x -= createOn.getBorderWidth() * 2;
			size.x -= sash.getSize().x;
			px = new Long((long) (size.x * l.doubleValue()));
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

			setPercent(l.doubleValue(), sash, above, below, isVertical,
					parentComposite, aboveMin, belowMin);
		} else if (px != null) {
			int i = (isVertical) ? parentComposite.getSize().x
					: parentComposite.getSize().y;
			if (i == 0) {
				return;
			}
			double pctAbove = px.doubleValue() / i;

			FormData aboveData = (FormData) above.getLayoutData();
			if (aboveData == null) {
				aboveData = Utils.getFilledFormData();
				above.setLayoutData(aboveData);
			}
			if (isVertical) {
				int parentWidth = parentComposite.getBounds().width;
				aboveData.width = (int) (parentWidth * pctAbove);
				if (parentWidth - aboveData.width < aboveMin) {
					aboveData.width = parentWidth - aboveMin;
				}
				if (noresize) {
					sash.setData("PX", new Long(aboveData.width));
				}
			} else {
				int parentHeight = parentComposite.getBounds().height;
				aboveData.height = (int) (parentHeight * pctAbove);
				if (parentHeight - aboveData.width < aboveMin) {
					aboveData.height = parentHeight - aboveMin;
				}
				if (noresize) {
					sash.setData("PX", new Long(aboveData.height));
				}
			}
			if (pctAbove >= 0 && pctAbove <= 1.0) {
				sashPct = pctAbove;
				sash.setData("PCT", new Double(pctAbove));
			}
			ignoreContainerAboveMin = px.longValue() < resizeContainerAboveMin;
			// layout in resize is not needed (and causes browser widget to blink)
		}
		if (e.type == SWT.Show) {
			parentComposite.layout(true);
		}

	}

	public void setPercent(double pct) {
		setPercent(pct, sash, above, below, isVertical, parentComposite, aboveMin,
				belowMin);
	}

	public double getPercent() {
		if (noresize) {
			Long px = (Long) sash.getData("PX");
			int i = (isVertical) ? parentComposite.getSize().x
					: parentComposite.getSize().y;

			return px.doubleValue() / i;
		}
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
	protected void setPercent(double pctAbove, Control sash, Composite above,
			Composite below, boolean bVertical, Control parentComposite,
			int minAbove, int belowMin) {
		FormData aboveData = (FormData) above.getLayoutData();
		if (aboveData == null) {
			return;
		}
		boolean layoutNeeded = false;
		if (bVertical) {
			int parentWidth = parentComposite.getBounds().width
				- (parentComposite.getBorderWidth() * 2) - sash.getSize().x;
			int newWidth = (int) (parentWidth * pctAbove);
			if (newWidth != aboveData.width) {
				aboveData.width = newWidth;
				layoutNeeded = true;
			}

			//System.out.println("ignore=" + ignoreContainerAboveMin + ";above=" + aboveWidth + ";d=" + d);
			if (!ignoreContainerAboveMin && resizeContainerAboveMin > 0 && pctAbove != 0.0
					&& pctAbove != 1.0) {
				minAbove = Math.max(resizeContainerAboveMin, minAbove);
			}
			if (pctAbove != 0.0
					&& parentWidth - aboveData.width - sash.getSize().x < minAbove) {
				aboveData.width = parentWidth - minAbove - sash.getSize().x;
				layoutNeeded = true;

				//d = (double) (aboveData.width + sash.getSize().x) / parentWidth;
			} else if (aboveData.width < belowMin) {
				layoutNeeded = true;
				aboveData.width = belowMin;
			} else {
				ignoreContainerAboveMin = aboveData.width <= resizeContainerAboveMin;
			}

			if (noresize) {
				sash.setData("PX", new Long(aboveData.width));
			}
		} else {
			int parentHeight = parentComposite.getBounds().height
					- (parentComposite.getBorderWidth() * 2) - sash.getSize().y;
			int newHeight = (int) (parentHeight * pctAbove);
			if (aboveData.height != newHeight) {
				aboveData.height = newHeight;
				layoutNeeded = true;
			}

			if (pctAbove != 0.0 && parentHeight - aboveData.height < minAbove
					&& parentHeight >= minAbove) {
				aboveData.height = parentHeight - minAbove;
				layoutNeeded = true;
			} else if (aboveData.height < belowMin) {
				layoutNeeded = true;
				aboveData.height = belowMin;
			}
			if (noresize) {
				sash.setData("PX", new Long(aboveData.height));
			}
		}
		if (layoutNeeded) {
			above.getParent().layout();
		}

		pctAbove = ensureVisibilityStates(pctAbove, above, below, bVertical);
		sash.setData("PCT", new Double(pctAbove));
		sashPct = pctAbove;
		if (sashPct != 0 && sashPct != 100) {
  		COConfigurationManager.setParameter("v3." + sID + ".splitAt",
  				(int) (pctAbove * 10000));
		}
	}

	private double ensureVisibilityStates(double pct, Composite above,
			Composite below, boolean bVertical) {
		if (pct > 1) {
			pct = 1;
		} else if (pct < 0) {
			pct = 0;
		}

		int sizeBelow = bVertical ? below.getSize().x : below.getSize().y;
		int sizeAbove = bVertical ? above.getSize().x : above.getSize().y;

		if ((pct == 1.0 || sizeBelow <= 1) && below != null && below.getVisible()) {
			below.setVisible(false);
			below.setData("SashSetVisibility", new Boolean(true));
		} else if (below != null && !below.isVisible()
				&& (below.getData("SashSetVisibility") != null)) {
			below.setVisible(true);
			below.setData("SashSetVisibility", null);
		}

		if ((pct == 0.0 || sizeAbove <= 1) && above != null && above.getVisible()) {
			above.setVisible(false);
			above.setData("SashSetVisibility", new Boolean(true));
		} else if (above != null && !above.isVisible()
				&& (above.getData("SashSetVisibility") != null)) {
			above.setVisible(true);
			above.setData("SashSetVisibility", null);
		}

		return pct;
	}

	/**
	 * @param toolbarHeight
	 *
	 * @since 3.1.1.1
	 */
	public void setBelowPX(int px) {
		double sashHeight = sash.getSize().y;
		double parentHeight = parentComposite.getBounds().height
				- (parentComposite.getBorderWidth() * 2);
		
		double want = parentHeight - sashHeight - px;
		double pct = want / parentHeight;
		setPercent(pct);
	}

	public void resetWidth() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				String sPos = properties.getStringValue(sConfigID + ".startpos");
				COConfigurationManager.removeParameter("v3." + sID + ".splitAt");
				COConfigurationManager.removeParameter("v3." + sID + ".splitAtPX");
				if (sPos != null) {
					sash.setData("PX", null);
					sash.setData("PCT", null);
					try {
						int l = NumberFormat.getInstance().parse(sPos).intValue();
						if (sPos.endsWith("%")) {
							sashPct = (double) l / 100;
							sash.setData("PCT", new Double(sashPct));
							setPercent(sashPct);
						} else {
							sash.setData("PX", new Long(l));
							double parentHeight = parentComposite.getBounds().height
									- (parentComposite.getBorderWidth() * 2);

							if (parentHeight != 0) {
								double want = l;
								double pct = want / parentHeight;
								setPercent(pct);
							}
						}
					} catch (Exception e) {
						Debug.out(e);
					}
				}
			}
		});
	}
}
