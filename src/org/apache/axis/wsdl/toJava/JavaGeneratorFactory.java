/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Axis" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.axis.wsdl.toJava;

import java.lang.reflect.Constructor;

import java.io.IOException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.OperationType;
import javax.wsdl.PortType;
import javax.wsdl.QName;
import javax.wsdl.Service;

import org.apache.axis.encoding.TypeMapping;
import org.apache.axis.encoding.DefaultSOAP12TypeMappingImpl;

import org.apache.axis.utils.JavaUtils;

import org.apache.axis.wsdl.gen.NoopGenerator;
import org.apache.axis.wsdl.gen.Generator;
import org.apache.axis.wsdl.gen.GeneratorFactory;

import org.apache.axis.wsdl.symbolTable.BaseTypeMapping;
import org.apache.axis.wsdl.symbolTable.BindingEntry;
import org.apache.axis.wsdl.symbolTable.DefinedElement;
import org.apache.axis.wsdl.symbolTable.Element;
import org.apache.axis.wsdl.symbolTable.MessageEntry;
import org.apache.axis.wsdl.symbolTable.Parameter;
import org.apache.axis.wsdl.symbolTable.Parameters;
import org.apache.axis.wsdl.symbolTable.PortTypeEntry;
import org.apache.axis.wsdl.symbolTable.ServiceEntry;
import org.apache.axis.wsdl.symbolTable.SchemaUtils;
import org.apache.axis.wsdl.symbolTable.SymbolTable;
import org.apache.axis.wsdl.symbolTable.SymTabEntry;
import org.apache.axis.wsdl.symbolTable.Type;
import org.apache.axis.wsdl.symbolTable.TypeEntry;

/**
* This is Wsdl2java's implementation of the GeneratorFactory
*/

public class JavaGeneratorFactory implements GeneratorFactory {
    protected Emitter     emitter;
    protected SymbolTable symbolTable;
    
    public static String COMPLEX_TYPE_FAULT = "ComplexTypeFault";
    public static String EXCEPTION_CLASS_NAME = "ExceptionClassName";

    /**
     * Default constructor.  Note that this class is unusable until setEmitter
     * is called.
     */

    public JavaGeneratorFactory() {
        addGenerators();
    } // ctor

    public JavaGeneratorFactory(Emitter emitter) {
        this.emitter = emitter;
        addGenerators();
    } // ctor

    public void setEmitter(Emitter emitter) {
        this.emitter = emitter;
    } // setEmitter

    private void addGenerators() {
        addMessageGenerators();
        addPortTypeGenerators();
        addBindingGenerators();
        addServiceGenerators();
        addTypeGenerators();
        addDefinitionGenerators();
    } // addGenerators

    /**
     * These addXXXGenerators are called by the constructor.
     * If an extender of this factory wants to CHANGE the set
     * of generators that are called per WSDL construct, they
     * should override these addXXXGenerators methods.  If all
     * an extender wants to do is ADD a generator, then the
     * extension should simply call addGenerator.
     * (NOTE:  It doesn't quite work this way, yet.  Only the
     * Definition generators fit this model at this point in
     * time.)
     */
    protected void addMessageGenerators() {
    } // addMessageGenerators

    protected void addPortTypeGenerators() {
    } // addPortTypeGenerators

    protected void addBindingGenerators() {
    } // addBindingGenerators

    protected void addServiceGenerators() {
    } // addServiceGenerators

    protected void addTypeGenerators() {
    } // addTypeGenerators

    protected void addDefinitionGenerators() {
        addGenerator(Definition.class, JavaDefinitionWriter.class); // for faults
        addGenerator(Definition.class, JavaDeployWriter.class); // for deploy.wsdd
        addGenerator(Definition.class, JavaUndeployWriter.class); // for undeploy.wsdd
    } // addDefinitionGenerators

    /**
     * Do the Wsdl2java generator pass:
     * - resolve name clashes
     * - construct signatures
     */
    public void generatorPass(Definition def, SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        javifyNames(symbolTable);
        setFaultContext(symbolTable);
        resolveNameClashes(symbolTable);
        determineInterfaceNames(symbolTable);
        if (emitter.isAllWanted()) {
            setAllReferencesToTrue();
        }
        else {
            ignoreNonSOAPBindings(symbolTable);
        }
        constructSignatures(symbolTable);
        determineIfHoldersNeeded(symbolTable);
    } // generatorPass

