/*
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.ui.swt.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

public class BubbleTextBox
{
	Text textWidget;
	private Composite cBubble;
	
	
	public BubbleTextBox(Composite parent, int style) {
		cBubble = new Composite(parent, SWT.DOUBLE_BUFFERED);
		cBubble.setLayout(new FormLayout());

		textWidget = new Text(cBubble, style & ~(SWT.BORDER | SWT.SEARCH));
		
		FormData fd = new FormData();
		fd.top = new FormAttachment(0, 2);
		fd.bottom = new FormAttachment(100, -2);
		fd.left = new FormAttachment(0, 17);
		fd.right = new FormAttachment(100, -14);
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

				int iconHeight = clientArea.height - 9;
				if (iconHeight > 13) {
					iconHeight = 13;
				}
				int iconY = clientArea.y + ((clientArea.height - iconHeight + 1) / 2);
				
				e.gc.setAlpha(120);
				e.gc.setLineWidth(2);
				e.gc.drawOval(clientArea.x + 6, iconY, 7, 6); 
				e.gc.drawPolyline(new int[] {
					clientArea.x + 12,
					iconY + 6,
					clientArea.x + 15,
					iconY + iconHeight,
				});
				
				boolean textIsBlank = textWidget.getText().length() == 0;
				if (!textIsBlank) {
					//e.gc.setLineWidth(1);
					e.gc.setAlpha(80);
					Rectangle rXArea = new Rectangle(clientArea.x + clientArea.width
							- 16, clientArea.y + 1, 11, clientArea.height - 2);
					cBubble.setData("XArea", rXArea);

					e.gc.drawPolyline(new int[] {
						clientArea.x + clientArea.width - 7,
						clientArea.y + 7,
						clientArea.x + clientArea.width - (7 + 5),
						clientArea.y + clientArea.height - 7,
					});
					e.gc.drawPolyline(new int[] {
						clientArea.x + clientArea.width - 7,
						clientArea.y + clientArea.height - 7,
						clientArea.x + clientArea.width - (7 + 5),
						clientArea.y + 7,
					});
				}
			}
		});
		
		cBubble.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				Rectangle r = (Rectangle) event.widget.getData("XArea");
				if (r != null && r.contains(event.x, event.y)) {
					textWidget.setText("");
				}
			}
		});
		
			// pick up changes in the text control's bg color and propagate to the bubble
		
		textWidget.addPaintListener(
			new PaintListener()
			{
				private Color existing_bg;
				
				public void 
				paintControl(
					PaintEvent arg0 )
				{
					Color current_bg = textWidget.getBackground();
					
					if ( current_bg != existing_bg ){
						
						existing_bg = current_bg;
						
						cBubble.redraw();
					}
				}
			});
	}
	
	public Composite getParent() {
		return cBubble;
	}
	
	public Text getTextWidget() {
		return textWidget;
	}
}
