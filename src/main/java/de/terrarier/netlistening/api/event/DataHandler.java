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

import de.terrarier.netlistening.api.DataComponent;
import de.terrarier.netlistening.api.DataContainer;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.AssumeNotNull;
import de.terrarier.netlistening.network.PreparedListener;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class DataHandler {

    // TODO: Make this multithreading safe!

    private final List<PreparedListener> listeners = new ArrayList<>();

    public void processData(@AssumeNotNull List<DataComponent<?>> data, @AssumeNotNull ConnectionImpl connection) {
        final int dataSize = data.size();
        if (dataSize == 0) {
            return;
        }

        final DataContainer container = new DataContainer(data);
        final DecodeEvent event = new DecodeEvent(connection, container);
        final int listenerSize = listeners.size();
        int hash = 1;

        for (int i = 0; i < listenerSize; i++) {
            final PreparedListener listener = listeners.get(i);
            final int length = listener.getTypes().length;
            if (length != 0) {
                if (length != dataSize) {
                    continue;
                }

                if (hash == 1) {
                    for (int j = 0; j < dataSize; j++) {
                        hash = 31 * hash + data.get(j).getType().hashCode();
                    }
                }

                if (hash != listener.hashCode()) {
                    continue;
                }
            }
            container.resetReaderIndex();
            listener.getWrapped().trigger(event);
        }
    }

    int addListener(@AssumeNotNull DecodeListener listener) {
        final int id = listeners.size();
        if (id == Character.MAX_VALUE) {
            throw new IllegalStateException("It is only possible to register at most " + (int) Character.MAX_VALUE + " listeners!");
        }
        try {
            listeners.add(new PreparedListener(listener));
        } catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
        return id;
    }

    void removeListener(int id) {
        listeners.remove(id);
    }

    public void unregisterListeners() {
        listeners.clear();
    }

}
