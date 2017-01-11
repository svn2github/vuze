/*
 * File    : GenericParameter.java
 * Created : Nov 21, 2003
 * By      : epall
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
package org.gudy.azureus2.pluginsimpl.local.ui.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.config.ConfigParameterListener;
import org.gudy.azureus2.plugins.ui.config.EnablerParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.pluginsimpl.local.PluginConfigImpl;

/**
 * @author epall
 *
 */
public class 
ParameterImpl 
	implements EnablerParameter, org.gudy.azureus2.core3.config.ParameterListener
{
	protected 	PluginConfigImpl	config;
	private 	String 			key;
	private 	String 			labelKey;
	private 	String 			label;
	private		int				mode = MODE_BEGINNER;
	
	private	boolean	enabled							= true;
	private boolean	visible							= true;
	private boolean generate_intermediate_events	= true;
	
	private List<Parameter> toDisable;
	private List<Parameter> toEnable;

	private List listeners;
	private List<ParameterImplListener> impl_listeners;

	private ParameterGroupImpl	parameter_group;
	
	public 
	ParameterImpl(
		PluginConfigImpl	_config,
		String 			_key, 
		String 			_labelKey )
	{
		config	= _config;
		key		= _key;
		labelKey 	= _labelKey;
		if ("_blank".equals(labelKey)) {
			labelKey = "!!";
		}
	}
	/**
	 * @return Returns the key.
	 */
	public String getKey()
	{
		return key;
	}

	public void addDisabledOnSelection(Parameter parameter) {
		if (toDisable == null) {
			toDisable = new ArrayList<Parameter>(1);
		}
		if (parameter instanceof ParameterGroupImpl) {
			ParameterImpl[] parameters = ((ParameterGroupImpl) parameter).getParameters();
			Collections.addAll(toDisable, parameters);
			return;
		}
		toDisable.add(parameter);
	}

	public void addEnabledOnSelection(Parameter parameter) {
		if (toEnable == null) {
			toEnable = new ArrayList<Parameter>(1);
		}
		if (parameter instanceof ParameterGroupImpl) {
			ParameterImpl[] parameters = ((ParameterGroupImpl) parameter).getParameters();
			Collections.addAll(toEnable, parameters);
			return;
		}
		toEnable.add(parameter);
	}

	public List getDisabledOnSelectionParameters() {
		return toDisable == null ? Collections.EMPTY_LIST : toDisable;
	}

	public List getEnabledOnSelectionParameters() {
		return toEnable == null ? Collections.EMPTY_LIST : toEnable;
	}

	public void
	parameterChanged(
		String		key )
	{
		fireParameterChanged();
	}
	
	protected void
	fireParameterChanged()
	{
		if (listeners == null) {
			return;
		}
		// toArray() since listener trigger may remove listeners
		Object[] listenerArray = listeners.toArray();
		for (int i = 0; i < listenerArray.length; i++) {
			try {
				Object o = listenerArray[i];
				if (o instanceof ParameterListener) {

					((ParameterListener) o).parameterChanged(this);

				} else {

					((ConfigParameterListener) o).configParameterChanged(this);
				}
			} catch (Throwable f) {

				Debug.printStackTrace(f);
			}
		}
	}
	
	public void
	setEnabled(
		boolean	e )
	{
		enabled = e;

		if (impl_listeners == null) {
			return;
		}
		// toArray() since listener trigger may remove listeners
		Object[] listenersArray = impl_listeners.toArray();
		for (int i = 0; i < listenersArray.length; i++) {
			try {
				ParameterImplListener l = (ParameterImplListener) listenersArray[i];
				l.enabledChanged(this);
			} catch (Throwable f) {

				Debug.printStackTrace(f);
			}
		}
	}
	
	public boolean
	isEnabled()
	{
		return( enabled );
	}
	
	public int 
	getMinimumRequiredUserMode() 
	{
		return( mode );
	}
	
	public void 
	setMinimumRequiredUserMode(
		int 	_mode )
	{
		mode	= _mode;
	}
	
	public void
	setVisible(
		boolean	_visible )
	{
		visible	= _visible;
	}
	
	public boolean
	isVisible()
	{
		return( visible );
	}
	
	public void
	setGenerateIntermediateEvents(
		boolean		b )
	{
		generate_intermediate_events = b;
	}
	
	public boolean
	getGenerateIntermediateEvents()
	{
		return( generate_intermediate_events );
	}
	
	public void
	setGroup(
		ParameterGroupImpl	_group )
	{
		parameter_group = _group;
	}
	
	public ParameterGroupImpl
	getGroup()
	{
		return( parameter_group );
	}
	
	public void
	addListener(
		ParameterListener	l )
	{
		if (listeners == null) {
			listeners = new ArrayList(1);
		}
		listeners.add(l);
		
		if ( listeners.size() == 1 ){
			
			COConfigurationManager.addParameterListener( key, this );
		}
	}
			
	public void
	removeListener(
		ParameterListener	l )
	{
		if (listeners == null) {
			return;
		}
		listeners.remove(l);
		
		if ( listeners.size() == 0 ){
			
			COConfigurationManager.removeParameterListener( key, this );
		}
	}
	
	public void
	addImplListener(
		ParameterImplListener	l )
	{
		if (impl_listeners == null) {
			impl_listeners = new ArrayList<ParameterImplListener>(1);
		}
		impl_listeners.add(l);
	}

	public void
	removeImplListener(
		ParameterImplListener	l )
	{
		if (impl_listeners == null) {
			return;
		}
		impl_listeners.remove(l);
	}
		
	public void
	addConfigParameterListener(
		ConfigParameterListener	l )
	{
		if (listeners == null) {
			listeners = new ArrayList(1);
		}
		listeners.add(l);
		
		if ( listeners.size() == 1 ){
			
			COConfigurationManager.addParameterListener( key, this );
		}
	}
			
	public void
	removeConfigParameterListener(
		ConfigParameterListener	l )
	{
		if (listeners == null) {
			return;
		}
		listeners.remove(l);
		
		if ( listeners.size() == 0 ){
			
			COConfigurationManager.removeParameterListener( key, this );
		}
	}
	
	public String getLabelText() {
		if (label == null) {
			label = MessageText.getString(labelKey);
		}
		return label;
	}

	public void setLabelText(String sText) {
		labelKey = null;
		label = sText;

		triggerLabelChanged(sText, false);
	}

	public String getLabelKey() {
		return labelKey;
	}
	
	public void setLabelKey(String sLabelKey) {
		labelKey = sLabelKey;
		label = null;

		triggerLabelChanged(labelKey, true);
	}
	
	public String 
	getConfigKeyName() 
	{
		return( key );
	}
	
	public boolean
	hasBeenSet()
	{
		return( COConfigurationManager.doesParameterNonDefaultExist( key ));
	}
	
	private void triggerLabelChanged(String text, boolean isKey) {
		if (impl_listeners == null) {
			return;
		}
		// toArray() since listener trigger may remove listeners
		Object[] listenersArray = impl_listeners.toArray();
		for (int i = 0; i < listenersArray.length; i++) {
			try {
				ParameterImplListener l = (ParameterImplListener) listenersArray[i];
				l.labelChanged(this, text, isKey);

			} catch (Throwable f) {

				Debug.printStackTrace(f);
			}
		}
	}
	
	public void
	destroy()
	{
		listeners = null;
		impl_listeners = null;
		toDisable = null;
		toEnable = null;

		COConfigurationManager.removeParameterListener( key, this );
	}
}
