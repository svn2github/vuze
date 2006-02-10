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
package org.gudy.azureus2.pluginsimpl.remote;

import java.util.WeakHashMap;
import org.gudy.azureus2.plugins.logging.Logger;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.lang.reflect.Array;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.pluginsimpl.remote.rpexceptions.*;

public class RemoteMethodInvoker {

    /**
     *
     *
     * Instance factory code.
     *
     *
     */

    private static int use_generic = 0;
    private boolean use_generic_classes;
    private static WeakHashMap instances;
    static {instances = new WeakHashMap();}

    private RemoteMethodInvoker(LoggerChannel log_channel, boolean generic) {
        this.log_channel = log_channel;
        this.use_generic_classes = generic;
    }

    public static RemoteMethodInvoker create(LoggerChannel log_channel, boolean generic) {

        synchronized (RemoteMethodInvoker.class) {
            if (use_generic == 0) {
                use_generic = (generic) ? 1: -1;
            }
            else {
                if ((use_generic == 1 && !generic) ||
                    (use_generic == -1 && generic)) {
                    throw new RuntimeException("mismatch of generic RemoteMethodInvoker");
                }
            }
        }

        synchronized (instances) {
            RemoteMethodInvoker result = (RemoteMethodInvoker)instances.get(log_channel);
            if (result==null) {
                result = new RemoteMethodInvoker(log_channel, generic);
                instances.put(log_channel, result);
            }
            return result;
        }
    }

    public static RemoteMethodInvoker create() {return create(null, false);}

    public static RemoteMethodInvoker create(LoggerChannel channel) {return create(channel, false);}

    public static RemoteMethodInvoker create(boolean generic) {return create(null, generic);}


    /**
     *
     * Logging code.
     *
     */

    private static boolean can_log_invocation = false;
    private static boolean can_log_resolution = false;
    private LoggerChannel log_channel;

    private void invoke_log(String s, Throwable t) {
        if (can_log_invocation && log_channel != null)
            log_channel.log(s, t);
    }

    private void invoke_log(String s) {
        if (can_log_invocation && log_channel != null)
            log_channel.log(s);
    }

    private void resolve_log(String s) {
        if (can_log_resolution && log_channel != null)
            log_channel.log(s);
    }

    public static void setLogResolution(boolean value) {can_log_resolution = value;}
    public static void setLogInvocation(boolean value) {can_log_invocation = value;}

    /**
     *
     *
     * High-level invocation methods.
     *
     *
     **/

    public Object invokeMethod(Object o, String meth_sig, Object[] args, boolean wrap_result) throws InvocationTargetException, NoSuchMethodException {
        String obj_as_string = RPUtils.describeObject(o);

        resolve_log("Resolving method " + meth_sig + " on " + obj_as_string);
        Method m = getMethod(o, MethodSignature.parse(meth_sig));
        if (m == null) {
            resolve_log("No matching method found.");
            throw new NoSuchMethodException(meth_sig);
        }

        invoke_log("Found method for " + meth_sig + " on " + obj_as_string + " - " + RPUtils.toString(m) + ", now invoking");

        Object result = null;
        try {
            /**
             * We don't use RMIC.invoke, because we want to log if either
             * of the two unexpected exceptions actually occur.
             */
            result = m.invoke(o, args);
        }
        catch (InvocationTargetException ite) {
            Throwable t = (ite.getCause() == null) ? ite: ite.getCause();
            invoke_log("Error during method invocation.", t);
            throw ite;
        }

        /**
         * We don't expect these errors whatsoever, hence them not being in the
         * throws clause.
         */
        catch (IllegalAccessException iae) {
            invoke_log("Unable to invoke " + meth_sig + " on " + obj_as_string, iae);
            throw new RuntimeException(iae);
        }
        catch (IllegalArgumentException iae) {
            invoke_log("Unable to invoke " + meth_sig + " on " + obj_as_string, iae);
            throw new RuntimeException(iae);
        }

        String log_message = "Method " + RPUtils.toString(m) + " returned normally, result=" + result;
        if (wrap_result) {
            invoke_log(log_message + ", about to transform object to be returned remotely.");
        }
        else {
            invoke_log(log_message);
            return result;
        }

        Object trans_result = prepareRemoteResult(result, m.getReturnType());
        if (result!=trans_result) {
            invoke_log("Value was transformed - previously " + RPUtils.describeObject(result) + ", now " + RPUtils.describeObject(trans_result));
        }
        return trans_result;
    }

