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

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import java.lang.annotation.*;
import java.util.Arrays;

import javassist.CtClass;
import javassist.ClassPool;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.annotation.*;

import javax.xml.bind.annotation.XmlType;

/**
 * @author Andreas Nilsson
 */
public class AnnotationUtilTest {

    private ClassPool classPool;

    private CtClass ctSourceClass;

    private CtClass ctTargetClass;

    @Before
    public void setup() throws NotFoundException {
        this.classPool = ClassPool.getDefault();
        this.ctSourceClass = classPool.get(DummySourceClass.class.getName());
        this.ctTargetClass = classPool.get(DummyTargetClass.class.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createAnnotationShouldNotAcceptNullClassPool() throws Exception {
        AnnotationUtil.createAnnotation(null, ctSourceClass.getClassFile().getConstPool(),
                DummySourceClass.class.getAnnotation(SampleTypeAnnotation.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void createAnnotationShouldNotAcceptNullConstPool() throws Exception {
        AnnotationUtil.createAnnotation(classPool, null, DummySourceClass.class.getAnnotation(SampleTypeAnnotation.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void createAnnotationShouldNotAcceptNullAnnotation() throws Exception {
        AnnotationUtil.createAnnotation(classPool, ctSourceClass.getClassFile().getConstPool(), null);
    }

    @Test
    public void annotationShouldBeCopiedToAttributes() throws Exception {
        AnnotationsAttribute annotationsAttribute = createAnnotationsAttribute(ctTargetClass);

        javassist.bytecode.annotation.Annotation newAnnotation
                = AnnotationUtil.createAnnotation(classPool,
                ctTargetClass.getClassFile().getConstPool(),
                DummySourceClass.class.getAnnotation(SampleTypeAnnotation.class));

        annotationsAttribute.addAnnotation(newAnnotation);

        ctTargetClass.setName("DummyTargetClass$annotationShouldBeCopiedToAttributes");
        ctTargetClass.setAttribute(AnnotationsAttribute.visibleTag, annotationsAttribute.get());

        Class resultingClass = ctTargetClass.toClass();

        SampleTypeAnnotation annotation = (SampleTypeAnnotation) resultingClass.getAnnotation(SampleTypeAnnotation.class);
        
        assertNotNull(annotation);

        assertEquals(true, annotation.booleanValue());
        assertEquals((byte) 1, annotation.byteValue());
        assertEquals('J', annotation.charValue());
        assertEquals((short) 2, annotation.shortValue());
        assertEquals(3, annotation.intValue());
        assertEquals(4L, annotation.longValue());
        assertEquals(5f, annotation.floatValue(), .1);
        assertEquals(6d, annotation.doubleValue(), .1);
        assertEquals("FooBar", annotation.stringValue());
        assertEquals(String.class, annotation.classValue());
        assertEquals("foo", annotation.annotationValue().name());
        assertArrayEquals(new String[] { "foo", "bar"}, annotation.arrayValue());
        assertEquals(SampleEnum.VALUE_TWO, annotation.enumValue());
    }

    //
    // Support methods
    //

    protected AnnotationsAttribute createAnnotationsAttribute(CtClass ctClass) {
        return new AnnotationsAttribute(ctClass.getClassFile().getConstPool(), AnnotationsAttribute.visibleTag);
    }

    //
    // Support classes
    //

    public enum SampleEnum {
        VALUE_ONE,
        VALUE_TWO
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface SampleTypeAnnotation {

        boolean booleanValue();

        byte byteValue();

        char charValue();

        short shortValue();

        int intValue();

        long longValue();

        float floatValue();

        double doubleValue();

        String stringValue();

        Class<?> classValue();

        XmlType annotationValue();

        String[] arrayValue();

        SampleEnum enumValue();

    }

    @SampleTypeAnnotation(
            booleanValue = true,
            byteValue = 1,
            charValue = 'J',
            shortValue = 2,
            intValue = 3,
            longValue = 4,
            floatValue = 5,
            doubleValue = 6,
            stringValue = "FooBar",
            classValue = String.class,
            annotationValue = @XmlType(name = "foo"),
            arrayValue = {"foo", "bar"},
            enumValue = SampleEnum.VALUE_TWO
    )
    public static class DummySourceClass {

    }

    public static class DummyTargetClass {

    }

}
