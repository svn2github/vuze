/*
 * Created on 29 juin 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.core.MessageText;

/**
 * @author René
 * 
 */
public abstract class AbstractIView implements IView {

  public void initialize(Composite composite){}
  public Composite getComposite(){ return null; }
  public void refresh(){}
  public void delete(){}

  public String getData(){ return null; }

  public String getFullTitle(){ return null; }

  public String getShortTitle() {
    return MessageText.getString(getData());
	}
  public void updateLanguage() {
    Messages.updateLanguageForControl(getComposite());
  }

}
