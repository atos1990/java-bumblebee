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

package com.googlecode.bumblebee.beans;

import static com.googlecode.bumblebee.beans.BeanUtil.getPropertyName;
import com.googlecode.bumblebee.dto.Assembler;
import com.googlecode.bumblebee.dto.AssemblyException;
import com.googlecode.bumblebee.dto.DataObject;
import com.googlecode.bumblebee.dto.Value;
import com.googlecode.bumblebee.dto.impl.AssemblerImpl;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.spy;

import java.util.*;

/**
 * @author Andreas Nilsson
 */
public class BeanUtilTestBase {

    public static class GetPropertyNameTest {

        @Test(expected = IllegalArgumentException.class)
        public void nullMethodShouldNotBeAccepted() {
            getPropertyName(null);
        }

        @Test(expected = InvalidAccessorException.class)
        public void methodWithInvalidPrefixShouldNotBeAccepted() throws Exception {
            getPropertyName(DummyClass.class.getMethod("doStuff"));
        }

        @Test(expected = InvalidAccessorException.class)
        public void methodWithParametersShouldNotBeAccepted() throws Exception {
            getPropertyName(DummyClass.class.getDeclaredMethod("getStuff", String.class));
        }

        @Test(expected = InvalidAccessorException.class)
        public void methodWithVoidReturnTypeShouldNotBeAccepted() throws Exception {
            getPropertyName(DummyClass.class.getDeclaredMethod("getStuff"));
        }

        @Test
        public void methodWithGetPrefixShouldMapToProperty() throws Exception {
            assertEquals("stringProperty", getPropertyName(DummyClass.class.getDeclaredMethod("getStringProperty")));
        }

        @Test(expected = InvalidAccessorException.class)
        public void methodWithOnlyPrefixShouldNotBeValid() throws Exception {
            getPropertyName(DummyClass.class.getDeclaredMethod("get"));
        }
    }

    public static class GetCollectionComponentType extends BeanUtilTestBase {

        @Test(expected = IllegalArgumentException.class)
        public void nullGenericTypeShouldNotBeAccepted() {
            BeanUtil.getCollectionComponentType(null);
        }

        @Test(expected = IllegalArgumentException.class)
        public void emptyGEnericTypeShouldNotBeAccepted() {
            BeanUtil.getCollectionComponentType("");
        }

        @Test(expected = IllegalArgumentException.class)
        public void typeClauseMustEndAtEOF() {
            BeanUtil.getCollectionComponentType("java.util.Collection<java.lang.String>foo");
        }

        @Test(expected = IllegalArgumentException.class)
        public void typeClauseMustBeClosed() {
            BeanUtil.getCollectionComponentType("java.util.Collection<java.lang.String");
        }

        @Test
        public void shouldDefaultToObjectIfNoTypeClause() {
            assertEquals(Object.class, BeanUtil.getCollectionComponentType("java.util.Collection"));
        }

        @Test
        public void shouldDefaultToObjectIfWildcard() {
            assertEquals(Object.class, BeanUtil.getCollectionComponentType("java.util.Collection<?>"));
        }

        @Test
        public void explicitTypeArgShouldBeResolved() {
            assertEquals(String.class, BeanUtil.getCollectionComponentType("java.util.Collection<java.lang.String>"));
        }

        @Test
        public void genericsInDeclarationShouldBeStripped() {
            assertEquals(String.class, BeanUtil.getCollectionComponentType("java.util.Collection<java.lang.String<foo>>"));
        }
    }

    public static class GetProperty extends BeanUtilTestBase {

        @Test(expected = IllegalArgumentException.class)
        public void nullObjectShouldNotBeAccepted() {
            BeanUtil.getProperty(null, "foo");
        }

        @Test(expected = IllegalArgumentException.class)
        public void nullPropertyShouldNotBeAccepted() {
            BeanUtil.getProperty(new Object(), null);
        }

        @Test(expected = IllegalArgumentException.class)
        public void emptyPropertyShouldNotBeAccepted() {
            BeanUtil.getProperty(new Object(), "");
        }