    /**
     * Since Wsdl2java doesn't emit anything for Messages, return the No-op generator.
     */
    private Writers messageWriters = new Writers();

    public Generator getGenerator(Message message, SymbolTable symbolTable) {
        MessageEntry mEntry = symbolTable.getMessageEntry(message.getQName());
        messageWriters.addStuff(new NoopGenerator(), mEntry, symbolTable);
        return messageWriters;
    } // getGenerator

    /**
     * Return Wsdl2java's JavaPortTypeWriter object.
     */
    private Writers portTypeWriters = new Writers();

    public Generator getGenerator(PortType portType, SymbolTable symbolTable) {
        PortTypeEntry ptEntry = symbolTable.getPortTypeEntry(portType.getQName());
        portTypeWriters.addStuff(new NoopGenerator(), ptEntry, symbolTable);
        return portTypeWriters;
    } // getGenerator

    /**
     * Return Wsdl2java's JavaBindingWriter object.
     */
    private Writers bindingWriters = new Writers();

    public Generator getGenerator(Binding binding, SymbolTable symbolTable) {
        Generator writer = new JavaBindingWriter(emitter, binding, symbolTable);
        BindingEntry bEntry = symbolTable.getBindingEntry(binding.getQName());
        bindingWriters.addStuff(writer, bEntry, symbolTable);
        return bindingWriters;
    } // getGenerator

    /**
     * Return Wsdl2java's JavaServiceWriter object.
     */
    private Writers serviceWriters = new Writers();

    public Generator getGenerator(Service service, SymbolTable symbolTable) {
        Generator writer = new JavaServiceWriter(emitter, service, symbolTable);
        ServiceEntry sEntry = symbolTable.getServiceEntry(service.getQName());
        serviceWriters.addStuff(writer, sEntry, symbolTable);
        return serviceWriters;
    } // getGenerator

    /**
     * Return Wsdl2java's JavaTypeWriter object.
     */
    private Writers typeWriters = new Writers();

    public Generator getGenerator(TypeEntry type, SymbolTable symbolTable) {
        Generator writer = new JavaTypeWriter(emitter, type, symbolTable);
        typeWriters.addStuff(writer, type, symbolTable);
        return typeWriters;
    } // getGenerator

    /**
     * Return Wsdl2java's JavaDefinitionWriter object.
     */
    private Writers defWriters = new Writers();

    public Generator getGenerator(Definition definition, SymbolTable symbolTable) {
        defWriters.addStuff(null, definition, symbolTable);
        return defWriters;
    } // getGenerator

    // Hack class just to play with the idea of adding writers
    class Writers implements Generator {
        Vector writers = new Vector();
        SymbolTable symbolTable = null;
        Generator baseWriter = null;

        // entry or def, but not both, will be a parameter.
        SymTabEntry entry = null;
        Definition def = null;

        public void addGenerator(Class writer) {
            writers.add(writer);
        } // addWriter

        public void addStuff(Generator baseWriter, SymTabEntry entry, SymbolTable symbolTable) {
            this.baseWriter = baseWriter;
            this.entry = entry;
            this.symbolTable = symbolTable;
        } // addStuff

        public void addStuff(Generator baseWriter, Definition def, SymbolTable symbolTable) {
            this.baseWriter = baseWriter;
            this.def = def;
            this.symbolTable = symbolTable;
        } // addStuff

        public void generate() throws IOException {
            if (baseWriter != null) {
                baseWriter.generate();
            }
            Class[] formalArgs = null;
            Object[] actualArgs = null;
            if (entry != null) {
                formalArgs = new Class[] {Emitter.class, entry.getClass(), SymbolTable.class};
                actualArgs = new Object[] {emitter, entry, symbolTable};
            }
            else {
                formalArgs = new Class[] {Emitter.class, Definition.class, SymbolTable.class};
                actualArgs = new Object[] {emitter, def, symbolTable};
            }
            for (int i = 0; i < writers.size(); ++i) {
                Class wClass = (Class) writers.get(i);
                Generator gen = null;
                try {
                    Constructor ctor = wClass.getConstructor(formalArgs);
                    gen = (Generator) ctor.newInstance(actualArgs);
                }
                catch (Throwable t) {
                    throw new IOException();
                }
                gen.generate();
            }
        } // generate
    } // class Writers

