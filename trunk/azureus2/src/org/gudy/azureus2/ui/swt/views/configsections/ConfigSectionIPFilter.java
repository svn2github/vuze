/*
 * File    : ConfigPanel*.java
 * Created : 11 mar. 2004
 * By      : TuxPaper
 * 
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
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
 *
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.ui.swt.views.configsections;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;

import com.aelitis.azureus.core.*;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.ipfilter.IpFilter;
import org.gudy.azureus2.core3.ipfilter.IpRange;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.DisplayFormatters;

public class ConfigSectionIPFilter implements UISWTConfigSection {
  AzureusCore	azureus_core;
  
  IpFilter filter;
  Table table;
  boolean noChange;
  
  FilterComparator comparator;
  
  class FilterComparator implements Comparator {
    
    boolean ascending = true;
    
    static final int FIELD_NAME = 0;
    static final int FIELD_START_IP = 1;
    static final int FIELD_END_IP = 2;
    
    int field = FIELD_START_IP;
   
    
    public int compare(Object arg0,Object arg1) {
      IpRange range0 = (IpRange) arg0;
      IpRange range1 = (IpRange) arg1;
      if(field == FIELD_NAME) {
        return (ascending ? 1 : -1) * ( range0.compareDescription( range1 ));
      }
      if(field == FIELD_START_IP) {
        return (ascending ? 1 : -1) * ( range0.compareStartIpTo( range1 ));
      }
      if(field == FIELD_END_IP) {
        return (ascending ? 1 : -1) * ( range0.compareEndIpTo( range1 ));
      }
      return 0;
    }
    
    public void setField(int newField) {      
      if(field == newField) ascending = ! ascending;
      field = newField;
    }
    
    
  }
  
  IpRange 	ipRanges[];
  Label		percentage_blocked;
  
  public
  ConfigSectionIPFilter(
  	AzureusCore		_azureus_core )
  {
  	azureus_core	= _azureus_core;
  	comparator = new FilterComparator();
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
    	LGLogger.logUnrepeatableAlert("Save of filter file fails", e);
    }
  }

  public void configSectionDelete() {
    
  }

  public Composite configSectionCreate(final Composite parent) {
    GridData gridData;

    filter = azureus_core.getIpFilterManager().getIPFilter();

    Composite gFilter = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    gFilter.setLayout(layout);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gFilter.setLayoutData(gridData);
    
    
    percentage_blocked  = new Label(gFilter, SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
	gridData.horizontalSpan = 3;

    percentage_blocked.setLayoutData(gridData);

    setPercentageBlocked();
    
    // start controls

    	// row: enable filter + allow/deny
    
	gridData = new GridData();
	gridData.horizontalSpan = 1;

    BooleanParameter enabled = new BooleanParameter(gFilter, "Ip Filter Enabled",true);
	enabled.setLayoutData( gridData ); 
    Messages.setLanguageText(enabled.getControl(), "ConfigView.section.ipfilter.enable");

	gridData = new GridData();
	gridData.horizontalSpan = 2;

    BooleanParameter deny = new BooleanParameter(gFilter, "Ip Filter Allow",false);
	deny.setLayoutData( gridData ); 
    Messages.setLanguageText(deny.getControl(), "ConfigView.section.ipfilter.allow");
  
    deny.addChangeListener(
    	new ParameterChangeListener()
		{
    		public void
    		parameterChanged(
    			Parameter	p,
    			boolean		caused_internally )
			{
    			setPercentageBlocked();
			}
		});
    
    	// row persist banning
    
	gridData = new GridData();
	gridData.horizontalSpan = 3;
	
    BooleanParameter persist_bad_data_banning = new BooleanParameter(gFilter, "Ip Filter Banning Persistent",true);
    persist_bad_data_banning.setLayoutData( gridData );
    Messages.setLanguageText(persist_bad_data_banning.getControl(), "ConfigView.section.ipfilter.persistblocking");

    	// row block bad + group ban
    
	gridData = new GridData();
	gridData.horizontalSpan = 1;
	
    BooleanParameter enable_bad_data_banning = new BooleanParameter(gFilter, "Ip Filter Enable Banning",true);
	enable_bad_data_banning.setLayoutData( gridData );
    Messages.setLanguageText(enable_bad_data_banning.getControl(), "ConfigView.section.ipfilter.enablebanning");

    	// block banning
    
	IntParameter block_banning = new IntParameter(gFilter, "Ip Filter Ban Block Limit" );
	gridData = new GridData();
	gridData.widthHint = 30;
	block_banning.setLayoutData( gridData );

	Label	block_label = new Label(gFilter, SWT.NULL);
	Messages.setLanguageText(block_label, "ConfigView.section.ipfilter.blockbanning");
	
	enable_bad_data_banning.setAdditionalActionPerformer(
	    		new ChangeSelectionActionPerformer( new Control[]{ block_banning.getControl(), block_label }));
	
		// table
	
    table = new Table(gFilter, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL);
    String[] headers = { "ConfigView.section.ipfilter.description", "ConfigView.section.ipfilter.start", "ConfigView.section.ipfilter.end" };
    int[] sizes = { 200, 110, 110 };
    int[] aligns = { SWT.LEFT, SWT.CENTER, SWT.CENTER };
    for (int i = 0; i < headers.length; i++) {
      TableColumn tc = new TableColumn(table, aligns[i]);
      tc.setText(headers[i]);
      tc.setWidth(sizes[i]);
      Messages.setLanguageText(tc, headers[i]); //$NON-NLS-1$
    }
    
    
    
    TableColumn[] columns = table.getColumns();
    columns[0].setData(new Integer(FilterComparator.FIELD_NAME));
    columns[1].setData(new Integer(FilterComparator.FIELD_START_IP));
    columns[2].setData(new Integer(FilterComparator.FIELD_END_IP));
    
    Listener listener = new Listener() {
      public void handleEvent(Event e) {
        TableColumn tc = (TableColumn) e.widget;
        int field = ((Integer) tc.getData()).intValue();
        comparator.setField(field);
        ipRanges = getSortedRanges(filter.getRanges());
        table.setItemCount(ipRanges.length);
        table.clearAll();
    	// bug 69398 on Windows
    	table.redraw();
      }
    };
    
    columns[0].addListener(SWT.Selection,listener);
    columns[1].addListener(SWT.Selection,listener);
    columns[2].addListener(SWT.Selection,listener);

    table.setHeaderVisible(true);

    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.heightHint = 400;
	gridData.horizontalSpan = 3;
    table.setLayoutData(gridData);

	gridData = new GridData();
	gridData.horizontalSpan = 3;

    Composite cArea = new Composite(gFilter, SWT.NULL);
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.numColumns = 3;
    cArea.setLayout(layout);
    cArea.setLayoutData(gridData);

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
        ipRanges = getSortedRanges(filter.getRanges());
        table.setItemCount(ipRanges.length);
        table.clearAll();
        table.redraw();
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

    ipRanges = getSortedRanges(filter.getRanges());

    table.addListener(SWT.SetData,new Listener() {
      public void handleEvent(Event event) {
        TableItem item = (TableItem) event.item;
		int index = table.indexOf (item);
		
			// seems we can get -1 here (see bug 1219314 )
		
		if ( index < 0 || index >= ipRanges.length ){
			return;
		}
		IpRange range = ipRanges[index];		
        item.setText(0, range.getDescription());
        item.setText(1, range.getStartIp());
        item.setText(2, range.getEndIp());
        item.setData(range);
      }
    });
    
    table.setItemCount(ipRanges.length);
    table.clearAll();
	// bug 69398 on Windows
	table.redraw();
    
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

 

  public void removeRange(IpRange range) {
  	filter.removeRange( range );
    //noChange = false;
    //refresh();
  }

  public void editRange(IpRange range) {
    new IpFilterEditor(azureus_core,table.getDisplay(), table, range);
    noChange = false;
    //refresh();
  }

  public void addRange() {
    new IpFilterEditor(azureus_core,table.getDisplay(), table, null);
    //noChange = false;
    //refresh();
  }

  public void refresh() {
    if (table == null || table.isDisposed() || noChange)
      return;
    noChange = true;
    TableItem[] items = table.getItems();
    for (int i = 0; i < items.length; i++) {      
      if (items[i] == null || items[i].isDisposed())
        continue;
      String tmp = items[i].getText(0);
      IpRange range = (IpRange) items[i].getData();
      
      String	desc = range.getDescription();

      if (desc != null && !desc.equals(tmp))
        items[i].setText(0, desc);

      tmp = items[i].getText(1);
      if (range.getStartIp() != null && !range.getStartIp().equals(tmp))
        items[i].setText(1, range.getStartIp());

      tmp = items[i].getText(2);
      if (range.getEndIp() != null && !range.getEndIp().equals(tmp))
        items[i].setText(2, range.getEndIp());

    }
  }
  
  protected IpRange[]
  getSortedRanges(
  		IpRange[]	ranges )
  {
  	Arrays.sort(
  		ranges,
		comparator);
  	
  	return( ranges );
	
  }
  
  protected void
  setPercentageBlocked()
  {
    long nbIPsBlocked = filter.getTotalAddressesInRange();
    
    if ( COConfigurationManager.getBooleanParameter( "Ip Filter Allow" )){
    	
    	nbIPsBlocked = 0x100000000L - nbIPsBlocked;
    }
    
    int percentIPsBlocked =  (int) (nbIPsBlocked * 1000L / (256L * 256L * 256L * 256L));
    
    String nbIps = "" + nbIPsBlocked;
    String percentIps = DisplayFormatters.formatPercentFromThousands(percentIPsBlocked);

    Messages.setLanguageText(percentage_blocked,"ConfigView.section.ipfilter.totalIPs",new String[]{nbIps,percentIps});

  }
}
