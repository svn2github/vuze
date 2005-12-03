/**
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.views;

import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.logging.impl.FileLogging;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;

/**
 * @author TuxPaper
 *
 * @since 2.3.0.5
 */
public class LoggerView extends AbstractIView implements ILogEventListener {

	private static final int COLOR_INFO = 0;

	private static final int COLOR_WARN = 1;

	private static final int COLOR_ERR = 2;

	private static Color[] colors = null;

	private static final int PREFERRED_LINES = 256;

	private static final int MAX_LINES = 1024 + PREFERRED_LINES;

	private static final SimpleDateFormat dateFormatter;

	private static final FieldPosition formatPos;

	private Display display;

	private Composite panel;

	private StyledText consoleText;

	private Object[] filter = null;

	private boolean bSupressScrolling = false;

	// LinkedList is better for removing entries when full
	private LinkedList buffer = new LinkedList();

	private boolean bPaused = false;

	private boolean bEnabled = false;

	// List of components we don't log.  
	// Array represents LogTypes (info, warning, error)
	private ArrayList[] ignoredComponents = new ArrayList[3];

	static {
		dateFormatter = new SimpleDateFormat("[h:mm:ss.SSS] ");
		formatPos = new FieldPosition(0);
	}

	public LoggerView() {
		for (int i = 0; i < ignoredComponents.length; i++) {
			ignoredComponents[i] = new ArrayList();
		}
	}

	public LoggerView(java.util.List initialList) {
		this();
		if (initialList != null)
			buffer.addAll(initialList);
		setEnabled(true);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
	 */
	public void initialize(Composite composite) {
		display = composite.getDisplay();

		if (colors == null) {
			colors = new Color[3];
			colors[COLOR_INFO] = Colors.blues[Colors.BLUES_MIDLIGHT];
			colors[COLOR_WARN] = Colors.colorWarning;
			colors[COLOR_ERR] = Colors.red_ConsoleView;
		}

		panel = new Composite(composite, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		panel.setLayout(layout);
		GridData gd = new GridData(SWT.DEFAULT, SWT.DEFAULT, true, true);
		gd.heightHint = 70;
		panel.setLayoutData(gd);

		consoleText = new StyledText(panel, SWT.READ_ONLY | SWT.V_SCROLL
				| SWT.H_SCROLL);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		consoleText.setLayoutData(gd);

		// XXX This doesn't work well, but it's better than nothing
		consoleText.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
				GC gc = new GC(consoleText);
				int charWidth = gc.getFontMetrics().getAverageCharWidth();
				gc.dispose();
				
				int areaWidth = consoleText.getBounds().width;
				consoleText.setTabs(areaWidth / 6 / charWidth);
			}
		});
		
		Composite cLeft = new Composite(panel, SWT.NULL);
		cLeft.setLayout(new GridLayout());
		gd = new GridData();
		cLeft.setLayoutData(gd);

