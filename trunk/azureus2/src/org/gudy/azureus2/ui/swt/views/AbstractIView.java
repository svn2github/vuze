/*
 * Created on 29 juin 2003
 *
 */
package org.gudy.azureus2.ui.swt.views;

import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Messages;

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

  public String getFullTitle(){
    return MessageText.getString(getData());
  }

  public String getShortTitle() {
    String shortTitle = getFullTitle();
    if(shortTitle != null && shortTitle.length() > 20) {
      shortTitle = shortTitle.substring(0,20) + "...";
    }
    return shortTitle;
	}
  
  public void updateLanguage() {
    Messages.updateLanguageForControl(getComposite());
  }

}
