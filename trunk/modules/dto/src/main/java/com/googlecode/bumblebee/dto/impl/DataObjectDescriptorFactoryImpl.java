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

import com.googlecode.bumblebee.beans.BeanUtil;
import com.googlecode.bumblebee.beans.InvalidAccessorException;
import com.googlecode.bumblebee.dto.*;
import net.sf.jdpa.NotNull;

import java.lang.reflect.Method;
import java.lang.annotation.Annotation;

/**
 * @author Andreas Nilsson
 */
public class DataObjectDescriptorFactoryImpl implements DataObjectDescriptorFactory {

    public <T> DataObjectDescriptor<T> createDataObjectDescriptor(@NotNull Class<T> dataObjectClass) {
        DataObjectDescriptorImpl<T> descriptor = null;

        if (!dataObjectClass.isInterface()) {
            throw new DataObjectValidationException("Class '" + dataObjectClass.getName() + "' must be an interface.");
        } else {
            DataObject dataObjectAnnotation = dataObjectClass.getAnnotation(DataObject.class);

            if (dataObjectAnnotation == null) {
                throw new DataObjectValidationException("Class '" + dataObjectClass.getName() + "' does not denote a DataObject. " +
                        "Please add @com.googlecode.bumblebee.dto.DataObject to your class.");
            } else {
                descriptor = new DataObjectDescriptorImpl<T>(dataObjectClass);

                scanTypeAnnotations(descriptor, dataObjectClass);
                scanInterface(descriptor, dataObjectClass);
            }
        }

        return descriptor;
    }

    protected void scanInterface(DataObjectDescriptorImpl descriptor, Class<?> dataObjectClass) {
        for (Method method : dataObjectClass.getDeclaredMethods()) {
            Value value = method.getAnnotation(Value.class);

            if (value == null) {
                throw new DataObjectValidationException("Method " + dataObjectClass.getSimpleName()
                        + "." + method.getName() + " is not a value-method.");
            } else {
                String propertyName = null;

                try {
                    propertyName = BeanUtil.getPropertyName(method);
                } catch (InvalidAccessorException e) {
                    throw new DataObjectValidationException("Property " + dataObjectClass.getSimpleName() + "."
                            + method.getName() + " is not a valid value method.", e);
                }

                if (!descriptor.isPropertyDefined(propertyName)) {
                    String expression = value.value();

                    if (expression.length() == 0) {
                        expression = propertyName;
                    }

                    descriptor.addValueDescriptor(new ValueDescriptorImpl(method.getReturnType(), method, propertyName, expression));
                }
            }
        }

        for (Class<?> superInterface : dataObjectClass.getInterfaces()) {
            scanInterface(descriptor, superInterface);
        }
    }

    protected void scanTypeAnnotations(DataObjectDescriptorImpl descriptor, Class<?> dataObjectClass) {
        for (Class<? extends Annotation> annotationType : dataObjectClass.getAnnotation(DataObject.class).inheritedAnnotations()) {
            descriptor.addInheritedAnnotation(annotationType);
        }
    }

}
