package org.gudy.azureus2.ui.swt.components.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Cursors;

import com.aelitis.azureus.ui.swt.utils.ColorCache;

/**
 * A link label using the color specified in the skin and the style consistent with
 * a linked skin text object
 * 
 * This is intended to be used in a regular SWT container (outside of the skin instance) for when the look and feel
 * of the skin link label is desirable.
 */
public class SkinLinkLabel
{

	private Label linkLabel = null;

	private Color textLinkColor = null;

	private Color textLinkColorHover = null;

	public SkinLinkLabel(Composite parent, final String url) {
		textLinkColor = ColorCache.getColor(parent.getDisplay(), 109, 165, 255);

		textLinkColorHover = ColorCache.getColor(parent.getDisplay(), 179, 208, 255);

		linkLabel = new Label(parent, SWT.NONE);
		linkLabel.setForeground(textLinkColor);
		linkLabel.setCursor(Cursors.handCursor);

		if (null != url && url.length() > 0) {
			linkLabel.setToolTipText(url);
		}

		Listener linkListener = new Listener() {
			public void handleEvent(Event event) {
				if (event.type == SWT.MouseDown) {
					Utils.launch(url);
				} else if (event.type == SWT.MouseEnter) {
					linkLabel.setForeground(textLinkColorHover);
				} else if (event.type == SWT.MouseExit) {
					linkLabel.setForeground(textLinkColor);
				}
			}
		};

		linkLabel.addListener(SWT.MouseDown, linkListener);
		linkLabel.addListener(SWT.MouseEnter, linkListener);
		linkLabel.addListener(SWT.MouseExit, linkListener);

	}

	public Control getControl() {
		return linkLabel;
	}

	public Font getFont() {
		return linkLabel.getFont();
	}

	public void setFont(Font font) {
		linkLabel.setFont(font);
	}
}
