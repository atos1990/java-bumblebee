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
import com.googlecode.bumblebee.dto.AssemblyException;
import com.googlecode.bumblebee.dto.DataObject;
import com.googlecode.bumblebee.dto.Value;
import com.googlecode.bumblebee.dto.PropertyValue;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import static net.sf.jdpa.cg.Code.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.eq;

import java.util.*;

/**
 * @author Andreas Nilsson
 */
public class AssemblerImplTestBase {

    protected DataObjectImplementationBuilder implementationBuilder = null;

    protected AssemblerImpl assembler = null;

    @Before
    public void setup() {
        implementationBuilder = spy(new DataObjectImplementationBuilder(ClassPool.getDefault()));
        assembler = new AssemblerImpl() {
            @Override
            protected DataObjectImplementationBuilder getDataObjectImplementationBuilder() {
                return implementationBuilder;
            }
        };
    }

    public static class CreateDataObjectImplementation extends AssemblerImplTestBase {

        @Test
        public void testCallSequenceOnEmptyDataObject() {
            assembler.createDataObjectImplementation(EmptyDataObject.class);

            // Check that a new class is defined
            verify(implementationBuilder).newDataObjectImplementation(eq(EmptyDataObject.class));

            // Check that all data object interfaces are implemented
            verify(implementationBuilder).addInterface((CtClass) anyObject(), eq(EmptyDataObject.class));
        }

        @Test
        @SuppressWarnings("unchecked")
        public void testCallSequenceOnDataObjectWithSingleProperty() {
            assembler.createDataObjectImplementation(DataObjectWithSingleStringProperty.class);

            // Check that a new class is defined
            verify(implementationBuilder).newDataObjectImplementation(eq(DataObjectWithSingleStringProperty.class));

            // Check that all data object interfaces are implemented
            verify(implementationBuilder).addInterface((CtClass) anyObject(), eq(DataObjectWithSingleStringProperty.class));

            // Check that a field with a corresponding accessor is added for the property
            verify(implementationBuilder).addField((CtClass) anyObject(), eq(String.class), eq("property"));
            verify(implementationBuilder).addAccessor((CtClass) anyObject(), eq("getProperty"), eq("property"));

            // Check that an init-method is added for the property
            verify(implementationBuilder).addInitializer((CtClass) anyObject(), eq("property"),
                    eq(set("property").of($this()).to(
                            cast(
                                    call("copy").of(BeanUtil.class).with(
                                            call("getProperty").of(BeanUtil.class).with($(0), constant("property")),
                                            $("class").of(String.class),
                                            $("class").of(String.class),
                                            $(1)
                                    )
                            ).to(String.class)
                    )));

            // Check that a constructor is added
            verify(implementationBuilder).addConversionConstructor((CtClass) anyObject(), (Collection<CtMethod>) anyObject());
        }

        @Test
        @Ignore("To messy to verify")
        public void testCallSequenceOnDataObjectWithSinglePrimitiveProperty() {
            assembler.createDataObjectImplementation(DataObjectWithSinglePrimitiveProperty.class);

            verify(implementationBuilder).addInitializer((CtClass) anyObject(), eq("property"), eq(
                    set("property").of($this()).to(
                            call("intValue").of(
                                    cast(
                                            call("getProperty").of(BeanUtil.class).with($(0), constant("property"))
                                    ).to(Integer.class)
                            )
                    )
            ));
        }

    }

    public static class AssembleTest extends AssemblerImplTestBase {

        @Test(expected = IllegalArgumentException.class)
        public void nullSourceShouldNotBeAccepted() {
            assembler.assemble(null, DummyDataObject.class);
        }

        @Test(expected = IllegalArgumentException.class)
        public void nullDataObjectTypeShouldNotBeAccepted() {
            assembler.assemble("foo", null);
        }

        @Test
        public void nonNullInstanceShouldBeReturned() {
            DummyDataObject dataObject = assembler.assemble(new DummySource(), DummyDataObject.class);
            assertNotNull(dataObject);
        }

        @Test
        public void simplePropertiesShouldBeExtracted() {
            DataObjectWithSingleStringProperty dataObject = assembler.assemble(new SourceWithSingleStringProperty(), DataObjectWithSingleStringProperty.class);
            assertEquals("foobar", dataObject.getProperty());
        }

