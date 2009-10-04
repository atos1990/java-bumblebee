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
import com.googlecode.bumblebee.dto.PropertyValue;
import com.googlecode.bumblebee.dto.ValueDescriptor;
import javassist.*;
import net.sf.jdpa.NotEmpty;
import net.sf.jdpa.NotNull;
import net.sf.jdpa.Pointcut;
import static net.sf.jdpa.cg.Code.*;
import net.sf.jdpa.cg.model.Statement;
import net.sf.jdpa.javassist.JavassistEmitter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Arrays;

/**
 * @author Andreas Nilsson
 */
public class DataObjectImplementationBuilder {

    private ClassPool classPool = null;

    private static long sequence = 1;

    public DataObjectImplementationBuilder(@NotNull ClassPool classPool) {
        this.classPool = classPool;
    }

    /**
     * Creates a new intermediate data object implementation class for the provided type. The
     * type is expected to be a valid data object class as is defined by
     * {@link com.googlecode.bumblebee.dto.DataObjectDescriptorFactory#createDataObjectDescriptor(Class)}.
     *
     * @param objectType The type of the data object for which the implementation class should
     *                   be defined.
     * @return An intermediate implementation class for the provided data object.
     */
    public CtClass newDataObjectImplementation(@NotNull Class<?> objectType) {
        long objectId = -1;

        synchronized (this) {
            objectId = sequence++;
        }

        return classPool.makeClass(String.format(objectType.getName() + "$impl$%06d", objectId));
    }

    public void addInterface(@NotNull CtClass implementationClass, @NotNull Class<?> interfaceType) {
        try {
            implementationClass.addInterface(classPool.get(interfaceType.getName()));
        } catch (NotFoundException e) {
            throw new DataObjectGenerationException("Failed to add interface " + interfaceType.getName()
                    + " to implementation class " + implementationClass.getName() + "; class file for interface could not be found", e);
        }
    }

    public CtField addField(@NotNull CtClass implementationClass, @NotNull Class<?> fieldType, @NotEmpty String fieldName) {
        CtClass ctFieldType = null;
        CtField ctField = null;

        try {
            ctFieldType = classPool.get(fieldType.getName());
        } catch (NotFoundException e) {
            throw new DataObjectGenerationException("Field add field " + fieldName + ":" + fieldType.getName() +
                    " to class " + implementationClass.getName() + "; class file for field type could not be found", e);
        }

        try {
            ctField = new CtField(ctFieldType, fieldName, implementationClass);
        } catch (CannotCompileException e) {
            throw new DataObjectGenerationException("Failed to create new field '" + fieldName + ":" + ctFieldType.getName() +
                    "' for declaring class " + implementationClass.getName(), e);
        }

        try {
            implementationClass.addField(ctField);
        } catch (CannotCompileException e) {
            throw new DataObjectGenerationException("Failed to add field " + fieldName + ":" + fieldType.getName() +
                    " to data object implementation class " + implementationClass.getName() + "; failed to compile field declaration", e);
        }

        return ctField;
    }

    public CtMethod addAccessor(@NotNull CtClass implementationClass, @NotEmpty String methodName, @NotEmpty String fieldName) {
        CtField field = null;
        CtMethod accessor = null;

        try {
            field = implementationClass.getDeclaredField(fieldName);
        } catch (NotFoundException e) {
            throw new DataObjectGenerationException("Cannot add accessor " + implementationClass.getName() +
                    "." + methodName + "; invalid field " + fieldName, e);
        }

        try {
            accessor = CtNewMethod.getter(methodName, field);
        } catch (CannotCompileException e) {
            throw new DataObjectGenerationException("Failed to compile accessor " + methodName + " for field " +
                    implementationClass.getName() + "." + field.getName(), e);
        }

        try {
            implementationClass.addMethod(accessor);
        } catch (CannotCompileException e) {
            throw new DataObjectGenerationException("Accessor for field " + implementationClass.getName() + "." +
                    field.getName() + " could not be added; compilation failed", e);
        }

        return accessor;
    }

