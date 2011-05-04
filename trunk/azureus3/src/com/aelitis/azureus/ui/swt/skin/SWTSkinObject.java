/**
 * 
 */
package com.aelitis.azureus.ui.swt.skin;

import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.ui.swt.debug.ObfusticateImage;
import org.gudy.azureus2.ui.swt.plugins.PluginUISWTSkinObject;

import com.aelitis.azureus.ui.swt.views.skin.SkinView;

/**
 * @author TuxPaper
 * @created Jun 12, 2006
 *
 */
public interface SWTSkinObject
	extends PluginUISWTSkinObject
{
	/**
	 * Retrieve the associated SWT Control used by the skin object
	 * 
	 * @return SWT Control
	 */
	public Control getControl();

	/**
	 * Retrieve the type of widget.
	 * @return
	 * 
	 * TODO Move widget types to SWTSkinObject
	 */
	public String getType();

	/**
	 * Retrieve the Skin Object ID that represents this object.  Typically the
	 * same as {@link #getConfigID()}, however, may be different if a config
	 * id is used to make independant copies
	 * 
	 * @return An unique Skin Object ID
	 */
	public String getSkinObjectID();

	/**
	 * Retrieve the Config ID which is ID in the skin config file.
	 * 
	 * @return Config ID
	 */
	public String getConfigID();

	public SWTSkinObject getParent();

	public SWTSkin getSkin();

	public void setVisible(boolean visible);

	public boolean isVisible();

	public void setDefaultVisibility();

	/**
	 * @param sConfigID
	 * @param sSuffix
	 */
	void setBackground(String sConfigID, String sSuffix);

	/**
	 * @param suffix
	 * @param level 
	 * @param walkUp TODO
	 * @return TODO
	 */
	String switchSuffix(String suffix, int level, boolean walkUp);
	
	String switchSuffix(String suffix, int level, boolean walkUp, boolean walkDown);

	/**
	 * Convenience method for switching suffix using defaults
	 * @param suffix
	 * @return
	 */
	String switchSuffix(String suffix);

	/**
	 * @return
	 */
	String getSuffix();

	/**
	 * @param properties
	 */
	void setProperties(SWTSkinProperties skinProperties);

	/**
	 * @return
	 */
	SWTSkinProperties getProperties();

	//void getTemplate(String property)

	public void addListener(SWTSkinObjectListener listener);

	public void removeListener(SWTSkinObjectListener listener);

	/**
	 * @return
	 */
	public SWTSkinObjectListener[] getListeners();

	public String getViewID();

	/**
	 * @param eventType
	 */
	void triggerListeners(int eventType);

	/**
	 * @param eventType
	 * @param params
	 */
	void triggerListeners(int eventType, Object params);

	/**
	 * 
	 *
	 * @since 3.0.1.3
	 */
	public void dispose();

	/**
	 * @param id
	 *
	 * @since 3.0.4.3
	 */
	void setTooltipID(String id);

	/**
	 * @return
	 *
	 * @since 3.0.5.3
	 */
	boolean getDefaultVisibility();
	
	public Object getData(String id);
	
	public void setData(String id, Object data);

	/**
	 * @return
	 *
	 * @since 3.1.1.1
	 */
	boolean isDisposed();

	/**
	 * @return
	 *
	 * @since 3.1.1.1
	 */
	boolean isDebug();

	/**
	 * @param walkup
	 * @return
	 *
	 * @since 3.1.1.1
	 */
	String getTooltipID(boolean walkup);

	/**
	 * @param b
	 *
	 * @since 3.1.1.1
	 */
	public void setDebug(boolean b);
	
	public void relayout();

	public void layoutComplete();

	public void setObfusticatedImageGenerator(ObfusticateImage obfusticatedImageGenerator);

	public SkinView getSkinView();

	public void setSkinView(SkinView sv);
}
