package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.swt.utils.ColorCache;

public class PaginationWidget
{
	private Composite parent = null;

	private Canvas canvas = null;

	private int hSpacing = 5;

	private int yOffset = 4;

	private Rectangle[] pages = new Rectangle[1];

	private int height = 8;

	private int width = 6;

	private FormData fd;

	public PaginationWidget(Composite parent) {
		if (null == parent || true == parent.isDisposed()) {
			throw new IllegalArgumentException("parent can not be null or disposed");
		}

		this.parent = parent;

		init();
	}

	private void init() {
		canvas = new Canvas(parent, SWT.NONE);

		parent.setLayout(new FormLayout());
		fd = new FormData();
		fd.top = new FormAttachment(0, 0);
		fd.bottom = new FormAttachment(100, 0);
		fd.left = new FormAttachment(0, 0);
		fd.right = new FormAttachment(100, 0);
		canvas.setLayoutData(fd);

		canvas.addPaintListener(new PaintListener() {

			public void paintControl(PaintEvent e) {
				if (pages.length > 1) {
					e.gc.setBackground(ColorCache.getColor(canvas.getDisplay(), 150, 150,
							150));
					for (int i = 0; i < pages.length; i++) {
						e.gc.fillRectangle(pages[i]);
					}
				}
			}
		});
	}

	public void setPageCount(int pageCount) {
		pages = new Rectangle[pageCount];

		/*
		 * First page
		 */
		pages[0] = new Rectangle(0, yOffset, width, height);

		/*
		 * Remaining pages
		 */
		int xOffset = width + hSpacing;
		for (int i = 1; i < pages.length; i++) {
			pages[i] = new Rectangle(xOffset, yOffset, width, height);
			xOffset += width + hSpacing;
		}
		fd.width = pageCount * (hSpacing + width);
		canvas.setSize(pageCount * (hSpacing + width), 16);

		Utils.relayout(canvas);
	}
}
