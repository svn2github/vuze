/*
 * File    : ConfigPanel*.java
 * Created : 11 mar. 2004
 * By      : TuxPaper
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

package org.gudy.azureus2.ui.swt.views.configsections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
import org.gudy.azureus2.ui.swt.views.ConfigView;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.core3.internat.LocaleUtil;
import org.gudy.azureus2.core3.internat.LocaleUtilDecoder;
import org.gudy.azureus2.core3.ipfilter.IpFilter;
import org.gudy.azureus2.core3.ipfilter.IpRange;
import org.gudy.azureus2.core3.logging.LGLogger;

public class ConfigSectionIPFilter implements ConfigSectionSWT {
  IpFilter filter;
  Table table;
  boolean noChange;
  
  public ConfigSectionIPFilter() {
  }

  public String configSectionGetParentSection() {
    return ConfigSection.SECTION_ROOT;
  }

	public String configSectionGetName() {
		return "ipfilter";
	}

  public void configSectionSave() {
    try{
      if (filter != null)
      	filter.save();
    }catch( Exception e ){
    	LGLogger.logAlert("Save of filter file fails", e);
    }
  }

  public void configSectionDelete() {
    
  }

  public Composite configSectionCreate(final Composite parent) {
    GridData gridData;
    Label label;

    filter = IpFilter.getInstance();

    Composite gFilter = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    gFilter.setLayout(layout);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gFilter.setLayoutData(gridData);

    // start controls
    BooleanParameter enabled = new BooleanParameter(gFilter, "Ip Filter Enabled",true); //$NON-NLS-1$
    Messages.setLanguageText(enabled.getControl(), "ConfigView.section.ipfilter.enable"); //$NON-NLS-1$


    BooleanParameter deny = new BooleanParameter(gFilter, "Ip Filter Allow",false); //$NON-NLS-1$
    Messages.setLanguageText(deny.getControl(), "ConfigView.section.ipfilter.allow");

    table = new Table(gFilter, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
    String[] headers = { "ConfigView.section.ipfilter.description", "ConfigView.section.ipfilter.start", "ConfigView.section.ipfilter.end" };
    int[] sizes = { 200, 110, 110 };
    int[] aligns = { SWT.LEFT, SWT.CENTER, SWT.CENTER };
    for (int i = 0; i < headers.length; i++) {
      TableColumn tc = new TableColumn(table, aligns[i]);
      tc.setText(headers[i]);
      tc.setWidth(sizes[i]);
      Messages.setLanguageText(tc, headers[i]); //$NON-NLS-1$
    }

    table.setHeaderVisible(true);

    gridData = new GridData(GridData.FILL_BOTH);
    table.setLayoutData(gridData);

    Composite cArea = new Composite(gFilter, SWT.NULL);
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.numColumns = 3;
    cArea.setLayout(layout);
    cArea.setLayoutData(new GridData());

    Button add = new Button(cArea, SWT.PUSH);
    gridData = new GridData(GridData.CENTER);
    gridData.widthHint = 100;
    add.setLayoutData(gridData);
    Messages.setLanguageText(add, "ConfigView.section.ipfilter.add");
    add.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        addRange();
      }
    });

    Button remove = new Button(cArea, SWT.PUSH);
    gridData = new GridData(GridData.CENTER);
    gridData.widthHint = 100;
    remove.setLayoutData(gridData);
    Messages.setLanguageText(remove, "ConfigView.section.ipfilter.remove");
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

    Button edit = new Button(cArea, SWT.PUSH);
    gridData = new GridData(GridData.CENTER);
    gridData.widthHint = 100;
    edit.setLayoutData(gridData);
    Messages.setLanguageText(edit, "ConfigView.section.ipfilter.edit");
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

    Control[] controls = new Control[3];
    controls[0] = add;
    controls[1] = remove;
    controls[2] = edit;
    IAdditionalActionPerformer enabler = new ChangeSelectionActionPerformer(controls);
    enabled.setAdditionalActionPerformer(enabler);

    populateTable();
    
		table.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
        resizeTable();
			}
		});

		gFilter.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
        resizeTable();
			}
		});
    
    return gFilter;
  }
  
  private void resizeTable() {
	  int iNewWidth = table.getClientArea().width - 
                    table.getColumn(1).getWidth() - 
                    table.getColumn(2).getWidth() - 20;
    if (iNewWidth > 50)
      table.getColumn(0).setWidth(iNewWidth);
  }

  private void populateTable() {
   IpRange[] IpRanges = filter.getRanges();
    Display display = table.getDisplay();
    if(display == null || display.isDisposed()) {
      return;
    }
      
     for (int i=0;i<IpRanges.length;i++){
        final IpRange range = IpRanges[i];
        display.asyncExec(new Runnable() {
          public void run() {
            if(table == null || table.isDisposed())
              return;
            TableItem item = new TableItem(table, SWT.NULL);
            item.setImage(0, ImageRepository.getImage("ipfilter"));
            item.setText(0, range.getDescription());
            item.setText(1, range.getStartIp());
            item.setText(2, range.getEndIp());
            item.setData(range);
          }
        });
      }
  }

  public void removeRange(IpRange range) {
  	filter.removeRange( range );
    noChange = false;
    refresh();
  }

  public void editRange(IpRange range) {
    new IpFilterEditor(table.getDisplay(), table, range);
    noChange = false;
    refresh();
  }

  public void addRange() {
    new IpFilterEditor(table.getDisplay(), table, null);
    noChange = false;
    refresh();
  }

  public void refresh() {
    if (table == null || table.isDisposed() || noChange)
      return;
    noChange = true;
    TableItem[] items = table.getItems();
    for (int i = 0; i < items.length; i++) {
      IpRange range = (IpRange) items[i].getData();
      if (items[i] == null || items[i].isDisposed())
        continue;
      String tmp = items[i].getText(0);
      if (range.getDescription() != null && !range.getDescription().equals(tmp))
        items[i].setText(0, range.getDescription());

      tmp = items[i].getText(1);
      if (range.getStartIp() != null && !range.getStartIp().equals(tmp))
        items[i].setText(1, range.getStartIp());

      tmp = items[i].getText(2);
      if (range.getEndIp() != null && !range.getEndIp().equals(tmp))
        items[i].setText(2, range.getEndIp());

    }
  }
}
