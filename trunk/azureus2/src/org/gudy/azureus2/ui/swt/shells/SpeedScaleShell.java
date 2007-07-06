/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.ui.swt.shells;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * Cheap ugly slider shell
 * 
 * @author TuxPaper
 * @created Jul 5, 2007
 *
 */
public class SpeedScaleShell
{
	private static final int TEXT_HEIGHT = 16;

	private static final int HEIGHT = TEXT_HEIGHT + 20;

	private static final int WIDTH = 120;

	private static final int BASELINE = HEIGHT - (HEIGHT - TEXT_HEIGHT) / 2;

	private static final int PADDING_X0 = 10;

	private static final int PADDING_X1 = 10;

	private static final int WIDTH_NO_PADDING = WIDTH - PADDING_X0 - PADDING_X1;

	private int value;

	private boolean cancelled = true;

	private int minValue;

	private int maxValue;

	public SpeedScaleShell() {
		minValue = 0;
		maxValue = -1;
	}

	/**
	 * Borks with 0 or -1 maxValue
	 * 
	 * @param startValue
	 * @return
	 *
	 * @since 3.0.1.7
	 */
	public boolean open(int startValue) {
		return open(startValue, false);
	}

	public boolean open(int startValue, final boolean assumeMouseDown) {
		value = startValue;
		cancelled = true;

		final Shell shell = new Shell(Utils.findAnyShell(), SWT.NO_TRIM);
		final Display display = shell.getDisplay();

		shell.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					shell.dispose();
				}
			}
		});

		shell.addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(MouseEvent e) {
				if ((e.stateMask & SWT.BUTTON_MASK) > 0 || assumeMouseDown) {
					int x = e.x + 1;
					if (e.x < PADDING_X0) {
						x = PADDING_X0;
					} else if (e.x > PADDING_X0 + WIDTH_NO_PADDING) {
						x = PADDING_X0 + WIDTH_NO_PADDING;
					}

					value = (x - PADDING_X0) * maxValue / WIDTH_NO_PADDING;
					shell.redraw();
				}
			}
		});

		shell.addMouseTrackListener(new MouseTrackListener() {

			public void mouseHover(MouseEvent e) {
			}

			public void mouseExit(MouseEvent e) {
				setCancelled(false);
				shell.dispose();
			}

			public void mouseEnter(MouseEvent e) {
			}
		});

		shell.addMouseListener(new MouseListener() {

			public void mouseUp(MouseEvent e) {
				setCancelled(false);
				shell.dispose();
			}

			public void mouseDown(MouseEvent e) {
			}

			public void mouseDoubleClick(MouseEvent e) {
			}

		});

		shell.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				int x = WIDTH_NO_PADDING * value / maxValue;

				e.gc.setAdvanced(true);
				e.gc.setAntialias(SWT.ON);

				e.gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_RED));
				e.gc.drawLine(PADDING_X0, BASELINE - 6, PADDING_X0, BASELINE + 6);
				e.gc.drawLine(PADDING_X0 + WIDTH_NO_PADDING, BASELINE - 6, PADDING_X0
						+ WIDTH_NO_PADDING, BASELINE + 6);
				e.gc.drawLine(PADDING_X0, BASELINE, PADDING_X0 + WIDTH_NO_PADDING,
						BASELINE);

				e.gc.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
				e.gc.fillRoundRectangle(PADDING_X0 + x - 2, BASELINE - 5, 5, 10, 10, 10);

				GCStringPrinter.printString(e.gc, getStringValue(), new Rectangle(
						0, 0, WIDTH, HEIGHT), true, false,
						SWT.CENTER | SWT.TOP | SWT.WRAP);
			}
		});

		Point location = display.getCursorLocation();
		
		// TODO: Handle offscreen
		location.y -= BASELINE;
		int x = WIDTH_NO_PADDING * value / maxValue;
		location.x -= PADDING_X0 + x;
		
		shell.setSize(WIDTH, HEIGHT);
		shell.setLocation(location);
		shell.open();

		try {
			while (!shell.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
		} catch (Throwable t) {
			Debug.out(t);
		}

		return !cancelled;
	}

	public int getValue() {
		return value;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public static void main(String[] args) {
		SpeedScaleShell speedScaleWidget = new SpeedScaleShell() {
			public String getStringValue() {
				return getValue() + "b/s";
			}
		};
		speedScaleWidget.setMaxValue(10000);
		System.out.println("returns " + speedScaleWidget.open(1000));
		System.out.println(speedScaleWidget.getValue());

	}

	public int getMinValue() {
		return minValue;
	}

	public void setMinValue(int minValue) {
		this.minValue = minValue;
	}

	public int getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(int maxValue) {
		this.maxValue = maxValue;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public String getStringValue() {
		return "" + value;
	}
}
