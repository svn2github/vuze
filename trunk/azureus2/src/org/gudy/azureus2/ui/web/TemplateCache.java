/* Written and copyright 2001-2003 Tobias Minich.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 *
 *
 * TemplateCache.java
 *
 * Created on 23. August 2003, 01:11
 */

package org.gudy.azureus2.ui.web;

import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

import HTML.Template;

import org.gudy.azureus2.core3.config.*;

/**
 *
 * @author  Tobias Minich
 */
public class TemplateCache {
  
  private static final boolean CACHE = false;
  
  private static TemplateCache templatecache;
  
  private Hashtable persistantargs = new Hashtable();
  private HashMap cache = new HashMap();
  private HashMap cache_need = new HashMap();
  private String cacheddir;
  
  /** Creates a new instance of TemplateCache */
  public TemplateCache() {
    UpdatePersistantArgs();
  }
  
  public Template get(String path) throws FileNotFoundException, IOException {
    if (!CACHE) {
      cache.remove(path);
      cache_need.remove(path);
    }
    if (!cacheddir.equals(COConfigurationManager.getStringParameter("Server_sTemplate_Directory")))
      UpdatePersistantArgs();
    Template t;
    if (cache.containsKey(path)) {
      t = (Template) cache.get(path);
      t.clearParams();
    } else {
      Hashtable args = (Hashtable) persistantargs.clone();
      args.put("filename", path);
      t = new Template(args);
      Iterator vars = t.vars.iterator();
      LinkedList li = new LinkedList();
      while (vars.hasNext()) {
        String s = (String) vars.next();
        String cl;
        if (s.indexOf('_')!=-1)
          cl = s.substring(0, s.indexOf('_'));
        else
          cl = s;
        if (!li.contains(cl))
          li.add(cl);
      }
      cache.put(path, t);
      cache_need.put(path, li);
    }
    return t;
  }
  
  public boolean needs(String path, String need) {
    return (cache_need.containsKey(path) && ((LinkedList) cache_need.get(path)).contains(need));
  }
  
  public void UpdatePersistantArgs() {
    String[] paths = new String[2];

    persistantargs.clear();
    cache.clear();
    cache_need.clear();
    
    cacheddir=COConfigurationManager.getStringParameter("Server_sTemplate_Directory");
    paths[0]=cacheddir;
    paths[1]="RES:org/gudy/azureus2/ui/web/template";
    //paths[2]="template";
    //paths[2]="/home/tobi/devel/azureus2/org/gudy/azureus2/server/template";
    
    
    persistantargs.put("path", paths);
    persistantargs.put("case_sensitive", "true");
    persistantargs.put("loop_context_vars", Boolean.TRUE);
    persistantargs.put("strict", Boolean.TRUE);
    //persistantargs.put("debug", Boolean.TRUE);
  }
  
  public synchronized static TemplateCache getInstance() {
    if(templatecache == null)
      templatecache = new TemplateCache();
    return templatecache;
  }
}
