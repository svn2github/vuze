package org.gudy.azureus2.ui.swt.components.widgets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.swt.utils.ColorCache;

/**
 * A simple pagination widget that displays a set of selectable graphic elements; each
 * one representing a page
 * 
 * To use just create the widget on a parent, set the page count, and add a <code>PageSelectionListener</code>
 * to be notified when the user clicks on a page.
 * 
 * @author khai
 *
 */
public class PaginationWidget
{
	private Composite parent = null;

	private Canvas canvas = null;

	private int hSpacing = 3;

	private int yOffset = 5;

	private Rectangle[] pages = new Rectangle[1];

	private int currentPage = 0;

	private int height = 5;

	private int width = 8;

	private FormData fd;

	private Color color_normal = null;

	private Color color_selected = null;

	private List listeners = new ArrayList();

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

		color_selected = ColorCache.getColor(canvas.getDisplay(), 204, 204, 204);
		color_normal = ColorCache.getColor(canvas.getDisplay(), 99, 99, 99);

		canvas.addPaintListener(new PaintListener() {

			public void paintControl(PaintEvent e) {
				if (pages.length > 1) {

					for (int i = 0; i < pages.length; i++) {
						if (i == currentPage) {
							e.gc.setBackground(color_selected);
						} else {
							e.gc.setBackground(color_normal);
						}
						e.gc.fillRectangle(pages[i]);
					}
				}
			}
		});

		Listener listener = new Listener() {

			public void handleEvent(Event event) {

				if (event.type == SWT.MouseDown) {
					for (int i = 0; i < pages.length; i++) {
						if (pages[i].contains(event.x, event.y)) {
							currentPage = i;
							canvas.redraw();
							notifyListeners(i);
							break;
						}
					}
				}
				if (event.type == SWT.MouseMove) {
					boolean pageFound = false;
					for (int i = 0; i < pages.length; i++) {
						if (pages[i].contains(event.x, event.y)) {
							String tooltipText = "Page " + (i + 1);
							if (false == tooltipText.equals(canvas.getToolTipText())) {
								canvas.setToolTipText(tooltipText);
							}
							canvas.setCursor(canvas.getDisplay().getSystemCursor(
									SWT.CURSOR_HAND));
							pageFound = true;
							break;
						}
					}

					if (false == pageFound) {
						canvas.setCursor(null);
						canvas.setToolTipText(null);
					}
				}
			}
		};

		canvas.addListener(SWT.MouseDown, listener);
		canvas.addListener(SWT.MouseMove, listener);

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

		if (parent.getLayoutData() instanceof FormData) {
			FormData pfd = (FormData) parent.getLayoutData();
			pfd.width = pageCount * (hSpacing + width);
		}
		fd.width = pageCount * (hSpacing + width);
		canvas.setSize(pageCount * (hSpacing + width), 16);

		Utils.relayout(parent);
	}

	public interface PageSelectionListener
	{
		public void pageSelected(int pageNumber);
	}

	public void addPageSelectionListener(PageSelectionListener listener) {
		if (false == listeners.contains(listener) && null != listener) {
			listeners.add(listener);
		}
	}

	private void notifyListeners(int selectedPage) {
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			PageSelectionListener listener = (PageSelectionListener) iterator.next();
			listener.pageSelected(selectedPage);
		}
	}

	public int getCurrentPage() {
		return currentPage;
	}

	public void setCurrentPage(int currentPage) {
		if (this.currentPage != currentPage) {
			this.currentPage = currentPage;
			canvas.redraw();
		}
	}

}
