/*
 * Created on 29 juin 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.widgets.Composite;

/**
 * @author René
 * 
 */
public abstract class AbstractIView implements IView {

  public void initialize(Composite composite){}
  public Composite getComposite(){ return null; }
  public void refresh(){}
  public void delete(){}
  public String getShortTitle(){ return null; }
  public String getFullTitle(){ return null; }

  public void updateLanguage() {
    Messages.updateLanguageForControl(getComposite());
  }

}
