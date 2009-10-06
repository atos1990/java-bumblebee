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
import javassist.*;
import net.sf.jdpa.NotNull;
import static net.sf.jdpa.cg.Code.*;
import net.sf.jdpa.cg.model.Expression;
import net.sf.jdpa.cg.model.Statement;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.net.URLClassLoader;
import java.net.URL;
import java.net.URISyntaxException;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Default assembler implementation.
 *
 * @author Andreas Nilsson
 */
public class AssemblerImpl implements Assembler {

    private static final Logger LOG = LoggerFactory.getLogger(AssemblerImpl.class);

    private Map<Class<?>, Class<?>> dataObjectImplementations
            = new HashMap<Class<?>, Class<?>>();

    private ReadWriteLock dataObjectImplementationLock = new ReentrantReadWriteLock();

    @SuppressWarnings("unchecked")
    public <T> T assemble(@NotNull Object source, @NotNull Class<T> dataObjectType) {
        return createDataObjectInstance(dataObjectType, new Class[] { Object.class, Assembler.class }, new Object[] { source, this });
    }

    public <T> T assemble(@NotNull Class<T> dataObjectType, PropertyValue ... properties) {
        return createDataObjectInstance(dataObjectType, new Class[] { PropertyValue[].class, Assembler.class }, new Object[] { properties, this });
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

    @SuppressWarnings("unchecked")
    public<T> Class<? extends T> getDataObjectImplementation(Class<T> descriptorType) {
        Class<? extends T> dataObjectImplementation = null;

        dataObjectImplementationLock.readLock().lock();

        try {
            dataObjectImplementation = (Class<? extends T>) dataObjectImplementations.get(descriptorType);

            if (dataObjectImplementation == null) {
                dataObjectImplementationLock.readLock().unlock();
                dataObjectImplementationLock.writeLock().lock();

                try {
                    dataObjectImplementation = (Class<? extends T>) dataObjectImplementations.get(descriptorType);

                    if (dataObjectImplementation == null) {
                        try {
                            dataObjectImplementation = (Class<? extends T>) Class.forName(DataObjectImplementationBuilder.getImplementationClassName(descriptorType));
                        } catch (ClassNotFoundException e) {
                            dataObjectImplementation = (Class<? extends T>) createDataObjectImplementation(descriptorType);
                        }

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
        ClassPool classPool = getClassPool();
        DataObjectImplementationBuilder implementationBuilder = getDataObjectImplementationBuilder(classPool);
        CtClass ctClass = implementationBuilder.newDataObjectImplementation(descriptorType);
        DataObjectDescriptor<?> descriptor = new DataObjectDescriptorFactoryImpl().createDataObjectDescriptor(descriptorType); // TODO Generify
        List<ValueDescriptor> valueDescriptors = descriptor.getValueDescriptors();
        List<CtMethod> initializers = new ArrayList<CtMethod>(valueDescriptors.size());

        implementationBuilder.addInterface(ctClass, descriptorType);
        implementationBuilder.transferTypeAnnotations(descriptor, ctClass);

        for (ValueDescriptor value : valueDescriptors) {
            Class<?> propertyType = value.getPropertyType();
            DTOELParser parser = null;
            Expression expression = null;
            Statement statement = null;
            InputStream in = null;
            CtField ctField = null;
            CtMethod ctAccessor = null;
            CtMethod ctMutator = null;

            try {
                in = new ByteArrayInputStream(value.getExpression().getBytes());
                parser = new DTOELParser(in, "UTF-8");
                expression = parser.Expression();
            } catch (ParseException e) {
                throw new DataObjectGenerationException("Failed to compile expression " + value.getExpression() +
                        " while generating implementation class for " + descriptorType.getName(), e);
            }

            // Create a new field to hold the value
            ctField = implementationBuilder.addField(ctClass, propertyType, value.getProperty());

            // Create an accessor for the field
            ctAccessor = implementationBuilder.addAccessor(ctClass, value.getAccessor().getName(), value.getProperty());

            // Create a mutator if the data object is not marked as immutable
            ctMutator = implementationBuilder.addMutator(ctClass, value);

            // Transfer annotations from the interface to the implementation class
            implementationBuilder.transferMethodAnnotations(descriptor, ctClass, value.getAccessor(), ctAccessor);

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

    protected DataObjectImplementationBuilder getDataObjectImplementationBuilder(ClassPool classPool) {
        return new DataObjectImplementationBuilder(classPool);
    }

    protected ClassPool getClassPool() {    
        ClassPool classPool = new ClassPool(ClassPool.getDefault());
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }

        while (classLoader != null) {
            if (classLoader instanceof URLClassLoader) {
                URLClassLoader urlClassLoader = (URLClassLoader) classLoader;

                for (URL url : urlClassLoader.getURLs()) {
                    try {
                        classPool.appendClassPath(new File(url.toURI()).getAbsolutePath());
                    } catch (NotFoundException e) {
                        LOG.warn("Failed to append URL '" + url + "' to classpath");
                    } catch (URISyntaxException e) {
                        LOG.warn("Failed to translate URL [" + url + "] to file", e);
                    }
                }
            }

            classLoader = classLoader.getParent();
        }

        return classPool;
    }

}
