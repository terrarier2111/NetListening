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

import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class EventManager {

    // TODO: Make this multithreading safe!

    private final Map<ListenerType, List<Listener<?>>[]> listeners = new ConcurrentHashMap<>();
    private final DataHandler handler;

    public EventManager(@AssumeNotNull DataHandler handler) {
        this.handler = handler;
    }

    @SuppressWarnings("unchecked")
    public long registerListener(@AssumeNotNull Listener<?> listener) {
        final Class<?> listenerClass = listener.getClass();
        final ListenerType type = ListenerType.resolveType(listenerClass);
        Objects.requireNonNull(type, "The type of the listener " + listenerClass.getName() + " cannot be resolved!");

        final EventListener.Priority priority = resolvePriority(listener);
        final List<Listener<?>>[] listenerPriorities = listeners.computeIfAbsent(type, k -> new List[5]);
        final int priorityId = priority.ordinal();
        List<Listener<?>> demanded = listenerPriorities[priorityId];

        if (demanded == null) {
            demanded = new ArrayList<>();
            listenerPriorities[priorityId] = demanded;
        }
        long listenerId = demanded.size();
        listenerId |= (long) priorityId << 32;
        listenerId |= (long) type.ordinal() << 40;

        if (type == ListenerType.DECODE) {
            listenerId |= ((long) handler.addListener((DecodeListener) listener)) << 16;
        }

        demanded.add(listener);
        return listenerId;
    }

    @ApiStatus.Experimental
    public void unregisterListeners(@AssumeNotNull ListenerType listenerType) {
        if (listenerType == ListenerType.DECODE) {
            handler.unregisterListeners();
        }
        final List<Listener<?>>[] listenerPriorities = listeners.get(listenerType);
        if (listenerPriorities != null) {
            for (int i = 0; i < 5; i++) {
                final List<Listener<?>> listeners = listenerPriorities[i];
                if (listeners != null) {
                    listeners.clear();
                }
            }
        }
    }

    public void unregisterListener(long listenerId) {
        final ListenerType type = ListenerType.VALUES[(byte) (listenerId >>> 40)];
        if (type == ListenerType.DECODE) {
            handler.removeListener((char) (listenerId >>> 16));
        }
        final List<Listener<?>>[] listenerPriorities = listeners.get(type);
        listenerPriorities[(byte) (listenerId >>> 32)].remove((char) listenerId);
    }

    public boolean callEvent(@AssumeNotNull ListenerType listenerType, @AssumeNotNull Event event) {
        return callEvent(listenerType, CancelAction.IGNORE, event);
    }

    public boolean callEvent(@AssumeNotNull ListenerType listenerType, @AssumeNotNull CancelAction cancelAction,
                             @AssumeNotNull Event event) {
        final List<Listener<?>>[] listeners = this.listeners.get(listenerType);
        if (listeners == null) {
            return false;
        }
        final boolean cancellable = event instanceof Cancellable;
        for (int i = 0; i < 5; i++) {
            final List<Listener<?>> priorityListeners = listeners[i];
            if (priorityListeners != null) {
                final int priorityListenersSize = priorityListeners.size();
                for (int j = 0; j < priorityListenersSize; j++) {
                    final Listener listener = priorityListeners.get(j);
                    try {
                        listener.trigger(event);
                    } catch (Throwable throwable) {
                        if (event.getClass() != ExceptionTrowEvent.class && !(throwable instanceof OutOfMemoryError)) {
                            handleExceptionThrown(throwable);
                        }
                    }
                    if (cancellable && cancelAction == CancelAction.INTERRUPT && ((Cancellable) event).isCancelled()) {
                        return true;
                    }
                }
            }
        }
        return cancellable && ((Cancellable) event).isCancelled();
    }

    @AssumeNotNull
    private EventListener.Priority resolvePriority(@AssumeNotNull Listener<?> listener) {
        try {
            final Method method = listener.getClass().getDeclaredMethod("trigger", Event.class);
            final EventListener eventListener = method.getAnnotation(EventListener.class);
            if (eventListener != null) {
                return eventListener.priority();
            }
        } catch (NoSuchMethodException e) {
            // This shouldn't occur ever!
            e.printStackTrace();
        }
        return EventListener.Priority.MEDIUM;
    }

    public void handleExceptionThrown(@AssumeNotNull Throwable thr) {
        final ExceptionTrowEvent exceptionTrowEvent = new ExceptionTrowEvent(thr);
        callEvent(ListenerType.EXCEPTION_THROW, exceptionTrowEvent);
        if (exceptionTrowEvent.isPrint()) {
            exceptionTrowEvent.getException().printStackTrace();
        }
    }

    @ApiStatus.Internal
    public enum CancelAction {

        IGNORE, INTERRUPT

    }

}
