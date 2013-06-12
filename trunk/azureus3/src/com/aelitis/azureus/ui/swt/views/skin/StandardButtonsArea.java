package com.aelitis.azureus.ui.swt.views.skin;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.Utils;

public abstract class StandardButtonsArea
{
	private Button def_button;

	private Button[] buttons;

	private Map<Integer, Boolean> buttonsEnabled = new HashMap<Integer, Boolean>();

	private static final int BUTTON_PADDING = 2;

	private static final int MIN_BUTTON_WIDTH = 50;

	private String[] buttonIDs;

	private Integer[] buttonVals;

	private int defaultButtonPos;

	public void setButtonIDs(String[] buttons) {
		this.buttonIDs = buttons == null ? new String[0] : buttons;
	}

	public void setButtonVals(Integer[] buttonVals) {
		this.buttonVals = buttonVals;
		int cancelPos = -1;
		for (int i = 0; i < buttonVals.length; i++) {
			Integer val = buttonVals[i];
			if (val == SWT.CANCEL) {
				cancelPos = i;
				break;
			}
		}
		if (cancelPos >= 0) {
			if (Constants.isOSX && cancelPos != 0) {
				String cancelButton = buttonIDs[cancelPos];

				for (int i = cancelPos; i > 0; i--) {
					if (defaultButtonPos == i) {
						defaultButtonPos = i - 1;
					}
					this.buttonIDs[i] = this.buttonIDs[i - 1];
					this.buttonVals[i] = this.buttonVals[i - 1];
				}
				if (defaultButtonPos == 0) {
					defaultButtonPos = 1;
				}
				buttonIDs[0] = cancelButton;
				buttonVals[0] = SWT.CANCEL;
			} // else if (cancelPos != buttons.length - 1) { // TODO: move to end
		}
	}

	public void setDefaultButtonPos(int defaultOption) {
		this.defaultButtonPos = defaultOption;
	}

	public int getButtonVal(int buttonPos) {
		if (buttonVals == null) {
			return buttonPos;
		}
		if (buttonPos < 0 || buttonPos >= buttonVals.length) {
			return SWT.CANCEL;
		}
		return buttonVals[buttonPos].intValue();
	}

	public int getButtonCount() {
		return buttonIDs.length;
	}

	public int getButtonPosFromVal(int buttonVal) {
		int pos = buttonVal;
		if (buttonVals != null) {
			for (int i = 0; i < buttonVals.length; i++) {
				int val = buttonVals[i];
				if (buttonVal == val) {
					pos = i;
					break;
				}
			}
		}
		return pos;
	}

	public void swt_createButtons(Composite cBottomArea) {
		FormData fd;
		Composite cCenterH = new Composite(cBottomArea, SWT.NONE);
		fd = new FormData();
		fd.height = 1;
		fd.width = 1;
		fd.left = new FormAttachment(0);
		fd.right = new FormAttachment(100);
		cCenterH.setLayoutData(fd);

		Composite cCenterV = new Composite(cBottomArea, SWT.NONE);
		fd = new FormData();
		fd.width = 1;
		fd.height = 1;
		fd.top = new FormAttachment(0);
		fd.bottom = new FormAttachment(100);
		cCenterV.setLayoutData(fd);

		Composite cButtonArea = new Composite(cBottomArea, SWT.NONE);
		// Fix button BG not right on Win7
		cButtonArea.setBackgroundMode(SWT.INHERIT_FORCE);
		fd = new FormData();
		fd.top = new FormAttachment(cCenterV, 0, SWT.CENTER);
		fd.left = new FormAttachment(cCenterH, 0, SWT.CENTER);
		cButtonArea.setLayoutData(fd);

		RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
		rowLayout.center = true;
		rowLayout.spacing = 8;
		rowLayout.pack = false;
		cButtonArea.setLayout(rowLayout);

		buttons = new Button[buttonIDs.length];
		for (int i = 0; i < buttonIDs.length; i++) {
			String buttonText = buttonIDs[i];
			if (buttonText == null) {
				continue;
			}
			Button button = buttons[i] = new Button(cButtonArea, SWT.PUSH);
			int buttonVal = buttonVals == null || i >= buttonVals.length ? i
					: buttonVals[i];
			Boolean b = buttonsEnabled.get(buttonVal);
			if (b == null) {
				b = Boolean.TRUE;
			}
			button.setEnabled(b);
			button.setText(buttonText);

			RowData rowData = new RowData();
			Point size = button.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			size.x += BUTTON_PADDING;
			if (size.x < MIN_BUTTON_WIDTH) {
				size.x = MIN_BUTTON_WIDTH;
			}
			rowData.width = size.x;
			button.setLayoutData(rowData);

			if (defaultButtonPos == i) {
				def_button = button;
			}
			button.setData("ButtonNo", new Integer(i));
			button.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					clicked(((Number) event.widget.getData("ButtonNo")).intValue());
				}
			});
		}

		cBottomArea.getParent().layout(true, true);

		cBottomArea.getShell().addShellListener(new ShellListener() {

			public void shellIconified(ShellEvent e) {
			}

			public void shellDeiconified(ShellEvent e) {
			}

			public void shellDeactivated(ShellEvent e) {
			}

			public void shellClosed(ShellEvent e) {
			}

			public void shellActivated(ShellEvent e) {
				// need to defer setting the default button to here as otherwise it doesn't
				// work (on windows at least...)

				if (def_button != null) {
					def_button.getShell().setDefaultButton(def_button);
				}
			}
		});
	}

	protected abstract void clicked(int intValue);

	public void setButtonEnabled(final int buttonVal, final boolean enable) {
		buttonsEnabled.put(buttonVal, enable);
		if (buttons == null) {
			return;
		}
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (buttons == null) {
					return;
				}
				int pos = getButtonPosFromVal(buttonVal);
				if (pos >= 0 && pos < buttons.length) {
					Button button = buttons[pos];
					if (button != null && !button.isDisposed()) {
						button.setEnabled(enable);
					}
				}
			}
		});
	}

	public Button[] getButtons() {
		return buttons;
	}
}
