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

import java.lang.annotation.*;

/**
 * Denotes a data transfer object (DTO, TO, VO). This annotation must be present on objects
 * that are dedicated to transferring data between a domain object and a transfer object/value object.
 *
 * @author Andreas Nilsson
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DataObject {

    /**
     * Defines whether or not the implementation class should inherit the annotations defined
     * in the data object interface. This might be necessary of you e.g. intend to use your
     * objects in conjunction with JAXB.
     * @return Whether or not annotations should be inherited by the implementation class.
     */
    Class<? extends Annotation>[] inheritedAnnotations() default { Annotation.class };

}
