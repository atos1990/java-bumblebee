// Copyright 2009 The original authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlecode.bumblebee.dto;

import com.googlecode.bumblebee.dto.impl.AssemblerImpl;

import java.util.Map;

/**
 * @author Andreas Nilsson
 */
public class Bumblebee {

    public static final Assembler DEFAULT_ASSEMBLER = new AssemblerImpl();

    public static <T> AssembleBuilder<T> assemble(final Class<T> dataObjectClass) {
        return new AssembleBuilder<T>() {
            @SuppressWarnings("unchecked")
            public <T> T from(Object source) {
                return (T) DEFAULT_ASSEMBLER.assemble(source, dataObjectClass);
            }
        };
    }

    public static <T> T create(final Class<T> dataObjectClass, PropertyValue ... values) {
        return DEFAULT_ASSEMBLER.assemble(dataObjectClass, values);
    }

    public static PropertyValue with(String propertyName, Object propertyValue) {
        return new PropertyValue(propertyName, propertyValue);
    }

    @SuppressWarnings("unchecked")
    public static<T> Class<? extends T> implementationOf(Class<T> dataObjectDefinition) {
        return (Class<? extends T>) DEFAULT_ASSEMBLER.getDataObjectImplementation(dataObjectDefinition);
    }

    public static interface AssembleBuilder<T> {

        public <T> T from(Object source);

    }

}
