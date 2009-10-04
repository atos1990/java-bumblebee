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

import com.googlecode.bumblebee.dto.Assembler;
import com.googlecode.bumblebee.dto.DataObjectGenerationException;
import javassist.*;
import static net.sf.jdpa.cg.Code.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

/**
 * @author Andreas Nilsson
 */
public class DataObjectImplementationBuilderTestBase {

    protected static long sequence = 1;

    protected ClassPool classPool = null;

    protected DataObjectImplementationBuilder dataObjectImplementationBuilder;

    protected CtClass implementationClass = null;

    @Before
    public void setup() {
        classPool = ClassPool.getDefault();
        dataObjectImplementationBuilder = new DataObjectImplementationBuilder(ClassPool.getDefault());
        implementationClass = classPool.makeClass(String.format("com.googlecode.bumblebee.dto.impl.DummyCtClass_%04d", sequence++));
    }

    public static class NewDataObjectImplementationTest extends DataObjectImplementationBuilderTestBase {

        @Test
        public void concreteCtClassShouldBeCreated() {
            CtClass ctClass = dataObjectImplementationBuilder.newDataObjectImplementation(EmptyDataObject2.class);
            Pattern classNamePattern = Pattern.compile("com\\.googlecode\\.bumblebee\\.dto\\.impl\\.EmptyDataObject2\\$impl\\$\\d{6}");

            assertTrue(ctClass.getName(), classNamePattern.matcher(ctClass.getName()).matches());
        }

        @Test(expected = IllegalArgumentException.class)
        public void nullClassShouldNotBeAccepted() {
            dataObjectImplementationBuilder.newDataObjectImplementation(null);
        }
    }

    public static class AddInterfaceTest extends DataObjectImplementationBuilderTestBase {

        @Test(expected = IllegalArgumentException.class)
        public void nullImplementationClassShouldNotBeAccepted() {
            dataObjectImplementationBuilder.addInterface(null, String.class);
        }

        @Test(expected = IllegalArgumentException.class)
        public void nullInterfaceClassShouldNotBeAccepted() {
            dataObjectImplementationBuilder.addInterface(implementationClass, null);
        }

        @Test
        public void interfaceShouldBeAdded() throws Exception {
            dataObjectImplementationBuilder.addInterface(implementationClass, EmptyDataObject.class);
            assertArrayEquals(new CtClass[]{
                    classPool.get(EmptyDataObject.class.getName())
            }, implementationClass.getInterfaces());
        }
    }

    public static class AddFieldTest extends DataObjectImplementationBuilderTestBase {

        @Test(expected = IllegalArgumentException.class)
        public void nullImplementationClassShouldNotBeAccepted() {
            dataObjectImplementationBuilder.addField(null, String.class, "foo");
        }

        @Test(expected = IllegalArgumentException.class)
        public void nullFieldTypeShouldNotBeAccepted() {
            dataObjectImplementationBuilder.addField(implementationClass, null, "foo");
        }

        @Test(expected = IllegalArgumentException.class)
        public void nullFieldNameShouldNotBeAccepted() {
            dataObjectImplementationBuilder.addField(implementationClass, String.class, null);
        }

        @Test(expected = IllegalArgumentException.class)
        public void emptyFieldNameShouldNotBeAccepted() {
            dataObjectImplementationBuilder.addField(implementationClass, String.class, "");
        }

        @Test
        public void fieldShouldBeAdded() throws Exception {
            dataObjectImplementationBuilder.addField(implementationClass, String.class, "foo");

            CtField[] fields = implementationClass.getFields();

            assertEquals(1, fields.length);
            assertEquals(String.class.getName(), fields[0].getType().getName());
            assertEquals("foo", fields[0].getName());
        }
    }

    public static class AddAccessorTest extends DataObjectImplementationBuilderTestBase {

        @Test(expected = IllegalArgumentException.class)
        public void nullImplementationClassShouldNotBeAccepted() {
            dataObjectImplementationBuilder.addAccessor(null, "getProperty", "property");
        }

        @Test(expected = IllegalArgumentException.class)
        public void nullAccessorNameShouldNotBeAccepted() {
            dataObjectImplementationBuilder.addAccessor(implementationClass, null, "property");
        }

        @Test(expected = IllegalArgumentException.class)
        public void emptyAccessorNameShouldNotBeAccepted() {
            dataObjectImplementationBuilder.addAccessor(implementationClass, "", "property");
        }

        @Test(expected = IllegalArgumentException.class)
        public void nullFieldNameShouldNotBeAccepted() {
            dataObjectImplementationBuilder.addAccessor(implementationClass, "getProperty", null);
        }

        @Test(expected = IllegalArgumentException.class)
        public void emptyFieldNameShouldNotBeAccepted() {
            dataObjectImplementationBuilder.addAccessor(implementationClass, "getProperty", "");
        }

