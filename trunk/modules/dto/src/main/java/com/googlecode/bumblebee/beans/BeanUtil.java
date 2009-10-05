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

import com.googlecode.bumblebee.dto.Assembler;
import com.googlecode.bumblebee.dto.AssemblyException;
import com.googlecode.bumblebee.dto.DataObject;
import net.sf.jdpa.NotEmpty;
import net.sf.jdpa.NotNull;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Utility class the provides some support for managing java beans.
 *
 * @author Andreas Nilsson
 */
public class BeanUtil {

    public static final List<String> ACCESSOR_PREFIXES = Arrays.asList(
            "get", "is", "has", "was", "can", "may", "will", "could", "had", "have"
    );

    public static String getPropertyName(@NotNull Method method) {
        String methodName = method.getName();
        String prefix = null;

        for (Iterator<String> i = ACCESSOR_PREFIXES.iterator(); i.hasNext() && prefix == null;) {
            String currentPrefix = i.next();

            if (methodName.startsWith(currentPrefix)) {
                if (methodName.length() <= currentPrefix.length()) {
                    throw new InvalidAccessorException("Method " + method.getDeclaringClass().getSimpleName()
                            + "." + methodName + " is not a property");
                } else if (Character.isUpperCase(methodName.charAt(currentPrefix.length()))) {
                    prefix = currentPrefix;
                }
            }
        }

        if (prefix == null) {
            throw new InvalidAccessorException("Method " + method.getDeclaringClass().getSimpleName() + "."
                    + method.getName() + " does not have a valid accessor prefix");
        } else if (method.getParameterTypes().length > 0) {
            throw new InvalidAccessorException("Accessor " + method.getDeclaringClass().getSimpleName() + "."
                    + method.getName() + " should have an empty parameter list");
        } else if (method.getReturnType().equals(void.class)) {
            throw new InvalidAccessorException("Accessor " + method.getDeclaringClass().getSimpleName() + "."
                    + method.getName() + " cannot have void return type");
        } else {
            return Character.toLowerCase(methodName.charAt(prefix.length())) + methodName.substring(prefix.length() + 1);
        }
    }

