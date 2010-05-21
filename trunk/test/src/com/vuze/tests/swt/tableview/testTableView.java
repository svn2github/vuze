package com.vuze.tests.swt.tableview;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.ui.swt.UIConfigDefaultsSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager;

import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.uiupdater.UIUpdaterSWT;

public class testTableView
{
	private static TableViewSWTImpl<TableViewTestDS> tv;
	private static boolean pause = false;

	public static void main(String[] args) {
		Display display = new Display();
		
		COConfigurationManager.initialise();
		UIConfigDefaultsSWT.initialize();

		Shell shell = new Shell(display, SWT.SHELL_TRIM);
		shell.setLayout(new FillLayout(SWT.VERTICAL));
		
		TableColumnCore[] columns = {
			new CT_ID(),
			new CT_InvalidOnly(),
			new CT_Live(),
		};
		
		TableColumnManager.getInstance().addColumns(columns);
		
		tv = new TableViewSWTImpl<TableViewTestDS>(TableViewTestDS.class, "test", "", columns, CT_ID.name);
		
		tv.initialize(shell);
		
		for (int i = 0; i < 1000; i++) {
			tv.addDataSource(new TableViewTestDS() {
			});
		}
		
		UIUpdaterSWT.getInstance().addUpdater(new UIUpdatable() {
			
			public void updateUI() {
				if (pause) {
					return;
				}
				tv.refreshTable(false);
			}
			
			public String getUpdateUIName() {
				return "tableTest";
			}
		});
		
		
		Button btnPauseRefresh = new Button(shell, SWT.TOGGLE);
		btnPauseRefresh.setText("Pause");
		btnPauseRefresh.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				pause = !pause;
			}
		});
		
		shell.open();
		
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
}
