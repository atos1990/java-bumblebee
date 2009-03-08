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

import static com.googlecode.bumblebee.dto.Bumblebee.assemble;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * @author Andreas Nilsson
 */
public class BumblebeeTest {

    @Test
    public void assembleShouldCopyPrimitives() {
        DataObjectWithPrimitives dataObject = assemble(DataObjectWithPrimitives.class).from(new ObjectWithPrimitives());

        assertEquals((byte) 1, dataObject.getByteProperty());
        assertEquals((short) 2, dataObject.getShortProperty());
        assertEquals('X', dataObject.getCharProperty());
        assertEquals((int) 3, dataObject.getIntProperty());
        assertEquals((long) 4, dataObject.getLongProperty());
        assertEquals((float) 5, dataObject.getFloatProperty(), 0.1F);
        assertEquals((double) 6, dataObject.getDoubleProperty(), 0.1D);
        assertEquals((String) "StringIsNotReallyAPrimitive", dataObject.getStringProperty());
    }

    @Test
    public void assembleShouldWrapPrimitivesIfNecessary() {
        DataObjectWithPrimitiveWrappers dataObject = assemble(DataObjectWithPrimitiveWrappers.class).from(new ObjectWithPrimitives());

        assertEquals(new Byte((byte) 1), dataObject.getByteProperty());
        assertEquals(new Short((short) 2), dataObject.getShortProperty());
        assertEquals(new Character('X'), dataObject.getCharProperty());
        assertEquals(new Integer(3), dataObject.getIntProperty());
        assertEquals(new Long(4), dataObject.getLongProperty());
        assertEquals(new Float(5F), dataObject.getFloatProperty());
        assertEquals(new Double(6D), dataObject.getDoubleProperty());
        assertEquals((String) "StringIsNotReallyAPrimitive", dataObject.getStringProperty());
    }

    @Test
    public void assembleShouldCopyOneToOneRelationship() {
        DataObjectWithOneToOneRelationship dataObject = assemble(DataObjectWithOneToOneRelationship.class).from(new ObjectWithOneToOneRelationship());
        DataObjectWithPrimitives dataObjectWithPrimitives = dataObject.getDataObjectWithPrimitives();

        assertEquals((byte) 1, dataObjectWithPrimitives.getByteProperty());
        assertEquals((short) 2, dataObjectWithPrimitives.getShortProperty());
        assertEquals('X', dataObjectWithPrimitives.getCharProperty());
        assertEquals((int) 3, dataObjectWithPrimitives.getIntProperty());
        assertEquals((long) 4, dataObjectWithPrimitives.getLongProperty());
        assertEquals((float) 5, dataObjectWithPrimitives.getFloatProperty(), 0.1F);
        assertEquals((double) 6, dataObjectWithPrimitives.getDoubleProperty(), 0.1D);
        assertEquals((String) "StringIsNotReallyAPrimitive", dataObjectWithPrimitives.getStringProperty());
    }

    @Test
    public void assembleShouldCopyOneToManyRelationship() {
        DataObjectWithOneToManyRelationship dataObject = assemble(DataObjectWithOneToManyRelationship.class)
                .from(new ObjectWithOneToManyRelationship());

        DataObjectWithOneToOneRelationship[] objects = dataObject.getObjects();
        assertEquals(3, objects.length);

        for (int i = 0; i < objects.length; i++) {
            DataObjectWithPrimitives object = objects[i].getDataObjectWithPrimitives();

            assertEquals((byte) (1 + i), object.getByteProperty());
            assertEquals((short) (2 + i), object.getShortProperty());
            assertEquals('X', object.getCharProperty());
            assertEquals((int) (3 + i), object.getIntProperty());
            assertEquals((long) (4 + i), object.getLongProperty());
            assertEquals((float) (5 + i), object.getFloatProperty(), 0.1F);
            assertEquals((double) (6 + i), object.getDoubleProperty(), 0.1D);
            assertEquals((String) "StringIsNotReallyAPrimitive", object.getStringProperty());
        }
    }

    // Support classes

    @DataObject
    public interface DataObjectWithOneToManyRelationship {

        @Value
        public DataObjectWithOneToOneRelationship[] getObjects();

    }

    public class ObjectWithOneToManyRelationship {

        private ObjectWithOneToOneRelationship[] objects = new ObjectWithOneToOneRelationship[]{
                new ObjectWithOneToOneRelationship(0), new ObjectWithOneToOneRelationship(1), new ObjectWithOneToOneRelationship(2)
        };

        public ObjectWithOneToOneRelationship[] getObjects() {
            return objects;
        }
    }

    @DataObject
    public interface DataObjectWithOneToOneRelationship {

        @Value("objectWithPrimitives")
        public DataObjectWithPrimitives getDataObjectWithPrimitives();

    }

    public class ObjectWithOneToOneRelationship {

        private ObjectWithPrimitives objectWithPrimitives = null;

        public ObjectWithOneToOneRelationship() {
            this(0);
        }

        public ObjectWithOneToOneRelationship(int n) {
            this.objectWithPrimitives = new ObjectWithPrimitives(n);
        }

        public ObjectWithPrimitives getObjectWithPrimitives() {
            return objectWithPrimitives;
        }
    }

    @DataObject
    public interface DataObjectWithPrimitiveWrappers {

        @Value
        public Byte getByteProperty();

        @Value
        public Short getShortProperty();

        @Value
        public Character getCharProperty();

        @Value
        public Integer getIntProperty();

        @Value
        public Long getLongProperty();

        @Value
        public Float getFloatProperty();

        @Value
        public Double getDoubleProperty();

        @Value
        public String getStringProperty();
    }

    @DataObject
    public interface DataObjectWithPrimitives {

        @Value
        public byte getByteProperty();

        @Value
        public short getShortProperty();

        @Value
        public char getCharProperty();

        @Value
        public int getIntProperty();

        @Value
        public long getLongProperty();

        @Value
        public float getFloatProperty();

        @Value
        public double getDoubleProperty();

        @Value
        public String getStringProperty();
    }

    public static class ObjectWithPrimitives {

        private int n = 0;

        public ObjectWithPrimitives() {
            this(0);
        }

        public ObjectWithPrimitives(int n) {
            this.n = n;
        }

        public byte getByteProperty() {
            return (byte) (n + 1);
        }

        public short getShortProperty() {
            return (byte) (n + 2);
        }

        public char getCharProperty() {
            return 'X';
        }

        public int getIntProperty() {
            return (n + 3);
        }

        public long getLongProperty() {
            return (n + 4L);
        }

        public float getFloatProperty() {
            return (n + 5F);
        }

        public double getDoubleProperty() {
            return (n + 6D);
        }

        public String getStringProperty() {
            return "StringIsNotReallyAPrimitive";
        }

    }
}
