package com.ensody.reactivestate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * An interface for ViewModels and any other objects that can trigger one-time events/actions and handle errors.
 *
 * Make sure you always launch coroutines via [launch] (instead of the scope) to get automatic error handling.
 *
 * @see [BaseReactiveState] for a ready-made base class (or delegate).
 */
public interface ReactiveState<E : ErrorEvents> : CoroutineLauncher {
    public val eventNotifier: EventNotifier<E>
}

/**
 * Base class/delegate for ViewModels and other objects that can trigger one-time events/actions and handle errors.
 *
 * Make sure you always launch coroutines via [launch] (instead of the scope) to get automatic error handling.
 *
 * Example:
 *
 * ```kotlin
 * // You can compose multiple events interfaces with simple inheritance (more elegant than sealed classes)
 * interface FooEvents : ErrorEvents, OtherEvents, AndMoreEvents {
 *     fun onUserIsUnauthorized()
 * }
 *
 * class FooViewModel(scope: CoroutineScope) : BaseReactiveState<FooEvents>(scope) {
 *     private val _messages = MutableStateFlow<List<Message>>(emptyList())
 *     val messages: StateFlow<List<Messages>> = _messages
 *
 *     init {
 *         loadMessages()
 *     }
 *
 *     fun loadMessages() {
 *         launch {
 *             // Let's pretend this function returns null for unauthorized requests
 *             val messages = retrieveMessagesFromBackend()
 *             if (messages == null) {
 *                 eventNotifier { onUserIsUnauthorized() }
 *             } else {
 *                 _messages.value = messages
 *             }
 *         }
 *     }
 * }
 * ```
 */
public open class BaseReactiveState<E : ErrorEvents>(scope: CoroutineScope) :
    ReactiveState<E>, SimpleCoroutineLauncher(scope) {

    override val eventNotifier: EventNotifier<E> = EventNotifier()

    override fun onError(error: Throwable) {
        eventNotifier { onError(error) }
    }
}

/**
 * Creates and attaches a child [ReactiveState].
 *
 * This merges the child's [ReactiveState.eventNotifier] and [ReactiveState.isAnyLoading] into the parent.
 *
 * Example:
 *
 * ```kotlin
 * // The parent has to also implement the child events
 * interface ParentEvents : ChildEvents {
 *     fun onSomeEvent()
 * }
 *
 * class ParentViewModel(scope: CoroutineScope) : BaseReactiveState<ParentEvents>(scope) {
 *     val childViewModel by childReactiveState { ChildViewModel(scope) }
 * }
 *
 * interface ChildEvents : ErrorEvents {
 *     fun onSomeChildEvent()
 * }
 *
 * class ChildViewModel(scope: CoroutineScope) : BaseReactiveState<ChildEvents>(scope) {
 *     init {
 *         launch {
 *             // ...
 *             eventNotifier {
 *                 onSomeChildEvent()
 *             }
 *         }
 *     }
 * }
 * ```
 */
public fun <E : ErrorEvents, P : ReactiveState<out E>, RS : ReactiveState<E>> P.childReactiveState(
    block: () -> RS,
): ReadOnlyProperty<Any?, RS> {
    val child = block()
    isAnyLoading.add(child.isAnyLoading)
    launch(withLoading = null) {
        eventNotifier.emitAll(child.eventNotifier)
    }
    return WrapperProperty(child)
}

/** Just wraps an eagerly computed value in a property to allow `val foo by bar` notation. */
private class WrapperProperty<T>(val data: T) : ReadOnlyProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = data
}
