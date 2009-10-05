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
import com.googlecode.bumblebee.dto.*;
import com.googlecode.bumblebee.dto.el.parser.DTOELParser;
import com.googlecode.bumblebee.dto.el.parser.ParseException;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import net.sf.jdpa.NotNull;
import static net.sf.jdpa.cg.Code.*;
import net.sf.jdpa.cg.model.Expression;
import net.sf.jdpa.cg.model.Statement;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Default assembler implementation.
 *
 * @author Andreas Nilsson
 */
public class AssemblerImpl implements Assembler {

    private Map<Class<?>, Class<?>> dataObjectImplementations
            = new HashMap<Class<?>, Class<?>>();

    private ReadWriteLock dataObjectImplementationLock = new ReentrantReadWriteLock();

    private ClassPool classPool = ClassPool.getDefault();

    @SuppressWarnings("unchecked")
    public <T> T assemble(@NotNull Object source, @NotNull Class<T> dataObjectType) {
        return createDataObjectInstance(dataObjectType, new Class[] { Object.class, Assembler.class }, new Object[] { source, this });
    }

    public <T> T assemble(@NotNull Class<T> dataObjectType, PropertyValue ... properties) {
        return createDataObjectInstance(dataObjectType, new Class[] { PropertyValue[].class }, new Object[] { properties });
    }

    @SuppressWarnings("unchecked")
    protected <T> T createDataObjectInstance(Class<T> dataObjectType, Class[] signature, Object[] args) {
        Class<?> dataObjectImplementationClass = getDataObjectImplementation(dataObjectType);
        Constructor constructor = null;

        try {
            constructor = dataObjectImplementationClass.getDeclaredConstructor(signature);
        } catch (NoSuchMethodException e) {
            throw new AssemblyException("The generated data object implementation class '" + dataObjectImplementationClass.getName() +
                    "' is not valid. No constructor (java.lang.Object, org.tools4j.dto.Assembler) was defined", e);
        }

        try {
            return (T) constructor.newInstance(args);
        } catch (InstantiationException e) {
            throw new DataObjectGenerationException("The data object implementation class could not be instantiated. " +
                    "Check the stack trace for more information.", e);
        } catch (IllegalAccessException e) {
            throw new DataObjectGenerationException("The generated implementation class did not defined a public constructor", e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new DataObjectGenerationException("The generated implementation class could not be instanted " +
                        "(constructor caused exception). See the stack trace for more information.", e);
            }
        }
    }

    protected Class<?> getDataObjectImplementation(Class<?> descriptorType) {
        Class<?> dataObjectImplementation = null;

        dataObjectImplementationLock.readLock().lock();

        try {
            dataObjectImplementation = dataObjectImplementations.get(descriptorType);

            if (dataObjectImplementation == null) {
                dataObjectImplementationLock.readLock().unlock();
                dataObjectImplementationLock.writeLock().lock();

                try {
                    dataObjectImplementation = dataObjectImplementations.get(descriptorType);

                    if (dataObjectImplementation == null) {
                        dataObjectImplementation = createDataObjectImplementation(descriptorType);
                        dataObjectImplementations.put(descriptorType, dataObjectImplementation);
                    }
                } finally {
                    dataObjectImplementationLock.readLock().lock();
                    dataObjectImplementationLock.writeLock().unlock();
                }
            }
        } finally {
            dataObjectImplementationLock.readLock().unlock();
        }

        return dataObjectImplementation;
    }

    protected Class<?> createDataObjectImplementation(Class<?> descriptorType) {
        DataObjectImplementationBuilder implementationBuilder = getDataObjectImplementationBuilder();
        CtClass ctClass = implementationBuilder.newDataObjectImplementation(descriptorType);
        DataObjectDescriptor<?> descriptor = new DataObjectDescriptorFactoryImpl().createDataObjectDescriptor(descriptorType); // TODO Generify
        List<ValueDescriptor> valueDescriptors = descriptor.getValueDescriptors();
        List<CtMethod> initializers = new ArrayList<CtMethod>(valueDescriptors.size());

        implementationBuilder.addInterface(ctClass, descriptorType);

        for (ValueDescriptor value : valueDescriptors) {
            Class<?> propertyType = value.getPropertyType();
            DTOELParser parser = null;
            Expression expression = null;
            Statement statement = null;
            InputStream in = null;

            try {
                in = new ByteArrayInputStream(value.getExpression().getBytes());
                parser = new DTOELParser(in, "UTF-8");
                expression = parser.Expression();
            } catch (ParseException e) {
                throw new DataObjectGenerationException("Failed to compile expression " + value.getExpression() +
                        " while generating implementation class for " + descriptorType.getName(), e);
            }

            implementationBuilder.addField(ctClass, propertyType, value.getProperty());
            implementationBuilder.addAccessor(ctClass, value.getAccessor().getName(), value.getProperty());
            implementationBuilder.addMutator(ctClass, value);

            if (propertyType.isPrimitive()) {
                // Figure out the corresponding wrapper type for the primitive
                String wrapperType = getWrapperType(propertyType.getName());

                // Unbox the wrapper type so it's assignable to the primitive field. If BeanUtil.getUnwrappableType
                // causes an IllegalArgumentException, the source value is null. This should cause an exception, since
                // we can't unbox a null value. So simply catch the exception and rethrow a nicer exception.

                statement =
                    $try(
                        set(value.getProperty()).of($this()).to(
                            unbox(cast(
                                call("getUnwrappableValue").of(BeanUtil.class).with(expression, constant(wrapperType))
                            ).to(wrapperType), wrapperType)
                        )
                    ).$catch(IllegalArgumentException.class.getName(), "e",
                        $throw($new(AssemblyException.class, cat(constant("Failed to assemble property '" +
                                    value.getProperty() + "': expression evaluates to null: '" + value.getExpression() +
                                    "' on "), call("getClass").of($(0)))))
                    );
            } else {
                Class<?> componentType = propertyType;
                String propertyTypeImage = getTypeImage(propertyType);
                String componentTypeImage = null;

                if (Collection.class.isAssignableFrom(propertyType)) {
                    componentType = BeanUtil.getCollectionComponentType(value.getAccessor().getGenericReturnType().toString());
                } else if (propertyType.isArray()) {
                    componentType = propertyType.getComponentType();
                }

                componentTypeImage = getTypeImage(componentType);

                statement = set(value.getProperty()).of($this()).to(
                    cast(
                        call("copy").of(BeanUtil.class).with(expression, $("class").of(propertyTypeImage), $("class").of(componentTypeImage), $(1))
                    ).to(getTypeImage(value.getPropertyType()))
                );
            }

            // Add an initializer to the implementation class
            initializers.add(implementationBuilder.addInitializer(ctClass, value.getProperty(), statement));
        }

        implementationBuilder.addDefaultConstructor(ctClass);
        implementationBuilder.addBuilderConstructor(ctClass);
        implementationBuilder.addConversionConstructor(ctClass, initializers);

        implementationBuilder.addEqualsMethod(ctClass, valueDescriptors);

        try {
            return ctClass.toClass();
        } catch (CannotCompileException e) {
            throw new DataObjectGenerationException("Compilation of data object implementation class failed. " +
                    "Check the stack trace for more information.", e);
        }
    }

    protected String getTypeImage(Class<?> type) {
        if (type.isArray()) {
            return getTypeImage(type.getComponentType()) + "[]";
        } else {
            return type.getName();
        }
    }

    protected DataObjectImplementationBuilder getDataObjectImplementationBuilder() {
        return new DataObjectImplementationBuilder(classPool);
    }

}
