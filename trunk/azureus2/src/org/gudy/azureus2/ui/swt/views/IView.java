/*
 * Created on 29 juin 2003
 *
 */
package org.gudy.azureus2.ui.swt.views;

import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.ui.swt.IconBarEnabler;

/**
 * @author Olivier
 * 
 */
public interface IView extends IconBarEnabler {
  /**
   * This method is called when the view is intanciated, it should initialize all GUI
   * components. Must NOT be blocking, or it'll freeze the whole GUI.
   * Caller is the GUI Thread.
   * @param composite the parent composite. Each view should create a child composite, and then use this child composite to add all elements to.
   */
  public void initialize(Composite composite);
  
  /**
   * This method is called after initialize so that the Tab is set its control
   * Caller is the GUI Thread.
   * @return the Composite that should be set as the control for the Tab item
   */
  public Composite getComposite();
  
  /**
   * This method is called on each refresh.
   * The view should not instanciate a Thread to refresh itself, unless this is for async purposes. In which case, don't forget to call the display.asyncexec method.
   * Called by the GUI Thread
   */
  public void refresh();
  
  /**
   * This method is caled when the view is destroyed.
   * Each color instanciated, images and such things should be disposed.
   * The caller is the GUI thread.
   *
   */
  public void delete();
  
  /**
   * Data 'could' store a key to a language file, in order to support multi-language titles
   * @return a String which is the key of this view title.
   */
  public String getData();
  
  public String getShortTitle();
  
  /**
   * Called in order to set / update the title of this View
   * @return the full title for the view
   */
  public String getFullTitle();
  
  /**
   * Called when the language needs updating
   *
   */
  public void updateLanguage();
  
  
  public void setTabListener();
}
