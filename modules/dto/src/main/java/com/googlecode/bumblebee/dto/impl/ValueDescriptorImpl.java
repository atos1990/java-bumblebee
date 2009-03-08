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

import com.googlecode.bumblebee.dto.ValueDescriptor;
import net.sf.jdpa.NotEmpty;
import net.sf.jdpa.NotNull;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * @author Andreas Nilsson
 */
public class ValueDescriptorImpl implements ValueDescriptor, Serializable {

    private Class<?> propertyType;

    private Method accessor;

    private String property;

    private String expression;

    public ValueDescriptorImpl(@NotNull Class<?> propertyType, @NotNull Method accessor,
                               @NotEmpty String property, @NotEmpty String expression) {
        this.propertyType = propertyType;
        this.accessor = accessor;
        this.property = property;
        this.expression = expression;
    }

    public Method getAccessor() {
        return accessor;
    }

    public Class<?> getPropertyType() {
        return propertyType;
    }

    public String getProperty() {
        return property;
    }

    public String getExpression() {
        return expression;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(propertyType)
                .append(accessor)
                .append(property)
                .append(expression)
                .toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !o.getClass().equals(getClass())) {
            return false;
        } else if (o == this) {
            return true;
        } else {
            ValueDescriptorImpl descriptor = (ValueDescriptorImpl) o;

            return new EqualsBuilder()
                    .append(propertyType, descriptor.propertyType)
                    .append(accessor, descriptor.accessor)
                    .append(property, descriptor.property)
                    .append(expression, descriptor.expression)
                    .isEquals();
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("propertyType", propertyType)
                .append("accessor", accessor)
                .append("property", property)
                .append("expression", expression)
                .toString();
    }
}
