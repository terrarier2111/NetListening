/*
Copyright 2021 Terrarier2111

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
package de.terrarier.netlistening.api;

import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.NotNull;

/**
 * @param <T> the type of the data contained in this DataComponent.
 * @author Terrarier2111
 * @since 1.0
 */
public final class DataComponent<T> {

    private final DataType<T> type;
    private T content;

    public DataComponent(@NotNull DataType<T> type, T content) {
        this.type = type;
        this.content = content;
    }

    /**
     * @return the type of the content.
     */
    @AssumeNotNull
    public DataType<T> getType() {
        return type;
    }

    /**
     * @return the contained data.
     */
    @NotNull
    public T getData() {
        return content;
    }

    /**
     * Sets the passed data as the content.
     *
     * @param data the new content value.
     */
    @Deprecated
    public void setData(T data) {
        content = data;
    }

}