		Button buttonPause = new Button(cLeft, SWT.CHECK);
		Messages.setLanguageText(buttonPause, "LoggerView.pause");
		gd = new GridData();
		buttonPause.setLayoutData(gd);
		buttonPause.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (e.widget == null || !(e.widget instanceof Button))
					return;
				Button btn = (Button) e.widget;
				bPaused = btn.getSelection();
				if (!bPaused && buffer != null) {
					refresh();
				}
			}
		});

		Button buttonClear = new Button(cLeft, SWT.PUSH);
		Messages.setLanguageText(buttonClear, "LoggerView.clear");
		gd = new GridData();
		buttonClear.setLayoutData(gd);
		buttonClear.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				consoleText.setText("");
			}
		});

		/** FileLogging filter, consisting of a List of types (info, warning, error)
		 * and a checkbox Table of component IDs.
		 */
		final String sFilterPrefix = "ConfigView.section.logging.filter";
		Composite gLogIDs = new Composite(panel, SWT.NULL);
		//Messages.setLanguageText(gLogIDs, sFilterPrefix);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.numColumns = 2;
		gLogIDs.setLayout(layout);
		gd = new GridData();
		gd.verticalSpan = 2;
		gLogIDs.setLayoutData(gd);

		final List listLogTypes = new List(gLogIDs, SWT.BORDER | SWT.SINGLE
				| SWT.V_SCROLL);
		gd = new GridData(SWT.NULL, SWT.BEGINNING, false, false);
		listLogTypes.setLayoutData(gd);

		final int[] logTypes = { LogEvent.LT_INFORMATION, LogEvent.LT_WARNING,
				LogEvent.LT_ERROR };
		for (int i = 0; i < logTypes.length; i++)
			listLogTypes.add(MessageText.getString("ConfigView.section.logging.log"
					+ i + "type"));
		listLogTypes.select(0);

		final LogIDs[] logIDs = FileLogging.configurableLOGIDs;
		//Arrays.sort(logIDs);

		final Composite cChecks = new Composite(gLogIDs, SWT.NULL);
		RowLayout rowLayout = new RowLayout(SWT.VERTICAL);
		rowLayout.wrap = true;
		rowLayout.marginLeft = 0;
		rowLayout.marginRight = 0;
		rowLayout.marginTop = 0;
		rowLayout.marginBottom = 0;
		cChecks.setLayout(rowLayout);
		gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd.heightHint = 65;
		cChecks.setLayoutData(gd);

		SelectionAdapter buttonClickListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int index = listLogTypes.getSelectionIndex();
				if (index < 0 || index >= logTypes.length)
					return;
				Button item = (Button) e.widget;
				if (item.getSelection())
					ignoredComponents[index].remove(item.getData("LOGID"));
				else
					ignoredComponents[index].add(item.getData("LOGID"));
			}
		};
		for (int i = 0; i < logIDs.length; i++) {
			Button btn = new Button(cChecks, SWT.CHECK);
			btn.setText(MessageText.getString(sFilterPrefix + "." + logIDs[i],
					logIDs[i].toString()));

			btn.setData("LOGID", logIDs[i]);
			btn.setSelection(true);

			btn.addSelectionListener(buttonClickListener);
		}

		// Update table when list selection changes
		listLogTypes.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int index = listLogTypes.getSelectionIndex();
				if (index < 0 || index >= logTypes.length)
					return;
				Control[] items = cChecks.getChildren();
				for (int i = 0; i < items.length; i++) {
					if (items[i] instanceof Button) {
						boolean checked = !ignoredComponents[index].contains(items[i]
								.getData("LOGID"));
						((Button) items[i]).setSelection(checked);
					}
				}
			}
		});

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.IView#getComposite()
	 */
	public Composite getComposite() {
		return panel;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.IView#refresh()
	 */
	public void refresh() {
		if (bPaused)
			return;

		synchronized (buffer) {
			if (consoleText == null || consoleText.isDisposed())
				return;
			ScrollBar sb = consoleText.getVerticalBar();
			boolean autoScroll = !bSupressScrolling
					&& (sb.getSelection() == (sb.getMaximum() - sb.getThumb()));

			for (int i = 0; i < buffer.size(); i++) {
				LogEvent event = (LogEvent) buffer.get(i);

				int nbLinesBefore = consoleText.getLineCount();
				if (nbLinesBefore > MAX_LINES)
					consoleText.replaceTextRange(0, consoleText
							.getOffsetAtLine(PREFERRED_LINES), "");

				final StringBuffer buf = new StringBuffer();
				dateFormatter.format(event.timeStamp, buf, formatPos);
				buf.append("{").append(event.logID).append("} ");

				buf.append(event.text);
				if (filter == null && event.relatedTo != null) {
					buf.append("; \t| ");
					for (int j = 0; j < event.relatedTo.length; j++) {
						Object obj = event.relatedTo[j];
						if (j > 0)
							buf.append("; ");
						if (obj instanceof LogRelation) {
							buf.append(((LogRelation) obj).getRelationText());
						} else {
							buf.append(obj.getClass().getName() + ": '"
									+ obj.toString() + "'");
						}
					}
				}
				buf.append('\n');

				consoleText.append(buf.toString());

				int nbLinesNow = consoleText.getLineCount();
				int colorIdx = -1;
				if (event.entryType == LogEvent.LT_INFORMATION)
					colorIdx = COLOR_INFO;
				else if (event.entryType == LogEvent.LT_WARNING)
					colorIdx = COLOR_WARN;
				else if (event.entryType == LogEvent.LT_ERROR)
					colorIdx = COLOR_ERR;

				if (colors != null && colorIdx >= 0)
					consoleText.setLineBackground(nbLinesBefore - 1, nbLinesNow
							- nbLinesBefore, colors[colorIdx]);

			}
			buffer.clear();
			if (autoScroll)
				consoleText.setSelection(consoleText.getText().length());
		}
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.IView#delete()
	 */
	public void delete() {
		MainWindow.getWindow().setConsole(null);
		Logger.removeListener(this);
		if (consoleText != null && !consoleText.isDisposed())
			consoleText.dispose();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
	 */
	public String getFullTitle() {
		return MessageText.getString("ConsoleView.title.full");
	}

	public String getData() {
		return "ConsoleView.title.short";
	}

	public synchronized void log(final LogEvent event) {
		if (display == null || display.isDisposed())
			return;

		if (ignoredComponents[logTypeToIndex(event.entryType)]
				.contains(event.logID))
			return;

		// Always display STDERR messages, as they may relate to the filter 
		boolean bMatch = (event.logID == LogIDs.STDERR || filter == null);

		if (!bMatch && event.relatedTo != null) {
			for (int i = 0; !bMatch && i < event.relatedTo.length; i++) {
				Object obj = event.relatedTo[i];

				if (obj == null)
					continue;

				for (int j = 0; !bMatch && j < filter.length; j++) {
					if (obj instanceof LogRelation) {
						//System.err.println(obj.getClass().getSimpleName() + " is Logrelation");

						Object newObj = ((LogRelation) obj).queryForClass(filter[j]
								.getClass());
						if (newObj != null)
							obj = newObj;
					}

					//System.err.println(obj.getClass().getName() + " matches " + filter[j].getClass().getSimpleName() + "?");

					if (obj == filter[j])
						bMatch = true;
				} // for filter
			} // for relatedTo
		}

		if (bMatch)
			synchronized (buffer) {
				if (buffer.size() >= 200)
					buffer.remove(0);
				buffer.add(event);
			}
	}

	public void setFilter(Object[] _filter) {
		filter = _filter;
		clearConsole();
	}

	private void clearConsole() {
		if (display == null || display.isDisposed())
			return;

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				consoleText.setText("");
			}
		});
	}

	public void setEnabled(boolean on) {
		if (bEnabled == on)
			return;
		bEnabled = on;
		if (on)
			Logger.addListener(this);
		else
			Logger.removeListener(this);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginView#getPluginViewName()
	 */
	public String getPluginViewName() {
		return "Console";
	}

	// TODO: Support multiple selection
	public void dataSourceChanged(Object newDataSource) {
		boolean bEnable = newDataSource != null;
		if (bEnable) {
			if (newDataSource instanceof Object[])
				setFilter((Object[]) newDataSource);
			else
				setFilter(new Object[] { newDataSource });
		}

		setEnabled(bEnable);
	}

	private int logTypeToIndex(int entryType) {
		switch (entryType) {
			case LogEvent.LT_INFORMATION:
				return 0;
			case LogEvent.LT_WARNING:
				return 1;
			case LogEvent.LT_ERROR:
				return 2;
		}
		return 0;
	}

	private int indexToLogType(int index) {
		switch (index) {
			case 0:
				return LogEvent.LT_INFORMATION;
			case 1:
				return LogEvent.LT_WARNING;
			case 2:
				return LogEvent.LT_ERROR;
		}
		return LogEvent.LT_INFORMATION;
	}
}