    public void addGenerator(Class wsdlClass, Class generator) {
        // This is just a hack right now... it just works with Service
        if (Message.class.isAssignableFrom(wsdlClass)) {
            messageWriters.addGenerator(generator);
        }
        else if (PortType.class.isAssignableFrom(wsdlClass)) {
            portTypeWriters.addGenerator(generator);
        }
        else if (Binding.class.isAssignableFrom(wsdlClass)) {
            bindingWriters.addGenerator(generator);
        }
        else if (Service.class.isAssignableFrom(wsdlClass)) {
            serviceWriters.addGenerator(generator);
        }
        else if (TypeEntry.class.isAssignableFrom(wsdlClass)) {
            typeWriters.addGenerator(generator);
        }
        else if (Definition.class.isAssignableFrom(wsdlClass)) {
            defWriters.addGenerator(generator);
        }
    } // addGenerator

    /**
     * Fill in the names of each SymTabEntry with the javaified name.
     * Note: This method also ensures that anonymous types are 
     * given unique java type names.
     */
    protected void javifyNames(SymbolTable symbolTable) {
        int uniqueNum = 0;
        HashMap anonQNames = new HashMap();
        Iterator it = symbolTable.getHashMap().values().iterator();
        while (it.hasNext()) {
            Vector v = (Vector) it.next();
            for (int i = 0; i < v.size(); ++i) {
                SymTabEntry entry = (SymTabEntry) v.elementAt(i);

                // Use the type or the referenced type's QName to generate the java name.      
                if (entry instanceof TypeEntry && entry.getName() == null) {
                    TypeEntry tEntry = (TypeEntry) entry;
                    String dims = tEntry.getDimensions();
                    TypeEntry refType = tEntry.getRefType();
                    while (refType != null) {
                        tEntry = refType;
                        dims += tEntry.getDimensions();
                        refType = tEntry.getRefType();
                    }
                    
                    // Get the QName to javify
                    QName typeQName = tEntry.getQName();
                    if ((typeQName.getLocalPart().indexOf(SymbolTable.ANON_TOKEN) >= 0) &&
                            (tEntry.getName() == null)) {
                        // This is an anonymous type name.
                        // Axis uses '>' as a nesting token to generate
                        // unique qnames for anonymous types.
                        // Only consider the localName after the last '>' when
                        // generating the java name
                        String localName = typeQName.getLocalPart();
                        localName = 
                            localName.substring(
                                localName.lastIndexOf(SymbolTable.ANON_TOKEN)+1);
                        typeQName = new QName(typeQName.getNamespaceURI(), localName);
                        // If there is already an existing type, there will be a 
                        // collision.  If there is an existing anon type, there will be a 
                        // collision.  In both cases, the java type name should be mangled.
                        TypeEntry existing = symbolTable.getType(typeQName);
                        if (anonQNames.get(typeQName) != null) {
                            localName += "Type" + uniqueNum++;
                            typeQName = new QName(typeQName.getNamespaceURI(), localName);
                        } 
                        anonQNames.put(typeQName, typeQName);
                        tEntry.setName(emitter.getJavaName(typeQName) + dims);
                    }
                    entry.setName(emitter.getJavaName(typeQName) + dims);
                }

                // If it is not a type, then use this entry's QName to generate its name.
                else {
                    entry.setName(emitter.getJavaName(entry.getQName()));
                }
            }
        }
    } // javifyNames

