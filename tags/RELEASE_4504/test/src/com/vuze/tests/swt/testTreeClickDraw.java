package com.vuze.tests.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

public class testTreeClickDraw
{

	public static void main(String[] args) {
		Display display = new Display();
		Shell shellMain = new Shell(display, SWT.SHELL_TRIM);
		FillLayout fillLayout = new FillLayout();
		fillLayout.marginHeight = 10;
		shellMain.setLayout(fillLayout);
		
		Composite c = new Composite(shellMain, SWT.NONE);
		c.setLayout(new FillLayout());
		
		final Tree tree = new Tree(c, SWT.NONE);
		
		TreeItem treeItem = new TreeItem(tree, SWT.NONE);
		treeItem.setText("New Item 1");
		treeItem = new TreeItem(tree, SWT.NONE);
		treeItem.setText("New Item 2");
		treeItem = new TreeItem(treeItem, SWT.NONE);
		treeItem.setText("New Item 2a");
		treeItem = new TreeItem(tree, SWT.NONE);
		treeItem.setText("New Item 3");
		treeItem = new TreeItem(tree, SWT.NONE);
		treeItem.setText("New Item 4");
		treeItem = new TreeItem(tree, SWT.NONE);
		treeItem.setText("New Item 5");
		
		tree.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				Point ptDisplayCursor = e.display.getCursorLocation();
				Point pt = tree.toControl(ptDisplayCursor);
				e.gc.setAntialias(SWT.OFF);
				e.gc.drawLine(pt.x - 2, pt.y - 2, pt.x + 2, pt.y + 2);
				e.gc.drawLine(pt.x - 2, pt.y + 2, pt.x + 2, pt.y - 2);
				
				TreeItem item = tree.getItem(pt);
				if (item != null) {
					Rectangle bounds = item.getBounds();
					bounds.height--;
					bounds.width--;
					e.gc.drawRectangle(bounds);
				}
			}
		});
		
		tree.addListener(SWT.MouseMove, new Listener() {
			public void handleEvent(Event event) {
				tree.redraw();
			}
		});

		shellMain.open();

		while (!shellMain.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
}
