package de.terrarier.netlistening.api.event;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @since 1.0
 * @author Terrarier2111
 */
@ApiStatus.Internal
public final class EventManager {

	private final Map<ListenerType, Set<Listener<?>>[]> listeners = new ConcurrentHashMap<>();
	private final DataHandler handler;
	
	public EventManager(@NotNull DataHandler handler) {
		this.handler = handler;
	}

	@SuppressWarnings("unchecked")
	public void registerListener(@NotNull Listener<?> listener) {
		final Class<?> listenerClass = listener.getClass();
		final ListenerType type = ListenerType.resolveType(listenerClass);
		Objects.requireNonNull(type, "The type of the listener " + listenerClass.getName() + " cannot be resolved!");
		
		if(type == ListenerType.DECODE) {
			handler.addListener((DecodeListener) listener);
		}

		final EventListener.Priority priority = resolvePriority(listener);
		final Set<Listener<?>>[] listenerPriorities = listeners.computeIfAbsent(type, k -> new Set[5]);
		final int ordinal = priority.ordinal();
		Set<Listener<?>> demanded = listenerPriorities[ordinal];

		if(demanded == null) {
			demanded = new HashSet<>();
			listenerPriorities[ordinal] = demanded;
		}

		demanded.add(listener);
	}

	public void unregisterListeners(@NotNull ListenerType listenerType) {
		if(listenerType == ListenerType.DECODE) {
			handler.unregisterListeners();
		}
		final Set<Listener<?>>[] listenerPriorities = listeners.get(listenerType);
		if(listenerPriorities != null) {
			for(int i = 0; i < 5; i++) {
				final Set<Listener<?>> listeners = listenerPriorities[i];
				if(listeners != null) {
					listeners.clear();
				}
			}
		}
	}

	public boolean callEvent(@NotNull ListenerType listenerType, @NotNull Event event) {
		return callEvent(listenerType, CancelAction.IGNORE, event);
	}

	public boolean callEvent(@NotNull ListenerType listenerType, @NotNull CancelAction cancelAction,
							 @NotNull Event event) {
		return callEvent(listenerType, cancelAction, new NoopEventProvider(event));
	}

	@SuppressWarnings("unchecked")
	public boolean callEvent(@NotNull ListenerType listenerType, @NotNull CancelAction cancelAction,
							 @NotNull EventProvider<?> eventProvider) {
		final Set<Listener<?>>[] listeners = this.listeners.get(listenerType);
		if(listeners == null) {
			return false;
		}
		boolean cancellable = false;
		Event event = null;
		for (int i = 0; i < 5; i++) {
			final Set<Listener<?>> priorityListeners = listeners[i];
			if(priorityListeners != null) {
				if(event == null) {
					event = eventProvider.provide();
					cancellable = event instanceof Cancellable;
				}
				for(Listener listener : priorityListeners) {
					try {
						listener.trigger(event);
					}catch (Throwable throwable) {
						if(event.getClass() != ExceptionTrowEvent.class && !(throwable instanceof OutOfMemoryError)) {
							final ExceptionTrowEvent exceptionTrowEvent = new ExceptionTrowEvent(throwable);
							handleExceptionThrown(exceptionTrowEvent);
						}
					}
					if(cancellable && cancelAction == CancelAction.INTERRUPT && ((Cancellable) event).isCancelled()) {
						return true;
					}
				}
			}
		}
		return cancellable && ((Cancellable) event).isCancelled();
	}

	@NotNull
	private EventListener.Priority resolvePriority(@NotNull Listener<?> listener) {
		try {
			final Method method = listener.getClass().getDeclaredMethod("trigger", Event.class);
			final EventListener eventListener = method.getAnnotation(EventListener.class);
			if(eventListener != null) {
				return eventListener.priority();
			}
		} catch (NoSuchMethodException e) {
			e.printStackTrace(); // TODO: Handle this better because it's something what shouldn't occur at all.
		}
		return EventListener.Priority.MEDIUM;
	}

	public void handleExceptionThrown(@NotNull ExceptionTrowEvent exceptionTrowEvent) {
		callEvent(ListenerType.EXCEPTION_THROW, exceptionTrowEvent);
		if (exceptionTrowEvent.isPrint()) {
			exceptionTrowEvent.getException().printStackTrace();
		}
	}

	public enum CancelAction {

		IGNORE, INTERRUPT

	}

	public interface EventProvider<T extends Event> {

		@NotNull
		T provide();

	}

	private static final class NoopEventProvider implements EventProvider<Event> {

		private final Event event;

		private NoopEventProvider(@NotNull Event event) {
			this.event = event;
		}

		@NotNull
		@Override
		public Event provide() {
			return event;
		}

	}

}