    /**
     * setFaultContext:
     * Processes the symbol table and sets the COMPLEX_TYPE_FAULT
     * on each TypeEntry that is a complexType and is referenced in
     * a fault message.  TypeEntries that are the base or derived
     * from such a TypeEntry are also marked with COMPLEX_TYPE_FAULT.
     * The containing MessageEntry is marked with cOMPLEX_TYPE_FAULT, and
     * all MessageEntries for faults are tagged with the
     * EXCEPTION_CLASS_NAME variable, which indicates the java exception
     * class name.
     * @param SymbolTable
     */
    private void setFaultContext(SymbolTable symbolTable) {
        Iterator it = symbolTable.getHashMap().values().iterator();
        while (it.hasNext()) {
            Vector v = (Vector) it.next();
            for (int i = 0; i < v.size(); ++i) {
                SymTabEntry entry = (SymTabEntry) v.elementAt(i);
                // Inspect each BindingEntry in the Symbol Table
                if (entry instanceof BindingEntry) {
                    BindingEntry bEntry = (BindingEntry) entry;
                    Binding binding = bEntry.getBinding();
                    // Get the associated PortType
                    PortTypeEntry ptEntry = 
                        symbolTable.getPortTypeEntry(
                            binding.getPortType().getQName());
                    PortType portType = ptEntry.getPortType();
                    Iterator operations = portType.getOperations().iterator();
                    // Inspect the Operations of the PortType
                    while(operations.hasNext()) {
                        Operation operation = (Operation) operations.next();
                        // Get the associated parameters of the operation.
                        Parameters parameters = bEntry.getParameters(operation);

                        // Inspect the faults of the operation
                        Iterator iFault = parameters.faults.values().iterator();
                        while(iFault.hasNext()) {
                            Fault fault = (Fault) iFault.next();
                            setFaultContext(fault, symbolTable);
                        }
                    }
                }
            }
        }
    } // setFaultContext

    /**
     * setFaultContext:
     * Helper routine for the setFaultContext method above.
     * Examines the indicated fault and sets COMPLEX_TYPE_FAULT
     * and EXCEPTION_CLASS_NAME as appropriate.
     * @param Fault to analyze
     * @param SymbolTable 
     */
    private void setFaultContext(Fault fault,
                                 SymbolTable symbolTable) {
        Vector parts = new Vector();
        // Get the parts of the fault's message.
        // An IOException is thrown if the parts cannot be
        // processed.  Skip such parts for this analysis
        try {
            symbolTable.getParametersFromParts(
                parts, 
                fault.getMessage().getOrderedParts(null),
                false,
                fault.getName(),
                "unknown");
        } catch (IOException e) {}
        
        // Inspect each TypeEntry referenced in a Fault Message Part
        String exceptionClassName = null;
        for(int j=0; j < parts.size(); j++) {
            TypeEntry te = ((Parameter)(parts.elementAt(j))).getType();
            if (te.getBaseType() != null ||
                te.isSimpleType()) {
                // Simple Type Exception
            } else {
                // Complex Type Exception
                Boolean isComplexFault = (Boolean) te.getDynamicVar(
                    JavaGeneratorFactory.COMPLEX_TYPE_FAULT);
                if (isComplexFault == null ||
                    !isComplexFault.booleanValue()) {
                    te.setDynamicVar(
                        JavaGeneratorFactory.COMPLEX_TYPE_FAULT, 
                        new Boolean(true));
                    // Mark all derived types as Complex Faults
                    HashSet derivedSet =
                        org.apache.axis.wsdl.symbolTable.Utils.getDerivedTypes(
                            te, symbolTable);
                    Iterator derivedI = derivedSet.iterator();
                    while(derivedI.hasNext()) {
                        TypeEntry derivedTE = (TypeEntry)
                            derivedI.next();
                        derivedTE.setDynamicVar(
                            JavaGeneratorFactory.COMPLEX_TYPE_FAULT, 
                            new Boolean(true));
                    }
                    // Mark all base types as Complex Faults
                    TypeEntry base = SchemaUtils.getComplexElementExtensionBase(
                        te.getNode(),
                        symbolTable);
                    while (base != null) {
                        base.setDynamicVar(
                            JavaGeneratorFactory.COMPLEX_TYPE_FAULT, 
                            new Boolean(true));
                        base = SchemaUtils.getComplexElementExtensionBase(
                            base.getNode(),
                            symbolTable);
                    }
                }
                exceptionClassName = emitter.getJavaName(te.getQName());
            }
        }
        // Set the name of the exception and
        // whether the exception is a complex type
        MessageEntry me = symbolTable.getMessageEntry(
            fault.getMessage().getQName());
        if (me != null) {
            if (exceptionClassName != null) {
                me.setDynamicVar(
                                 JavaGeneratorFactory.COMPLEX_TYPE_FAULT, 
                                 new Boolean(true));
                me.setDynamicVar(
                                 JavaGeneratorFactory.EXCEPTION_CLASS_NAME, 
                                 exceptionClassName);
            } else {
                me.setDynamicVar(
                                 JavaGeneratorFactory.EXCEPTION_CLASS_NAME, 
                                 emitter.getJavaName(me.getQName()));
            }
            
        }
    }