    public CtMethod addInitializer(@NotNull CtClass implementationClass, @NotEmpty String fieldName, @NotNull Statement statement) {
        String methodName = "init_" + fieldName;
        CtClass ctObject = null;
        CtClass ctAssembler = null;
        CtMethod method = null;
        JavassistEmitter emitter = null;
        String body = null;

        try {
            implementationClass.getField(fieldName);
        } catch (NotFoundException e) {
            throw new DataObjectGenerationException("Cannot add initializer for field '" + fieldName + "' (field does not exist)");
        }

        try {
            ctObject = classPool.get(Object.class.getName());
        } catch (NotFoundException e) {
            // Should never occure
            throw new DataObjectGenerationException("Failed to locate class file for java.lang.Object", e);
        }

        try {
            ctAssembler = classPool.get(Assembler.class.getName());
        } catch (NotFoundException e) {
            throw new DataObjectGenerationException("Failed to locate class file for " + Assembler.class.getName(), e);
        }

        method = new CtMethod(CtClass.voidType, methodName, new CtClass[]{ctObject, ctAssembler}, implementationClass);
        emitter = new JavassistEmitter(classPool, method, Pointcut.BEFORE);

        // Start working on the real method body. This should replace the empty body define earlier.
        body = "{ " + emitter.generateCode(statement) + "; }";

        method.setModifiers(Modifier.PRIVATE);

        try {
            method.setBody(body);
        } catch (CannotCompileException e) {
            throw new DataObjectGenerationException("Failed to generate initializer for property '" + fieldName +
                    "'. Generated code was not valid: " + body, e);
        }

        try {
            implementationClass.addMethod(method);
        } catch (CannotCompileException e) {
            throw new DataObjectGenerationException("Failed to create initializer for property " + fieldName, e);
        }

        return method;
    }

    public CtConstructor addBuilderConstructor(@NotNull CtClass implementationClass) {
        CtConstructor constructor = null;
        CtClass ctPropertyValueArray = null;
        CtClass ctAssembler = null;
        StringBuilder statementBuffer = new StringBuilder();

        try {
            ctPropertyValueArray = classPool.get(PropertyValue[].class.getName());
        } catch (NotFoundException e) {
            throw new DataObjectGenerationException("Failed to locate class file for PropertyValue[]");
        }

        try {
            ctAssembler = classPool.get(Assembler.class.getName());
        } catch (NotFoundException e) {
            throw new DataObjectGenerationException("Failed to locate class file for Assembler");
        }

        try {
            constructor = CtNewConstructor.make(new CtClass[] { ctPropertyValueArray, ctAssembler }, new CtClass[0], implementationClass);
        } catch (CannotCompileException e) {
            throw new DataObjectGenerationException("Failed to generate constructor (PropertyValue[], Assembler)");
        }

        statementBuffer.append("for (int i = 0; i < $1.length; i++) {");
            statementBuffer.append("com.googlecode.bumblebee.dto.PropertyValue propertyValue = $1[i];");
            statementBuffer.append("try {");
                statementBuffer.append("getClass().getDeclaredField(propertyValue.getPropertyName()).set(this, $1[i].getPropertyValue());");
            statementBuffer.append("} catch (Exception e) {");
                statementBuffer.append("throw new com.googlecode.bumblebee.dto.DataObjectGenerationException(\"Failed to set property \" + propertyValue.getPropertyName(), e);");
            statementBuffer.append("}");
        statementBuffer.append("}");

        try {
            constructor.setBody(statementBuffer.toString());
        } catch (CannotCompileException e) {
            throw new DataObjectGenerationException("Failed to compile constructor: " + statementBuffer, e);
        }

        try {
            implementationClass.addConstructor(constructor);
        } catch (CannotCompileException e) {
            throw new DataObjectGenerationException("Failed to compile constructor: " + statementBuffer, e);
        }

        return constructor;
    }

