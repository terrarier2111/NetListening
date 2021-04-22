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
 * @since 1.0
 * @author Terrarier2111
 */
@ApiStatus.Internal
public final class EventManager {

	private final Map<ListenerType, List<Listener<?>>[]> listeners = new ConcurrentHashMap<>();
	private final DataHandler handler;
	
	public EventManager(@AssumeNotNull DataHandler handler) {
		this.handler = handler;
	}

	@SuppressWarnings("unchecked")
	public void registerListener(@AssumeNotNull Listener<?> listener) {
		final Class<?> listenerClass = listener.getClass();
		final ListenerType type = ListenerType.resolveType(listenerClass);
		Objects.requireNonNull(type, "The type of the listener " + listenerClass.getName() + " cannot be resolved!");
		
		if(type == ListenerType.DECODE) {
			handler.addListener((DecodeListener) listener);
		}

		final EventListener.Priority priority = resolvePriority(listener);
		final List<Listener<?>>[] listenerPriorities = listeners.computeIfAbsent(type, k -> new List[5]);
		final int ordinal = priority.ordinal();
		List<Listener<?>> demanded = listenerPriorities[ordinal];

		if(demanded == null) {
			demanded = new ArrayList<>();
			listenerPriorities[ordinal] = demanded;
		}

		demanded.add(listener);
	}

	public void unregisterListeners(@AssumeNotNull ListenerType listenerType) {
		if(listenerType == ListenerType.DECODE) {
			handler.unregisterListeners();
		}
		final List<Listener<?>>[] listenerPriorities = listeners.get(listenerType);
		if(listenerPriorities != null) {
			for(int i = 0; i < 5; i++) {
				final List<Listener<?>> listeners = listenerPriorities[i];
				if(listeners != null) {
					listeners.clear();
				}
			}
		}
	}

	public boolean callEvent(@AssumeNotNull ListenerType listenerType, @AssumeNotNull Event event) {
		return callEvent(listenerType, CancelAction.IGNORE, event);
	}

	public boolean callEvent(@AssumeNotNull ListenerType listenerType, @AssumeNotNull CancelAction cancelAction,
							 @AssumeNotNull Event event) {
		return callEvent(listenerType, cancelAction, new NoopEventProvider(event));
	}

	@SuppressWarnings("unchecked")
	public boolean callEvent(@AssumeNotNull ListenerType listenerType, @AssumeNotNull CancelAction cancelAction,
							 @AssumeNotNull EventProvider<?> eventProvider) {
		final List<Listener<?>>[] listeners = this.listeners.get(listenerType);
		if(listeners == null) {
			return false;
		}
		boolean cancellable = false;
		Event event = null;
		for (int i = 0; i < 5; i++) {
			final List<Listener<?>> priorityListeners = listeners[i];
			if(priorityListeners != null) {
				if(event == null) {
					event = eventProvider.provide();
					cancellable = event instanceof Cancellable;
				}
				for(int j = 0; j < priorityListeners.size(); j++) {
					final Listener listener = priorityListeners.get(j);
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

	@AssumeNotNull
	private EventListener.Priority resolvePriority(@AssumeNotNull Listener<?> listener) {
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

	public void handleExceptionThrown(@AssumeNotNull ExceptionTrowEvent exceptionTrowEvent) {
		callEvent(ListenerType.EXCEPTION_THROW, exceptionTrowEvent);
		if (exceptionTrowEvent.isPrint()) {
			exceptionTrowEvent.getException().printStackTrace();
		}
	}

	public enum CancelAction {

		IGNORE, INTERRUPT

	}

	public interface EventProvider<T extends Event> {

		@AssumeNotNull
		T provide();

	}

	private static final class NoopEventProvider implements EventProvider<Event> {

		private final Event event;

		private NoopEventProvider(@AssumeNotNull Event event) {
			this.event = event;
		}

		@AssumeNotNull
		@Override
		public Event provide() {
			return event;
		}

	}

}
