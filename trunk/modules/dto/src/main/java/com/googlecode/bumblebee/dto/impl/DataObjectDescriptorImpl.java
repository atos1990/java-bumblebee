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
import java.util.*;
import java.lang.annotation.Annotation;

/**
 * @author Andreas Nilsson
 */
public class DataObjectDescriptorImpl<T> implements DataObjectDescriptor<T>, Serializable {

    public Class<T> objectType = null;

    private Map<String, ValueDescriptorImpl> valueDescriptors = new HashMap<String, ValueDescriptorImpl>();

    private Set<Class<? extends Annotation>> inheritedAnnotations = new LinkedHashSet<Class<? extends Annotation>>();

    public DataObjectDescriptorImpl(@NotNull Class<T> objectType) {
        this.objectType = objectType;
    }

    public Class<T> getObjectType() {
        return objectType;
    }

    public List<ValueDescriptor> getValueDescriptors() {
        return new ArrayList<ValueDescriptor>(valueDescriptors.values());
    }

    @SuppressWarnings("unchecked")
    public Class<? extends Annotation>[] getInheritedAnnotations() {
        return inheritedAnnotations.toArray(new Class[inheritedAnnotations.size()]);
    }

    public boolean isAnnotationTypeInherited(@NotNull Class<? extends Annotation> annotationType) {
        for (Class<? extends Annotation> inheritedAnnotationType : inheritedAnnotations) {
            if (inheritedAnnotationType.isAssignableFrom(annotationType)) {
                return true;
            }
        }

        return false;
    }

    public void addValueDescriptor(@NotNull ValueDescriptorImpl valueDescriptor) {
        valueDescriptors.put(valueDescriptor.getProperty(), valueDescriptor);
    }

    public void addInheritedAnnotation(@NotNull Class<? extends Annotation> annotationType) {
        inheritedAnnotations.add(annotationType);
    }

    public boolean isPropertyDefined(@NotNull String propertyName) {
        return valueDescriptors.containsKey(propertyName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataObjectDescriptorImpl that = (DataObjectDescriptorImpl) o;

        if (!inheritedAnnotations.equals(that.inheritedAnnotations)) return false;
        if (!objectType.equals(that.objectType)) return false;
        if (!valueDescriptors.equals(that.valueDescriptors)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = objectType.hashCode();
        result = 31 * result + valueDescriptors.hashCode();
        result = 31 * result + inheritedAnnotations.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DataObjectDescriptorImpl{" +
                "objectType=" + objectType +
                ", valueDescriptors=" + valueDescriptors +
                ", inheritedAnnotations=" + inheritedAnnotations +
                '}';
    }
}
