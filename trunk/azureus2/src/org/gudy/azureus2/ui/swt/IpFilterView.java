/*
 * File    : IpFilterView.java
 * Created : 8 oct. 2003 12:23:19
 * By      : Olivier 
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.ui.swt;

import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core.IpFilter;
import org.gudy.azureus2.core.IpRange;
import org.gudy.azureus2.core.MessageText;

/**
 * @author Olivier
 * 
 */
public class IpFilterView extends AbstractIView {

  IpFilter filter;
  Composite panel;
  Table table;

  public IpFilterView() {
    filter = IpFilter.getInstance();
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.AbstractIView#initialize(org.eclipse.swt.widgets.Composite)
   */
  public void initialize(Composite composite) {
    panel = new Composite(composite, SWT.NULL);
    GridData gridData = new GridData(GridData.FILL_BOTH);
    panel.setLayoutData(gridData);

    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    panel.setLayout(layout);

    table = new Table(panel, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
    String[] headers = { "ipFilter.description", "ipFilter.start", "ipFilter.end" };
    int[] sizes = { 300, 120, 120 };
    int[] aligns = { SWT.LEFT, SWT.CENTER, SWT.CENTER };
    for (int i = 0; i < headers.length; i++) {
      TableColumn tc = new TableColumn(table, aligns[i]);
      tc.setText(headers[i]);
      tc.setWidth(sizes[i]);
      Messages.setLanguageText(tc, headers[i]); //$NON-NLS-1$
    }

    table.setHeaderVisible(true);

    gridData = new GridData(GridData.FILL_BOTH);
    gridData.verticalSpan = 4;
    table.setLayoutData(gridData);

    Button add = new Button(panel, SWT.PUSH);
    gridData = new GridData(GridData.CENTER);
    gridData.widthHint = 100;
    add.setLayoutData(gridData);
    Messages.setLanguageText(add, "ipFilter.add");
    add.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        addRange();
      }
    });

    Button remove = new Button(panel, SWT.PUSH);
    gridData = new GridData(GridData.CENTER);
    gridData.widthHint = 100;
    remove.setLayoutData(gridData);
    Messages.setLanguageText(remove, "ipFilter.remove");
    remove.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        TableItem[] selection = table.getSelection();
        if (selection.length == 0)
          return;
        removeRange((IpRange) selection[0].getData());
        table.remove(table.indexOf(selection[0]));
        selection[0].dispose();
      }
    });

    Button edit = new Button(panel, SWT.PUSH);
    gridData = new GridData(GridData.CENTER);
    gridData.widthHint = 100;
    edit.setLayoutData(gridData);
    Messages.setLanguageText(edit, "ipFilter.edit");
    edit.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        TableItem[] selection = table.getSelection();
        if (selection.length == 0)
          return;
        editRange((IpRange) selection[0].getData());
      }
    });

    table.addMouseListener(new MouseAdapter() {
      public void mouseDoubleClick(MouseEvent arg0) {
        TableItem[] selection = table.getSelection();
        if (selection.length == 0)
          return;
        editRange((IpRange) selection[0].getData());
      }
    });

    Button save = new Button(panel, SWT.PUSH);
    gridData = new GridData(GridData.GRAB_VERTICAL | GridData.VERTICAL_ALIGN_END);
    gridData.widthHint = 100;
    save.setLayoutData(gridData);
    Messages.setLanguageText(save, "ipFilter.save");
    save.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        filter.save();
      }
    });

    populateTable();

  }

  private void populateTable() {
    List ipRanges = filter.getIpRanges();
    synchronized (ipRanges) {
      Iterator iter = ipRanges.iterator();
      while (iter.hasNext()) {
        IpRange range = (IpRange) iter.next();
        TableItem item = new TableItem(table, SWT.NULL);
        item.setImage(0, ImageRepository.getImage("ipfilter"));
        item.setText(0, range.description);
        item.setText(1, range.startIp);
        item.setText(2, range.endIp);
        item.setData(range);
      }
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.AbstractIView#getComposite()
   */
  public Composite getComposite() {
    return panel;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.AbstractIView#getShortTitle()
   */
  public String getShortTitle() {
    return MessageText.getString("ipFilter.shortTitle");
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.AbstractIView#getFullTitle()
   */
  public String getFullTitle() {
    return MessageText.getString("ipFilter.fullTitle");
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.AbstractIView#refresh()
   */
  public void refresh() {
    if (table == null || table.isDisposed())
      return;
    TableItem[] items = table.getItems();
    for (int i = 0; i < items.length; i++) {
      IpRange range = (IpRange) items[i].getData();
      if (items[i] == null || items[i].isDisposed())
        continue;
      String tmp = items[i].getText(0);
      if (range.description != null && !range.description.equals(tmp))
        items[i].setText(0, range.description);

      tmp = items[i].getText(1);
      if (range.description != null && !range.startIp.equals(tmp))
        items[i].setText(1, range.startIp);

      tmp = items[i].getText(2);
      if (range.description != null && !range.endIp.equals(tmp))
        items[i].setText(2, range.endIp);

    }
  }

  /* (non-Javadoc)
     * @see org.gudy.azureus2.ui.swt.IView#delete()
     */
  public void delete() {
    MainWindow.getWindow().setIpFilter(null);
    Utils.disposeComposite(panel);
  }

  public void removeRange(IpRange range) {
    List ranges = filter.getIpRanges();
    synchronized (ranges) {
      ranges.remove(range);
    }
  }

  public void editRange(IpRange range) {
    new IpFilterEditor(panel.getDisplay(), table, filter.getIpRanges(), range);
  }

  public void addRange() {
    new IpFilterEditor(panel.getDisplay(), table, filter.getIpRanges(), null);
  }

}
