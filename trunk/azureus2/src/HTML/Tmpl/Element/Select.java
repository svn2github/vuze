/*
*      HTML.Template:  A module for using HTML Templates with java
*
*      Copyright (c) 2002 Philip S Tellis (philip.tellis@iname.com)
*
*      This module is free software; you can redistribute it
*      and/or modify it under the terms of either:
*
*      a) the GNU General Public License as published by the Free
*      Software Foundation; either version 1, or (at your option)
*      any later version, or
*
*      b) the "Artistic License" which comes with this module.
*
*      This program is distributed in the hope that it will be
*      useful, but WITHOUT ANY WARRANTY; without even the implied
*      warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
*      PURPOSE.  See either the GNU General Public License or the
*      Artistic License for more details.
*
*      You should have received a copy of the Artistic License
*      with this module, in the file ARTISTIC.  If not, I'll be
*      glad to provide one.
*
*      You should have received a copy of the GNU General Public
*      License along with this program; if not, write to the Free
*      Software Foundation, Inc., 59 Temple Place, Suite 330,
*      Boston, MA 02111-1307 USA
*/
/*
 *     Select element added by Tobias Minich October 2003
 *     (Modified from the Condiditonal element)
 *
 *     Syntax:
 *      Start the block with either
 *        <TMPL_SELECT [variable]>
 *      or
 *        <TMPL_SELECT name="[variable]" equals="[value]">
 *
 *      In the first case the block to the first <TMPL_CASE [value]> will
 *      be used as default (the variable is not defined or the value isn't
 *      covered in a block). In the second case the first block is used if
 *      the variables content equals value.
 *
 *      Following are 1+ <TMPL_CASE [value]> blocks, each terminated by the
 *      next <TMPL_CASE [value]> or </TMPL_SELECT> to finish the whole 
 *      element. The value is everything after the space behind TMPL_CASE. 
 *      You can quote it, but don't have to.
 *      If the value is omitted, the block is used as default (see above).
 *
 */

package HTML.Tmpl.Element;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.NoSuchElementException;

public class Select extends Element
{
	private boolean control_val = false;
	private Hashtable data;
        private Vector def;
        private String currentbranch = null;

	public Select(String name, String init)
	{
		this.name = name;
                this.type = "select";
		this.data = new Hashtable();
                if (init != null) {
                    currentbranch = init;
                    Vector newone = new Vector();
                    this.data.put(init, newone);
                }
                this.def = new Vector();
	}

	public void addBranch(String name)
	{
                if ((name == null) || (name == ""))
                  currentbranch = null;
                else {
                  currentbranch = name;
                  if (!data.containsKey(name)) {
                    Vector newone = new Vector();
                    data.put(name, newone);
                  }
                }
	}

	public void add(String text)
	{
		if (currentbranch == null)
                        def.addElement(text);
                else
                        ((Vector) data.get(currentbranch)).addElement(text);
	}

	public void add(Element node)
	{
		if (currentbranch == null)
                        def.addElement(node);
                else
                        ((Vector) data.get(currentbranch)).addElement(node);
	}
        /*
	public void setControlValue(Object control_val)
			throws IllegalArgumentException
	{
		this.control_val = process_var(control_val);
	}
        */
	public String parse(Hashtable params)
	{
		/*
                if(!params.containsKey(this.name))
			this.control_val = false;
		else	
			setControlValue(params.get(this.name));
               */
		StringBuffer output = new StringBuffer();
                
                if (params.containsKey(this.name)) {
                  String value = params.get(this.name).toString();
                  Enumeration de;
                  if (data.containsKey(value))
                    de = ((Vector) data.get(value)).elements();
                  else
                    de = this.def.elements();
                  if (!de.hasMoreElements())
                    return "";

                  while(de.hasMoreElements()) {
                          Object e = de.nextElement();
                          if(e.getClass().getName().endsWith(".String"))
                                  output.append((String)e);
                          else
                                  output.append(((Element)e).parse(params));
                  }
                } else {
                  if (this.def.isEmpty())
                    return "";
                  else {
                    Enumeration de = this.def.elements();
                    while(de.hasMoreElements()) {
                            Object e = de.nextElement();
                            if(e.getClass().getName().endsWith(".String"))
                                    output.append((String)e);
                            else
                                    output.append(((Element)e).parse(params));
                    }
                  }
                }
                return output.toString();
	}

	public String typeOfParam(String param)
			throws NoSuchElementException
	{
                Collection vals = data.values();
                Iterator val = vals.iterator();
		while(val.hasNext())
		{
                        Vector v = (Vector) val.next();
			if(v == null)
				continue;
			for(Enumeration e = v.elements(); 
				e.hasMoreElements();)
			{
				Object o = e.nextElement();
				if(o.getClass().getName().endsWith(".String"))
					continue;
				if(((Element)o).Name().equals(param))
					return ((Element)o).Type();
			}
		}
                if (!def.isEmpty()) {
			for(Enumeration e = def.elements(); 
				e.hasMoreElements();)
			{
				Object o = e.nextElement();
				if(o.getClass().getName().endsWith(".String"))
					continue;
				if(((Element)o).Name().equals(param))
					return ((Element)o).Type();
			}
                }
		throw new NoSuchElementException(param);
	}
        /*
	private boolean process_var(Object control_val) 
			throws IllegalArgumentException 
	{
		String control_class = "";

		if(control_val == null)
			return false;
		
		control_class=control_val.getClass().getName();
		if(control_class.indexOf(".") > 0)
			control_class = control_class.substring(
					control_class.lastIndexOf(".")+1);

		if(control_class.equals("String")) {
			return !(((String)control_val).equals("") || 
				((String)control_val).equals("0"));
		} else if(control_class.equals("Vector")) {
			return !((Vector)control_val).isEmpty();
		} else if(control_class.equals("Boolean")) {
			return ((Boolean)control_val).booleanValue();
		} else if(control_class.equals("Integer")) {
			return (((Integer)control_val).intValue() != 0);
		} else {
			throw new IllegalArgumentException("Unrecognised type");
		}
	}
        */
}

