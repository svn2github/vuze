/**
 * Created on 10-Jan-2006
 * Created by Allan Crooks
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.ui.webplugin.remoteui.xml.server;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.pluginsimpl.remote.MethodSignature;
import org.gudy.azureus2.pluginsimpl.remote.RPUtils;
import org.gudy.azureus2.pluginsimpl.remote.RPException;
import org.gudy.azureus2.pluginsimpl.remote.RPObject;
import org.gudy.azureus2.pluginsimpl.remote.RPRequest;
import org.gudy.azureus2.pluginsimpl.remote.RPRequestAccessController;
import org.gudy.azureus2.pluginsimpl.remote.RPRequestHandler;
import org.gudy.azureus2.pluginsimpl.remote.RPReply;
import org.gudy.azureus2.core3.xml.util.XMLElement;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocument;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentNode;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentException;
import org.gudy.azureus2.core3.xml.simpleparser.SimpleXMLParserDocumentFactory;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentAttribute;
import org.gudy.azureus2.pluginsimpl.remote.rpexceptions.*;

/**
 * Main methods here:
 *    <init>: Takes input and output stream and starts off the processing.
 *            Is responsible for parsing XML, writing data to output stream,
 *            and capturing all unexpected exceptions.
 *
 *    process: Mainly involved in chaining together the main subfunctions.
 *             Will get RPRequest object from XML document, get RPReply
 *             returned and serialise reply into XMLElement object.
 *
 *    deserialiseObject: Takes XML node and is involve in reconstructing the
 *                       object's fields - usually just for RPRequest objects.
 *
 *    serialiseObject: Takes any object (including primitive wrapper objects)
 *                     and tries to generate their XML.
 *
 *    deserialiseValue: Deserialises any object, either normal Java types or
 *                      resolves it to be RPObject instances (or their
 *                      delegation). No other calls are performed, we don't
 *                      deserialise any fields.
 */

public class XMLRequestProcessor2 {
    protected RPRequestHandler request_handler;
    protected SimpleXMLParserDocument request;
    protected boolean serialise_debug;
    protected boolean deserialise_debug;
    protected LoggerChannel logger;

