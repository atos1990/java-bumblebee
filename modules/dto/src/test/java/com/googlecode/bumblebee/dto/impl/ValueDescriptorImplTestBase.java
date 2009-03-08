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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * @author Andreas Nilsson
 */
public class ValueDescriptorImplTestBase {

    protected Method valueAccessor1 = null;

    protected Method valueAccessor2 = null;

    @Before
    public void setup() throws Exception {
        valueAccessor1 = DataObjectWithStringProperty.class.getDeclaredMethod("getProperty");
        valueAccessor2 = DataObjectWithIntegerProperty.class.getDeclaredMethod("getProperty");
    }

    public static class ConstructorTest extends ValueDescriptorImplTestBase {

        @Test(expected = IllegalArgumentException.class)
        public void nullPropertyTypeShouldNotBeAccepted() {
            new ValueDescriptorImpl(null, valueAccessor1, "foo", "bar");
        }

        @Test(expected = IllegalArgumentException.class)
        public void nullExpressionShouldNotBeAccepted() {
            new ValueDescriptorImpl(String.class, valueAccessor1, "property", null);
        }

        @Test(expected = IllegalArgumentException.class)
        public void emptyExpressionShouldNotBeAccepted() {
            new ValueDescriptorImpl(String.class, valueAccessor1, "property", "");
        }

        @Test(expected = IllegalArgumentException.class)
        public void nullPropertyShouldNotBeAccepted() {
            new ValueDescriptorImpl(String.class, valueAccessor1, null, "expression");
        }

        @Test(expected = IllegalArgumentException.class)
        public void emptyPropertyShouldNotBeAccepted() {
            new ValueDescriptorImpl(String.class, valueAccessor1, "", "expression");
        }

        @Test
        public void propertyTypeShouldBeRetained() {
            assertEquals(String.class, new ValueDescriptorImpl(String.class, valueAccessor1, "foo", "bar").getPropertyType());
        }

        @Test
        public void expressionShouldBeRetained() {
            assertEquals("foo", new ValueDescriptorImpl(String.class, valueAccessor1, "property", "foo").getExpression());
            assertEquals("bar", new ValueDescriptorImpl(String.class, valueAccessor1, "property", "bar").getExpression());
        }

        @Test
        public void propertyShouldBeRetained() {
            assertEquals("foo", new ValueDescriptorImpl(String.class, valueAccessor1, "foo", "expression").getProperty());
            assertEquals("bar", new ValueDescriptorImpl(String.class, valueAccessor1, "bar", "expression").getProperty());
        }

        @Test(expected = IllegalArgumentException.class)
        public void nullAccessorShouldNotBeAccepted() {
            new ValueDescriptorImpl(String.class, null, "foo", "bar");
        }

        @Test
        public void accessorShouldBeRetained() {
            assertEquals(valueAccessor1, new ValueDescriptorImpl(String.class, valueAccessor1, "foo", "bar").getAccessor());
        }
    }

    public static class EqualsTest extends ValueDescriptorImplTestBase {

        @Test
        public void descriptorsWithDifferentExpressionsShouldNotBeEqual() {
            assertFalse(new ValueDescriptorImpl(String.class, valueAccessor1, "property", "foo").equals(
                    new ValueDescriptorImpl(String.class, valueAccessor1, "property", "bar")));
        }

        @Test
        public void descriptorsWithDifferentPropertiesShouldNotBeEqual() {
            assertFalse(new ValueDescriptorImpl(String.class, valueAccessor1, "foo", "expression").equals(
                    new ValueDescriptorImpl(String.class, valueAccessor1, "bar", "expression")));
        }

        @Test
        public void descriptorsWithSameStateShouldBeEqual() {
            assertEquals(new ValueDescriptorImpl(String.class, valueAccessor1, "property", "expression"),
                    new ValueDescriptorImpl(String.class, valueAccessor1, "property", "expression"));
        }

        @Test
        public void descriptorsWithDifferentPropertyTypesShouldNotBeEqual() {
            assertFalse(new ValueDescriptorImpl(String.class, valueAccessor1, "foo", "bar").equals(
                    new ValueDescriptorImpl(Integer.class, valueAccessor1, "foo", "bar")));
        }

        @Test
        public void descriptorsWithDifferentAccessorsShouldNotBeEqual() {
            assertFalse(new ValueDescriptorImpl(String.class, valueAccessor1, "foo", "bar").equals(
                    new ValueDescriptorImpl(String.class, valueAccessor2, "foo", "bar")));
        }

        @Test
        public void descriptorShouldNotBeEqualToString() {
            assertFalse(new ValueDescriptorImpl(String.class, valueAccessor1, "property", "foo").equals("foo"));
        }

        @Test
        public void descriptorShouldNotBeEqualToNull() {
            assertFalse(new ValueDescriptorImpl(String.class, valueAccessor1, "property", "foo").equals(null));
        }
    }

    public static class HashCodeTest extends ValueDescriptorImplTestBase {

        @Test
        public void allPropertiesShouldBeIncludedInHashCode() {
            assertEquals(new ValueDescriptorImpl(String.class, valueAccessor1, "foo", "bar").hashCode(), new ValueDescriptorImpl(String.class, valueAccessor1, "foo", "bar").hashCode());
            assertFalse(new ValueDescriptorImpl(String.class, valueAccessor1, "property", "foo").hashCode() == new ValueDescriptorImpl(String.class, valueAccessor1, "property", "bar").hashCode());
            assertFalse(new ValueDescriptorImpl(String.class, valueAccessor1, "foo", "expression").hashCode() == new ValueDescriptorImpl(String.class, valueAccessor1, "bar", "expression").hashCode());
            assertFalse(new ValueDescriptorImpl(String.class, valueAccessor1, "foo", "bar").hashCode() == new ValueDescriptorImpl(Integer.class, valueAccessor1, "foo", "bar").hashCode());
            assertFalse(new ValueDescriptorImpl(String.class, valueAccessor1, "foo", "bar").hashCode() == new ValueDescriptorImpl(String.class, valueAccessor2, "foo", "bar").hashCode());


        }
    }

}
