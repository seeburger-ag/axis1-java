package org.apache.axis.soap.impl.llom;

import org.apache.axis.om.impl.llom.OMElementImpl;
import org.apache.axis.om.OMElement;
import org.apache.axis.om.OMNamespace;
import org.apache.axis.om.OMXMLParserWrapper;
import org.apache.axis.om.OMException;
import org.apache.axis.soap.SOAPFaultDetail;
import org.apache.axis.soap.SOAPFault;
import org.apache.axis.soap.impl.llom.soap12.SOAP12Constants;

import javax.xml.namespace.QName;
import java.util.Iterator;

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
 * author : Eran Chinthaka (chinthaka@apache.org)
 */

public abstract class SOAPFaultDetailImpl extends SOAPElement implements SOAPFaultDetail{

    protected SOAPFaultDetailImpl(SOAPFault parent, boolean extractNamespaceFromParent) throws SOAPProcessingException {
        super(parent, SOAP12Constants.SOAP_FAULT_DETAIL_LOCAL_NAME, extractNamespaceFromParent);
    }

    protected SOAPFaultDetailImpl(SOAPFault parent, OMXMLParserWrapper builder) {
        super(parent, SOAP12Constants.SOAP_FAULT_DETAIL_LOCAL_NAME, builder);
    }

    public void addDetailEntry(OMElement detailElement) {
        this.addChild(detailElement);
    }

    public Iterator getAllDetailEntries() {
        return this.getChildren();  
    }
}