    public RPReply process(Object o, RPRequest r) {
        if (o instanceof RPObject) {
            throw new IllegalArgumentException("object must not be RPObject - it must be the delegate object");
        }
        Object reply = null;
        RPException error = null;
        try {
            reply = invokeMethod(o, r.getMethod(), r.getParams(), true);
        }
        catch (NoSuchMethodException nsme) {
            error = new RPUnknownMethodException(r.getMethod());
        }
        catch (InvocationTargetException ite) {
            error = new RPRemoteMethodInvocationException(ite.getCause());
        }

        /**
         * If we happen to return a plugin interface, we need to ensure that
         * it maintains the same connection ID as the one we are currently
         * using, otherwise clients will think a new connection has been
         * opened.
         */
        if (reply instanceof RPPluginInterface) {
            if (r.connection_id != 0) {
                ((RPPluginInterface)reply)._connection_id = r.connection_id;
            }
        }

        return new RPReply((error == null) ? reply : error);
    }

    public Method getMethod(Class c, MethodSignature ms) {
        Method[] methods = c.getMethods();
        for (int i=0; i<methods.length; i++) {
            if (!methods[i].getName().equals(ms.method_name)) {
                continue;
            }
            resolve_log("Found method " + RPUtils.toString(methods[i]) + " on " + RPUtils.getName(c) + ", testing to see if it matches " + ms);
            Class[] params = methods[i].getParameterTypes();
            if (params.length != ms.arg_classes.length) {
                resolve_log("Number of parameters differ (want " + ms.arg_classes.length + ", got " + params.length + ")");
                continue;
            }
            boolean has_arg_type_mismatch = false;
            for (int j=0; j<params.length; j++) {
                if (!RPUtils.issubclassByName(params[j], ms.arg_classes[j])) {
                    resolve_log("Parameter " + (j + 1) + ": want " + ms.arg_classes[j] + ", got " + RPUtils.getName(params[j]));
                    has_arg_type_mismatch = true;
                    break;
                }
            }
            if (has_arg_type_mismatch) {
                continue;
            }
            resolve_log("Method matches.");
            return methods[i];
        }
        return null;
    }

    public Method getMethod(Object o, MethodSignature ms) {
        Class[] interfaces = RPUtils.getPluginAPIInterfacesForClass(o.getClass());
        resolve_log("Trying to find " + ms + " for " + o + ", checking interface classes: " + Arrays.asList(interfaces));
        Method result = null;
        for (int i=0; i<interfaces.length; i++) {
            result = getMethod(interfaces[i], ms);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public Object prepareRemoteResult(Object result, Class return_type) throws InvocationTargetException, NoSuchMethodException {
        if (result == null) {return result;}
        if (return_type.isArray()) {
            Class return_component_class = return_type.getComponentType();
            Class remote_component_class = asRemoteClass(return_component_class);

            if (remote_component_class == null) {
                return result;
            }

            Object[] result_array = (Object[])result;
            Object[] remote_array = (Object[])Array.newInstance(remote_component_class, result_array.length);

            for (int i=0; i<result_array.length; i++) {
                remote_array[i] = prepareRemoteResult(result_array[i], return_component_class);
            }
            return remote_array;
        }
        else {

            Class remote_class = asRemoteClass(return_type);
            if (remote_class == null)
                return result;
            else if (remote_class == GenericRPObject.class)
                return GenericRPObject.create(result);
            else if (remote_class == GenericRPPluginInterface.class)
                return GenericRPPluginInterface.create((PluginInterface)result);
            else {
                // All RP classes should have this method defined,.
                Method create = remote_class.getMethod("create", new Class[] {return_type});
                return RPUtils.invoke(create, null, new Object[]{result});
            }
        }
    }

    private Class asRemoteClass(Class c) {
        if (this.use_generic_classes) {
            if (!RPUtils.isPluginAPIPackage(c.getPackage())) {
                return null;
            }
            else if (c.isAssignableFrom(PluginInterface.class)) {
                return GenericRPPluginInterface.class;
            }
            else {
                return GenericRPObject.class;
            }
        }
        else {
            return RPUtils.asRemoteClass(c);
        }
    }

}