    protected void determineInterfaceNames(SymbolTable symbolTable) {
        Iterator it = symbolTable.getHashMap().values().iterator();
        while (it.hasNext()) {
            Vector v = (Vector) it.next();
            for (int i = 0; i < v.size(); ++i) {
                SymTabEntry entry = (SymTabEntry) v.elementAt(i);
                if (entry instanceof BindingEntry) {
                    // The SEI (Service Endpoint Interface) name
                    // is normally the portType name.
                    // But the binding info MIGHT force the SEI
                    // name to be the binding name.
                    BindingEntry bEntry = (BindingEntry) entry;
                    String seiName = null;
                    if (bEntry.hasLiteral() && symbolTable.isWrapped()) {
                        seiName = bEntry.getName();
                    }
                    else {
                        PortTypeEntry ptEntry = symbolTable.getPortTypeEntry(
                                bEntry.getBinding().getPortType().getQName());
                        seiName = ptEntry.getName();
                    }                    bEntry.setDynamicVar(JavaBindingWriter.INTERFACE_NAME, seiName);
                }
            }
        }
    } // determineInterfaceNames

    /**
     * Messages, PortTypes, Bindings, and Services can share the same name.  If they do in this
     * Definition, force their names to be suffixed with _PortType and _Service, respectively.
     */
    protected void resolveNameClashes(SymbolTable symbolTable) {

        // Keep a list of anonymous types so we don't try to resolve them twice.
        HashSet anonTypes = new HashSet();

        Iterator it = symbolTable.getHashMap().values().iterator();
        while (it.hasNext()) {
            Vector v = new Vector((Vector) it.next());  // New vector we can temporarily add to it

            // Remove MessageEntries since they are not mapped
            int index = 0;
            while (index < v.size()) {
                if (v.elementAt(index) instanceof MessageEntry) {
                    v.removeElementAt(index);
                } else {
                    index++;
                }
            }

            if (v.size() > 1) {
                boolean resolve = true;
                // Common Special Case:
                // If a Type and Element have the same QName, and the Element
                // uses type= to reference the Type, then they are the same class so 
                // don't bother mangling.
                if (v.size() == 2 &&
                    ((v.elementAt(0) instanceof Element &&
                      v.elementAt(1) instanceof Type) ||
                     (v.elementAt(1) instanceof Element &&
                      v.elementAt(0) instanceof Type))) {
                    Element e = null;
                    if (v.elementAt(0) instanceof Element) {
                        e = (Element)v.elementAt(0);
                    } else {
                        e = (Element)v.elementAt(1);
                    }
                    QName eType = Utils.getNodeTypeRefQName(e.getNode(), "type");
                    if (eType != null && eType.equals(e.getQName()))
                        resolve = false;
                }

                // Other Special Case:
                // If the names are already different, no mangling is needed.
                if (resolve) {
                    resolve = false;  // Assume false
                    String name = null;
                    for (int i = 0; i < v.size() && !resolve; ++i) {
                        SymTabEntry entry = (SymTabEntry) v.elementAt(i);
                         if (entry instanceof MessageEntry ||
                             entry instanceof BindingEntry) {
                             ; // Don't process these
                         } else if (name== null) {
                             name = entry.getName();
                         } else if (name.equals(entry.getName())) {
                             resolve = true;  // Need to do resolution
                         } 

                    }
                }

                // Full Mangle if resolution is necessary.
                if (resolve) {
                    boolean firstType = true;
                    for (int i = 0; i < v.size(); ++i) {
                        SymTabEntry entry = (SymTabEntry) v.elementAt(i);
                        if (entry instanceof Element) {
                            entry.setName(mangleName(entry.getName(),
                                    "_ElemType"));

                            // If this global element was defined using 
                            // an anonymous type, then need to change the
                            // java name of the anonymous type to match.
                            QName anonQName = new QName(entry.getQName().getNamespaceURI(),
                                                        SymbolTable.ANON_TOKEN +
                                                        entry.getQName().getLocalPart());
                            TypeEntry anonType = symbolTable.getType(anonQName);
                            if (anonType != null) {
                                anonType.setName(entry.getName());
                                anonTypes.add(anonType);
                            }
                        }
                        else if (entry instanceof TypeEntry) {
                            // Search all other types for java names that match this one.
                            // The sameJavaClass method returns true if the java names are
                            // the same (ignores [] ).
                            if (firstType) {
                                firstType = false;
                                Vector types = symbolTable.getTypes();
                                for (int j = 0; j < types.size(); ++j) {
                                    TypeEntry type = (TypeEntry)
                                            types.elementAt(j);
                                    if (type != entry && 
                                            !(type instanceof Element) &&
                                            type.getBaseType() == null &&
                                            sameJavaClass(
                                                    ((Type) entry).getName(),
                                                    type.getName())) {
                                        v.add(type);  
                                    }
                                }
                            }
                            // If this is an anonymous type, it's name was resolved in
                            // the previous if block.  Don't reresolve it.
                            if (!anonTypes.contains(entry)) {
                                entry.setName(mangleName(entry.getName(), "_Type"));
                            }
                        }
                        else if (entry instanceof PortTypeEntry) {
                            entry.setName(mangleName(entry.getName(), "_Port"));
                        }
                        else if (entry instanceof ServiceEntry) {
                            entry.setName(mangleName(entry.getName(),
                                    "_Service"));
                        }
                        // else if (entry instanceof MessageEntry) {
                        //     we don't care about messages
                        // }
                        else if (entry instanceof BindingEntry) {
                            BindingEntry bEntry = (BindingEntry) entry;

                            // If there is no literal use, then we never see a
                            // class named directly from the binding name.  They
                            // all have suffixes:  Stub, Skeleton, Impl.
                            // If there IS literal use, then the SDI will be
                            // named after the binding name, so there is the
                            // possibility of a name clash.
                            if (bEntry.hasLiteral()) {
                                entry.setName(mangleName(entry.getName(),
                                        "_Binding"));
                            }
                        }
                    }
                }
            }
        }
    } // resolveNameClashes

