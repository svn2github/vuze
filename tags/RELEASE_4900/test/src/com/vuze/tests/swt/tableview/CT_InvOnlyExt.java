package com.vuze.tests.swt.tableview;

import java.util.ArrayList;
import java.util.List;

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
	implements TableCellRefreshListener, TableCellSWTPaintListener,
	TableCellAddedListener
{
	public static String name = new Object() {
	}.getClass().getEnclosingClass().getSimpleName();

	private static Timer timer;

	private static List<TableCell> cells = new ArrayList<TableCell>();

	private static String ID_TICS = name + ".num1";

	private static String ID_CELLPAINTS = name + ".numCP";

	private static String ID_REFRESHES = name + ".num2";

	static {
		timer = new Timer("Simple Timer", 1);

		timer.setIndestructable();

		timer.setWarnWhenFull();

		timer.addEvent("updateLiveExt", SystemTime.getOffsetTime(1000),
				new TimerEventPerformer() {
					public void perform(TimerEvent event) {
						TableCell[] array = cells.toArray(new TableCell[0]);
						for (TableCell cell : array) {
							if (cell.isDisposed()) {
								synchronized (cells) {
									cells.remove(cell);
								}
								continue;
							}
							TableViewTestDS ds = (TableViewTestDS) cell.getDataSource();
							int num = MapUtils.getMapInt(ds.map, ID_TICS, 0) + 1;
							ds.map.put(ID_TICS, num);
							cell.invalidate();
						}
						timer.addEvent("updateLiveExt", SystemTime.getOffsetTime(1000),
								this);
					}
				});
	}

	public CT_InvOnlyExt() {
		super(name, 190, "test");
		addDataSourceType(TableViewTestDS.class);
		setRefreshInterval(TableColumn.INTERVAL_INVALID_ONLY);
		setVisible(true);
	}

	public void refresh(TableCell cell) {
		TableViewTestDS ds = (TableViewTestDS) cell.getDataSource();

		int num = MapUtils.getMapInt(ds.map, ID_TICS, 0);

		int num2 = MapUtils.getMapInt(ds.map, ID_REFRESHES, 0) + 1;
		ds.map.put(ID_REFRESHES, num2);

		cell.setSortValue(0);
		cell.setText("tics=" + num + ";refr=" + num2);
	}

	public void cellPaint(GC gc, TableCellSWT cell) {
		TableViewTestDS ds = (TableViewTestDS) cell.getDataSource();

		int num = MapUtils.getMapInt(ds.map, ID_CELLPAINTS, 0) + 1;
		ds.map.put(ID_CELLPAINTS, num);
		GCStringPrinter.printString(gc, "" + num, cell.getBounds(), true, true,
				SWT.RIGHT);
	}

	public void cellAdded(final TableCell cell) {
		synchronized (cells) {
			cells.add(cell);
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellDisposeListener#dispose(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void dispose(TableCell cell) {
		synchronized (cells) {
			cells.remove(cell);
		}
	}
}
