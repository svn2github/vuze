package com.aelitis.azureus.ui.swt.views.skin;

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.UIFunctionsUserPrompter;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog.SkinnedDialogClosedListener;

public class VuzeMessageBox
	implements UIFunctionsUserPrompter, SkinnedDialogClosedListener
{

	private static final int BUTTON_PADDING = 2;

	private static final int MIN_BUTTON_WIDTH = 50;

	private String title;

	private String text;

	private String[] buttonIDs;

	private Integer[] buttonVals;

	private Button def_button;

	private int defaultButtonPos;

	private int result = -1;

	private ArrayList<UserPrompterResultListener> resultListeners = new ArrayList<UserPrompterResultListener>(1);

	private VuzeMessageBoxListener vuzeMessageBoxListener;

	private SWTSkinObjectContainer soExtra;

	private SkinnedDialog dlg;

	private String iconResource;

	private String subtitle;
	
	private java.util.List<rbInfo> listRBs = new ArrayList<rbInfo>();

	private SWTSkin skin;

	private String textIconResource;

	private boolean closed;

	private boolean opened;

	private Button[] buttons;

	private Map<Integer, Boolean> buttonsEnabled = new HashMap<Integer, Boolean>();

	public VuzeMessageBox(final String title, final String text,
			final String[] buttons, final int defaultOption) {
		this.title = title;
		this.text = text;
		this.buttonIDs = buttons == null ? new String[0] : buttons;
		this.defaultButtonPos = defaultOption;
	}
	
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
	
	private int getButtonVal(int buttonPos) {
		if (buttonVals == null) {
			return buttonPos;
		}
		if (buttonPos < 0 || buttonPos >= buttonVals.length) {
			return SWT.CANCEL;
		}
		return buttonVals[buttonPos].intValue();
	}

	
	public void setSubTitle(String s) {
		subtitle = s;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#getAutoCloseInMS()
	 */
	public int getAutoCloseInMS() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#getHtml()
	 */
	public String getHtml() {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#getRememberID()
	 */
	public String getRememberID() {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#getRememberText()
	 */
	public String getRememberText() {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#isAutoClosed()
	 */
	public boolean isAutoClosed() {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#open(com.aelitis.azureus.ui.UserPrompterResultListener)
	 */
	public void open(final UserPrompterResultListener l) {
		opened = true;
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				// catch someone calling close() while we are opening
				if (closed) {
					return;
				}
				synchronized (VuzeMessageBox.this) {
					_open(l);
				}
			}
		});
	}

	protected void _open(UserPrompterResultListener l) {
		if (l != null) {
  		synchronized (resultListeners) {
  			resultListeners.add(l);
  		}
		}
		dlg = new SkinnedDialog("skin3_dlg_generic", "shell", SWT.DIALOG_TRIM) {
			protected void setSkin(SWTSkin skin) {
				super.setSkin(skin);
				
				//skin.DEBUGLAYOUT = true;
				
				VuzeMessageBox.this.skin = skin;
				synchronized (listRBs) {
					for (rbInfo rb : listRBs) {
						addResourceBundle(rb.cla, rb.path, rb.name);
					}
					listRBs.clear();
				}

			}
			
			public void open() {
				
				super.open();
				
					// need to defer setting the default button to here as otherwise it doesn't
					// work (on windows at least...)
				
				if( def_button != null ){
					def_button.getShell().setDefaultButton(def_button);
				}
			}
		};
		
		dlg.setTitle(title);
		dlg.addCloseListener(this);

		SWTSkinObjectText soTopTitle = (SWTSkinObjectText) skin.getSkinObject("top-title");
		if (soTopTitle != null) {
			soTopTitle.setText(subtitle == null ? title : subtitle);
		}

		SWTSkinObjectText soText = (SWTSkinObjectText) skin.getSkinObject("middle-title");
		if (soText != null) {
			soText.setText(text);
		}
		
		if (iconResource != null) {
  		SWTSkinObjectImage soTopLogo = (SWTSkinObjectImage) dlg.getSkin().getSkinObject("top-logo");
  		if (soTopLogo != null) {
  			soTopLogo.setImageByID(iconResource, null);
  		}
		}
		
		if (textIconResource != null) {
  		SWTSkinObjectImage soIcon = (SWTSkinObjectImage) dlg.getSkin().getSkinObject("text-icon");
  		if (soIcon != null) {
  			soIcon.setImageByID(textIconResource, null);
  		}
		}
		
		SWTSkinObjectContainer soBottomArea = (SWTSkinObjectContainer) skin.getSkinObject("bottom-area");
		if (soBottomArea != null) {
			if (buttonIDs.length == 0) {
				soBottomArea.setVisible(false);
			} else {
				createButtons(soBottomArea);
			}
		}

		if (vuzeMessageBoxListener != null) {
			soExtra = (SWTSkinObjectContainer) skin.getSkinObject("middle-extra");
			try {
				vuzeMessageBoxListener.shellReady(dlg.getShell(), soExtra);
			} catch (Exception e) {
				Debug.out(e);
			}
		}

		if (closed) {
			return;
		}
		dlg.open();
	}

	private void createButtons(SWTSkinObjectContainer soBottomArea) {
		FormData fd;
		Composite cBottomArea = soBottomArea.getComposite();
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
			int buttonVal = buttonVals == null || i >= buttonVals.length ? i : buttonVals[i];
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
					close(((Number) event.widget.getData("ButtonNo")).intValue());
				}
			});
		}

		cBottomArea.getParent().layout(true, true);
		

	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#setAutoCloseInMS(int)
	 */
	public void setAutoCloseInMS(int autoCloseInMS) {
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#setHtml(java.lang.String)
	 */
	public void setHtml(String html) {
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#setIconResource(java.lang.String)
	 */
	public void setIconResource(String resource) {
		this.iconResource = resource;
		if (dlg != null) {
  		SWTSkinObjectImage soTopLogo = (SWTSkinObjectImage) dlg.getSkin().getSkinObject("top-logo");
  		if (soTopLogo != null) {
  			soTopLogo.setImageByID(iconResource, null);
  		}
		}
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#setRelatedObject(java.lang.Object)
	 */
	public void setRelatedObject(Object relatedObject) {
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#setRelatedObjects(java.lang.Object[])
	 */
	public void setRelatedObjects(Object[] relatedObjects) {
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#setRemember(java.lang.String, boolean, java.lang.String)
	 */
	public void setRemember(String rememberID, boolean rememberByDefault,
			String rememberText) {
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#setRememberText(java.lang.String)
	 */
	public void setRememberText(String rememberText) {
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#setUrl(java.lang.String)
	 */
	public void setUrl(String url) {
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#waitUntilClosed()
	 */
	public int waitUntilClosed() {
		if (opened) {
			final AESemaphore2 sem = new AESemaphore2("waitUntilClosed");
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					if (dlg == null) {
						sem.release();
						return;
					}
					if (!opened) {
						dlg.open();
					}
					Shell shell = dlg.getShell();
					if (shell == null || shell.isDisposed()) {
						sem.release();
						return;
					}

					shell.addDisposeListener(new DisposeListener() {
						public void widgetDisposed(DisposeEvent e) {
							sem.release();
						}
					});
				}
			});

			if (Utils.isThisThreadSWT()) {
				// on swt thread, so execSWTThread just ran and we should have a shell
				if (dlg != null) {
					Shell shell = dlg.getShell();
					if (shell != null) {
						Display d = shell.getDisplay();
						while (!shell.isDisposed()) {
							if (!d.readAndDispatch()) {
								d.sleep();
							}
						}
					}
					return getButtonVal(result);
				}
			}
			sem.reserve();
		}

		return getButtonVal(result);
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog.SkinnedDialogClosedListener#skinDialogClosed(com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog)
	 */
	public void skinDialogClosed(SkinnedDialog dialog) {
		synchronized (resultListeners) {
			int realResult = getButtonVal(result);
			for (UserPrompterResultListener l : resultListeners) {
				try {
					l.prompterClosed(realResult);
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		}
	}

	public void setListener(VuzeMessageBoxListener l) {
		this.vuzeMessageBoxListener = l;
	}

	/**
	 * @deprecated Since buttons can swap around, you should use {@link #closeWithButtonVal(int)}
	 */
	public void close(int buttonNo) {
		synchronized (VuzeMessageBox.this) {
  		this.closed = true;
  		this.result = buttonNo;
  		if (dlg != null) {
  			dlg.close();
  		}
		}
	}
	
	private int getButtonPosFromVal(int buttonVal) {
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

	public void closeWithButtonVal(int buttonVal) {
		synchronized (VuzeMessageBox.this) {
  		this.closed = true;
  		this.result = getButtonPosFromVal(buttonVal);
  		if (dlg != null) {
  			dlg.close();
  		}
		}
	}

	public void addResourceBundle(Class cla, String path, String name) {

		synchronized (listRBs) {
			if (skin == null) {	
				listRBs.add(new rbInfo(cla, path, name));
				return;
			}
		}

		String sFile = path + name;
		ClassLoader loader = cla.getClassLoader();
		ResourceBundle subBundle = ResourceBundle.getBundle(sFile,
				Locale.getDefault(), loader);
		

		SWTSkinProperties skinProperties = skin.getSkinProperties();
		skinProperties.addResourceBundle(subBundle, path, loader);
	}

	public void setTextIconResource(String resource) {
		this.textIconResource = resource;
		if (dlg != null) {
  		SWTSkinObjectImage soIcon = (SWTSkinObjectImage) dlg.getSkin().getSkinObject("text-icon");
  		if (soIcon != null) {
  			soIcon.setImageByID(textIconResource, null);
  		}
		}
	}
	
	public void addListener(UserPrompterResultListener l) {
		if (l == null) {
			return;
		}
		synchronized (resultListeners) {
			resultListeners.add(l);
		}
	}
	
	public void setDefaultButtonByPos(int pos) {
		if (dlg == null) {
			defaultButtonPos = pos;
		}
	}

	
	private static class rbInfo {
		public rbInfo(Class cla, String path, String name) {
			super();
			this.cla = cla;
			this.path = path;
			this.name = name;
		}
		Class cla;
		String path;
		String name;
	}
}
