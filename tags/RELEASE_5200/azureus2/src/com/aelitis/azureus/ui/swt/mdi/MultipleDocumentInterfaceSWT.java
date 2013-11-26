/**
 * 
 */
package com.aelitis.azureus.ui.swt.mdi;

import org.gudy.azureus2.ui.swt.plugins.PluginUISWTSkinObject;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore;

import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;

/**
 * @author TuxPaper
 * @created Jan 29, 2010
 *
 */
public interface MultipleDocumentInterfaceSWT
	extends MultipleDocumentInterface
{
	public MdiEntry getEntryBySkinView(Object skinView);

	public UISWTViewCore getCoreViewFromID(String id);

	/**
	 * If you prefix the 'preferedAfterID' string with '~' then the operation will actually
	 * switch to 'preferedBeforeID'
	 * @param parentID
	 * @param l
	 * @param id
	 * @param closeable
	 * @param datasource
	 * @param preferredAfterID
	 * @return
	 */
	public MdiEntry createEntryFromEventListener(String parentID,
			UISWTViewEventListener l, String id, boolean closeable, Object datasource, String preferredAfterID);

	public MdiEntry createEntryFromView(String parentID, UISWTViewCore view,
			String id, Object datasource, boolean closeable, boolean show,
			boolean expand);

	public MdiEntrySWT getEntrySWT(String id);

	public MdiEntrySWT getCurrentEntrySWT();

	public MdiEntrySWT getEntryFromSkinObject(PluginUISWTSkinObject skinObject);
}
