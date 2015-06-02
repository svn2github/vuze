package com.vuze.tests.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

public class PaintCircleText
{
	public static void main(String[] args) {
		Display display = new Display();
		Shell shellMain = new Shell(display, SWT.SHELL_TRIM);
		shellMain.setLayout(new FillLayout());

		
		shellMain.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				String text = "round and around goes the text";
				Transform transform = new Transform(e.display);
				System.out.println(e.width);
				transform.translate(e.width / 2, e.height / 2);
				transform.rotate(-90);
				int r = 0;
				int ofs = e.gc.textExtent("" + text.charAt(0)).x;
				for (int i = 0; i < text.length(); i++) {
					String c = "" + text.charAt(i);
					e.gc.setTransform(transform);
					
					float pct = 1.0f - ((float) i) / ((text.length() - 1) / 2.0f);
					
					e.gc.drawText(c, (i >= text.length() / 2) ? 0 : (int) (-ofs * pct), e.height / -2, true);
					e.gc.drawText(c, 0, 0, true);
					
					transform.rotate(180.0f / (text.length() - 1));
					r += 10;
				}
				
				transform.dispose();
			}
		});
		
		
		shellMain.setSize(200, 200);
		shellMain.open();

		while (!shellMain.isDisposed()) {
			try {
				if (!display.readAndDispatch())
					display.sleep();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		display.dispose();
	}
}
