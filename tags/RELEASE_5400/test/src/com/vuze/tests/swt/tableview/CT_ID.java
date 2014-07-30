package com.vuze.tests.swt.tableview;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.ui.common.table.TableCellCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.util.MapUtils;

public class CT_ID
	extends CoreTableColumn
	implements TableCellAddedListener, TableCellSWTPaintListener, TableCellRefreshListener, TableCellToolTipListener
{
	public static double id = 0;
	public static String name = new Object() { }.getClass().getEnclosingClass().getSimpleName();
	
	public CT_ID() {
		super(name, 170, "test");
		addDataSourceType(TableViewTestDS.class);
		setRefreshInterval(TableColumn.INTERVAL_INVALID_ONLY);
		setVisible(true);
	}

	public void cellAdded(TableCell cell) {
		TableRowCore row = (cell.getTableRow() instanceof TableRowCore) ? (TableRowCore)cell.getTableRow() : null;
//		if (row != null) {
//			row.setHeight((int) (16 + (Math.random() * 100)));
//		}
		String indent = row == null || row.getParentRowCore() == null ? "" : "  ";
		TableViewTestDS ds = (TableViewTestDS) cell.getDataSource();
		Object mapObject = MapUtils.getMapObject(ds.map, "ID", null, Number.class);
		if (mapObject instanceof Number) {
			double overideID = ((Double) mapObject).doubleValue();
			cell.setSortValue(overideID);
			cell.setText(indent + overideID + (row == null ? "" :  ":" + row.getHeight()));
		} else {
  		id++;
  		cell.setSortValue(id);
  		cell.setText(indent + Double.toString(id) + (row == null ? "" : ":" + row.getHeight()));
  		ds.map.put("ID", id);
		}
	}
	
	public void refresh(TableCell cell) {
		int id = ((Number) cell.getSortValue()).intValue();
		if (id % 10 == 1) {
			cell.setForeground(200, 0, 0);
			cell.getTableRow().setForeground(150, 0, 0);
		}
	}

	public void cellPaint(GC gc, TableCellSWT cell) {
		TableViewTestDS ds = (TableViewTestDS) cell.getDataSource();
		
		int num = MapUtils.getMapInt(ds.map, name + ".numCP", 0) + 1;
		ds.map.put(name + ".numCP", num);
		GCStringPrinter.printString(gc, Integer.toString(num), cell.getBounds(), true, true, SWT.RIGHT);
	}

	public void cellHover(TableCell cell) {
		TableRow row = cell.getTableRow();
		if (row instanceof TableRowCore) {
  		TableRowCore rowCore = (TableRowCore)cell.getTableRow();
  		TableCellCore cellCore = (TableCellCore) cell;
  		cell.setToolTip(rowCore.getIndex() + ". r.vis? " + rowCore.isVisible());
		}
	}

	public void cellHoverComplete(TableCell cell) {
	}
}
