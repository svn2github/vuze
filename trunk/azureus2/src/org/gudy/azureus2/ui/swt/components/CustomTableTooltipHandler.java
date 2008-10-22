/*
 * Created on Oct 21, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package org.gudy.azureus2.ui.swt.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

public class 
CustomTableTooltipHandler 
	implements Listener
{
	Shell toolTipShell = null;

	Shell mainShell = null;

	Label toolTipLabel = null;

	private final Table table;

	/**
	 * Initialize
	 */
	public CustomTableTooltipHandler(
		Table _table) 
	{
		table = _table;
		mainShell = table.getShell();

		table.addListener(SWT.Dispose, this);
		table.addListener(SWT.KeyDown, this);
		table.addListener(SWT.MouseMove, this);
		table.addListener(SWT.MouseHover, this);
		mainShell.addListener(SWT.Deactivate, this);
		table.addListener(SWT.Deactivate, this);
		
		table.setToolTipText( "" );	// disable native tooltip
	}

	public void handleEvent(Event event) {
		switch (event.type) {
			case SWT.MouseHover: {
				if (toolTipShell != null && !toolTipShell.isDisposed())
					toolTipShell.dispose();

				TableItem item = table.getItem( new Point( event.x, event.y ));
				
				if (item == null)
					return;
				
				Object oToolTip = item.getData( "tooltip" );
				
				if ( oToolTip == null ){
				
					oToolTip = item.getText(0);
				}
				
				// TODO: support composite, image, etc
				if (oToolTip == null || !(oToolTip instanceof String))
					return;
				String sToolTip = (String) oToolTip;

				Display d = table.getDisplay();
				if (d == null)
					return;

				// We don't get mouse down notifications on trim or borders..
				toolTipShell = new Shell(table.getShell(), SWT.ON_TOP);
				FillLayout f = new FillLayout();
				try {
					f.marginWidth = 3;
					f.marginHeight = 1;
				} catch (NoSuchFieldError e) {
					/* Ignore for Pre 3.0 SWT.. */
				}
				toolTipShell.setLayout(f);
				toolTipShell.setBackground(d.getSystemColor(SWT.COLOR_INFO_BACKGROUND));

				toolTipLabel = new Label(toolTipShell, SWT.WRAP);
				toolTipLabel.setForeground(d.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
				toolTipLabel.setBackground(d.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
				//toolTipShell.setData("TableCellSWT", item);
				toolTipLabel.setText(sToolTip.replaceAll("&", "&&"));
				// compute size on label instead of shell because label
				// calculates wrap, while shell doesn't
				Point size = toolTipLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				if (size.x > 600) {
					size = toolTipLabel.computeSize(600, SWT.DEFAULT, true);
				}
				size.x += toolTipShell.getBorderWidth() * 2 + 2;
				size.y += toolTipShell.getBorderWidth() * 2;
				try {
					size.x += toolTipShell.getBorderWidth() * 2 + (f.marginWidth * 2);
					size.y += toolTipShell.getBorderWidth() * 2 + (f.marginHeight * 2);
				} catch (NoSuchFieldError e) {
					/* Ignore for Pre 3.0 SWT.. */
				}
				Point pt = table.toDisplay(event.x, event.y);
				Rectangle displayRect;
				try {
					displayRect = table.getMonitor().getClientArea();
				} catch (NoSuchMethodError e) {
					displayRect = table.getDisplay().getClientArea();
				}
				if (pt.x + size.x > displayRect.x + displayRect.width) {
					pt.x = displayRect.x + displayRect.width - size.x;
				}

				if (pt.y + size.y > displayRect.y + displayRect.height) {
					pt.y -= size.y + 2;
				} else {
					pt.y += 21;
				}

				if (pt.y < displayRect.y)
					pt.y = displayRect.y;

				toolTipShell.setBounds(pt.x, pt.y, size.x, size.y);
				toolTipShell.setVisible(true);

				break;
			}

			case SWT.Dispose:
				if (mainShell != null && !mainShell.isDisposed())
					mainShell.removeListener(SWT.Deactivate, this);
				if (table != null && !table.isDisposed())
					mainShell.removeListener(SWT.Deactivate, this);
				// fall through

			default:
				if (toolTipShell != null) {
					toolTipShell.dispose();
					toolTipShell = null;
					toolTipLabel = null;
				}
				break;
		} // switch
	} // handlEvent()
}
