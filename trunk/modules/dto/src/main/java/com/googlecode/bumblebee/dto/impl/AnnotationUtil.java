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

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.*;
import net.sf.jdpa.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

import com.googlecode.bumblebee.dto.UnexpectedIntrospectionException;

/**
 * @author Andreas Nilsson
 */
public class AnnotationUtil {

    public static javassist.bytecode.annotation.Annotation createAnnotation(@NotNull ClassPool classPool,
                                                                            @NotNull ConstPool constPool,
                                                                            @NotNull Annotation annotation)
            throws NotFoundException {

        CtClass ctAnnotationClass = classPool.get(annotation.annotationType().getName());
        javassist.bytecode.annotation.Annotation newAnnotation = new javassist.bytecode.annotation.Annotation(constPool, ctAnnotationClass);

        for (Method method : annotation.annotationType().getMethods()) {
            boolean isAttribute = false;

            try {
                Annotation.class.getDeclaredMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException e) {
                isAttribute = true;
            }

            if (isAttribute) {
                Class returnType = method.getReturnType();
                Object returnValue = null;

                try {
                    returnValue = method.invoke(annotation);
                } catch (IllegalAccessException e) {
                    throw new UnexpectedIntrospectionException("Access to attribute '" + method.getName() + "' was denied", e);
                } catch (InvocationTargetException e) {
                    throw new UnexpectedIntrospectionException("Failed to access attribute '" + method.getName() + "' of annotation type '" + annotation.annotationType().getName() + "'", e);
                }

                newAnnotation.addMemberValue(method.getName(), createMemberValue(classPool, constPool, returnType, returnValue));
            }
        }

        return newAnnotation;
    }

    private static MemberValue createMemberValue(ClassPool classPool, ConstPool constPool, Class type, Object value) throws NotFoundException {
        MemberValue memberValue = null;

        if (type.isAnnotation()) {
            memberValue = new AnnotationMemberValue(createAnnotation(classPool, constPool, (Annotation) value), constPool);
        } else if (boolean.class.equals(type)) {
            memberValue = new BooleanMemberValue((Boolean) value, constPool);
        } else if (byte.class.equals(type)) {
            memberValue = new ByteMemberValue((Byte) value, constPool);
        } else if (char.class.equals(type)) {
            memberValue = new CharMemberValue((Character) value, constPool);
        } else if (short.class.equals(type)) {
            memberValue = new ShortMemberValue((Short) value, constPool);
        } else if (int.class.equals(type)) {
            memberValue = new IntegerMemberValue(constPool, (Integer) value);
        } else if (long.class.equals(type)) {
            memberValue = new LongMemberValue((Long) value, constPool);
        } else if (float.class.equals(type)) {
            memberValue = new FloatMemberValue((Float) value, constPool);
        } else if (double.class.equals(type)) {
            memberValue = new DoubleMemberValue((Double) value, constPool);
        } else if (String.class.equals(type)) {
            memberValue = new StringMemberValue((String) value, constPool);
        } else if (Class.class.equals(type)) {
            memberValue = new ClassMemberValue(((Class) value).getName(), constPool);
        } else if (type.isEnum()) {
            int typeIndex = constPool.addUtf8Info(type.getName());
            int valueIndex = constPool.addUtf8Info(value.toString());

            memberValue = new EnumMemberValue(typeIndex, valueIndex, constPool);
        } else if (type.isArray()) {
            MemberValue[] elements = new MemberValue[Array.getLength(value)];

            for (int i = 0; i < elements.length; i++) {
                elements[i] = createMemberValue(classPool, constPool, type.getComponentType(), Array.get(value, i));
            }

            ArrayMemberValue arrayMemberValue = new ArrayMemberValue(constPool);
            arrayMemberValue.setValue(elements);
            memberValue = arrayMemberValue;
        }

        return memberValue;
    }


}