    /**
     * Change the indicated type name into a mangled form using the mangle string.
     */
    private String mangleName(String name, String mangle) {
        int index = name.indexOf("[");
        if (index >= 0) {
            String pre = name.substring(0, index);
            String post = name.substring(index);
            return pre + mangle + post;
        }
        else
            return name + mangle;
    }

    /**
     * Returns true if same java class, ignore []                                 
     */
    private boolean sameJavaClass(String one, String two) {     
        int index1 = one.indexOf("[");
        int index2 = two.indexOf("[");
        if (index1 > 0)
            one = one.substring(0, index1);
        if (index2 > 0)
            two = two.substring(0, index2);
        return one.equals(two);
    }

    /**
     * The --all flag is set on the command line (or generateAll(true) is called
     * on WSDL2Java). Set all symbols as referenced (except nonSOAP bindings
     * which we don't know how to deal with).
     */
    protected void setAllReferencesToTrue() {
        Iterator it = symbolTable.getHashMap().values().iterator();
        while (it.hasNext()) {
            Vector v = (Vector) it.next();
            for (int i = 0; i < v.size(); ++i) {
                SymTabEntry entry = (SymTabEntry) v.elementAt(i);
                if (entry instanceof BindingEntry &&
                        ((BindingEntry) entry).getBindingType() !=
                        BindingEntry.TYPE_SOAP) {
                    entry.setIsReferenced(false);
                }
                else {
                    entry.setIsReferenced(true);
                }
            }
        }
    } // setAllReferencesToTrue

