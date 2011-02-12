/*
 * File    : CategoryManagerImpl.java
 * Created : 09 feb. 2004
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

package org.gudy.azureus2.core3.category.impl;

import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.gudy.azureus2.core3.category.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;

import com.aelitis.azureus.core.rssgen.RSSGeneratorPlugin;

public class 
CategoryManagerImpl 
	implements RSSGeneratorPlugin.Provider 
{
  private static final String PROVIDER = "categories";

  private static final String UNCAT_NAME 	= "__uncategorised__";
  private static final String ALL_NAME 		= "__all__";
  
  private static CategoryManagerImpl catMan;
  private static CategoryImpl catAll = null;
  private static CategoryImpl catUncategorized = null;
  private static boolean doneLoading = false;
  private static AEMonitor	class_mon	= new AEMonitor( "CategoryManager:class" );
  
  private Map<String,CategoryImpl> categories 			= new HashMap<String,CategoryImpl>();
  private AEMonitor	categories_mon	= new AEMonitor( "Categories" );
  
  private static final int LDT_CATEGORY_ADDED     = 1;
  private static final int LDT_CATEGORY_REMOVED   = 2;
  private static final int LDT_CATEGORY_CHANGED   = 3;
  private ListenerManager category_listeners = ListenerManager.createManager(
    "CatListenDispatcher",
    new ListenerManagerDispatcher()
    {
      public void
      dispatch(Object   _listener,
               int      type,
               Object   value )
      {
        CategoryManagerListener target = (CategoryManagerListener)_listener;

        if ( type == LDT_CATEGORY_ADDED )
          target.categoryAdded((Category)value);
        else if ( type == LDT_CATEGORY_REMOVED )
            target.categoryRemoved((Category)value);
        else if ( type == LDT_CATEGORY_CHANGED )
            target.categoryChanged((Category)value);
        }
    });


  protected
  CategoryManagerImpl()
  {
  	loadCategories();
  }
  
  public void addCategoryManagerListener(CategoryManagerListener l) {
    category_listeners.addListener( l );
  }

  public void removeCategoryManagerListener(CategoryManagerListener l) {
    category_listeners.removeListener( l );
  }

  public static CategoryManagerImpl getInstance() {
  	try{
  		class_mon.enter();
	    if (catMan == null)
	      catMan = new CategoryManagerImpl();
	    return catMan;
  	}finally{
  		
  		class_mon.exit();
  	}
  }

  protected void loadCategories() {
    if (doneLoading)
      return;
    doneLoading = true;

    FileInputStream fin = null;
    BufferedInputStream bin = null;
 
    makeSpecialCategories();

   
    try {
      //open the file
      File configFile = FileUtil.getUserFile("categories.config");
      fin = new FileInputStream(configFile);
      bin = new BufferedInputStream(fin, 8192);
     
      Map map = BDecoder.decode(bin);

      List catList = (List) map.get("categories");
      for (int i = 0; i < catList.size(); i++) {
        Map mCategory = (Map) catList.get(i);
        try {
          String catName = new String((byte[]) mCategory.get("name"), Constants.DEFAULT_ENCODING);
          
          Long l_maxup 		= (Long)mCategory.get( "maxup" );
          Long l_maxdown 	= (Long)mCategory.get( "maxdown" );
          Map<String,String>	attributes = BDecoder.decodeStrings((Map)mCategory.get( "attr" ));
          
          if ( attributes == null ){
        	  
        	  attributes = new HashMap<String, String>();
          }
          
          if ( catName.equals( UNCAT_NAME )){
        	  
        	  catUncategorized.setUploadSpeed(l_maxup==null?0:l_maxup.intValue());
        	  catUncategorized.setDownloadSpeed(l_maxdown==null?0:l_maxdown.intValue());
        	  catUncategorized.setAttributes( attributes );
        	  
          }else if ( catName.equals( ALL_NAME )){
            	  
              catAll.setAttributes( attributes );

          }else{
	          categories.put( 
	        	catName,
	        	  new CategoryImpl( 
	        		  catName, 
	        		  l_maxup==null?0:l_maxup.intValue(),
	        		  l_maxdown==null?0:l_maxdown.intValue(),
	        			attributes ));
          }
        }
        catch (UnsupportedEncodingException e1) {
          //Do nothing and process next.
        }
      }
    }
    catch (FileNotFoundException e) {
      //Do nothing
    }
    catch (Exception e) {
    	Debug.printStackTrace( e );
    }
    finally {
      try {
        if (bin != null)
          bin.close();
      }
      catch (Exception e) {}
      try {
        if (fin != null)
          fin.close();
      }
      catch (Exception e) {}
      
      checkConfig();
    }
  }

  protected void saveCategories(Category category ){
	  saveCategories();
	  
      category_listeners.dispatch( LDT_CATEGORY_CHANGED, category );
  }
  protected void saveCategories() {
    try{
    	categories_mon.enter();
    
      Map map = new HashMap();
      List list = new ArrayList(categories.size());

      Iterator<CategoryImpl> iter = categories.values().iterator();
      while (iter.hasNext()) {
        CategoryImpl cat = iter.next();

        if (cat.getType() == Category.TYPE_USER) {
          Map catMap = new HashMap();
          catMap.put( "name", cat.getName());
          catMap.put( "maxup", new Long(cat.getUploadSpeed()));
          catMap.put( "maxdown", new Long(cat.getDownloadSpeed()));
          catMap.put( "attr", cat.getAttributes());
          list.add(catMap);
        }
      }
      
      Map uncat = new HashMap();
      uncat.put( "name", UNCAT_NAME );
      uncat.put( "maxup", new Long(catUncategorized.getUploadSpeed()));
      uncat.put( "maxdown", new Long(catUncategorized.getDownloadSpeed()));
      uncat.put( "attr", catUncategorized.getAttributes());
      list.add( uncat );
      
      Map allcat = new HashMap();
      allcat.put( "name", ALL_NAME );
      allcat.put( "attr", catAll.getAttributes());
      list.add( allcat );
      
      map.put("categories", list);


      FileOutputStream fos = null;

      try {
        //encode the data
        byte[] torrentData = BEncoder.encode(map);

         File oldFile = FileUtil.getUserFile("categories.config");
         File newFile = FileUtil.getUserFile("categories.config.new");

         //write the data out
        fos = new FileOutputStream(newFile);
        fos.write(torrentData);
         fos.flush();
         fos.getFD().sync();

          //close the output stream
         fos.close();
         fos = null;

         //delete the old file
         if ( !oldFile.exists() || oldFile.delete() ) {
            //rename the new one
            newFile.renameTo(oldFile);
         }

      }
      catch (Exception e) {
      	Debug.printStackTrace( e );
      }
      finally {
        try {
          if (fos != null)
            fos.close();
        }
        catch (Exception e) {}
      }
    }finally{
    	
    	checkConfig();
    	 
    	categories_mon.exit();
    }
  }

  public Category createCategory(String name) {
    makeSpecialCategories();
    CategoryImpl newCategory = getCategory(name);
    if (newCategory == null) {
      newCategory = new CategoryImpl(name, 0, 0, new HashMap<String,String>());
      categories.put(name, newCategory);
      saveCategories();

      category_listeners.dispatch( LDT_CATEGORY_ADDED, newCategory );
      return (Category)categories.get(name);
    }
    return newCategory;
  }

  public void removeCategory(Category category) {
    if (categories.containsKey(category.getName())) {
      categories.remove(category.getName());
      saveCategories();
      category_listeners.dispatch( LDT_CATEGORY_REMOVED, category );
    }
  }

  public Category[] getCategories() {
    if (categories.size() > 0)
      return (Category[])categories.values().toArray(new Category[categories.size()]);
    return (new Category[0]);
  }

  public CategoryImpl getCategory(String name) {
    return categories.get(name);
  }

  public Category getCategory(int type) {
    if (type == Category.TYPE_ALL)
      return catAll;
    if (type == Category.TYPE_UNCATEGORIZED)
      return catUncategorized;
    return null;
  }

  private void makeSpecialCategories() {
    if (catAll == null) {
      catAll = new CategoryImpl("Categories.all", Category.TYPE_ALL, new HashMap<String,String>());
      categories.put("Categories.all", catAll);
    }
    
    if (catUncategorized == null) {
      catUncategorized = new CategoryImpl("Categories.uncategorized", Category.TYPE_UNCATEGORIZED, new HashMap<String,String>());
      categories.put("Categories.uncategorized", catUncategorized);
    }
  }
  
  
  
  private void
  checkConfig()
  {
	  boolean	gen_enabled = false;
	  
	  for ( CategoryImpl cat: categories.values()){
		  
		  if ( cat.getBooleanAttribute( Category.AT_RSS_GEN )){
			  
			  gen_enabled = true;
			  
			  break;
		  }
	  }
	  
	  if ( gen_enabled ){
		  
		  RSSGeneratorPlugin.registerProvider( PROVIDER, this  );
		  
	  }else{
		  
		  RSSGeneratorPlugin.unregisterProvider( PROVIDER );
	  }
  }
  
	public boolean
	isEnabled()
	{
		return( true );
	}

	public boolean
	generate(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		return( true );
	}
}
