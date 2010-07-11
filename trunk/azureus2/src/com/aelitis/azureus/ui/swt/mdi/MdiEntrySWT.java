/**
 * 
 */
package com.aelitis.azureus.ui.swt.mdi;

import org.gudy.azureus2.ui.swt.views.IView;

import com.aelitis.azureus.ui.mdi.MdiEntry;

/**
 * @author TuxPaper
 * @created Jan 29, 2010
 *
 */
public interface MdiEntrySWT
	extends MdiEntry
{
	//public SWTSkinObject getSkinObject();

	public IView getIView();
}
