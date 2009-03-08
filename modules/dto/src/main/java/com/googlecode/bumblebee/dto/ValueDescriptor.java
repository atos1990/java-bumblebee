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

import java.lang.reflect.Method;

/**
 * A <code>ValueDescriptor</code> is used to describe a value of a data object. It contains information
 * about the target property of the data object and the expression used to extract the data from the
 * source object.
 *
 * @author Andreas Nilsson
 */
public interface ValueDescriptor {

    /**
     * Returns the accessor of the value in the data object class.
     *
     * @return The value accessor.
     */
    public Method getAccessor();

    /**
     * Returns the type of the property in the data object interface. This corresponds to the
     * return type of the corresponding accessor.
     *
     * @return The type of the property in the DTO.
     */
    public Class<?> getPropertyType();

    /**
     * Returns the property in the data object that the expression should be mapped to.
     *
     * @return The target property of the data object.
     */
    public String getProperty();

    /**
     * Returns the expression used to dig out the original value from the source object.
     *
     * @return An expression that defines what data should be fetched from the source object.
     */
    public String getExpression();

}