    /**
     * If a binding's type is not TYPE_SOAP, then we don't use that binding
     * or that binding's portType.
     */
    protected void ignoreNonSOAPBindings(SymbolTable symbolTable) {

        // Look at all uses of the portTypes.  If none of the portType's bindings are of type
        // TYPE_SOAP, then turn off that portType's isReferenced flag.

        Vector unusedPortTypes = new Vector();
        Vector usedPortTypes = new Vector();

        Iterator it = symbolTable.getHashMap().values().iterator();
        while (it.hasNext()) {
            Vector v = (Vector) it.next();
            for (int i = 0; i < v.size(); ++i) {
                SymTabEntry entry = (SymTabEntry) v.elementAt(i);
                if (entry instanceof BindingEntry) {
                    BindingEntry bEntry = (BindingEntry) entry;
                    Binding binding = bEntry.getBinding();
                    PortType portType = binding.getPortType();
                    PortTypeEntry ptEntry =
                      symbolTable.getPortTypeEntry(portType.getQName());

                    if (bEntry.getBindingType() == BindingEntry.TYPE_SOAP) {
                        // If a binding is of type TYPE_SOAP, then mark its portType used
                        // (ie., add it to the usedPortTypes list.  If the portType was
                        // previously marked as unused, unmark it (in other words, remove it
                        // from the unusedPortTypes list).
                        usedPortTypes.add(ptEntry);
                        if (unusedPortTypes.contains(ptEntry)) {
                            unusedPortTypes.remove(ptEntry);
                        }
                    }
                    else {
                        bEntry.setIsReferenced(false);

                        // If a binding is not of type TYPE_SOAP, then mark its portType as
                        // unused ONLY if it hasn't already been marked as used.
                        if (!usedPortTypes.contains(ptEntry)) {
                            unusedPortTypes.add(ptEntry);
                        }
                    }
                }
            }
        }

        // Go through all the portTypes that are marked as unused and set their isReferenced flags
        // to false.
        for (int i = 0; i < unusedPortTypes.size(); ++i) {
            PortTypeEntry ptEntry = (PortTypeEntry) unusedPortTypes.get(i);
            ptEntry.setIsReferenced(false);
        }
    } // ignoreNonSOAPBindings

    protected void constructSignatures(SymbolTable symbolTable) {
        Iterator it = symbolTable.getHashMap().values().iterator();
        while (it.hasNext()) {
            Vector v = (Vector) it.next();
            for (int i = 0; i < v.size(); ++i) {
                SymTabEntry entry = (SymTabEntry) v.elementAt(i);
                if (entry instanceof BindingEntry) {
                    BindingEntry bEntry = (BindingEntry) entry;
                    Binding binding = bEntry.getBinding();
                    PortTypeEntry ptEntry = 
                            symbolTable.getPortTypeEntry(binding.getPortType().getQName());
                    PortType portType = ptEntry.getPortType();
                    Iterator operations = portType.getOperations().iterator();
                    while(operations.hasNext()) {
                        Operation operation = (Operation) operations.next();
                        OperationType type = operation.getStyle();
                        String name = operation.getName();
                        Parameters parameters = bEntry.getParameters(operation);
                        if (type == OperationType.SOLICIT_RESPONSE) {
                            parameters.signature = "    // " + JavaUtils.getMessage(
                                    "invalidSolResp00", name);
                            System.err.println(JavaUtils.getMessage(
                                    "invalidSolResp00", name));
                        }
                        else if (type == OperationType.NOTIFICATION) {
                            parameters.signature = "    // " + JavaUtils.getMessage(
                                    "invalidNotif00", name);
                            System.err.println(JavaUtils.getMessage(
                                    "invalidNotif00", name));
                        }
                        else { // ONE_WAY or REQUEST_RESPONSE
                            parameters.signature = constructSignature( 
                                   parameters, name);
                        }
                    }
                }
            }
        }
    } // constructSignatures