    public CtConstructor addConversionConstructor(@NotNull CtClass implementationClass, @NotNull Collection<CtMethod> initializers) {
        CtConstructor constructor = null;
        CtClass ctObject = null;
        CtClass ctAssembler = null;
        JavassistEmitter emitter = null;
        String body = null;
        List<Statement> statements = new ArrayList<Statement>(initializers.size());

        try {
            ctObject = classPool.get(Object.class.getName());
        } catch (NotFoundException e) {
            throw new DataObjectGenerationException("Failed to locate class file for java.lang.Object", e);
        }

        try {
            ctAssembler = classPool.get(Assembler.class.getName());
        } catch (NotFoundException e) {
            throw new DataObjectGenerationException("Failed to locate class file for " + Assembler.class.getName(), e);
        }

        try {
            constructor = CtNewConstructor.make(new CtClass[]{ctObject, ctAssembler}, new CtClass[]{}, implementationClass);
        } catch (CannotCompileException e) {
            throw new DataObjectGenerationException("Constructor " + implementationClass.getSimpleName() + "(java.lang.Object) could not be created", e);
        }

        emitter = new JavassistEmitter(classPool, constructor, Pointcut.BEFORE);

        for (CtMethod initializer : initializers) {
            statements.add(call(initializer.getName()).with($(0), $(1)));
        }

        if (initializers.isEmpty()) {
            // Javassist will generate a default call to a super constructor with the same constructor if
            // no body is defined. Add an explicit call to the super-constructor to avoid this.
            body = "super();";
        } else {
            body = emitter.generateCode(body(statements.toArray(new Statement[statements.size()])));
        }

        try {
            constructor.setBody("{ " + body + " }");
        } catch (CannotCompileException e) {
            throw new DataObjectGenerationException("Failed to generate constructor for data object implementation class. Body is not valid: " + body, e);
        }

        try {
            implementationClass.addConstructor(constructor);
        } catch (CannotCompileException e) {
            throw new DataObjectGenerationException("Constructor could not be added to data object implementation class. See stack trace for details.", e);
        }

        return constructor;
    }

    public CtMethod addEqualsMethod(CtClass implementationClass, List<ValueDescriptor> values) {
        CtClass ctObject = null;
        StringBuilder body = new StringBuilder();
        CtMethod ctEquals = null;

        try {
            ctObject = classPool.get(Object.class.getName());
        } catch (NotFoundException e) {
            throw new DataObjectGenerationException("Failed to locate class file for java.lang.Object)");
        }

        body.append("{");
        body.append("if ($1 == null || !getClass().equals($1.getClass())) return false;");
        body.append(implementationClass.getName()).append(" that = (").append(implementationClass.getName()).append(") $1;");

        for (ValueDescriptor value : values) {
            if (value.getPropertyType().isPrimitive()) {
                body.append("if (this.").append(value.getProperty()).append("!=that.").append(value.getProperty()).append(") return false;");
            } else if (value.getPropertyType().isArray()) {
                body.append("if (this.").append(value.getProperty()).append("==null && that.")
                        .append(value.getProperty()).append("!=null || this.")
                        .append(value.getProperty()).append(" != null && !java.util.Arrays.equals(this.")
                        .append(value.getProperty()).append(", that.")
                        .append(value.getProperty()).append(")) return false;");
            } else {
                body.append("if (this.").append(value.getProperty()).append("==null && that.")
                        .append(value.getProperty()).append("!=null || this.")
                        .append(value.getProperty()).append(" != null && !this.")
                        .append(value.getProperty()).append(".equals(that.")
                        .append(value.getProperty()).append(")) return false;");
            }                                  
        }

        body.append("return true;");
        body.append("}");

        try {
            ctEquals = CtNewMethod.make(CtClass.booleanType, "equals", new CtClass[] { ctObject }, new CtClass[0], body.toString(), implementationClass);
        } catch (CannotCompileException e) {
            throw new DataObjectGenerationException("Failed to create equals method: " + body.toString(), e);
        }

        try {
            implementationClass.addMethod(ctEquals);
        } catch (CannotCompileException e) {
            throw new DataObjectGenerationException("Failed to add equals method to implementation class: " + body.toString(), e);
        }

        return ctEquals;
    }

}
