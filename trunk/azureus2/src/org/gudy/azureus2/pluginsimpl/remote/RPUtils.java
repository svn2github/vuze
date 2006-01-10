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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.lang.reflect.Method;
import java.lang.reflect.Array;

public class RPUtils {

    private RPUtils() {}

    public static final String PLUGIN_PACKAGE_ROOT = "org.gudy.azureus2.plugins";
    public static final String REMOTE_PLUGIN_PACKAGE_ROOT = "org.gudy.azureus2.pluginsimpl.remote";

    /**
     *
     * Plugin package aware methods.
     *
     */

    public static boolean isPluginAPIPackage(Package p) {
        return p != null && p.getName().startsWith(PLUGIN_PACKAGE_ROOT);
    }

    public static boolean isRemotePluginImplPackage(Package p) {
        return p != null && p.getName().startsWith(REMOTE_PLUGIN_PACKAGE_ROOT);
    }

    /**
     *
     * Reflection helper methods.
     *
     */
    public static Object invoke(Method m, Object o, Object[] args) throws InvocationTargetException {
        try {
            return m.invoke(o, args);
        }
        catch (IllegalAccessException iae) {
            /* Shouldn't happen! */
            throw new RuntimeException(iae);
        }
        catch (IllegalArgumentException iae) {
            /* Shouldn't happen! */
            throw new RuntimeException(iae);
        }
    }

    public static Class asRemoteClass(Class c) {
        if (c.isArray()) {
            throw new RuntimeException("array type given");
        }
        Package p = c.getPackage();
        if (p==null || !isPluginAPIPackage(p)) {
            return null;
        }

        if (!c.isInterface()) {
            return null;
        }

        /**
         * This works out what sub-package off the package root the class is
         * located.
         */
        String sub_package_with_dot = "";
        if (PLUGIN_PACKAGE_ROOT.length() < p.getName().length()) {
            sub_package_with_dot = p.getName().substring(PLUGIN_PACKAGE_ROOT.length() + 1) + ".";
        }

        String remote_class_name = REMOTE_PLUGIN_PACKAGE_ROOT + "." + sub_package_with_dot + "RP" + getName(c);

        try {
            return Class.forName(remote_class_name);
        }
        catch (ClassNotFoundException cnfe) {
            return null;
        }

    }

    public static Class asLocalClass(Class c) {
        if (c.isArray()) {
            throw new RuntimeException("array type given");
        }
        Package p = c.getPackage();
        if (p==null || !isRemotePluginImplPackage(p)) {
            return null;
        }

        if (c.isInterface()) {
            return null;
        }

        /**
         * This works out what sub-package off the package root the class is
         * located.
         */
        String sub_package_with_dot = "";
        if (REMOTE_PLUGIN_PACKAGE_ROOT.length() < p.getName().length()) {
            sub_package_with_dot = p.getName().substring(REMOTE_PLUGIN_PACKAGE_ROOT.length() + 1) + ".";
        }

        String class_name = getName(c);

        /**
         * All classes which are meant to be remote representations of a plugin
         * interface class should have an RP prefix.
         */
        if (class_name.startsWith("RP")) {
            class_name = class_name.substring(2);
        }
        else {
            return null;
        }

        String remote_class_name = PLUGIN_PACKAGE_ROOT + "." + sub_package_with_dot + class_name;

        try {
            return Class.forName(remote_class_name);
        }
        catch (ClassNotFoundException cnfe) {
            return null;
        }

    }

    public static Class[] getPluginAPIInterfacesForClass(Class c) {
        ArrayList result = new ArrayList();
        getPluginAPIInterfacesForClass(c, result);
        return (Class[])result.toArray(new Class[result.size()]);
    }

    public static Class getPluginAPIInterfaceForClass(Class c) {
        return getPluginAPIInterfacesForClass(c, null);
    }

    private static Class getPluginAPIInterfacesForClass(Class c, ArrayList l) {
        if (c.isInterface() && isPluginAPIPackage(c.getPackage())) {
            if (l == null) {return c;}
            l.add(c);
        }
        Class[] interfaces = c.getInterfaces();
        Class result = null;
        for (int i=0; i<interfaces.length; i++) {
            result = getPluginAPIInterfacesForClass(interfaces[i], l);
            if (result != null) {return result;}
        }
        return null;
    }

    public static String getName(Class c) {
        return getName(c, false);
    }

    public static String getName(Class c, boolean include_package) {
        int array_dimensions = 0;
        while (c.isArray()) {
            c = c.getComponentType();
            array_dimensions++;
        }
        String name_with_pkg = c.getName();
        String class_name = name_with_pkg;

        if (!include_package) {
            int package_separator = name_with_pkg.lastIndexOf('.');

            if (package_separator == -1) {
                class_name = name_with_pkg; // no package attached.
            }
            else {
                class_name = name_with_pkg.substring(package_separator + 1);
            }
        }

        for (int i=0; i<array_dimensions; i++) {class_name += "[]";}
        return class_name;
    }

    /**
     * No array support yet.
     */
    public static boolean issubclassByName(Class c, String name) {

        if (getName(c).equals(name)) {
            return true;
        }

        Class [] interfaces = c.getInterfaces();
        for (int i=0; i<interfaces.length; i++) {
            if (getName(interfaces[i]).equals(name)) {
                return true;
            }
        }

        Class superclass = c.getSuperclass();
        if (superclass != null && !superclass.equals(Object.class)) {
            if (issubclassByName(superclass, name)) {
                return true;
            }
        }

        return false;

    }

    public static String toString(Method m) {
        Class[] param_types = m.getParameterTypes();
        ArrayList param_names = new ArrayList(param_types.length);
        for (int i=0; i<param_types.length; i++) {
            param_names.add(getName(param_types[i]));
        }
        return MethodSignature.asString(getName(m.getReturnType()), m.getName(), (String[])param_names.toArray(new String[param_types.length]));
    }

    public static String describeObject(Object o) {
        if (o==null) {return "null";}
        Class c = o.getClass();
        int array_length = -1;
        if (c.isArray()) {
            array_length = Array.getLength(o);
            c = c.getComponentType();
        }
        String result = getName(c);
        if (array_length!=-1) {
            result+= ("[" + array_length + "]");
        }
        result += '@' + Integer.toHexString(System.identityHashCode(o));
        return result;
    }

    public static boolean hasDescriptiveToStringMethod(Object o) {
        if (o == null) {return true;}
        if (o instanceof Number) {return true;}
        if (o instanceof String) {return true;}
        if (o instanceof Character) {return true;}
        if (o instanceof RPObject) {return true;}
        return false;
    }

    public static String exceptionToString (Throwable e) {
        Throwable cause = e.getCause();
        if (cause != null){
            String m = cause.getMessage();
            if (m != null){
                return m;
            }
            return cause.toString();
        }
        String m = e.getMessage();
        if (m != null) {
            return m;
        }
        return e.toString();
    }

}