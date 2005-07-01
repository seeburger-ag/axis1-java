/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.axis2.transport.http;

import org.apache.axis2.Constants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.SessionContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.engine.AxisFault;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;

/**
 * Class AxisServlet
 */
public class AxisServlet extends HttpServlet {
    /**
     * Field engineRegistry
     */

    private ConfigurationContext configContext;

    private ListingAgent lister;

    /**
     * Method init
     *
     * @param config
     * @throws ServletException
     */
    public void init(ServletConfig config) throws ServletException {
        try {
            ServletContext context = config.getServletContext();
            String repoDir = context.getRealPath("/WEB-INF");
            ConfigurationContextFactory erfac = new ConfigurationContextFactory();
            configContext = erfac.buildConfigurationContext(repoDir);
            configContext.setProperty(Constants.CONTAINER_MANAGED, Constants.VALUE_TRUE);
            lister = new ListingAgent(configContext);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * Method doGet
     *
     * @param httpServletRequest
     * @param httpServletResponse
     * @throws ServletException
     * @throws IOException
     */
    protected void doGet(
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse)
        throws ServletException, IOException {
            httpServletResponse.setContentType("text/xml; charset=utf-8");
            MessageContext msgContext  = null;
        try {

            Object sessionContext =
                httpServletRequest.getSession().getAttribute(Constants.SESSION_CONTEXT_PROPERTY);
            if (sessionContext == null) {
                sessionContext = new SessionContext(null);
                httpServletRequest.getSession().setAttribute(
                    Constants.SESSION_CONTEXT_PROPERTY,
                    sessionContext);
            }

            Enumeration enu = httpServletRequest.getParameterNames();
            HashMap map = new HashMap();
            while (enu.hasMoreElements()) {
                String name = (String) enu.nextElement();
                String value = httpServletRequest.getParameter(name);
                map.put(name, value);
            }

            msgContext =
                new MessageContext(
                    configContext,
                    (SessionContext) sessionContext,
                    configContext.getAxisConfiguration().getTransportIn(
                        new QName(Constants.TRANSPORT_HTTP)),
                    configContext.getAxisConfiguration().getTransportOut(
                        new QName(Constants.TRANSPORT_HTTP)));
                    msgContext.setDoingREST(true);
            msgContext.setServerSide(true);
            msgContext.setProperty(HTTPConstants.HTTPOutTransportInfo,new ServletBasedOutTransportInfo(httpServletResponse));

            boolean processed =
                HTTPTransportUtils.processHTTPGetRequest(
                    msgContext,
                    httpServletRequest.getInputStream(),
                    httpServletResponse.getOutputStream(),
                    httpServletRequest.getContentType(),
                    httpServletRequest.getHeader(HTTPConstants.HEADER_SOAP_ACTION),
                    httpServletRequest.getRequestURL().toString(),
                    configContext,
                    map);
            if (!processed) {
                lister.handle(httpServletRequest, httpServletResponse);
            }
        } catch (Exception e) {
            AxisEngine engine = new AxisEngine(configContext);
            if(msgContext!= null){
                msgContext.setProperty(MessageContext.TRANSPORT_OUT, httpServletResponse.getOutputStream());
                engine.handleFault(msgContext,e);            
            }
        }

    }

    /*
    * (non-Javadoc)
    * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
    */

    /**
     * Method doPost
     *
     * @param req
     * @param res
     * @throws ServletException
     * @throws IOException
     */
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
            MessageContext msgContext = null;
        try {
            Object sessionContext =
                req.getSession().getAttribute(Constants.SESSION_CONTEXT_PROPERTY);
            if (sessionContext == null) {
                sessionContext = new SessionContext(null);
                req.getSession().setAttribute(Constants.SESSION_CONTEXT_PROPERTY, sessionContext);
            }
            msgContext =
                new MessageContext(
                    configContext,
                    (SessionContext) sessionContext,
                    configContext.getAxisConfiguration().getTransportIn(
                        new QName(Constants.TRANSPORT_HTTP)),
                    configContext.getAxisConfiguration().getTransportOut(
                        new QName(Constants.TRANSPORT_HTTP)));
            msgContext.setProperty(HTTPConstants.HTTPOutTransportInfo,new ServletBasedOutTransportInfo(res));
            res.setContentType("text/xml; charset=utf-8");
            HTTPTransportUtils.processHTTPPostRequest(
                msgContext,
                req.getInputStream(),
                res.getOutputStream(),
                req.getContentType(),
                req.getHeader(HTTPConstants.HEADER_SOAP_ACTION),
                req.getRequestURL().toString(),
                configContext);
            Object contextWritten = msgContext.getOperationContext().getProperty(Constants.RESPONSE_WRITTEN);
            if (contextWritten == null || !Constants.VALUE_TRUE.equals(contextWritten)) {
                res.setStatus(HttpServletResponse.SC_ACCEPTED);
            }
        } catch (AxisFault e) {
            AxisEngine engine = new AxisEngine(configContext);
            if(msgContext!= null){
                msgContext.setProperty(MessageContext.TRANSPORT_OUT, res.getOutputStream());
                engine.handleFault(msgContext,e);            
            }
        }
    }
}