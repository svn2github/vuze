/*
 * Created on 2 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.core.BooleanParameter;
import org.gudy.azureus2.core.ConfigurationManager;

/**
 * @author Olivier
 * 
 */
public class ConfigView implements IView {

  private static final int upRates[] =
    {
      0,
      2,
      3,
      4,
      5,
      6,
      7,
      8,
      9,
      10,
      11,
      12,
      13,
      14,
      15,
      20,
      25,
      30,
      35,
      40,
      45,
      50,
      60,
      70,
      80,
      90,
      100,
      150,
      200,
      250,
      300,
      350,
      400,
      450,
      500,
      600,
      700,
      800,
      900,
      1000 };

  Composite gConfig;
  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
   */
  public void initialize(Composite composite) {
    final ConfigurationManager config = ConfigurationManager.getInstance();
    gConfig = new Composite(composite, SWT.NULL);
    GridLayout configLayout = new GridLayout();
    configLayout.numColumns = 2;
    gConfig.setLayout(configLayout);

    new Label(gConfig, SWT.NULL).setText("Override IP address sent to tracker");
    GridData gridData = new GridData();
    gridData.widthHint = 100;
    new StringParameter(gConfig, "Override Ip", "").setLayoutData(gridData);

    new Label(gConfig, SWT.NULL).setText("Use Fast Resume Mode");
    new BooleanParameter(gConfig, "Use Resume", false);

    new Label(gConfig, SWT.NULL).setText("Allocate new files");
    final Button bAllocate = new Button(gConfig, SWT.CHECK);
    bAllocate.setSelection(config.getBooleanParameter("Allocate New", true));

    new Label(gConfig, SWT.NULL).setText("Disconnect seeds when seed");
    new BooleanParameter(gConfig, "Disconnect Seed", false);

    new Label(gConfig, SWT.NULL).setText("Auto-Switch to low-priority when seeding");
    new BooleanParameter(gConfig, "Switch Priority", true);

    new Label(gConfig, SWT.NULL).setText("Servers lowest port");
    gridData = new GridData();
    gridData.widthHint = 40;
    new IntParameter(gConfig, "Low Port", 6881).setLayoutData(gridData);

    new Label(gConfig, SWT.NULL).setText("Servers highest port");
    gridData = new GridData();
    gridData.widthHint = 40;
    new IntParameter(gConfig, "High Port", 6889).setLayoutData(gridData);

    new Label(gConfig, SWT.NULL).setText("Maximum number of connections per torrent (0 : unlimited) ");
    gridData = new GridData();
    gridData.widthHint = 30;
    new IntParameter(gConfig, "Max Clients", 0).setLayoutData(gridData);

    new Label(gConfig, SWT.NULL).setText("Default max uploads per torrent");
    final String upLabels[] = new String[99];
    final int upValues[] = new int[99];
    for (int i = 0; i < 99; i++) {
      upLabels[i] = " " + (i + 2);
      upValues[i] = i + 2;
    }
    new IntListParameter(gConfig, "Max Uploads", 4, upLabels, upValues);

    new Label(gConfig, SWT.NULL).setText("Max Upload Speed (globally)");
    final String upsLabels[] = new String[upRates.length];
    final int upsValues[] = new int[upRates.length];
    upsLabels[0] = "Unlimited";
    upsValues[0] = 0;
    for (int i = 1; i < upRates.length; i++) {
      upsLabels[i] = " " + upRates[i] + "kB/s";
      upsValues[i] = 1024 * upRates[i];
    }
    new IntListParameter(gConfig, "Max Upload Speed", 0, upsLabels, upsValues);

    Button enter = new Button(gConfig, SWT.PUSH);
    enter.setText("Save");
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_END);
    gridData.horizontalSpan = 2;
    enter.setLayoutData(gridData);

    enter.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent event) {
        config.setParameter("updated", 1);
        config.save();
      }
    });

  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getComposite()
   */
  public Composite getComposite() {
    return gConfig;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#refresh()
   */
  public void refresh() {
    // TODO Auto-generated method stub

  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#delete()
   */
  public void delete() {
    // TODO Auto-generated method stub

  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getShortTitle()
   */
  public String getShortTitle() {
    return "Configuration";
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
   */
  public String getFullTitle() {
    return "Configuration";
  }

}
