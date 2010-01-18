/*
 * Created on Jan 17, 2010 2:19:53 AM
 * Copyright (C) 2010 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.views.FilesView;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Jan 17, 2010
 *
 */
public class ColumnFiles
	extends CoreTableColumn
	implements TableCellMouseListener
{
	public static final Class DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "files";
	
	public ColumnFiles(String sTableID) {
		super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_CENTER, 60, sTableID);
		setRefreshInterval(INTERVAL_INVALID_ONLY);
	}

	public void cellMouseTrigger(final TableCellMouseEvent event) {
		final DownloadManager dm = (DownloadManager) event.cell.getDataSource();
		
		if (event.eventType == TableRowMouseEvent.EVENT_MOUSEUP && event.button == 1) {
			Utils.execSWTThreadLater(0, new AERunnable() {
				
				public void runSupport() {
					openFilesMiniView(dm, event.cell);
				}
			});
		}
	}

	private void openFilesMiniView(DownloadManager dm, TableCell cell) {
		Shell shell = ShellFactory.createShell(Utils.findAnyShell(), SWT.SHELL_TRIM);
		
		shell.setLayout(new FillLayout());
		
		FilesView view = new FilesView(false);
		view.dataSourceChanged(dm);
		
		view.initialize(shell);
		
		Composite composite = view.getComposite();
		//composite.setLayoutData(null);
		shell.setLayout(new FillLayout());
		
		view.viewActivated();
		view.refresh();
		
		shell.layout(true, true);
		
		Rectangle bounds = ((TableCellSWT)cell).getBoundsOnDisplay();
		bounds.y += bounds.height;
		bounds.width = 630;
		bounds.height = (16 * dm.getDiskManagerFileInfo().length) + 60;
		Rectangle realBounds = shell.computeTrim(0, 0, bounds.width, bounds.height);
		realBounds.width -= realBounds.x;
		realBounds.height -= realBounds.y;
		realBounds.x = bounds.x;
		realBounds.y = bounds.y;
		if (bounds.height > 500) {
			bounds.height = 500;
		}
		
		shell.setText(dm.getDisplayName());
		
		shell.setBounds(realBounds);
		
		Utils.verifyShellRect(shell, true);
		shell.open();
	}

}
