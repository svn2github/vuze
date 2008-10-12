package org.gudy.azureus2.plugins.ui.tables;

import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.ui.swt.Utils;


/**
 * 
 * Provides a simple way to get a TableCell refreshed more often than the normal GUI refresh cycle
 * It always clocks at 100ms
 * as well as time synchronization methods for cells showing animated icons
 * @author olivier
 *
 */
public class TableCellRefresher {
	
	private static TableCellRefresher instance = null;
	
	private  AEThread2 refresher;
	
	private  List	cells;
	
	private  long iterationNumber;
	
	private TableCellRefresher() {
		cells = new ArrayList();
		
		refresher = new AEThread2("Cell Refresher",true) {
			public void run() {
				try {
					
					iterationNumber = 0;
					
					while(true) {
						
						List cellsCopy;
						
						synchronized (cells) {
							 cellsCopy = new ArrayList(cells.size());
							 cellsCopy.addAll(cells);
							 cells.clear();
						}
						
						for(int i = 0 ; i < cellsCopy.size()  ;i++) {
							final ColumnCell cc = (ColumnCell) cellsCopy.get(i);
							try {
								//cc.cell.invalidate();
								if(cc.column instanceof TableCellRefreshListener) {
									Utils.execSWTThread( new Runnable() {
										public void run() {
											((TableCellRefreshListener)cc.column).refresh(cc.cell);
										}
									});
									
								}
							} catch (Throwable t) {
								t.printStackTrace();
							}
							
							
						}

						Thread.sleep(100);
						
						iterationNumber++;
					}
				}catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		
		refresher.start();
	}
	
	
	private class ColumnCell {
		TableColumn column;
		TableCell cell;
	}
	
	private void _addColumnCell(TableColumn column,TableCell cell) {
		ColumnCell cc = new ColumnCell();
		cc.column = column;
		cc.cell = cell;
		synchronized (cells) {
			cells.add(cc);
		}
	}
	
	private int _getRefreshIndex(int refreshEvery100ms, int nbIndices) {
		if(refreshEvery100ms <= 0) return 1;
		if(nbIndices <= 0) return 1;
		
		return (int) ( (iterationNumber / refreshEvery100ms) % nbIndices);
	}
	
	private static synchronized TableCellRefresher getInstance() {
		if(instance == null) {
			instance = new TableCellRefresher();
		}
		return instance;
	}
	
	//Add a cell to be refreshed within the next iteration
	//The cell will only get refreshed once
	public static void addCell(TableColumn column,TableCell cell) {
		
		getInstance()._addColumnCell(column,cell);
	}
	
	
	
	public static int getRefreshIndex(int refreshEvery100ms, int nbIndices) {
		return getInstance()._getRefreshIndex(refreshEvery100ms, nbIndices);
	}

}
