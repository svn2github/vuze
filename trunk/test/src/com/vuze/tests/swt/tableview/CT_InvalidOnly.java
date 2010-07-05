package com.vuze.tests.swt.tableview;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.util.MapUtils;

public class CT_InvalidOnly
	extends CoreTableColumn
	implements TableCellRefreshListener, TableCellSWTPaintListener
{
	public static String name = new Object() { }.getClass().getEnclosingClass().getSimpleName();
	private static String ID_TICS = name + ".num1";
	private static String ID_CELLPAINTS = name + ".numCP";

	public CT_InvalidOnly() {
		super(name, 110, "test");
		addDataSourceType(TableViewTestDS.class);
		setRefreshInterval(TableColumn.INTERVAL_INVALID_ONLY);
		setVisible(true);
	}

	public void refresh(TableCell cell) {
		TableViewTestDS ds = (TableViewTestDS) cell.getDataSource();

		int num = MapUtils.getMapInt(ds.map, ID_TICS, 0) + 1;
		ds.map.put(ID_TICS, num);

		cell.setSortValue(0);
		cell.setText(Integer.toString(num));
	}

	public void cellPaint(GC gc, TableCellSWT cell) {
		TableViewTestDS ds = (TableViewTestDS) cell.getDataSource();

		int num = MapUtils.getMapInt(ds.map, ID_CELLPAINTS, 0) + 1;
		ds.map.put(ID_CELLPAINTS, num);
		GCStringPrinter.printString(gc, Integer.toString(num), cell.getBounds(), true, true,
				SWT.RIGHT);
	}
}
