package de.terrarier.netlistening.api.event;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class EventManager {

	private final Map<ListenerType, Set<Listener<?>>[]> listeners = new HashMap<>();
	private final DataHandler handler;
	
	@SuppressWarnings("unchecked")
	public EventManager(@NotNull DataHandler handler) {
		this.handler = handler;
		for(ListenerType type : ListenerType.VALUES) {
			listeners.put(type, new Set[5]);
		}
	}
	
	public void registerListener(@NotNull Listener<?> listener) {
		final Class<?> listenerClass = listener.getClass();
		final ListenerType type = ListenerType.resolveType(listenerClass);

		if(type == null) {
			throw new NullPointerException("The type of the listener " + listenerClass.getName() + " cannot be resolved!");
		}
		
		if(type == ListenerType.DECODE) {
			handler.addListener((DecodeListener) listener);
		}

		final EventListener.Priority priority = resolvePriority(listener);

		final Set<Listener<?>>[] listenerPriorities = listeners.get(type);
		final int ordinal = priority.ordinal();
		Set<Listener<?>> demanded = listenerPriorities[ordinal];

		if(demanded == null) {
			demanded = new HashSet<>();
			listenerPriorities[ordinal] = demanded;
		}

		demanded.add(listener);
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
		boolean cancellable = false;
		Event event = null;
		final Set<Listener<?>>[] listeners = this.listeners.get(listenerType);
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
					}catch (Exception e) {
						if(event.getClass() != ExceptionTrowEvent.class) { // TODO: Check if this check is necessary
							final ExceptionTrowEvent exceptionTrowEvent = new ExceptionTrowEvent(e);
							callEvent(ListenerType.EXCEPTION_THROW, exceptionTrowEvent);
							if (exceptionTrowEvent.isPrint()) {
								exceptionTrowEvent.getException().printStackTrace();
							}
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
			e.printStackTrace(); // TODO: Handle this better cuz it's something what shouldn't occur at all
		}
		return EventListener.Priority.MEDIUM;
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
