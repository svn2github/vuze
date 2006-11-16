/*
 * Created on 16-Jan-2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.swt.config.generic;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;

public class GenericIntParameter
{
	private static boolean DEBUG = true;

	private GenericParameterAdapter adapter;

	private int iMinValue = 0;

	private int iMaxValue = -1;

	private int iDefaultValue;

	private String sParamName;

	private boolean bGenerateIntermediateEvents = false;

	private boolean bTriggerOnFocusOut = false;

	private Spinner spinner;

	private TimerEvent timedSaveEvent = null;

	private TimerEventPerformer timerEventSave;

	public GenericIntParameter(GenericParameterAdapter adapter,
			Composite composite, final String name) {
		iDefaultValue = adapter.getIntValue(name);
		initialize(adapter, composite, name);
	}

	/** @deprecated */
	public GenericIntParameter(GenericParameterAdapter adapter,
			Composite composite, final String name, int defaultValue) {
		iDefaultValue = defaultValue;
		initialize(adapter, composite, name);
	}

	public GenericIntParameter(GenericParameterAdapter adapter,
			Composite composite, String name, int minValue, int maxValue) {
		iDefaultValue = adapter.getIntValue(name);
		iMinValue = minValue;
		iMaxValue = maxValue;
		initialize(adapter, composite, name);
	}

	public void initialize(GenericParameterAdapter _adapter, Composite composite,
			String name) {
		adapter = _adapter;
		sParamName = name;

		timerEventSave = new TimerEventPerformer() {
			public void perform(TimerEvent event) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						if (DEBUG) {
							debug("setIntValue to " + spinner.getSelection() + " via timeEventSave");
						}
						adapter.setIntValue(sParamName, spinner.getSelection());
					}
				});
			}
		};

		int value = adapter.getIntValue(name, iDefaultValue);

		spinner = new Spinner(composite, SWT.BORDER);
		setMinimumValue(iMinValue);
		setMaximumValue(iMaxValue);
		spinner.setSelection(value);

		spinner.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (bGenerateIntermediateEvents) {
					adapter.setIntValue(sParamName, spinner.getSelection());
				} else {
					bTriggerOnFocusOut = true;
					cancelTimedSaveEvent();

					if (DEBUG) {
						debug("create timeSaveEvent");
					}
					timedSaveEvent = SimpleTimer.addEvent("IntParam Saver",
							SystemTime.getOffsetTime(750), timerEventSave);
				}
			}
		});

		spinner.addListener(SWT.FocusOut, new Listener() {
			public void handleEvent(Event event) {
				if (bTriggerOnFocusOut) {
					if (DEBUG) {
						debug("focus out setIntValue/trigger");
					}
					cancelTimedSaveEvent();
					adapter.setIntValue(sParamName, spinner.getSelection());
				}
			}
		});
	}

	private void cancelTimedSaveEvent() {
		if (timedSaveEvent != null
				&& (!timedSaveEvent.hasRun() || !timedSaveEvent.isCancelled())) {
			if (DEBUG) {
				debug("cancel timeSaveEvent");
			}
			timedSaveEvent.cancel();
		}
	}

	/**
	 * @param string
	 */
	private void debug(String string) {
		System.out.println("[GenericIntParameter:" + sParamName + "] " + string);
	}

	public void setMinimumValue(int value) {
		iMinValue = value;
		if (iMinValue > 0 && getValue() < iMinValue) {
			setValue(iMinValue);
		}
		spinner.setMinimum(value);
	}

	public void setMaximumValue(int value) {
		iMaxValue = value;
		if (iMaxValue != -1 && getValue() > iMaxValue) {
			setValue(iMaxValue);
		}
		spinner.setMaximum(iMaxValue == -1 ? Integer.MAX_VALUE : iMaxValue);
	}

	public String getName() {
		return (sParamName);
	}

	public void setValue(int value) {
		if (!spinner.isDisposed()) {
  		if (spinner.getSelection() != value) {
  			if (DEBUG) {
  				debug("spinner.setSelection(" + value + ")");
  			}
  			spinner.setSelection(value);
  		}
  		adapter.setIntValue(sParamName, spinner.getSelection());
		} else {
  		adapter.setIntValue(sParamName, value);
		}
	}

	public void setValue(int value, boolean force_adapter_set) {
		if (force_adapter_set) {
			setValue(value);
		} else if (spinner.getSelection() != value) {
			spinner.setSelection(value);
		}
	}

	public int getValue() {
		return (adapter.getIntValue(sParamName, iDefaultValue));
	}

	public void resetToDefault() {
		if (adapter.resetIntDefault(sParamName)) {
			setValue(adapter.getIntValue(sParamName));
		} else {
			setValue(getValue());
		}
	}

	public void setLayoutData(Object layoutData) {
		spinner.setLayoutData(layoutData);
	}

	public Control getControl() {
		return spinner;
	}

	public boolean isGeneratingIntermediateEvents() {
		return bGenerateIntermediateEvents;
	}

	public void setGenerateIntermediateEvents(boolean generateIntermediateEvents) {
		bGenerateIntermediateEvents = generateIntermediateEvents;
	}
}