    /**
     * Evaluates the specified property on the provided object. All valid prefixes will be tested in priority
     * order according to the following list: get, is, has, was, can, may, will, could, had, have.
     *
     * @param object       The object on which the property should be evaluated.
     * @param propertyName The property to fetch from the provided object.
     * @return The property value.
     */
    @SuppressWarnings("unchecked")
    public static Object getProperty(@NotNull Object object, @NotEmpty String propertyName) {
        if (object instanceof Collection) {
            Collection collection = (Collection) object;
            Collection intermediate = (object instanceof Set ? new HashSet(collection.size()) : new ArrayList(collection.size()));

            for (Object element : collection) {
                intermediate.add(getProperty(element, propertyName));
            }

            return intermediate;
        } else if (object.getClass().isArray()) {
            int length = Array.getLength(object);
            Collection intermediate = new ArrayList(length);

            for (int i = 0; i < length; i++) {
                intermediate.add(getProperty(Array.get(object, i), propertyName));
            }

            return intermediate;
        } else {
            Method method = null;
            String intermediate = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);


            for (String prefix : ACCESSOR_PREFIXES) {
                try {
                    method = object.getClass().getDeclaredMethod(prefix + intermediate);
                } catch (NoSuchMethodException e) {
                    // Ignore
                }
            }

            if (method == null) {
                throw new PropertyAccessException("No accessor for property " + object.getClass().getSimpleName() + "." + propertyName + " could be found.");
            }

            try {
                method.setAccessible(true);
            } catch (SecurityException e) {
                // Ignore this exception. If the method is public, we'll manage to invoke it anyway. If it is not public
                // we want an exception that describes the access violation properly.
            }

            try {
                return method.invoke(object);
            } catch (IllegalAccessException e) {
                throw new PropertyAccessException("Failed to access property " + object.getClass().getSimpleName() + "." +
                        propertyName + ". Make sure the accessor " + method.getName() + " is public.", e);
            } catch (InvocationTargetException e) {
                throw new PropertyAccessException("Accessor of property " + object.getClass().getSimpleName() + "." +
                        propertyName + " caused an exception. Check the stack trace for details.", e);
            }
        }
    }

    public static Object getUnwrappableValue(@NotNull Object value, @NotEmpty String wrapperType) {
        // TODO Cast if e.g. value is an Integer and wrapperType is Double
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Object copy(Object object, @NotNull Class propertyType, @NotNull Class componentType, @NotNull Assembler assembler) {
        if (propertyType.isArray()) {
            if (!(object instanceof Collection) && object instanceof Iterable) {
                object = ((Iterable) object).iterator();
            }

            if (object instanceof Iterator) {
                object = copyIterator(object);
            }

            if (object instanceof Collection) {
                Collection collection = (Collection) object;
                Object array = Array.newInstance(componentType, collection.size());
                int index = 0;

                for (Object element : collection) {
                    Array.set(array, index++, copy(element, componentType, componentType, assembler));
                }

                return array;
            } else if (object != null && object.getClass().isArray()) {
                int length = Array.getLength(object);
                Object array = Array.newInstance(componentType, length);

                for (int i = 0; i < length; i++) {
                    Array.set(array, i, copy(Array.get(object, i), componentType, componentType, assembler));
                }

                return array;
            } else {
                throw new IllegalArgumentException("Can't copy " + (object == null ? "null" : object.getClass().getName()) + " to " + componentType.getName() + "[]");
            }
        } else if (Collection.class.isAssignableFrom(propertyType)) {
            if (!(object instanceof Collection) && object instanceof Iterable) {
                object = ((Iterable) object).iterator();
            }

            if (object instanceof Iterator) {
                object = copyIterator(object);

            }

            if (object instanceof Collection) {
                Collection source = (Collection) object;
                Collection target = newCollection(propertyType, source.size());

                for (Object element : source) {
                    Object copy = copy(element, componentType, componentType, assembler);

                    if (!componentType.isInstance(copy)) {
                        throw new AssemblyException("Incompatible element in collection " + source + ": element " + element + " is not assignable to " + componentType.getName());
                    }

                    target.add(copy);
                }

                return target;
            } else if (object.getClass().isArray()) {
                if (!componentType.isAssignableFrom(object.getClass().getComponentType())) {
                    throw new AssemblyException("Invalid component type " + object.getClass().getComponentType().getName() + ": expected " + componentType.getName());
                } else {
                    int length = Array.getLength(object);
                    Collection target = newCollection(propertyType, length);

                    for (int i = 0; i < length; i++) {
                        target.add(Array.get(object, i));
                    }

                    return target;
                }
            }
        } else if (propertyType.getAnnotation(DataObject.class) != null && object != null) {
            object = assembler.assemble(object, propertyType);
        }

        return object;
    }

    protected static Object copyIterator(Object object) {
        List collection = new LinkedList();
        Iterator i = (Iterator) object;

        while (i.hasNext()) {
            collection.add(i.next());
        }

        object = collection;
        return object;
    }

    protected static Collection newCollection(Class<?> type, int size) {
        if (type.equals(Collection.class) || type.equals(List.class)) {
            return new ArrayList(size);
        } else if (type.equals(Set.class)) {
            return new HashSet(size);
        } else if (type.isInterface()) {
            throw new AssemblyException("Unsupported collection type " + type.getName());
        } else if ((type.getModifiers() & Modifier.ABSTRACT) != 0) {
            throw new AssemblyException("Unsupported collection type " + type.getName() + ": type is not recognized and can't be instantiated");
        } else {
            try {
                return (Collection) type.newInstance();
            } catch (InstantiationException e) {
                throw new IllegalArgumentException("Failed to instantiate collection type " + type.getName(), e);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Default constructor of collection type " + type.getName() + " is not accessible", e);
            }
        }
    }

    /**
     * Extracts the component type of a field or a return value with type java.util.Collection. The format
     * of the generic type is e.g. "java.util.Collection&lt;java.lang.String&gt;".
     *
     * @param genericType The generic type declaration.
     * @return The component type of the collection.
     */
    public static Class<?> getCollectionComponentType(@NotEmpty String genericType) {
        int n = genericType.indexOf('<');
        String componentTypeName = Object.class.getName();

        if (n != -1) {
            int m = genericType.lastIndexOf('>');

            if (m == -1) {
                throw new IllegalArgumentException("Type clause is not closed in generic declaration: " + genericType);
            } else if (m != genericType.length() - 1) {
                throw new IllegalArgumentException("Type clause did not terminate at EOF in generic declaration: " + genericType);
            } else {
                componentTypeName = genericType.substring(n + 1, m);
                n = componentTypeName.indexOf('<');

                if (n != -1) {
                    // Strip generics on component type
                    componentTypeName = componentTypeName.substring(0, n);
                }
            }
        }

        if (componentTypeName.equals("?")) {
            // Default to java.lang.Object if the component type is not defined
            componentTypeName = Object.class.getName();
        }

        try {
            return Class.forName(componentTypeName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Invalid generic declaration: " + genericType + ": component type " + componentTypeName + " could not be found");
        }
    }

    public static Class<?> getComponentTypeOfProperty(@NotNull Class<?> type, @NotEmpty String propertyName) {
        String getterName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        Method method = null;
        Class returnType = null;

        try {
            method = type.getDeclaredMethod(getterName);
        } catch (NoSuchMethodException e) {
            throw new PropertyAccessException("No property [" + propertyName + "] exists in class [" + type.getName() + "]");
        }

        returnType = method.getReturnType();

        if (returnType.isArray()) {
            return returnType.getComponentType();
        } else if (Collection.class.isAssignableFrom(returnType)) {
            return getCollectionComponentType(method.getGenericReturnType().toString());
        }

        return null;
    }

}