        @Test
        public void accessorShouldBeAdded() throws Exception {
            dataObjectImplementationBuilder.addField(implementationClass, String.class, "foo");
            dataObjectImplementationBuilder.addAccessor(implementationClass, "getFoo", "foo");

            CtMethod[] methods = implementationClass.getDeclaredMethods();
            assertEquals(1, methods.length);

            CtMethod method = methods[0];
            assertEquals("getFoo", method.getName());
            assertEquals("java.lang.String", method.getReturnType().getName());

            Object object = implementationClass.toClass().newInstance();
            Field field = object.getClass().getDeclaredField("foo");

            field.setAccessible(true);
            field.set(object, "foobar");

            assertEquals("foobar", object.getClass().getDeclaredMethod("getFoo").invoke(object));
        }
    }

    public static class AddInitializer extends DataObjectImplementationBuilderTestBase {

        @Test(expected = IllegalArgumentException.class)
        public void nullImplementationClassShouldNotBeAccepted() {
            dataObjectImplementationBuilder.addInitializer(null, "foo", body());
        }

        @Test(expected = IllegalArgumentException.class)
        public void nullFieldNameShouldNotBeAccepted() {
            dataObjectImplementationBuilder.addInitializer(implementationClass, null, body());
        }

        @Test(expected = IllegalArgumentException.class)
        public void emptyFieldNameShouldNotBeAccepted() {
            dataObjectImplementationBuilder.addInitializer(implementationClass, "", body());
        }

        @Test(expected = IllegalArgumentException.class)
        public void nullExpressionShouldNotBeAccepted() {
            dataObjectImplementationBuilder.addInitializer(implementationClass, "foo", null);
        }

        @Test(expected = DataObjectGenerationException.class)
        public void generationShouldFailedIfFieldNameIsInvalid() {
            dataObjectImplementationBuilder.addInitializer(implementationClass, "foo", body());
        }

        @Test
        public void initializerShouldBeAdded() throws Exception {
            dataObjectImplementationBuilder.addField(implementationClass, String.class, "foo");
            dataObjectImplementationBuilder.addInitializer(implementationClass, "foo", set("foo").of($this()).to(constant("foobar")));

            Class clazz = implementationClass.toClass();
            Object instance = clazz.newInstance();
            Method method = clazz.getDeclaredMethod("init_foo", Object.class, Assembler.class);
            Field field = clazz.getDeclaredField("foo");

            field.setAccessible(true);
            assertNull(field.get(instance));

            method.setAccessible(true);
            method.invoke(instance, new Object(), null);

            assertEquals("foobar", field.get(instance));
        }
    }

    public static class AddConstructor extends DataObjectImplementationBuilderTestBase {

        @Test(expected = IllegalArgumentException.class)
        public void nullInitializersShouldNotBeAccepted() {
            dataObjectImplementationBuilder.addConversionConstructor(implementationClass, null);
        }

        @Test(expected = IllegalArgumentException.class)
        public void nullImplementationClassShouldNotBeAccepted() {
            dataObjectImplementationBuilder.addConversionConstructor(null, Collections.<CtMethod>emptyList());
        }

        @Test
        public void constructorWithSourceAndAssemblerShouldBeDeclared() throws Exception {
            dataObjectImplementationBuilder.addConversionConstructor(implementationClass, Collections.<CtMethod>emptyList());

            CtConstructor[] constructors = implementationClass.getConstructors();
            assertEquals(1, constructors.length);

            CtConstructor constructor = constructors[0];
            CtClass[] parameterTypes = constructor.getParameterTypes();
            assertEquals(2, parameterTypes.length);

            assertEquals(Object.class.getName(), parameterTypes[0].getName());
            assertEquals(Assembler.class.getName(), parameterTypes[1].getName());
        }

        @Test
        public void generatedConstructorShouldCallInitializers() throws Exception {
            CtMethod ctMethod = null;

            // Add a field for verification
            dataObjectImplementationBuilder.addField(implementationClass, String.class, "foo");

            // Add an initializer for the field. This method should set the value of the field according to the provided expression.
            ctMethod = dataObjectImplementationBuilder.addInitializer(implementationClass, "foo", set("foo").of($this()).to(constant("foobar")));

            // Create a constructor for the implementation class.
            dataObjectImplementationBuilder.addConversionConstructor(implementationClass, Arrays.asList(ctMethod));

            Class clazz = implementationClass.toClass();
            Constructor constructor = clazz.getConstructor(Object.class, Assembler.class);
            Object instance = constructor.newInstance("source", null);

            Field field = clazz.getDeclaredField("foo");

            field.setAccessible(true);
            assertEquals("foobar", field.get(instance));
        }

    }

}
