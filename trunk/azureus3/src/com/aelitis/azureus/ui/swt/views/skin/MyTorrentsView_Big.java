package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.SWT;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.views.MyTorrentsView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.ui.common.table.TableColumnCore;

public class MyTorrentsView_Big
	extends MyTorrentsView
{
	public MyTorrentsView_Big(AzureusCore _azureus_core, boolean isSeedingView,
			TableColumnCore[] basicItems) {
		super(_azureus_core, isSeedingView, basicItems);
	}

	protected TableViewSWT createTableView(TableColumnCore[] basicItems) {
		return new TableViewSWTImpl(isSeedingView
				? TableManager.TABLE_MYTORRENTS_COMPLETE_BIG
				: TableManager.TABLE_MYTORRENTS_INCOMPLETE_BIG, "MyTorrentsView_Big",
				basicItems, "#", SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL
						| SWT.BORDER);
	}

	protected int getRowDefaultHeight() {
		return 36;
	}

}
