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

import net.sf.jdpa.NotNull;

import java.util.List;

/**
 * A <code>DataObjectDescriptor</code> constitutes a descriptor of a data object, i.e. java interface
 * with the <code>DataObject</code> annotation.
 *
 * @author Andreas Nilsson
 * @param <T> The class described by a descriptor instance.
 */
public interface DataObjectDescriptor<T> {

    @NotNull
    public Class<T> getObjectType();

    @NotNull
    public List<ValueDescriptor> getValueDescriptors();

    public boolean isPropertyDefined(String propertyName);

}
