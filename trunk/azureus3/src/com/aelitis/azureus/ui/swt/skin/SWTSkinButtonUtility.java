package com.aelitis.azureus.ui.swt.skin;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * Simple encapsulation of SWTSkinObjectContainer that provides typical button 
 * funtionality
 *
 */
public class SWTSkinButtonUtility
{
	ArrayList listeners = new ArrayList();

	private final SWTSkinObject skinObject;

	public static class ButtonListenerAdapter
	{
		public void pressed(SWTSkinButtonUtility buttonUtility) {
		}
		
		public boolean held(SWTSkinButtonUtility buttonUtility) {
			return false;
		}

		public void disabledStateChanged(SWTSkinButtonUtility buttonUtility,
				boolean disabled) {
		}
	}

	public SWTSkinButtonUtility(SWTSkinObject skinObject) {
		this.skinObject = skinObject;
		Listener l = new Listener() {
			boolean bDownPressed;
			private TimerEvent timerEvent;

			public void handleEvent(Event event) {
				if (event.type == SWT.MouseDown) {
					if (timerEvent == null) {
						timerEvent = SimpleTimer.addEvent("MouseHold",
								SystemTime.getOffsetTime(1000), new TimerEventPerformer() {
									public void perform(TimerEvent event) {
										timerEvent = null;

										if (!bDownPressed) {
											return;
										}
										bDownPressed = false;

										boolean stillPressed = true;
										for (Iterator iter = listeners.iterator(); iter.hasNext();) {
											ButtonListenerAdapter l = (ButtonListenerAdapter) iter.next();
											stillPressed &= !l.held(SWTSkinButtonUtility.this);
										}
										bDownPressed = stillPressed;
									}
								});
					}
					bDownPressed = true;
					return;
				} else {
					if (timerEvent != null) {
						timerEvent.cancel();
						timerEvent = null;
					}
					if (!bDownPressed) {
						return;
					}
				}

				bDownPressed = false;

				if (isDisabled()) {
					return;
				}

				for (Iterator iter = listeners.iterator(); iter.hasNext();) {
					ButtonListenerAdapter l = (ButtonListenerAdapter) iter.next();
					l.pressed(SWTSkinButtonUtility.this);
				}
			}
		};
		if (skinObject instanceof SWTSkinObjectContainer) {
			Utils.addListenerAndChildren((Composite) skinObject.getControl(),
					SWT.MouseUp, l);
			Utils.addListenerAndChildren((Composite) skinObject.getControl(),
					SWT.MouseDown, l);
		} else {
			skinObject.getControl().addListener(SWT.MouseUp, l);
			skinObject.getControl().addListener(SWT.MouseDown, l);
		}
	}

	public boolean isDisabled() {
		return skinObject.getSuffix().equals("-disabled");
	}

	public void setDisabled(boolean disabled) {
		String suffix = disabled ? "-disabled" : "";
		if (skinObject.getSuffix().equals(suffix)) {
			return;
		}
		skinObject.switchSuffix(suffix, 1, true);

		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			ButtonListenerAdapter l = (ButtonListenerAdapter) iter.next();
			l.disabledStateChanged(SWTSkinButtonUtility.this, disabled);
		}
	}

	public void addSelectionListener(ButtonListenerAdapter listener) {
		if (listeners.contains(listener)) {
			return;
		}
		listeners.add(listener);
	}

	public SWTSkinObject getSkinObject() {
		return skinObject;
	}

	public void setTextID(final String id) {
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				if (skinObject instanceof SWTSkinObjectText) {
					SWTSkinObjectText skinTextObject = (SWTSkinObjectText) skinObject;
					skinTextObject.setTextID(id);
				} else if (skinObject instanceof SWTSkinObjectContainer) {
					SWTSkinObject[] children = ((SWTSkinObjectContainer) skinObject).getChildren();
					if (children.length > 0 && children[0] instanceof SWTSkinObjectText) {
						SWTSkinObjectText skinTextObject = (SWTSkinObjectText) children[0];
						skinTextObject.setTextID(id);
					}
				}
				Utils.relayout(skinObject.getControl());
			}
		});
	}

	public void setImage(final String id) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (skinObject instanceof SWTSkinObjectImage) {
					SWTSkinObjectImage skinImageObject = (SWTSkinObjectImage) skinObject;
					skinImageObject.setImageByID(id, null);
				} else if (skinObject instanceof SWTSkinObjectContainer) {
					SWTSkinObject[] children = ((SWTSkinObjectContainer) skinObject).getChildren();
					if (children.length > 0 && children[0] instanceof SWTSkinObjectImage) {
						SWTSkinObjectImage skinImageObject = (SWTSkinObjectImage) children[0];
						skinImageObject.setImageByID(id, null);
					}
				}
			}
		});
	}

	public void setTooltipID(final String id) {
		if (skinObject instanceof SWTSkinObjectImage) {
			SWTSkinObjectImage skinImageObject = (SWTSkinObjectImage) skinObject;
			skinImageObject.setTooltipByID(id);
		} else if (skinObject instanceof SWTSkinObjectContainer) {
			SWTSkinObject[] children = ((SWTSkinObjectContainer) skinObject).getChildren();
			if (children.length > 0 && children[0] instanceof SWTSkinObjectImage) {
				SWTSkinObjectImage skinImageObject = (SWTSkinObjectImage) children[0];
				skinImageObject.setTooltipByID(id);
			}
		}
	}
}
