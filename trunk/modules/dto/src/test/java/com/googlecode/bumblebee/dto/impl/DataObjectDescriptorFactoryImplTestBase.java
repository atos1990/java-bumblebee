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

import com.googlecode.bumblebee.dto.*;
import org.junit.Assert;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Andreas Nilsson
 */
public class DataObjectDescriptorFactoryImplTestBase {

    protected Method valueAccessor = null;

    protected DataObjectDescriptorFactoryImpl descriptorFactory = null;

    @Before
    public void setup() throws Exception {
        valueAccessor = DataObjectWithStringProperty.class.getDeclaredMethod("getProperty");
        descriptorFactory = new DataObjectDescriptorFactoryImpl();
    }

    public static class CreateDataObjectDescriptorTest extends DataObjectDescriptorFactoryImplTestBase {

        @Test(expected = IllegalArgumentException.class)
        public void nullClassShouldNotBeAccepted() {
            descriptorFactory.createDataObjectDescriptor(null);
        }

        @Test(expected = DataObjectValidationException.class)
        public void nonDataClassShouldNotBeValid() {
            descriptorFactory.createDataObjectDescriptor(String.class);
        }

        @Test(expected = DataObjectValidationException.class)
        public void nonInterfaceShouldNotBeValid() {
            descriptorFactory.createDataObjectDescriptor(DataObjectAsClass.class);
        }

        @Test
        public void emptyClassShouldHaveEmptyDescriptor() {
            DataObjectDescriptor descriptor = descriptorFactory.createDataObjectDescriptor(EmptyDataObject.class);
            assertTrue(descriptor.getValueDescriptors().isEmpty());
            assertEquals(EmptyDataObject.class, descriptor.getObjectType());
        }

        @Test
        public void simpleValueShouldBeIncludedInDescriptor() throws Exception {
            DataObjectDescriptor descriptor = descriptorFactory.createDataObjectDescriptor(DataObjectWithSingleSimpleProperty.class);

            Assert.assertArrayEquals(new ValueDescriptorImpl[]{
                    new ValueDescriptorImpl(String.class, DataObjectWithSingleSimpleProperty.class.getMethod("getId"), "id", "id")
            }, descriptor.getValueDescriptors().toArray());
        }

        @Test(expected = DataObjectValidationException.class)
        public void valueAnnotationOnAccessorWithVoidReturnShouldNotBeAccepted() {
            descriptorFactory.createDataObjectDescriptor(DataObjectWithValueAnnotationOnGetterWithInvalidReturnType.class);
        }

        @Test(expected = DataObjectValidationException.class)
        public void nonValueMethodsShouldNotBeAccepted() {
            descriptorFactory.createDataObjectDescriptor(DataObjectWithNonValueMethods.class);
        }

        @Test(expected = DataObjectValidationException.class)
        public void methodWithArgumentsShouldNotBeAccepted() {
            descriptorFactory.createDataObjectDescriptor(DataObjectWithAccessorWithArgs.class);
        }

        @Test
        public void valueWithoutExpressionShouldDefaultToPropertyName() throws Exception {
            DataObjectDescriptor descriptor = descriptorFactory.createDataObjectDescriptor(DataObjectWithUnqualifiedValue.class);

            assertArrayEquals(new ValueDescriptorImpl[]{
                    new ValueDescriptorImpl(String.class, DataObjectWithUnqualifiedValue.class.getMethod("getStuff"), "stuff", "stuff")
            }, descriptor.getValueDescriptors().toArray());
        }

        @Test
        public void valuesFromSingleInterfaceShouldBeIncluded() throws Exception {
            DataObjectDescriptor descriptor = descriptorFactory.createDataObjectDescriptor(ExtendedDataObject.class);

            assertArrayEquals(new ValueDescriptorImpl[]{
                    new ValueDescriptorImpl(String.class, DataObjectWithUnqualifiedValue.class.getMethod("getStuff"), "stuff", "stuff")
            }, descriptor.getValueDescriptors().toArray());
        }

        @Test
        public void valuesFromMultipleInterfacesShouldBeIncluded() throws Exception {
            DataObjectDescriptor<DataObjectWithMultipleInterfaces> descriptor =
                    descriptorFactory.createDataObjectDescriptor(DataObjectWithMultipleInterfaces.class);

            assertArrayEquals(new ValueDescriptorImpl[]{
                    new ValueDescriptorImpl(int.class, DataObjectWithSingleIntegerValue.class.getMethod("getAnIntValue"), "anIntValue", "anIntValue"),
                    new ValueDescriptorImpl(String.class, DataObjectWithUnqualifiedValue.class.getMethod("getStuff"), "stuff", "stuff")
            }, sortByProperty(descriptor.getValueDescriptors()).toArray());
        }

        @Test
        public void overriddenValueShouldBeIgnored() throws Exception {
            DataObjectDescriptor descriptor = descriptorFactory.createDataObjectDescriptor(DataObjectWithOverriddenValue.class);
            assertArrayEquals(new ValueDescriptorImpl[]{
                    new ValueDescriptorImpl(int.class, DataObjectWithOverriddenValue.class.getMethod("getAnIntValue"), "anIntValue", "foo.bar")
            }, descriptor.getValueDescriptors().toArray());
        }

        protected List<ValueDescriptor> sortByProperty(List<ValueDescriptor> values) {
            Collections.sort(values, new Comparator<ValueDescriptor>() {
                public int compare(ValueDescriptor v1, ValueDescriptor v2) {
                    return v1.getProperty().compareTo(v2.getProperty());
                }
            });

            return values;
        }
    }

    //
    // Support classes
    //

    @DataObject
    public static interface DataObjectWithOverriddenValue extends DataObjectWithSingleIntegerValue {

        @Value("foo.bar")
        public int getAnIntValue();
    }

    @DataObject
    public static interface DataObjectWithMultipleInterfaces extends DataObjectWithUnqualifiedValue, DataObjectWithSingleIntegerValue {

    }

    @DataObject
    public static interface DataObjectWithSingleIntegerValue {

        @Value
        public int getAnIntValue();

    }

    @DataObject
    public static interface ExtendedDataObject extends DataObjectWithUnqualifiedValue {

    }

    @DataObject
    public static interface DataObjectWithUnqualifiedValue {

        @Value
        public String getStuff();

    }

    @DataObject
    public static interface EmptyDataObject {
    }

    @DataObject
    public static class DataObjectAsClass {

    }

    @DataObject
    public static interface DataObjectWithNonValueMethods {
        public void doStuff();
    }

    @DataObject
    public static interface DataObjectWithValueAnnotationOnGetterWithInvalidReturnType {

        @Value("stuff")
        public void getStuff();

    }

    @DataObject
    public static interface DataObjectWithAccessorWithArgs {

        @Value("stuff")
        public String getStuff(String arg);

    }

    @DataObject
    public static interface DataObjectWithSingleSimpleProperty {

        @Value("id")
        public String getId();

    }

}
