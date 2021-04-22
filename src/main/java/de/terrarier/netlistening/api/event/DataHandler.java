package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.api.DataComponent;
import de.terrarier.netlistening.api.DataContainer;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.AssumeNotNull;
import de.terrarier.netlistening.network.PreparedListener;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 1.0
 * @author Terrarier2111
 */
@ApiStatus.Internal
public final class DataHandler {

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
                    for(int j = 0; j < dataSize; j++) {
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

	void addListener(@AssumeNotNull DecodeListener listener) {
        try {
            listeners.add(new PreparedListener(listener));
        } catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
    }

	public void unregisterListeners() {
		listeners.clear();
	}

}
