package com.vuze.tests.swt.tableview;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.util.MapUtils;

public class CT_ID
	extends CoreTableColumn
	implements TableCellAddedListener, TableCellSWTPaintListener
{
	public static double id = 0;
	public static String name = new Object() { }.getClass().getEnclosingClass().getSimpleName();
	
	public CT_ID() {
		super(name, 150, "test");
		setForDataSourceType(TableViewTestDS.class);
		setRefreshInterval(TableColumn.INTERVAL_INVALID_ONLY);
		setVisible(true);
	}

	public void cellAdded(TableCell cell) {
		TableViewTestDS ds = (TableViewTestDS) cell.getDataSource();
		Object mapObject = MapUtils.getMapObject(ds.map, "ID", null, Number.class);
		if (mapObject instanceof Number) {
			double overideID = ((Double) mapObject).doubleValue();
			cell.setSortValue(overideID);
			cell.setText("" + overideID);
		} else {
  		id++;
  		cell.setSortValue(id);
  		cell.setText("" + id);
		}
	}

	public void cellPaint(GC gc, TableCellSWT cell) {
		TableViewTestDS ds = (TableViewTestDS) cell.getDataSource();

		int num = MapUtils.getMapInt(ds.map, name + ".numCP", 0) + 1;
		ds.map.put(name + ".numCP", num);
		GCStringPrinter.printString(gc, "" + num, cell.getBounds(), true, true, SWT.RIGHT);
	}
}
