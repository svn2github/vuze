/*
 * Created on 29 juin 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * @author Olivier
 * 
 */
public class TestView implements IView {

   String title;
   Table table;
   
   public TestView(String title)
   {
     this.title = title;         
   }
   
   public void initialize(Composite composite)
   {
    table = new Table(composite,SWT.MULTI | SWT.FULL_SELECTION);
    table.setHeaderVisible(true);
    for(int j = 0 ; j < 5 ; j++)
        {    
         TableColumn col =  new TableColumn(table,SWT.NULL);
         col.setWidth(50);
        }

        for(int i = 0 ; i < 10 ; i ++)
        {    
          TableItem item = new TableItem(table,SWT.NULL);
          for(int j = 0 ; j < 5 ; j++)
            item.setText(j,i + ":" +j);
        }
   }
   
  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#delete()
   */
  public void delete() {
    // TODO Auto-generated method stub
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getComposite()
   */
  public Composite getComposite() {
    // TODO Auto-generated method stub
    return table;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
   */
  public String getFullTitle() {
    // TODO Auto-generated method stub
    return title;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getShortTitle()
   */
  public String getShortTitle() {
    // TODO Auto-generated method stub
    return title;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#refresh()
   */
  public void refresh() {
    // TODO Auto-generated method stub

  }

}
