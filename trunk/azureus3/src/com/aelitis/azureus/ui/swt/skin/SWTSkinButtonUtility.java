package com.aelitis.azureus.ui.swt.skin;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import org.gudy.azureus2.core3.util.AERunnable;
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

		public void disabledStateChanged(SWTSkinButtonUtility buttonUtility,
				boolean disabled) {
		}
	}

	public SWTSkinButtonUtility(SWTSkinObject skinObject) {
		this.skinObject = skinObject;
		Listener l = new Listener() {
			boolean bDownPressed;
			public void handleEvent(Event event) {
				if (event.type == SWT.MouseDown) {
					bDownPressed = true;
					return;
				} else if (!bDownPressed) {
					return;
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
		Utils.execSWTThread(new AERunnable() {
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
}
