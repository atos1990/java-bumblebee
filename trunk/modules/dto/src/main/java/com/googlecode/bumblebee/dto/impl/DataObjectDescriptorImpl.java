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

package com.googlecode.bumblebee.dto.impl;

import com.googlecode.bumblebee.dto.DataObjectDescriptor;
import com.googlecode.bumblebee.dto.ValueDescriptor;
import net.sf.jdpa.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Andreas Nilsson
 */
public class DataObjectDescriptorImpl<T> implements DataObjectDescriptor<T>, Serializable {

    public Class<T> objectType = null;

    private Map<String, ValueDescriptorImpl> valueDescriptors = new HashMap<String, ValueDescriptorImpl>();

    public DataObjectDescriptorImpl(@NotNull Class<T> objectType) {
        this.objectType = objectType;
    }

    public Class<T> getObjectType() {
        return objectType;
    }

    public List<ValueDescriptor> getValueDescriptors() {
        return new ArrayList<ValueDescriptor>(valueDescriptors.values());
    }

    public void addValueDescriptor(@NotNull ValueDescriptorImpl valueDescriptor) {
        valueDescriptors.put(valueDescriptor.getProperty(), valueDescriptor);
    }

    public boolean isPropertyDefined(@NotNull String propertyName) {
        return valueDescriptors.containsKey(propertyName);
    }

    @Override
    public int hashCode() {
        return objectType.hashCode() ^ valueDescriptors.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !o.getClass().equals(getClass())) {
            return false;
        } else if (o == this) {
            return true;
        } else {
            DataObjectDescriptorImpl dataObjectDescriptor = (DataObjectDescriptorImpl) o;

            return objectType.equals(dataObjectDescriptor.objectType) &&
                    valueDescriptors.equals(dataObjectDescriptor.valueDescriptors);
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + " <objectType = " + objectType.getName()
                + ", valueDescriptors = " + valueDescriptors.toString() + ">";
    }
}