        @Test
        public void propertiesWithAllValidPrefixesShouldBeExtracted() {
            ObjectWithProperties object = new ObjectWithProperties();

            assertEquals("property1", BeanUtil.getProperty(object, "property1"));
            assertEquals(true, BeanUtil.getProperty(object, "property2"));
            assertEquals(false, BeanUtil.getProperty(object, "property3"));
            assertEquals(true, BeanUtil.getProperty(object, "property4"));
            assertEquals(false, BeanUtil.getProperty(object, "property5"));
            assertEquals(true, BeanUtil.getProperty(object, "property6"));
            assertEquals(false, BeanUtil.getProperty(object, "property7"));
            assertEquals(true, BeanUtil.getProperty(object, "property8"));
            assertEquals(false, BeanUtil.getProperty(object, "property9"));
            assertEquals(true, BeanUtil.getProperty(object, "property10"));
        }

        @Test(expected = PropertyAccessException.class)
        public void invalidPropertyShouldNoBeAccepted() {
            BeanUtil.getProperty(new ObjectWithProperties(), "foo");
        }

        // Local support classes

        public static class ObjectWithProperties {

            public String getProperty1() {
                return "property1";
            }

            public boolean isProperty2() {
                return true;
            }

            public boolean hasProperty3() {
                return false;
            }

            public boolean wasProperty4() {
                return true;
            }

            public boolean canProperty5() {
                return false;
            }

            public boolean mayProperty6() {
                return true;
            }

            public boolean willProperty7() {
                return false;
            }

            public boolean couldProperty8() {
                return true;
            }

            public boolean hadProperty9() {
                return false;
            }

            public boolean haveProperty10() {
                return true;
            }

        }

    }

    public static class Copy extends BeanUtilTestBase {

        private Assembler assembler = null;

        @Before
        public void setup() {
            assembler = spy(new AssemblerImpl());
        }

        @Test(expected = IllegalArgumentException.class)
        public void nullPropertyTypeShouldNotBeAccepted() {
            BeanUtil.copy("foo", null, String.class, assembler);
        }

        @Test(expected = IllegalArgumentException.class)
        public void nullComponentTypeShouldNotBeAccepted() {
            BeanUtil.copy("foo", String.class, null, assembler);
        }

        @Test(expected = IllegalArgumentException.class)
        public void nullAssemblerShouldNotBeAccepted() {
            BeanUtil.copy("foo", String.class, String.class, null);
        }

        @Test
        public void plainObjectShouldNotBeCopied() {
            assertEquals("foo", BeanUtil.copy("foo", String.class, String.class, assembler));
        }

        @Test
        public void arrayOfStringsShouldBeCopied() {
            String[] source = new String[]{"1", "2", "3"};
            String[] target = (String[]) BeanUtil.copy(source, String[].class, String.class, assembler);

            assertNotSame(source, target);
            assertArrayEquals(source, target);
        }

        @Test
        public void collectionOfStringsShouldBeCopiedToArray() {
            Collection<String> source = Arrays.asList("1", "2", "3");
            String[] target = (String[]) BeanUtil.copy(source, String[].class, String.class, assembler);

            assertArrayEquals(source.toArray(), target);
        }

        @Test
        public void iteratorOfStringsShouldBeCopiedToArray() {
            Iterator<String> source = Arrays.asList("1", "2", "3").iterator();
            String[] target = (String[]) BeanUtil.copy(source, String[].class, String.class, assembler);

            assertArrayEquals(new String[]{"1", "2", "3"}, target);
        }

        @Test
        public void iterableOfStringsShouldBeCopiedToCollection() {
            final Iterator<String> iterator = Arrays.asList("1", "2", "3").iterator();
            Iterable<String> source = new Iterable<String>() {
                public Iterator<String> iterator() {
                    return iterator;
                }
            };

            String[] target = (String[]) BeanUtil.copy(source, String[].class, String.class, assembler);
            assertArrayEquals(new String[]{"1", "2", "3"}, target);
        }

        @Test(expected = IllegalArgumentException.class)
        public void singleElementToArrayShouldNotBeAccepted() {
            BeanUtil.copy("foo", String[].class, String.class, assembler);
        }

        @Test
        public void collectionShouldBeCopiedToCollection() {
            Collection<String> source = Arrays.asList("1", "2", "3");
            Collection<String> target = (Collection<String>) BeanUtil.copy(source, Collection.class, String.class, assembler);

            assertTrue(target instanceof ArrayList);
            assertNotSame(source, target);
            assertArrayEquals(source.toArray(), target.toArray());
        }