    protected XMLRequestProcessor2(RPRequestHandler _request_handler, RPRequestAccessController _access_controller, String _client_ip, InputStream _request, OutputStream _reply, PluginInterface pi, LoggerChannel rp_channel, boolean serialise_debug, boolean deserialise_debug, boolean space_out_xml) {

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new OutputStreamWriter(_reply, Constants.DEFAULT_ENCODING));
        }
        catch(UnsupportedEncodingException e){
            Debug.printStackTrace(e);
            pw = new PrintWriter(_reply);
        }

        this.serialise_debug = serialise_debug;
        this.deserialise_debug = deserialise_debug;
        this.request_handler = _request_handler;
        this.logger = rp_channel;
        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

        XMLElement response_xml = new XMLElement("RESPONSE", true);
        try{
            this.request = SimpleXMLParserDocumentFactory.create( _request);
            process(_client_ip , _access_controller, response_xml, pi);
        }
        catch(Throwable e){
            Debug.printStackTrace(e);
            response_xml.clear();
            RPException rpe = null;
            if (e instanceof SimpleXMLParserDocumentException) {
                rpe = new RPMalformedXMLException(e);
            }
            else if (e instanceof RPException) {
                rpe = (RPException)e;
            }
            else {
                rpe = new RPInternalProcessException(e);
            }
            response_xml.addContent(describeError(rpe));

        }
        finally{
            response_xml.printTo(pw, space_out_xml);
            pw.flush();
            pw.close();
        }
    }

    protected void process(String client_ip, RPRequestAccessController access_controller, XMLElement response_xml, PluginInterface pi) throws Exception {

        debug_in("About to deserialise the RPRequest");
        RPRequest req_obj = (RPRequest)deserialiseObject(this.request, RPRequest.class);

        log_general("REQUEST: " +
            "method=" + req_obj.getMethod() + ", " +
            "object=" + req_obj.getObject());

        req_obj.setClientIP(client_ip);
        req_obj.setPluginInterface(pi);

        RPReply reply = request_handler.processRequest(req_obj, access_controller);

        debug_out("About to serialise the RPReply");
        Map props = reply.getProperties();
        Iterator it = props.entrySet().iterator();
        XMLElement response_attr = null;
        Map.Entry mapping = null;
        while(it.hasNext()) {
            mapping = (Map.Entry)it.next();
            response_attr = new XMLElement((String)mapping.getKey());
            response_attr.addContent((String)mapping.getValue());
            response_xml.addContent(response_attr);
        }

        Object response = null;
        try {
            response = reply.getResponse();
        }
        /* We don't need to catch all exceptions, we can just let them
           propogate. */
        catch (RPException e) {
            debug_out("RPException occurred - possibly occurred during the method invocation", e);
            log_general("RESPONSE (ERROR): " +
                ((e.getCause() == null) ? e : e.getCause()));
            Debug.printStackTrace(e);
            response_xml.clear();
            response_xml.addContent(describeError(e));
            return;
        }

        if (RPUtils.hasDescriptiveToStringMethod(response)) {
            log_general("RESPONSE: " + response);
        }
        else {
            log_general("RESPONSE: value=" + response + ", type=" + RPUtils.describeObject(response));
        }

        if (response != null) {
            if (response.getClass().isArray()) {
                response_xml.setAutoOrdering(false);
            }
            serialiseObject(response, 0xFFFFFFFF, response_xml);
        }
    }

    public XMLElement describeError(RPException rpe) {
        XMLElement xe = new XMLElement("ERROR", true);

        Class c = rpe.getErrorClass();
        if (c != null) {
            xe.addAttribute("class", RPUtils.getName(c));
        }

        String type = rpe.getRPType();
        if (type != null) {
            xe.addAttribute("type", type);
        }

        xe.addContent(rpe.getSerialisationMessage());

        Throwable t = rpe.getSerialisableObject();

        try {
            if (t != null) {
                serialiseObject(t, ~Modifier.PRIVATE, xe);
            }
        }
        catch (RuntimeException re) {
            /**
             * Only uncheck this comment if you're a developer. Of course, only
             * developers would actually be uncommenting this line. ;)
             */
            // throw re;
            Debug.out("Error serialising error object.");
            Debug.printStackTrace(re);
        }
        return xe;
    }

    protected Object deserialiseObject(SimpleXMLParserDocumentNode node, Class cla) throws Exception {

        /**
         * hack I'm afraid, when deserialising request objects we need to use
         * the method to correctly deserialise parameters
         */
        String request_method = null;

        if ( cla == RPRequest.class ){
            request_method = node.getChild( "METHOD" ).getValue().trim();
        }
        try{
            debug_in("Beginning deserialisation of " + RPUtils.getName(cla) + " instance.");
            Object obj = cla.newInstance();
            Field[] fields = cla.getDeclaredFields();
            for (int i=0;i<fields.length;i++) {
                Field field = fields[i];
                int modifiers = field.getModifiers();

                // Field is either transient and / or static, so skip it.
                if ((modifiers & (Modifier.TRANSIENT | Modifier.STATIC)) !=0)
                    continue;

                String name = field.getName();
                SimpleXMLParserDocumentNode child = node.getChild(name);

                // There's no value for this field given in the XML.
                if (child == null)
                    continue;

                Class type = field.getType();

                debug_in("Deserialising field (\"" + name + "\", type: " + RPUtils.getName(type) + ")");

                if (type.isArray()) {
                    Class sub_type = type.getComponentType();

                    SimpleXMLParserDocumentNode[] entries = child.getChildren();
                    String[] bits = null;
                    int arr_length = entries.length;
                    if (request_method != null) {
                    	
                    	/**
                    	 * If we have a method signature, we will allow the number
                    	 * of arguments allowed there to determine how many arguments
                    	 * we are expecting - this gives us the caller a way of passing
                    	 * null as an argument (by omitting the ENTRY for the value they
                    	 * want to use null for).
                    	 */ 
                    	bits = MethodSignature.parse(request_method).arg_classes;
                    	arr_length = bits.length;
                    }
                    else {
                    	bits = (String[])Collections.nCopies(arr_length, "String").toArray(new String[arr_length]);
                    }

                    Object array = Array.newInstance(sub_type, arr_length);

                    debug_in("Attempting to deserialise " + entries.length + " entries for this field (which is an array type).");

                    for (int j=0; j<entries.length; j++) {

                        SimpleXMLParserDocumentNode array_child = entries[j];
                        SimpleXMLParserDocumentAttribute index_attr = array_child.getAttribute("index");
                        String index_str = index_attr==null?null:index_attr.getValue().trim();
                        int array_index = index_str==null?j:Integer.parseInt(index_str);
                        String bit = bits[array_index];
                        String sub_value = array_child.getValue().trim();
                        Object value_to_store = deserialiseValue(array_child, sub_value, bit, true);
                        Array.set(array, array_index, value_to_store);

                    }

                    debug_in("Finished deserialising array, about to set value.");
                    field.set(obj, array);
                    debug_in("Field \"" + name + "\" deserialised and set.");
                }
                else {
                    String value = child.getValue().trim();

                    // We need to look at remote objects - because we need to
                    // get the object field to store on RPRequest.
                    Object obj_value = deserialiseValue(child, value, RPUtils.getName(type), true);
                    field.set(obj, obj_value);
                    debug_in("Field \"" + name + "\" deserialised and set.");
                }
            } // end for loop
            return obj;

        }
        catch (RPException e) {
            throw e;
        }
    }

    protected void serialiseObject(Object obj, int original_modifier_filter, XMLElement container) {
        int modifier_filter = original_modifier_filter & (~(Modifier.TRANSIENT | Modifier.STATIC));

        Class cla = null;
        if (obj != null) {
        	cla = obj.getClass();
        }
        
        String cla_name = (cla == null) ? "null" : RPUtils.getName(cla);
        
        debug_out("Serialising object of type \"" + cla_name + "\"");
        String value = XMLSerialisationUtils.serialise(obj, cla);
        if (value != null) {
            debug_out("Value was easily serialised into a string format.");
            container.addContent(value);
            return;
        }

        if (cla.isArray()) {
            int len = Array.getLength(obj);
            debug_out("Object is array type - processing " + len + " item(s).");
            XMLElement entry_xml = null;
            for (int i=0;i<len;i++) {
                entry_xml = new XMLElement("ENTRY", true);
                entry_xml.addAttribute("index", i);
                serialiseObject(Array.get(obj, i), original_modifier_filter, entry_xml);
                container.addContent(entry_xml);
            }
            debug_out("Finished serialising array.");
            return;
        }

        String obj_descr = RPUtils.describeObject(obj);

        debug_out("Need to use XMLSerialisationUtils to serialise " + obj_descr);
        Map[] attribute_data = XMLSerialisationUtils.getAttributeData(obj, modifier_filter);
        Map attribute_types = attribute_data[0];
        Map attribute_values = attribute_data[1];

        debug_out("Going to process " + attribute_values.size() + " attributes on " + obj_descr);

        Iterator attr_itr = attribute_values.entrySet().iterator();
        Map.Entry me = null;
        String key = null;
        while (attr_itr.hasNext()) {
            me = (Map.Entry)attr_itr.next();
            key = (String)me.getKey();
            if (me.getValue() != null) {
                debug_out("About to serialise attribute \"" + key + "\"");
                Class attr_class = (Class)attribute_types.get(key);
                if (attr_class == null) {
                    throw new NullPointerException("Trying to serialise attribute on " + obj + " (which belongs to " + container + ") - attrname: " + key + ", attrvalue: " + me.getValue() + ", but type not given!");
                }
                XMLElement attribute_content = container.makeContent(key, !attr_class.isArray());
                serialiseObject(me.getValue(), original_modifier_filter, attribute_content);
            }
            else {
                debug_out("Attribute \"" + key + "\" was null, skipping.");
            }
        }
        debug_out("Finished serialising " + obj_descr);
    }

    public Object deserialiseValue(SimpleXMLParserDocumentNode node, String string_value, String class_type, boolean rp_lookup) {

        Object parsed_value = null;
        Class result_class = XMLSerialisationUtils.getClass(class_type);
        if (result_class != null) {
            debug_in("Attempting to deserialise simple value of type \"" + RPUtils.getName(result_class) + "\"");

            try {
                parsed_value = XMLSerialisationUtils.deserialise(string_value, result_class);
            }
            catch (Exception e) {
                throw new RPDeserialiseParseException(e, string_value, result_class);
            }
        }

        if (parsed_value != null) {
            debug_in("Deserialisation of simple value successful.");
            return parsed_value;
        }

        if (rp_lookup) {
            debug_in("About to see if there is an object reference to process.");
            SimpleXMLParserDocumentNode obj_node_parent, obj_node = null;

            /**
             * We'll be a bit flexible with how we handle object references.
             *
             * In most cases, they should be embedded in a OBJECT/_object_id
             * tag. However, that's not always possible - an RPRequest instance
             * never has the ID in an OBJECT tag.
             *
             * Given that we can be called when node is either the parent of
             * an OBJECT tag or an _object_id tag, we'll cope with both
             * situations.
             */
            obj_node_parent = node.getChild("OBJECT");
            if (obj_node_parent == null) {
                obj_node_parent = node;
            }

            obj_node = obj_node_parent.getChild("_object_id");

            if (obj_node != null) {
                debug_in("Found object ID node, processing...");
                String oid_str = obj_node.getValue().trim();
                long oid = Long.parseLong(oid_str);
                RPObject local_rp_obj = RPObject._lookupLocal(oid);

                /**
                 * If we need an RPObject, then we'll just use this object.
                 * (RPRequest wants a RPObject).
                 */
                if (class_type.equals("RPObject")) {
                    debug_in("Found object (type required was RPObject, so we don't need to unwrap the value).");
                    return local_rp_obj;
                }

                /**
                 * Otherwise, we want to provide the underlying object
                 * (like if we are passing a parameter).
                 */

                // Type check.
                Object local_obj = local_rp_obj._getDelegate();
                if (!RPUtils.issubclassByName(local_obj.getClass(), class_type)) {
                    throw new RPDeserialiseClassMismatchException(class_type, local_rp_obj._getName());
                }

                debug_in("Found object with ID - returning " + RPUtils.describeObject(local_obj));
                return local_obj;
            }
            else {
                debug_in("No object ID node found.");
            }
        }
        throw new RPUnsupportedInputTypeException(class_type);
    }

    // Logging.
    public void debug_in(String s) {
        if (deserialise_debug) {
            this.logger.log(s);
        }
    }

    public void debug_in(String s, Throwable t) {
        if (deserialise_debug) {
            this.logger.log(s, t);
        }
    }

    public void debug_out(String s) {
        if (serialise_debug) {
            this.logger.log(s);
        }
    }

    public void debug_out(String s, Throwable t) {
        if (serialise_debug) {
            this.logger.log(s, t);
        }
    }

    public void log_general(String s) {
        this.logger.log(s);
    }

}
