/*
Copyright 2021 The Netty Project/Terrarier2111

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package de.terrarier.netlistening.utils;

import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.*;

/**
 * @author The Netty Project/Terrarier2111
 * @since 1.10
 */
@ApiStatus.Internal
public final class TypeParameterResolver {

    private TypeParameterResolver() {
        throw new UnsupportedOperationException("This class may not be instantiated!");
    }

    @AssumeNotNull
    public static Class<?> find(@AssumeNotNull Object object, @AssumeNotNull Class<?> parametrizedSuperclass,
            @AssumeNotNull String typeParamName) {

        final Class<?> thisClass = object.getClass();
        Class<?> currentClass = thisClass;
        while(true) {
            if (currentClass.getSuperclass() == parametrizedSuperclass) {
                int typeParamIndex = -1;
                final TypeVariable<?>[] typeParams = currentClass.getSuperclass().getTypeParameters();
                for (int i = 0; i < typeParams.length; i ++) {
                    if (typeParamName.equals(typeParams[i].getName())) {
                        typeParamIndex = i;
                        break;
                    }
                }

                if (typeParamIndex < 0) {
                    throw new IllegalStateException(
                            "unknown type parameter '" + typeParamName + "': " + parametrizedSuperclass);
                }

                final Type genericSuperType = currentClass.getGenericSuperclass();
                if (!(genericSuperType instanceof ParameterizedType)) {
                    return Object.class;
                }

                final Type[] actualTypeParams = ((ParameterizedType) genericSuperType).getActualTypeArguments();

                Type actualTypeParam = actualTypeParams[typeParamIndex];
                if (actualTypeParam instanceof ParameterizedType) {
                    actualTypeParam = ((ParameterizedType) actualTypeParam).getRawType();
                }
                if (actualTypeParam instanceof Class) {
                    return (Class<?>) actualTypeParam;
                }
                if (actualTypeParam instanceof GenericArrayType) {
                    Type componentType = ((GenericArrayType) actualTypeParam).getGenericComponentType();
                    if (componentType instanceof ParameterizedType) {
                        componentType = ((ParameterizedType) componentType).getRawType();
                    }
                    if (componentType instanceof Class) {
                        return Array.newInstance((Class<?>) componentType, 0).getClass();
                    }
                }
                if (actualTypeParam instanceof TypeVariable) {
                    // Resolved type parameter points to another type parameter.
                    final TypeVariable<?> v = (TypeVariable<?>) actualTypeParam;
                    if (!(v.getGenericDeclaration() instanceof Class)) {
                        return Object.class;
                    }

                    currentClass = thisClass;
                    parametrizedSuperclass = (Class<?>) v.getGenericDeclaration();
                    typeParamName = v.getName();
                    if (parametrizedSuperclass.isAssignableFrom(thisClass)) {
                        continue;
                    }
                    return Object.class;
                }

                throw new IllegalStateException(
                        "cannot determine the type of the type parameter '" + typeParamName + "': " + thisClass);
            }
            currentClass = currentClass.getSuperclass();
            if (currentClass == null) {
                throw new IllegalStateException(
                        "cannot determine the type of the type parameter '" + typeParamName + "': " + thisClass);
            }
        }
    }

}
