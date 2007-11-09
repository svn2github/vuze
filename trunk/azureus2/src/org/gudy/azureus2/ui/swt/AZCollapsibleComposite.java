package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.Cursors;

public class AZCollapsibleComposite
	extends Composite
{
	private Label actionLabel = null;

	private Composite content = null;

	private int contentHeight = 50;

	private boolean isCollapsed = false;

	private static final int[] onPoints = {
		0,
		2,
		8,
		2,
		4,
		6
	};

	private static final int[] offPoints = {
		2,
		-1,
		2,
		8,
		6,
		4
	};

	public AZCollapsibleComposite(Composite parent) {
		super(parent, SWT.NONE);
		createControls();
	}

	private void createControls() {
		GridLayout gLayout = new GridLayout();
		gLayout.verticalSpacing = 3;
		setLayout(gLayout);

		actionLabel = new Label(this, SWT.READ_ONLY);
		GridData gData = new GridData(SWT.BEGINNING, SWT.BOTTOM, false, false);
		gData.horizontalIndent = 10;

		actionLabel.setLayoutData(gData);
		actionLabel.setText("Details");
		actionLabel.setCursor(Cursors.handCursor);

		content = new Composite(this, SWT.NONE);
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		content.setLayout(new FillLayout());

		/*
		 * Toggle the collapsed state of the content Composite 
		 */
		actionLabel.addMouseListener(new MouseAdapter() {

			public void mouseDown(MouseEvent e) {

				if (true == isCollapsed) {
					Point p = computeSize(SWT.DEFAULT, SWT.DEFAULT);
					p.y = contentHeight;
					setSize(p);
					
				} else {
					Point p = computeSize(SWT.DEFAULT, SWT.DEFAULT);
					contentHeight = p.y;
					p.y = content.getBounds().y;
					setSize(p);
					
				}

				isCollapsed = !isCollapsed;

				layout(true, true);
			}
		});
		
		
		addPaintListener(new PaintListener() {
		
			public void paintControl(PaintEvent e) {
				if(true == isCollapsed){
					drawArrow(true, e.gc);
				}
				else{
					drawArrow(false, e.gc);
				}
				
		
			}
		
		});

	}

	
	
	private void drawArrow(boolean up, GC gc) {
		
		gc.setForeground(Colors.red);
		if (true == up) {
			gc.fillPolygon(translate(onPoints, 5,5));
		} else {
			gc.fillPolygon(translate(offPoints, 5,5));
		}
		gc.dispose();
		redraw();
	}

	private int[] translate(int[] data, int x, int y) {
		int[] target = new int[data.length];
		for (int i = 0; i < data.length; i += 2) {
			target[i] = data[i] + x;
		}
		for (int i = 1; i < data.length; i += 2) {
			target[i] = data[i] + y;
		}
		return target;
	}
	
	
	/**
	 * @return the content
	 */
	public Composite getContent() {
		return content;
	}

}
