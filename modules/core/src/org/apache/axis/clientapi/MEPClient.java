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
 *
 *  Runtime state of the engine
 */
package org.apache.axis.clientapi;

import javax.xml.namespace.QName;

import org.apache.axis.addressing.EndpointReference;
import org.apache.axis.context.MessageContext;
import org.apache.axis.context.ServiceContext;
import org.apache.axis.description.OperationDescription;
import org.apache.axis.description.TransportOutDescription;
import org.apache.axis.engine.AxisFault;
import org.apache.axis.om.OMAbstractFactory;
import org.apache.axis.om.OMElement;
import org.apache.axis.soap.SOAPEnvelope;
import org.apache.axis.soap.SOAPFactory;
import org.apache.axis.soap.impl.llom.SOAPProcessingException;
import org.apache.axis.soap.impl.llom.soap11.SOAP11Constants;
import org.apache.axis.soap.impl.llom.soap12.SOAP12Constants;

/**
 * This is the Super Class for all the MEPClients, All the MEPClient will extend this.
 */
public abstract class MEPClient {
    protected ServiceContext serviceContext;
    protected final String mep;
    protected String soapVersionURI = SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI;

    public MEPClient(ServiceContext service, String mep) {
        this.serviceContext = service;
        this.mep = mep;
    }

    protected void verifyInvocation(OperationDescription axisop) throws AxisFault {
        if (axisop == null) {
            throw new AxisFault("OperationDescription can not be null");
        }

        if (mep.equals(axisop.getMessageExchangePattern())) {
            throw new AxisFault(
                "This mepClient supports only "
                    + mep
                    + " And the Axis Operations suppiled supports "
                    + axisop.getMessageExchangePattern());
        }

        if (serviceContext.getServiceConfig().getOperation(axisop.getName()) == null) {
            serviceContext.getServiceConfig().addOperation(axisop);
        }
    }

    protected MessageContext prepareTheSystem(OMElement toSend) throws AxisFault {
        MessageContext msgctx = new MessageContext(serviceContext.getEngineContext());

        SOAPEnvelope envelope = createDefaultSOAPEnvelope();
        envelope.getBody().addChild(toSend);
        msgctx.setEnvelope(envelope);
        return msgctx;
    }

    public TransportOutDescription inferTransport(EndpointReference epr) throws AxisFault {
        String transport = null;
        if (epr != null) {
            String toURL = epr.getAddress();
            int index = toURL.indexOf(':');
            if (index > 0) {
                transport = toURL.substring(0, index);
            }
        }

        if (transport != null) {
            return serviceContext.getEngineContext().getAxisConfiguration().getTransportOut(
                new QName(transport));

        } else {
            throw new AxisFault("Cannot Infer transport from the URL");
        }

    }

    public SOAPEnvelope createDefaultSOAPEnvelope() {
        SOAPFactory fac = null;
        if (SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(soapVersionURI)) {
            fac = OMAbstractFactory.getSOAP12Factory();
        } else {
            fac = OMAbstractFactory.getSOAP11Factory();
        }
        return fac.getDefaultEnvelope();
    }

    /**
     * @param string
     */
    public void setSoapVersionURI(String string) {
        soapVersionURI = string;
    }

}
