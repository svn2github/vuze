/**
 * 
 */
package com.aelitis.azureus.ui.swt.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * @author TuxPaper
 * @created Jun 8, 2006
 *
 */
public class SWTSkinImageChanger implements Listener
{
	private final String suffix;

	private final int eventOn;

	private final int eventOff;

	private Control lastControl;

	/**
	 * Default Constructor
	 * 
	 * @param suffix
	 * @param eventOff 
	 * @param eventOn 
	 */
	public SWTSkinImageChanger(String suffix, int eventOn, int eventOff) {
		this.suffix = suffix;
		this.eventOn = eventOn;
		this.eventOff = eventOff;
	}

	public void handleEvent(Event event) {
		Control control = (Control) event.widget;

		try {
			if (event.type == SWT.MouseExit && lastControl != null) {
				if (control.getParent() == lastControl) {
					return;
				}
			}

			SWTSkinObject skinObject = (SWTSkinObject) control.getData("SkinObject");
			if (skinObject != null) {
				String sSuffix = (event.type == eventOn) ? suffix : "";
				//System.out.println(System.currentTimeMillis() + ": " + skinObject + "--" + suffix);

				skinObject.switchSuffix(sSuffix, 2, true);
			}
		} finally {
			lastControl = control;
		}
	}
}