        @Test
        public void primitivesShouldBeExtracted() {
            DataObjectWithSinglePrimitiveProperty dataObject = assembler.assemble(new SourceWithSinglePrimitive(), DataObjectWithSinglePrimitiveProperty.class);
            assertEquals(314, dataObject.getProperty());
        }

        @Test
        public void wrapperTypeShouldBeUnboxedIfDataValueIsPrimitive() {
            DataObjectWithSinglePrimitiveProperty dataObject = assembler.assemble(new SourceWithSingleWrapperType(278), DataObjectWithSinglePrimitiveProperty.class);
            assertEquals(278, dataObject.getProperty());
        }

        @Test(expected = AssemblyException.class)
        public void generationShouldFailIfUnboxIsRequiredAndSourceValueIsNull() {
            assembler.assemble(new SourceWithSingleWrapperType(null), DataObjectWithSinglePrimitiveProperty.class);
        }

        @Test
        public void cascadedDataObjectsShouldBeCopied() {
            DataObjectWithRelationship dataObject = assembler.assemble(new SourceWithRelationship(100, new SourceWithSingleWrapperType(200)), DataObjectWithRelationship.class);

            assertEquals(new Integer(100), dataObject.getIntValue());
            assertEquals(200, dataObject.getRelationship().getProperty());
        }

        @Test
        @Ignore
        public void collectionOfStringsShouldBeAssembled() {
            ObjectWithStringsData dto = assembler.assemble(new ObjectWithStringList("foo", "bar"), ObjectWithStringsData.class);
            assertTrue(dto.getStrings().getClass().equals(ArrayList.class));
            assertArrayEquals(new String[]{"foo", "bar"}, dto.getStrings().toArray());
        }

        // Local support classes

        @DataObject
        public interface ObjectWithStringsData {

            @Value
            public Collection<String> getStrings();
        }

        public class ObjectWithStringList {

            private List<String> strings = new LinkedList<String>();

            public ObjectWithStringList(String... strings) {
                this.strings.addAll(Arrays.asList(strings));
            }

            public List<String> getStrings() {
                return this.strings;
            }

        }

    }

    public static class AssembleFromPropertiesTest extends AssemblerImplTestBase {

        @Test(expected = IllegalArgumentException.class)
        public void assembleShouldNotAcceptNullDataObjectType() {
            assembler.assemble(null);
        }

        @Test                                   
        public void assembleShouldReturnPlainInstanceIfNoPropertiesAreProvided() {
            DummyDataObject dataObject = assembler.assemble(DummyDataObject.class);
            assertNotNull(dataObject);
            assertNull(dataObject.getString());
        }

        @Test
        public void providedPropertiesShouldBeTransferedToObject() {
            DummyDataObject dataObject = assembler.assemble(DummyDataObject.class, new PropertyValue("string", "FooBar"));
            assertEquals("FooBar", dataObject.getString());
        }

        // Local support classes

        @DataObject
        public static interface DummyDataObject {

            @Value
            public String getString();

        }

    }

    // Support classes

    @DataObject
    public static interface DataObjectWithRelationship {

        @Value("integerValue")
        public Integer getIntValue();

        @Value
        public DataObjectWithSinglePrimitiveProperty getRelationship();

    }

    public static class SourceWithRelationship {

        private int integerValue;

        private SourceWithSingleWrapperType relationship;

        public SourceWithRelationship(int integerValue, SourceWithSingleWrapperType relationship) {
            this.integerValue = integerValue;
            this.relationship = relationship;
        }

        public int getIntegerValue() {
            return integerValue;
        }

        public SourceWithSingleWrapperType getRelationship() {
            return relationship;
        }
    }

    public static class SourceWithSingleWrapperType {

        private Integer value;

        public SourceWithSingleWrapperType(Integer value) {
            this.value = value;
        }

        public Integer getProperty() {
            return value;
        }
    }

    public static class SourceWithSinglePrimitive {
        public int getProperty() {
            return 314;
        }
    }

    @DataObject
    public static interface DataObjectWithSinglePrimitiveProperty {

        @Value
        public int getProperty();

    }

    @DataObject
    public static interface DataObjectWithSingleStringProperty {

        @Value
        public String getProperty();

    }

    @DataObject
    public static class SourceWithSingleStringProperty {

        public String getProperty() {
            return "foobar";
        }

    }

    @DataObject
    public static interface DummyDataObject {

    }

    public static class DummySource {

    }

}
