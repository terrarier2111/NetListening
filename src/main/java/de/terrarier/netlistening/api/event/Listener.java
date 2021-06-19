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
package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.internal.AssumeNotNull;

/**
 * @param <T> the event which is handled by the listener.
 * @author Terrarier2111
 * @since 1.0
 */
public interface Listener<T extends Event> {

    /**
     * This method is getting called each time the event
     * passed as the generic type parameter gets called.
     *
     * @param value the event which is called.
     */
    void trigger(@AssumeNotNull T value);

}
