/**
 * Created on Sep 21, 2008
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.ui.swt.skin;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * Native checkbox
 * 
 * @author TuxPaper
 * @created Dec 24, 2008
 *
 */
public class SWTSkinObjectTextbox
	extends SWTSkinObjectBasic
{
	private Text textWidget;
	
	private Composite cBubble;
	
	private String text = "";

	public SWTSkinObjectTextbox(SWTSkin skin, SWTSkinProperties properties,
			String id, String configID, SWTSkinObject parentSkinObject) {
		super(skin, properties, id, configID, "textbox", parentSkinObject);

		Composite createOn;
		if (parent == null) {
			createOn = skin.getShell();
		} else {
			createOn = (Composite) parent.getControl();
		}
		
		int style = SWT.BORDER;
		
		String styleString = properties.getStringValue(sConfigID + ".style");
		if (styleString != null) {
			String[] styles = styleString.toLowerCase().split(",");
			Arrays.sort(styles);
			if (Arrays.binarySearch(styles, "readonly") >= 0) {
				style |= SWT.READ_ONLY;
			}
			if (Arrays.binarySearch(styles, "wrap") >= 0) {
				style |= SWT.WRAP;
			}
			if (Arrays.binarySearch(styles, "multiline") >= 0) {
				style |= SWT.MULTI | SWT.V_SCROLL;
			} else {
				style |= SWT.SINGLE;
			}
			if (Arrays.binarySearch(styles, "search") >= 0) {
				style |= SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL;
				if (Constants.isWindows) {
					cBubble = new Composite(createOn, SWT.NONE);
					cBubble.setLayout(new FormLayout());
				}
			}
		}
		
		if ((style & SWT.WRAP) == 0 && (style & SWT.MULTI) > 0) {
			style |= SWT.H_SCROLL;
		}


		if (cBubble == null) {
			textWidget = new Text(createOn, style);
		} else {
			textWidget = new Text(cBubble, style & ~(SWT.BORDER | SWT.SEARCH));
			
			FormData fd = new FormData();
			fd.top = new FormAttachment(0, 2);
			fd.bottom = new FormAttachment(100, -2);
			fd.left = new FormAttachment(0, 20);
			fd.right = new FormAttachment(100, -15);
			textWidget.setLayoutData(fd);

			cBubble.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent e) {
					Rectangle clientArea = cBubble.getClientArea();
					e.gc.setBackground(textWidget.getBackground());
					e.gc.setAdvanced(true);
					e.gc.setAntialias(SWT.ON);
					e.gc.fillRoundRectangle(clientArea.x, clientArea.y,
							clientArea.width - 1, clientArea.height - 1, clientArea.height,
							clientArea.height);
					e.gc.setAlpha(127);
					e.gc.drawRoundRectangle(clientArea.x, clientArea.y,
							clientArea.width - 1, clientArea.height - 1, clientArea.height,
							clientArea.height);

					e.gc.setLineCap(SWT.CAP_ROUND);

					e.gc.setAlpha(120);
					e.gc.setLineWidth(2);
					e.gc.drawOval(clientArea.x + 6, clientArea.y + 5, 7, 6); 
					e.gc.drawPolyline(new int[] {
						clientArea.x + 12,
						clientArea.y + 11,
						clientArea.x + 15,
						clientArea.y + clientArea.height - 5,
					});
					
					//e.gc.setLineWidth(1);
					e.gc.setAlpha(80);
					Rectangle rXArea = new Rectangle(clientArea.x + clientArea.width - 14,
							clientArea.y + 6, 7, 7);
					cBubble.setData("XArea", rXArea);

					e.gc.drawPolyline(new int[] {
						clientArea.x + clientArea.width - 7,
						clientArea.y + 6,
						clientArea.x + clientArea.width - (7 + 7),
						clientArea.y + clientArea.height - 6,
					});
					e.gc.drawPolyline(new int[] {
						clientArea.x + clientArea.width - 7,
						clientArea.y + clientArea.height - 6,
						clientArea.x + clientArea.width - (7 + 7),
						clientArea.y + 6,
					});
				}
			});
			
			cBubble.addListener(SWT.MouseDown, new Listener() {
				public void handleEvent(Event event) {
					Rectangle r = (Rectangle) event.widget.getData("XArea");
					System.out.println(r + ";;;;" + event.x + "," + event.y);
					if (r != null && r.contains(event.x, event.y)) {
						textWidget.setText("");
					}
				}
			});
		}
		
		textWidget.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				text = textWidget.getText();
			}
		});
		
		String message = properties.getStringValue(configID + ".message", (String) null);
		if (message != null && message.length() > 0) {
			textWidget.setMessage(message);
		}

		setControl(cBubble == null ? textWidget : cBubble);
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectBasic#switchSuffix(java.lang.String, int, boolean)
	public String switchSuffix(String suffix, int level, boolean walkUp,
			boolean walkDown) {
		suffix = super.switchSuffix(suffix, level, walkUp, walkDown);

		if (suffix == null) {
			return null;
		}

		String sPrefix = sConfigID + ".text";
		String text = properties.getStringValue(sPrefix + suffix);
		if (text != null) {
			setText(text);
		}

		return suffix;
	}

	public void setText(final String val) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (textWidget != null && !textWidget.isDisposed()) {
					textWidget.setText(val == null ? "" : val);
					text = val;
				}
			}
		});

	}

	public String getText() {
		return text;
	}

	public Text getTextControl() {
		return textWidget;
	}

}