        @Test
        public void collectionShouldBeCopiedToSet() {
            Collection<String> source = Arrays.asList("1", "2", "3");
            Set<String> target = (Set<String>) BeanUtil.copy(source, Set.class, String.class, assembler);

            assertTrue(target instanceof HashSet);
            assertEquals(3, target.size());
            assertTrue(target.contains("1"));
            assertTrue(target.contains("2"));
            assertTrue(target.contains("3"));
        }

        @Test
        public void collectionShouldBeCopiedToList() {
            Collection<String> source = Arrays.asList("1", "2", "3");
            List<String> target = (List<String>) BeanUtil.copy(source, List.class, String.class, assembler);

            assertNotSame(source, target);
            assertArrayEquals(source.toArray(), target.toArray());
        }

        @Test
        public void collectionShouldBeCopiedToLinkedList() {
            Collection<String> source = Arrays.asList("1", "2", "3");
            LinkedList<String> target = (LinkedList<String>) BeanUtil.copy(source, LinkedList.class, String.class, assembler);

            assertNotSame(source, target);
            assertArrayEquals(source.toArray(), target.toArray());
        }

        @Test(expected = AssemblyException.class)
        public void incompatibleElementInCollectionShouldNotBeAccepted() {
            BeanUtil.copy(Arrays.asList("1", "2", "3"), List.class, Integer.class, assembler);
        }

        @Test
        public void arrayShouldBeCopiedToList() {
            String[] source = new String[]{"1", "2", "3"};
            List<String> target = (List<String>) BeanUtil.copy(source, List.class, String.class, assembler);

            assertArrayEquals(source, target.toArray());
        }

        @Test(expected = AssemblyException.class)
        public void arrayWithInvalidComponentTypeShouldNotBeCopiedToCollection() {
            BeanUtil.copy(new String[]{"1"}, List.class, Integer.class, assembler);
        }

        @Test
        public void iteratorShouldBeCopiedToCollection() {
            Iterator<String> source = Arrays.asList("1", "2", "3").iterator();
            List<String> target = (List<String>) BeanUtil.copy(source, List.class, String.class, assembler);

            assertArrayEquals(new String[]{"1", "2", "3"}, target.toArray());
        }

        @Test
        public void iterableShouldBeCopiedToCollection() {
            final Iterator<String> iterator = Arrays.asList("1", "2", "3").iterator();
            Iterable<String> source = new Iterable<String>() {
                public Iterator<String> iterator() {
                    return iterator;
                }
            };

            List<String> target = (List<String>) BeanUtil.copy(source, List.class, String.class, assembler);
            assertArrayEquals(new String[]{"1", "2", "3"}, target.toArray());
        }

        @Test
        public void dataObjectShouldBeAssembled() {
            DummySource source = new DummySource("foobar");
            SimpleDataObject target = (SimpleDataObject) BeanUtil.copy(source, SimpleDataObject.class, SimpleDataObject.class, assembler);

            assertEquals(source.getStringProperty(), target.getStringProperty());
        }

        @Test
        public void collectionOfDataObjectsShouldBeAssembled() {
            Collection<DummySource> source = Arrays.asList(new DummySource("foo"), new DummySource("bar"));
            List<SimpleDataObject> target = (List<SimpleDataObject>) BeanUtil.copy(source, List.class, SimpleDataObject.class, assembler);

            assertEquals(2, target.size());
            assertEquals("foo", target.get(0).getStringProperty());
            assertEquals("bar", target.get(1).getStringProperty());
        }

        @Test
        public void arrayOfDataObjectsShouldBeAssembled() {
            DummySource[] source = new DummySource[]{new DummySource("foo"), new DummySource("bar")};
            SimpleDataObject[] target = (SimpleDataObject[]) BeanUtil.copy(source, SimpleDataObject[].class, SimpleDataObject.class, assembler);

            assertEquals(2, target.length);
            assertEquals("foo", target[0].getStringProperty());
            assertEquals("bar", target[1].getStringProperty());
        }
    }

    // Support classes

    @DataObject
    public static interface SimpleDataObject {

        @Value
        public String getStringProperty();
    }

    public static class DummySource {

        private String stringProperty;

        public DummySource(String stringProperty) {
            this.stringProperty = stringProperty;
        }

        public String getStringProperty() {
            return stringProperty;
        }
    }

    public static class DummyClass {

        public String get() {
            return null;
        }

        public String doStuff() {
            return null;
        }

        public String getStuff(String arg) {
            return null;
        }

        public void getStuff() {
        }

        public String getStringProperty() {
            return null;
        }

    }

}
