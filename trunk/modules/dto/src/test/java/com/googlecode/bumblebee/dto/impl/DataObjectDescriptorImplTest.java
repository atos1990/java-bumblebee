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

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.annotation.Annotation;

import com.googlecode.bumblebee.dto.DataObjectDescriptor;

/**
 * @author Andreas Nilsson
 */
public class DataObjectDescriptorImplTest {

    private Method valueAccessor = null;

    @Before
    public void setup() throws Exception {
        valueAccessor = DataObjectWithStringProperty.class.getDeclaredMethod("getProperty");
    }

    public static class ConstructorTest extends DataObjectDescriptorImplTest {

        @Test(expected = IllegalArgumentException.class)
        public void nullClassShouldNotBeAccepted() {
            new DataObjectDescriptorImpl<String>(null);
        }

        @Test
        public void objectTypeShouldBeRetained() {
            assertEquals(String.class, new DataObjectDescriptorImpl<String>(String.class).getObjectType());
        }
    }


    public static class AddValueDescriptorTest extends DataObjectDescriptorImplTest {
        @Test(expected = IllegalArgumentException.class)
        public void nullValueShouldNotBeAccepted() {
            new DataObjectDescriptorImpl<String>(String.class).addValueDescriptor(null);
        }
    }


    public static class IsPropertyDefinedTest extends DataObjectDescriptorImplTest {

        @Test(expected = IllegalArgumentException.class)
        public void nullValueShouldNotBeAccepted() {
            new DataObjectDescriptorImpl<String>(String.class).isPropertyDefined(null);
        }

        @Test
        public void falseShouldBeReturnedIfNoValueHasBeenAdded() {
            assertFalse(new DataObjectDescriptorImpl<String>(String.class).isPropertyDefined("foo"));
        }

        @Test
        public void trueShouldBeReturnedIfValueHasBeenDefined() throws Exception {
            DataObjectDescriptorImpl descriptor = new DataObjectDescriptorImpl<String>(String.class);

            descriptor.addValueDescriptor(new ValueDescriptorImpl(String.class,
                    DataObjectWithStringProperty.class.getDeclaredMethod("getProperty"), "foo", "bar"));

            assertTrue(descriptor.isPropertyDefined("foo"));
        }
    }

    @Test
    public void emptyDescriptorsShouldBeEqualIfObjectTypesAreEqual() {
        assertEquals(new DataObjectDescriptorImpl<String>(String.class), new DataObjectDescriptorImpl<String>(String.class));
    }

    @Test
    public void emptyDescriptorsShouldNotBeEqualIfObjectTypesAreDifferent() {
        assertFalse(new DataObjectDescriptorImpl<String>(String.class).equals(new DataObjectDescriptorImpl<Integer>(Integer.class)));
    }

    @Test
    public void descriptorShouldNotBeEqualToNull() {
        assertFalse(new DataObjectDescriptorImpl<String>(String.class).equals(null));
    }

    @Test
    public void descriptorShouldNotBeEqualToString() {
        assertFalse(new DataObjectDescriptorImpl<String>(String.class).equals("String"));
    }

    @Test
    public void descriptorShouldBeEqualToItself() {
        DataObjectDescriptorImpl descriptor = new DataObjectDescriptorImpl<String>(String.class);

        assertEquals(descriptor, descriptor);
    }

    @Test
    public void descriptorWithDifferentValuesShouldNotBeEqual() {
        DataObjectDescriptorImpl descriptor1 = new DataObjectDescriptorImpl<String>(String.class);
        DataObjectDescriptorImpl descriptor2 = new DataObjectDescriptorImpl<String>(String.class);

        descriptor1.addValueDescriptor(new ValueDescriptorImpl(String.class, valueAccessor, "foo", "foo"));
        assertFalse(descriptor1.equals(descriptor2));

        descriptor2.addValueDescriptor(new ValueDescriptorImpl(String.class, valueAccessor, "foo", "bar"));
        assertFalse(descriptor1.equals(descriptor2));
    }

    @Test
    public void descriptorsWithEqualTypesAndValuesShouldBeEqual() {
        DataObjectDescriptorImpl descriptor1 = new DataObjectDescriptorImpl<String>(String.class);
        DataObjectDescriptorImpl descriptor2 = new DataObjectDescriptorImpl<String>(String.class);

        descriptor1.addValueDescriptor(new ValueDescriptorImpl(String.class, valueAccessor, "foo", "bar"));
        descriptor2.addValueDescriptor(new ValueDescriptorImpl(String.class, valueAccessor, "foo", "bar"));

        assertEquals(descriptor1, descriptor2);
    }

    @Test
    public void emptyDescriptorsWithTheSameTypeShouldHaveEqualHashCodes() {
        assertEquals(new DataObjectDescriptorImpl<String>(String.class).hashCode(),
                new DataObjectDescriptorImpl<String>(String.class).hashCode());
    }

    @Test
    public void descriptorsWithDifferentTypesShouldHaveDifferentHashCodes() {
        assertFalse(new DataObjectDescriptorImpl<String>(String.class).hashCode() ==
                new DataObjectDescriptorImpl<Integer>(Integer.class).hashCode());
    }

    @Test
    public void descriptorsWithDifferentValuesShouldHaveDifferentHashCodes() {
        DataObjectDescriptorImpl descriptor1 = new DataObjectDescriptorImpl<String>(String.class);
        DataObjectDescriptorImpl descriptor2 = new DataObjectDescriptorImpl<String>(String.class);

        descriptor1.addValueDescriptor(new ValueDescriptorImpl(String.class, valueAccessor, "foo", "foo"));
        descriptor2.addValueDescriptor(new ValueDescriptorImpl(String.class, valueAccessor, "bar", "bar"));

        assertFalse(descriptor1.hashCode() == descriptor2.hashCode());
    }

    @Test
    public void descriptorsWithEqualValuesShouldHaveEqualHashCodes() {
        DataObjectDescriptorImpl<String> descriptor1 = new DataObjectDescriptorImpl<String>(String.class);
        DataObjectDescriptorImpl<String> descriptor2 = new DataObjectDescriptorImpl<String>(String.class);

        descriptor1.addValueDescriptor(new ValueDescriptorImpl(String.class, valueAccessor, "foo", "foo"));
        descriptor2.addValueDescriptor(new ValueDescriptorImpl(String.class, valueAccessor, "foo", "foo"));

        assertEquals(descriptor1.hashCode(), descriptor2.hashCode());
    }

    @Test
    public void inheritedAnnotationsShouldInitiallyBeEmpty() {
        assertEquals(0, new DataObjectDescriptorImpl<String>(String.class).getInheritedAnnotations().length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addInheritedAnnotationShouldNotAcceptNullValue() {
        new DataObjectDescriptorImpl<String>(String.class).addInheritedAnnotation(null);
    }

    @Test
    public void addedInheritedAnnotationShouldBeEnumerable() {
        DataObjectDescriptorImpl<String> descriptor = new DataObjectDescriptorImpl<String>(String.class);
        Class<? extends Annotation> annotationType = Test.class;

        descriptor.addInheritedAnnotation(annotationType);

        assertArrayEquals(new Class[] { Test.class }, descriptor.getInheritedAnnotations());
    }

}
