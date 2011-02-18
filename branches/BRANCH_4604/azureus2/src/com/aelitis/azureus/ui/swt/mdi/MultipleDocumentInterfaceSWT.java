/**
 * 
 */
package com.aelitis.azureus.ui.swt.mdi;

import org.gudy.azureus2.ui.swt.plugins.PluginUISWTSkinObject;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.views.IView;

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

	public IView getIViewFromID(String id);

	public MdiEntry createEntryFromEventListener(String parentID,
			UISWTViewEventListener l, String id, boolean closeable, Object datasource);

	public MdiEntry createEntryFromIView(String parentID, IView iview, String id,
			Object datasource, boolean closeable, boolean show, boolean expand);

	public MdiEntrySWT getEntrySWT(String id);

	public MdiEntrySWT getCurrentEntrySWT();

	public MdiEntrySWT getEntryFromSkinObject(PluginUISWTSkinObject skinObject);
}
