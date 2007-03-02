/**
 * 
 */
package com.aelitis.azureus.ui.swt.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;

/**
 * @author TuxPaper
 * @created Jun 8, 2006
 *
 * XXX NOT USED XXX
 */
public class SWTTextPaintListener
	implements PaintListener
{
	private int align;

	private Color bgcolor;

	private Color fgcolor;

	private Font font;

	private String text;

	private SWTSkinProperties skinProperties;

	/**
	 * 
	 */
	public SWTTextPaintListener(SWTSkin skin, Control createOn, String sConfigID) {
		skinProperties = skin.getSkinProperties();

		bgcolor = skinProperties.getColor(sConfigID + ".color");
		text = skinProperties.getStringValue(sConfigID + ".text");
		fgcolor = skinProperties.getColor(sConfigID + ".text.color");
		align = SWT.NONE;

		String sAlign = skinProperties.getStringValue(sConfigID + ".align");
		if (sAlign != null) {
			align = SWTSkinUtils.getAlignment(sAlign, SWT.NONE);
		}

		String sSize = skinProperties.getStringValue(sConfigID + ".text.size");

		if (sSize != null) {
			FontData[] fd = createOn.getFont().getFontData();

			try {
				char firstChar = sSize.charAt(0);
				if (firstChar == '+' || firstChar == '-') {
					sSize = sSize.substring(1);
				}

				int iSize = Integer.parseInt(sSize);

				if (firstChar == '+') {
					fd[0].height += iSize;
				} else if (firstChar == '-') {
					fd[0].height -= iSize;
				} else {
					fd[0].height = iSize;
				}

				font = new Font(createOn.getDisplay(), fd);
				createOn.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent e) {
						font.dispose();
					}
				});
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
	}

	public void paintControl(PaintEvent e) {
		e.gc.setClipping(e.x, e.y, e.width, e.height);

		if (bgcolor != null) {
			e.gc.setBackground(bgcolor);
		}
		if (fgcolor != null) {
			e.gc.setForeground(fgcolor);
		}

		if (font != null) {
			e.gc.setFont(font);
		}

		if (text != null) {
			Rectangle clientArea = ((Composite) e.widget).getClientArea();
			GCStringPrinter.printString(e.gc, text, clientArea, true, true, align);
		}
	}
}
