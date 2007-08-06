/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.ui.swt.shells.uiswitcher;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;

/**
 * @author TuxPaper
 * @created Feb 5, 2007
 *
 */
public class UISwitcherWindow
{
	private static String CFG_PREFIX = "window.uiswitcher.";

	private static String RESOURCE_LOC = "com/aelitis/azureus/ui/swt/shells/uiswitcher/images/";

	private static String[] IMAGES = {
		"NewUI_130.png",
		"AdvancedUI_130.png",
		"ClassicUI_130.png"
	};

	private static String[] IDS = {
		"NewUI",
		"AdvancedUI",
		"ClassicUI"
	};

	private Shell shell;

	private Button btnOk;

	private int ui = -1;

	private List disposeList = new ArrayList();

	public UISwitcherWindow() {
		// XXX forcing to allowCancel is temporary
		this(null, true);
	}

	/**
	 * 
	 */
	public UISwitcherWindow(Shell parentShell, final boolean allowCancel) {
		try {
			final Image[] images = new Image[IMAGES.length];
			final Button[] buttons = new Button[IMAGES.length];
			GridData gd;

			int style = SWT.BORDER | SWT.TITLE | SWT.RESIZE;
			if (allowCancel) {
				style |= SWT.CLOSE;
			}
			shell = ShellFactory.createShell(parentShell, style);
			shell.setText(MessageText.getString(CFG_PREFIX + "title"));
			Utils.setShellIcon(shell);

			shell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					Utils.disposeSWTObjects(disposeList);
				}
			});

			Display display = shell.getDisplay();

			ClassLoader cl = UISwitcherWindow.class.getClassLoader();

			GridLayout layout = new GridLayout();
			layout.horizontalSpacing = 0;
			layout.marginWidth = 5;
			layout.marginHeight = 0;
			layout.verticalSpacing = 1;
			shell.setLayout(layout);

			Label title = new Label(shell, SWT.WRAP);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.verticalIndent = 3;
			title.setLayoutData(gd);

			Messages.setLanguageText(title, CFG_PREFIX + "text");

			Listener radioListener = new Listener() {
				public void handleEvent(Event event) {
					int idx;
					if (event.widget instanceof Composite) {
						Long l = (Long) event.widget.getData("INDEX");
						idx = l.intValue();
					} else {
						Composite c = ((Control) event.widget).getParent();
						Long l = (Long) c.getData("INDEX");
						idx = l.intValue();
					}
					for (int i = 0; i < buttons.length; i++) {
						boolean selected = idx == i;
						Composite c = buttons[i].getParent();
						c.getParent().setBackground(
								selected ? c.getDisplay().getSystemColor(
										SWT.COLOR_LIST_SELECTION) : null);
						Color fg = selected ? c.getDisplay().getSystemColor(
								SWT.COLOR_LIST_SELECTION_TEXT) : null;
						Control[] children = c.getChildren();
						for (int j = 0; j < children.length; j++) {
							Control control = children[j];
							control.setForeground(fg);

						}
						buttons[i].setSelection(selected);
					}
				}
			};

			FontData[] fontData = shell.getFont().getFontData();
			fontData[0].setHeight((int) (fontData[0].getHeight() * 1.5));
			fontData[0].setStyle(SWT.BOLD);
			final Font headerFont = new Font(shell.getDisplay(), fontData);
			disposeList.add(headerFont);

			for (int i = 0; i < IMAGES.length; i++) {
				String id = IMAGES[i];

				final Composite c = new Composite(shell, SWT.NONE);
				c.setBackgroundMode(SWT.INHERIT_DEFAULT);
				gd = new GridData(GridData.FILL_BOTH);
				gd.verticalIndent = 0;
				c.setLayoutData(gd);
				GridLayout gridLayout = new GridLayout(2, false);
				gridLayout.horizontalSpacing = 0;
				gridLayout.marginWidth = 5;
				gridLayout.marginHeight = 3;
				gridLayout.verticalSpacing = 0;
				c.setLayout(gridLayout);
				c.setData("INDEX", new Long(i));

				c.addListener(SWT.MouseDown, radioListener);

				Label label = new Label(c, SWT.CENTER);
				label.addListener(SWT.MouseDown, radioListener);

				try {
					InputStream is = cl.getResourceAsStream(RESOURCE_LOC + id);
					if (is != null) {
						images[i] = new Image(display, is);
						label.setImage(images[i]);
						disposeList.add(images[i]);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				Composite c2 = new Composite(c, SWT.NONE);
				c2.setData("INDEX", new Long(i));
				c2.setLayout(new GridLayout());
				c2.setLayoutData(new GridData(GridData.FILL_BOTH));

				buttons[i] = new Button(c2, SWT.RADIO);
				buttons[i].setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
				Messages.setLanguageText(buttons[i], CFG_PREFIX + IDS[i] + ".title");
				buttons[i].setData("INDEX", new Long(i));
				buttons[i].addListener(SWT.Selection, radioListener);
				buttons[i].setFont(headerFont);

				buttons[i].addTraverseListener(new TraverseListener() {

					public void keyTraversed(TraverseEvent e) {
						if (e.detail == SWT.TRAVERSE_ARROW_NEXT) {
							e.doit = true;
							e.detail = SWT.TRAVERSE_TAB_NEXT;
						} else if (e.detail == SWT.TRAVERSE_ARROW_PREVIOUS) {
							e.detail = SWT.TRAVERSE_TAB_PREVIOUS;
							e.doit = true;
						} else if (e.detail == SWT.TRAVERSE_TAB_NEXT
								|| e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
							btnOk.setFocus();
							e.doit = false;
						} else if (e.detail == SWT.TRAVERSE_RETURN) {
							e.doit = true;
						} else if (e.detail == SWT.TRAVERSE_ESCAPE) {
							e.doit = false;
							if (allowCancel) {
								ui = -1;
								shell.dispose();
							}
						} else {
							e.doit = false;
						}
					}

				});

				buttons[i].addListener(SWT.KeyDown, new Listener() {
					// @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
					public void handleEvent(Event event) {
						if (event.keyCode == SWT.ARROW_UP) {
							shell.getDisplay().getFocusControl().traverse(
									SWT.TRAVERSE_ARROW_PREVIOUS);
						} else if (event.keyCode == SWT.ARROW_DOWN) {
							shell.getDisplay().getFocusControl().traverse(
									SWT.TRAVERSE_ARROW_NEXT);
						}
					}
				});

				Label info = new Label(c2, SWT.WRAP);
				gd = new GridData(GridData.FILL_BOTH);
				gd.horizontalIndent = 20;
				info.setLayoutData(gd);

				Messages.setLanguageText(info, CFG_PREFIX + IDS[i] + ".text");
				info.addListener(SWT.MouseDown, radioListener);
			}

			Event eventSelectFirst = new Event();
			eventSelectFirst.widget = buttons[0];
			radioListener.handleEvent(eventSelectFirst);
			
			Composite cBottom = new Composite(shell, SWT.NONE);
			layout = new GridLayout(2, false);
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			cBottom.setLayout(layout);
			cBottom.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			
			Label lblBottom = new Label(cBottom, SWT.WRAP);
			Messages.setLanguageText(lblBottom, "window.uiswitcher.bottom.text");
			gd = Utils.getWrappableLabelGridData(1,
					GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
			gd.horizontalIndent = 10;
			lblBottom.setLayoutData(gd);
			

			btnOk = new Button(cBottom, SWT.PUSH);
			Messages.setLanguageText(btnOk, "Button.ok");
			shell.setDefaultButton(btnOk);
			btnOk.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					for (int i = 0; i < buttons.length; i++) {
						if (buttons[i].getSelection()) {
							ui = i;
							break;
						}
					}
					shell.dispose();
				}
			});
			gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
			gd.widthHint = 80;
			btnOk.setLayoutData(gd);

			Point point = shell.computeSize(630, SWT.DEFAULT);
			shell.setSize(point);
			
			Utils.centreWindow(shell);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int open() {
		shell.open();

		while (!shell.isDisposed()) {
			if (!shell.getDisplay().readAndDispatch()) {
				shell.getDisplay().sleep();
			}
		}
		return ui;
	}

	public static void main(String[] args) {
		Display display = Display.getDefault();
		UISwitcherWindow window = new UISwitcherWindow(null, false);
		System.out.println(window.open());
	}
}
