package com.vuze.tests.swt.tableview;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.util.MapUtils;

public class CT_InvOnlyExt
	extends CoreTableColumn
	implements TableCellRefreshListener,
		TableCellSWTPaintListener,
		TableCellAddedListener
{
	public static String name = new Object() { }.getClass().getEnclosingClass().getSimpleName();
	private static Timer timer;

	static {
		timer = new Timer("Simple Timer", 5000);
		
		timer.setIndestructable();
		
		timer.setWarnWhenFull();
	}

	public CT_InvOnlyExt() {
		super(name, 150, "test");
		setForDataSourceType(TableViewTestDS.class);
		setRefreshInterval(TableColumn.INTERVAL_INVALID_ONLY);
		setVisible(true);
	}

	public void refresh(TableCell cell) {
		TableViewTestDS ds = (TableViewTestDS) cell.getDataSource();

		int num = MapUtils.getMapInt(ds.map, name + ".num1", 0);
		
		int num2 = MapUtils.getMapInt(ds.map, name + ".num2", 0) + 1;
		ds.map.put(name + ".num2", num2);
		
		cell.setSortValue(0);
		cell.setText("tics=" + num + ";refr=" + num2);
	}

	public void cellPaint(GC gc, TableCellSWT cell) {
		TableViewTestDS ds = (TableViewTestDS) cell.getDataSource();

		int num = MapUtils.getMapInt(ds.map, name + ".numCP", 0) + 1;
		ds.map.put(name + ".numCP", num);
		GCStringPrinter.printString(gc, "" + num, cell.getBounds(), true, true, SWT.RIGHT);
	}

	public void cellAdded(final TableCell cell) {
		timer.addPeriodicEvent("updateLiveExt", 1000, new TimerEventPerformer() {
			public void perform(TimerEvent event) {
				if (cell.isDisposed()) {
					event.cancel();
					return;
				}
				TableViewTestDS ds = (TableViewTestDS) cell.getDataSource();
				int num = MapUtils.getMapInt(ds.map, name + ".num1", 0) + 1;
				ds.map.put(name + ".num1", num);
				cell.invalidate();
 			}
		});
	}
}
