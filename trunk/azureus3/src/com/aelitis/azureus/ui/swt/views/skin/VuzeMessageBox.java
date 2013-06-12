package com.aelitis.azureus.ui.swt.views.skin;

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.UIFunctionsUserPrompter;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog.SkinnedDialogClosedListener;

public class VuzeMessageBox
	implements UIFunctionsUserPrompter, SkinnedDialogClosedListener
{

	private String title;

	private String text;

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

	private StandardButtonsArea buttonsArea;

	public VuzeMessageBox(final String title, final String text,
			final String[] buttons, final int defaultOption) {
		this.title = title;
		this.text = text;
		buttonsArea = new StandardButtonsArea() {
			protected void clicked(int intValue) {
				close(intValue);
			}
		};
		buttonsArea.setButtonIDs(buttons);
		buttonsArea.setDefaultButtonPos(defaultOption);
	}
	
	public void setButtonEnabled(final int buttonVal, final boolean enable) {
		buttonsArea.setButtonEnabled(buttonVal, enable);
	}
	
	public void setButtonVals(Integer[] buttonVals) {
		buttonsArea.setButtonVals(buttonVals);
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
		
		if (iconResource == null && textIconResource == null && soTopTitle != null && soText != null) {
			soTopTitle.setStyle(soText.getStyle() & ~(SWT.RIGHT | SWT.CENTER));
		}
		
		SWTSkinObjectContainer soBottomArea = (SWTSkinObjectContainer) skin.getSkinObject("bottom-area");
		if (soBottomArea != null) {
			if (buttonsArea.getButtonCount() == 0) {
				soBottomArea.setVisible(false);
			} else {
				buttonsArea.swt_createButtons(soBottomArea.getComposite());
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
	
	public Button[] getButtons() {
		return buttonsArea.getButtons();
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
					return buttonsArea.getButtonVal(result);
				}
			}
			sem.reserve();
		}

		return buttonsArea.getButtonVal(result);
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog.SkinnedDialogClosedListener#skinDialogClosed(com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog)
	 */
	public void skinDialogClosed(SkinnedDialog dialog) {
		synchronized (resultListeners) {
			int realResult = buttonsArea.getButtonVal(result);
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
	
	public void closeWithButtonVal(int buttonVal) {
		synchronized (VuzeMessageBox.this) {
  		this.closed = true;
  		this.result = buttonsArea.getButtonPosFromVal(buttonVal);
  		if (dlg != null) {
  			dlg.close();
  		}
		}
	}

	public void addResourceBundle(Class<?> cla, String path, String name) {

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
			buttonsArea.setDefaultButtonPos(pos);
		}
	}

	
	private static class rbInfo {
		public rbInfo(Class<?> cla, String path, String name) {
			super();
			this.cla = cla;
			this.path = path;
			this.name = name;
		}
		Class<?> cla;
		String path;
		String name;
	}
}
