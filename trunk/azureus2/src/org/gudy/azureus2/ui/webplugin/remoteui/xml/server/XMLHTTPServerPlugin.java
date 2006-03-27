/*
 * File    : XMLServerPlugin.java
 * Created : 13-Mar-2004
 * By      : parg
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

package org.gudy.azureus2.ui.webplugin.remoteui.xml.server;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.ui.webplugin.*;
import org.gudy.azureus2.core3.util.Debug;

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.io.*;

import org.gudy.azureus2.plugins.tracker.web.*;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.pluginsimpl.remote.*;
import org.gudy.azureus2.pluginsimpl.remote.rpexceptions.*;
import org.gudy.azureus2.plugins.*;

public class
XMLHTTPServerPlugin
    extends WebPlugin
{
    public static final int DEFAULT_PORT    = 6884;

    protected static Properties defaults = new Properties();

    static{

        defaults.put( WebPlugin.CONFIG_PORT, new Integer( DEFAULT_PORT ));
        defaults.put( WebPlugin.PR_HIDE_RESOURCE_CONFIG, new Boolean(true));
    }

    protected RPRequestHandler              request_handler;
    protected RPRequestAccessController     access_controller;
    protected boolean serialise_debug = false;
    protected boolean deserialise_debug = false;
    protected LoggerChannel channel = null;
    protected boolean log_to_plugin = false;

    public
    XMLHTTPServerPlugin()
    {
        super(defaults);
    }

    public void initialize(PluginInterface _plugin_interface) throws PluginException {

        super.initialize( _plugin_interface );

        /**
         * Set up configuration settings.
         */
        BasicPluginConfigModel  config = getConfigModel();

        LabelParameter debug_section_description = config.addLabelParameter2("xmlhttp.config.group.debug.description");

        final BooleanParameter deserialise_param = config.addBooleanParameter2("xmlhttp.config.option.debug.deserialisation", "xmlhttp.config.option.debug.deserialisation", false);

        final BooleanParameter method_lookup_param = config.addBooleanParameter2("xmlhttp.config.option.debug.method_lookup", "xmlhttp.config.option.debug.method_lookup", false);

        final BooleanParameter method_invoke_param = config.addBooleanParameter2("xmlhttp.config.option.debug.method_invocation", "xmlhttp.config.option.debug.method_invocation", false);

        final BooleanParameter serialise_param = config.addBooleanParameter2("xmlhttp.config.option.debug.serialisation", "xmlhttp.config.option.debug.serialisation", false);

        config.createGroup("xmlhttp.config.group.debug",
            new Parameter[] {debug_section_description, deserialise_param,
                method_lookup_param, method_invoke_param, serialise_param});

        LabelParameter log_section_description = config.addLabelParameter2("xmlhttp.config.group.logging.description");

        BooleanParameter log_to_plugin_param = config.addBooleanParameter2("xmlhttp.config.option.log_to_plugin", "xmlhttp.config.option.log_to_plugin", true);

        BooleanParameter log_to_console = config.addBooleanParameter2("xmlhttp.config.option.log_to_console", "xmlhttp.config.option.log_to_console", false);

        this.log_to_plugin = log_to_plugin_param.getValue();

        config.createGroup("xmlhttp.config.group.logging",
            new Parameter[] {log_section_description, log_to_plugin_param,
                log_to_console});

        /**
         * Add event listeners.
         */
        ParameterListener pl = new ParameterListener() {
            public void parameterChanged(Parameter param) {
                boolean new_value = ((BooleanParameter)param).getValue();
                if (param == method_lookup_param) {
                    RemoteMethodInvoker.setLogResolution(new_value);
                }
                else if (param == method_invoke_param) {
                    RemoteMethodInvoker.setLogInvocation(new_value);
                }
                else if (param == deserialise_param) {
                    XMLHTTPServerPlugin.this.deserialise_debug = new_value;
                }
                else if (param == serialise_param) {
                    XMLHTTPServerPlugin.this.serialise_debug = new_value;
                }
            }
        };

        deserialise_param.addListener(pl);
        serialise_param.addListener(pl);
        method_lookup_param.addListener(pl);
        method_invoke_param.addListener(pl);

        final BasicPluginViewModel view_model = this.getViewModel();
        if (log_to_console.getValue()) {
            channel = _plugin_interface.getLogger().getChannel("XML/HTTP");
        }
        else {
            channel = _plugin_interface.getLogger().getNullChannel("XML/HTTP");
        }

        if (log_to_plugin) {
            channel.addListener(new LoggerChannelListener() {
                public void messageLogged(int type, String message) {
                    view_model.getLogArea().appendText("  " + message + "\n");
                }
                public void messageLogged(String message, Throwable error) {
                    this.messageLogged(-1, message);
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    error.printStackTrace(pw);
                    pw.flush();
                    this.messageLogged(-1, sw.toString());
                }
            });
        }


        /**
         * Use configuration settings.
         */
        this.deserialise_debug = deserialise_param.getValue();
        this.serialise_debug = serialise_param.getValue();

        request_handler = new RPRequestHandler(_plugin_interface, true);
        access_controller = new WebPluginAccessController(_plugin_interface);
    }

    public boolean
    generateSupport(
        TrackerWebPageRequest       request,
        TrackerWebPageResponse      response )

        throws IOException
    {
        String  url = request.getURL().toLowerCase();

        if ( url.equals( "process.cgi") || url.equals( "/process.cgi")){

            InputStream is = null;

            /**
             * Two things we're doing here:
             *   1) Reporting the user-agent, just for the fun of it.
             *   2) For GTSdll v1, we need to generate XML which resembles the
             *      old style (spaced out), just to make life for clients who
             *      don't want to upgrade (or can't upgrade yet).
             */
            Map req_headers = request.getHeaders();
            String user_agent = (String)req_headers.get("user-agent");
            boolean space_out_xml = false;

            /**
             * GTSdll v1 doesn't send a User-Agent string, but does send Host
             * and Referer fields, so we'll check against that.
             */
            if (user_agent == null) {
                if ("www.adv-clan.com".equals(req_headers.get("host")) &&
                    "www.adv-clan.com".equals(req_headers.get("referer"))) {

                    user_agent = "GTSdll v1 (auto-detected)";
                    space_out_xml = true;
                }
            }

            String user_agent_text = (user_agent == null) ? "" : ", User-Agent: " + user_agent;

            if (this.log_to_plugin) {
                this.getViewModel().getLogArea().appendText("REQUEST START: " + request.getClientAddress() + ", time: " + DateFormat.getTimeInstance().format(new Date()) + user_agent_text + "\n");
            }

            try{
                response.setContentType("text/xml; charset=\"utf-8\"");

                new XMLRequestProcessor2(
                            request_handler,
                            access_controller,
                            request.getClientAddress(),
                            request.getInputStream(),
                            response.getOutputStream(),
                            this.plugin_interface,
                            this.channel,
                            this.serialise_debug,
                            this.deserialise_debug,
                            space_out_xml);

                if (this.log_to_plugin) {
                    this.getViewModel().getLogArea().appendText("REQUEST END\n");

                    if ( is != null ){
                        is.close();
                    }
                }

                return true;

            }
            catch (Throwable t) {
                Debug.out("Serious error in XML / HTTP plugin - error escaped to this level");
                Debug.printStackTrace(t);
                if (is != null) {
                    try {is.close();}
                    catch (IOException ioe) {}
                }
                return true;
            }

        }

        return( false );
    }

}
