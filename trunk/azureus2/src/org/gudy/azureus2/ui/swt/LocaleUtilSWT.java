/*
 * LocaleUtilSWT.java
 *
 * Created on 29. August 2003, 17:32
 */

package org.gudy.azureus2.ui.swt;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.aelitis.azureus.core.*;
import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.ui.swt.mainwindow.*;

/**
 *
 * @author  Tobias Minich
 */

public class 
LocaleUtilSWT 
	implements LocaleUtilListener
{
  boolean waitForUserInput = true;
    
  protected static boolean 				rememberEncodingDecision = true;
  protected static LocaleUtilDecoder 	rememberedDecoder 		 = null;
  protected static Object				remembered_on_behalf_of;
  

  public 
  LocaleUtilSWT(
  	AzureusCore		core ) 
  {
  	core.getLocaleUtil().addListener( this );
  }
  
  
  public LocaleUtilDecoderCandidate
  selectDecoder(
  	LocaleUtil						locale_util,
	Object							decision_owner,
  	LocaleUtilDecoderCandidate[]	candidates )
  {
  	if ( decision_owner != remembered_on_behalf_of ){
  		
  		remembered_on_behalf_of		= decision_owner;
  		rememberedDecoder			= null;
  	}
  	
    if( rememberEncodingDecision && rememberedDecoder != null) {
    	
      for (int i = 0; i < candidates.length; i++) {
      	
        if(candidates[i].getValue() != null && rememberedDecoder == candidates[i].getDecoder()) {
        			  
          return( candidates[i] );
        }
      }
    }

    LocaleUtilDecoderCandidate	default_candidate	= candidates[0];
    
    String defaultString = candidates[0].getValue();

    Arrays.sort(candidates);
   
    boolean always_prompt = COConfigurationManager.getBooleanParameter("File.Decoder.Prompt", false );
    
    if ( !always_prompt ){
    	
    	int minlength = candidates[0].getValue().length();
   
	    // If the default string length == minlength assumes that
	    // the array encoding is from default charset
    	
	    if (defaultString != null && defaultString.length() == minlength) {
	    	
	      return( default_candidate );
	    }

	    	// see if we can try and apply a default encoding
	    
	    String	default_name = COConfigurationManager.getStringParameter( "File.Decoder.Default", "" );
	    
	    if ( default_name.length() > 0 ){
			for (int i = 0; i < candidates.length; i++) {
			  if(candidates[i].getValue() != null && candidates[i].getDecoder().getName().equals( default_name )) {
	        	
				return( candidates[i] );
			  }
			}
		}
    }
    
    ArrayList choosableCandidates = new ArrayList(5);
    
    choosableCandidates.add(candidates[0]);
       
    LocaleUtilDecoder[]	general_decoders = locale_util.getGeneralDecoders();
    
    // add all general candidates with names not already in the list
    for (int j = 0; j < general_decoders.length; j++) {
    	
      for (int i = 1; i < candidates.length; i++) {
      	
      	if (candidates[i].getValue()==null || candidates[i].getDecoder()==null) continue;
      	
        if(		general_decoders[j] != null && 
        		general_decoders[j].getName().equals(candidates[i].getDecoder().getName())){
        	
        	if (!choosableCandidates.contains(candidates[i])) {
       
        		choosableCandidates.add(candidates[i]);
        		
        		break;
        	}
        }
      }
    }
    
    // add the remaining possible locales
   	for (int i = 1; i < candidates.length; i++){
   		
   		if (candidates[i].getValue()==null || candidates[i].getDecoder()==null) continue;
   		
   		if(!choosableCandidates.contains(candidates[i])){
   			
   			choosableCandidates.add(candidates[i]);
   		}
   	}
 

    final LocaleUtilDecoderCandidate[] candidatesToChoose = (LocaleUtilDecoderCandidate[]) choosableCandidates.toArray(new LocaleUtilDecoderCandidate[choosableCandidates.size()]);
       
    waitForUserInput	= true;
    
    MainWindow window = MainWindow.getWindow();
    
    	// can get here if torrent already added in non-swt ui mode with dodgy encoding
    
    if ( window == null ){
    	
    	return( default_candidate );
    }
    
    MainWindow.getWindow().getDisplay().asyncExec(new Runnable() {
      public void run() {
      	try{
        	showChoosableEncodingWindow(MainWindow.getWindow().getShell(), candidatesToChoose);
        	
      	}catch( Throwable e ){
      		
      		e.printStackTrace();	
      	}finally{
      	
        	synchronized( LocaleUtilSWT.this ){
        
        		waitForUserInput = false;
        	}
        }
      }
    });
    
    while(true) {
    
      synchronized( this ){
      	
      	if ( !waitForUserInput ){
      		
      		break;
      	}
      }
		
      try {
        Thread.sleep(100);
      } catch (Exception ignore) {
      }
    }

    int choosedIndex = 0;
    for (int i = 1; i < candidatesToChoose.length; i++) {
      if(candidatesToChoose[i].getValue() != null && rememberedDecoder == candidatesToChoose[i].getDecoder()) {
        choosedIndex = i;
        break;
      }
    }

    return ( candidatesToChoose[choosedIndex] ); 
  }    

  private void showChoosableEncodingWindow(final Shell shell, final LocaleUtilDecoderCandidate[] candidates) {
    final Display display = shell.getDisplay();
    final Shell s = new Shell(shell, SWT.TITLE | SWT.PRIMARY_MODAL);
    s.setImage(ImageRepository.getImage("azureus")); //$NON-NLS-1$
    s.setText(MessageText.getString("LocaleUtil.title")); //$NON-NLS-1$
    GridData gridData;
    s.setLayout(new GridLayout(1, true));
    s.setLayoutData(gridData = new GridData());

/*
    Label label = new Label(s, SWT.NONE);
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
    label.setLayoutData(gridData);
    label.setText("Bitte wählen Sie das Encoding, welches am besten passt");
*/
    Group gChoose = new Group(s, SWT.NULL);
    gChoose.setLayout(new GridLayout(3, false));
    Messages.setLanguageText(gChoose, "LocaleUtil.section.chooseencoding"); //$NON-NLS-1$
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gridData.horizontalSpan = 3;
    gChoose.setLayoutData(gridData);

    Label label = new Label(gChoose, SWT.LEFT);
    gridData = new GridData();
    gridData.horizontalSpan = 3;
    label.setLayoutData(gridData);
    Messages.setLanguageText(label, "LocaleUtil.label.chooseencoding"); //$NON-NLS-1$

    final Table table = new Table(gChoose, SWT.SINGLE | SWT.FULL_SELECTION);
    table.setLinesVisible(true);
    table.setHeaderVisible(true);
    gridData = new GridData();
    gridData.horizontalSpan = 3;
    table.setLayoutData(gridData);

    String[] titlesPieces = { "filename", "encoding"}; //$NON-NLS-1$ //$NON-NLS-2$
    for (int i = 0; i < titlesPieces.length; i++) {
      TableColumn column = new TableColumn(table, SWT.LEFT);
      Messages.setLanguageText(column, "LocaleUtil.column.".concat(titlesPieces[i])); //$NON-NLS-1$
    }

    // add candidates to table
    for (int i = 0; i < candidates.length; i++) {
      TableItem item = new TableItem(table, SWT.NULL);
      item.setText(0, candidates[i].getValue());
      item.setText(1, candidates[i].getDecoder().getName());
    }
    int lastSelectedIndex = 0;
    for (int i = 1; i < candidates.length; i++) {
      if(candidates[i].getValue() != null && candidates[i].getDecoder() == rememberedDecoder ) {
        lastSelectedIndex = i;
        break;
      }
    }
    table.select(lastSelectedIndex);

    // resize all columns to fit the widest entry 
    table.getColumn(0).pack();
    table.getColumn(1).pack();

    label = new Label(gChoose, SWT.LEFT);
    gridData = new GridData();
    gridData.horizontalSpan = 3;
    label.setLayoutData(gridData);
    Messages.setLanguageText(label, "LocaleUtil.label.hint.doubleclick"); //$NON-NLS-1$

    final Button ok = new Button(gChoose, SWT.PUSH);
    ok.setText(" ".concat(MessageText.getString("Button.next")).concat(" ")); //$NON-NLS-1$ //$NON-NLS-3$ //$NON-NLS-2$
    ok.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
    label = new Label(gChoose, SWT.NULL);
    label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END));
    Messages.setLanguageText(label, "LocaleUtil.label.checkbox.rememberdecision"); //$NON-NLS-1$

    final Button checkBox = new Button(gChoose, SWT.CHECK);
    checkBox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
    checkBox.setSelection(rememberEncodingDecision);

    s.pack();
    
    Utils.centreWindow(s);
 
    ok.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent event) {
        setChoosedIndex(s, table, checkBox, candidates);
      }
    });

    table.addMouseListener(new MouseAdapter() {
      public void mouseDoubleClick(MouseEvent mEvent) {
        setChoosedIndex(s, table, checkBox, candidates);
      }
    });

    s.open();
    while (!s.isDisposed()) {
      try {
        if (!display.readAndDispatch())
          display.sleep();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void setChoosedIndex(final Shell s, final Table table, final Button checkBox, LocaleUtilDecoderCandidate[] candidates) {
    int selectedIndex = table.getSelectionIndex();
    if(-1 == selectedIndex) 
      return;
    rememberEncodingDecision = checkBox.getSelection();
        
	if ( rememberEncodingDecision ){
		
		rememberedDecoder = candidates[selectedIndex].getDecoder();
	}else{
		rememberedDecoder = null;
	}

    s.dispose();
  }

  
}
