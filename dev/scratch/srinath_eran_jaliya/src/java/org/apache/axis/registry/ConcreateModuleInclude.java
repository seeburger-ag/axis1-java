/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

package org.apache.axis.registry;

import java.util.ArrayList;

/**
 * @author hemapani@opensource.lk
 */
public class ConcreateModuleInclude implements ModuleInclude {
    private ArrayList modules;

    public ConcreateModuleInclude(){
        this.modules = new ArrayList();
    }
    public Module getModule(int index) {
        return (Module)modules.get(index);
    }
    public int getModuleCount() {
       return modules.size();
    }
    public synchronized void addModule(Module module) {
        modules.add(module);
    }
}