    /**
     * Construct the signature, which is used by both the interface and the stub.
     */
    private String constructSignature(Parameters parms, String opName) {
        String name  = Utils.xmlNameToJava(opName);
        String ret = parms.returnType == null ? "void" : parms.returnType.getName();
        String signature = "    public " + ret + " " + name + "(";

        boolean needComma = false;

        for (int i = 0; i < parms.list.size(); ++i) {
            Parameter p = (Parameter) parms.list.get(i);

            if (needComma) {
                signature = signature + ", ";
            }
            else {
                needComma = true;
            }

            String javifiedName = Utils.xmlNameToJava(p.getName());
            if (p.getMode() == Parameter.IN) {
                signature = signature + p.getType().getName() + " " + javifiedName;
            }
            else {
                signature = signature + Utils.holder(p.getType(), emitter) + " "
                        + javifiedName;
            }
        }
        signature = signature + ") throws java.rmi.RemoteException";
        if (parms.faults != null) {
            // Collect the list of faults into a single string, separated by commas.
            
            Iterator i = parms.faults.values().iterator();
            while (i.hasNext()) {
                Fault fault = (Fault) i.next();
                String exceptionName =
                  Utils.getFullExceptionName(fault, emitter);
                signature = signature + ", " + exceptionName;
            }
        }
        return signature;
    } // constructSignature

    /**
     * Find all inout/out parameters and add a flag to the Type of that parameter saying a holder
     * is needed.
     */
    protected void determineIfHoldersNeeded(SymbolTable symbolTable) {
        Iterator it = symbolTable.getHashMap().values().iterator();
        while (it.hasNext()) {
            Vector v = (Vector) it.next();
            for (int i = 0; i < v.size(); ++i) {
                if (v.get(i) instanceof BindingEntry) {
                    // If entry is a BindingEntry, look at all the Parameters
                    // in its portType
                    BindingEntry bEntry = (BindingEntry) v.get(i);
//                    PortTypeEntry ptEntry = 
//                            symbolTable.getPortTypeEntry(bEntry.getBinding().getPortType().getQName());
                    Iterator operations =
                            bEntry.getParameters().values().iterator();
                    while (operations.hasNext()) {
                        Parameters parms = (Parameters) operations.next();
                        for (int j = 0; j < parms.list.size(); ++j) {
                            Parameter p =
                                    (Parameter)parms.list.get(j);
                            
                            // If the given parameter is an inout or out parameter, then
                            // set a HOLDER_IS_NEEDED flag using the dynamicVar design.
                            if (p.getMode() != Parameter.IN) {
                                p.getType().setDynamicVar(
                                        JavaTypeWriter.HOLDER_IS_NEEDED,
                                        new Boolean(true));

                                // If the type is a DefinedElement, need to 
                                // set HOLDER_IS_NEEDED on the anonymous type.
                                QName anonQName = SchemaUtils.
                                    getElementAnonQName(p.getType().getNode());
                                if (anonQName != null) {
                                    TypeEntry anonType = 
                                        symbolTable.getType(anonQName);
                                    if (anonType != null) {
                                        anonType.setDynamicVar(
                                            JavaTypeWriter.HOLDER_IS_NEEDED,
                                            new Boolean(true));
                                    }                                    
                                }
                            }
                        }
                    }
                }
            }
        }
    } // determineIfHoldersNeeded

    /**
     * Get TypeMapping to use for translating
     * QNames to java base types
     */
    BaseTypeMapping btm = null;
    public void setBaseTypeMapping(BaseTypeMapping btm) {
        this.btm = btm;
    }
    public BaseTypeMapping getBaseTypeMapping() {
        if (btm == null) {
            btm = new BaseTypeMapping() {
                    TypeMapping defaultTM = DefaultSOAP12TypeMappingImpl.create();
                    public String getBaseName(QName qNameIn) {
                        javax.xml.rpc.namespace.QName qName = 
                            new javax.xml.rpc.namespace.QName(
                              qNameIn.getNamespaceURI(),                                 
                              qNameIn.getLocalPart());
                        Class cls = defaultTM.getClassForQName(qName);
                        if (cls == null)
                            return null;
                        else 
                            return JavaUtils.getTextClassName(cls.getName());
                    }
                };    
        }
        return btm;
    }

} // class JavaGeneratorFactory
