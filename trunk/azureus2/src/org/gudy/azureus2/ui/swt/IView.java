/*
 * Created on 29 juin 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.widgets.Composite;

/**
 * @author Olivier
 * 
 */
public interface IView {
  public void initialize(Composite composite);
  public Composite getComposite();
  public void refresh();
  public void delete();
  public String getShortTitle();
  public String getFullTitle();
  public void updateLanguage();  
}
