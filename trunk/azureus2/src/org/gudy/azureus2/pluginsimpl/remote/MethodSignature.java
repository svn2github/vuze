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

public class MethodSignature {

    public static MethodSignature parse(String meth_sig) {
        int method_arg_start = meth_sig.indexOf("[");
        if (method_arg_start == -1) {
            return new MethodSignature(meth_sig, new String[0]);
        }
        String method_name = meth_sig.substring(0, method_arg_start);

        /**
         * If the signature doesn't end up with a close square bracket,
         * this is likely to be a malformed method signature. We could
         * raise an error here, but for now, we'll just return a
         * MethodSignature object which will work (except that it won't
         * ever find a matching method).
         */
        if (meth_sig.charAt(meth_sig.length()-1)!=']') {
            return new MethodSignature(meth_sig, new String[0]);
        }

        String meth_sig_args = meth_sig.substring(method_arg_start+1, meth_sig.length()-1);
        String[] class_types = meth_sig_args.split(",");
        return new MethodSignature(method_name, class_types);
    }

    public String method_name;
    public String[] arg_classes;

    public MethodSignature(String method_name, String[] arg_classes) {
        this.method_name = method_name;
        this.arg_classes = arg_classes;
    }

    public String toString() {
        return asString(null, this.method_name, this.arg_classes);
    }

    public static String asString(String return_type, String method_name, String[] arg_types) {
        String result = (return_type==null) ? "" : return_type + " ";
        result += method_name + "(";
        for (int i=0; i<arg_types.length; i++) {
            if (i!=0) {result += ", ";}
            result += arg_types[i];
        }
        result += ")";
        return result;
    }

}
