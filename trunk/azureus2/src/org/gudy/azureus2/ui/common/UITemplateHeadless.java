/*
 * Created on 10.11.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.gudy.azureus2.ui.common;

import org.gudy.azureus2.ui.common.util.LGLogger2Log4j;
import org.gudy.azureus2.ui.common.util.LocaleUtilHeadless;

/**
 * @author tobi
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public abstract class UITemplateHeadless
	extends UITemplate
	implements org.gudy.azureus2.core3.internat.ILocaleUtilChooser, IUserInterface {

	public org.gudy.azureus2.core3.internat.LocaleUtil getProperLocaleUtil(){
	
		return new LocaleUtilHeadless();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.common.IUserInterface#init(boolean, boolean)
	 */
	public void init(boolean first, boolean others) {
		super.init(first, others);
		if (first)
			LGLogger2Log4j.set();

	}

}
