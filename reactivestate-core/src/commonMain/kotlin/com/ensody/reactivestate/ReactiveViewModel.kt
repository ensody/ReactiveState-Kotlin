package com.ensody.reactivestate

/**
 * Base class for ViewModels which provide a [context] for common APIs across ViewModels.
 *
 * In particular, the [ReactiveViewModelContext.preInit] hook can be used to launch coroutines via a [CoroutineLauncher]
 * directly from the ViewModel constructor, so you can have nicer error handling and loading indicators.
 *
 * Note that [context] is `open` so you can define your own subclass with a customized context class, providing
 * additional useful attributes specific to your app.
 */
@ExperimentalReactiveStateApi
public abstract class ReactiveViewModel(
    /** This is `open` to allow overriding with a more specific type. */
    public open val context: ReactiveViewModelContext,
) : BaseReactiveState<ErrorEvents>(context.scope), OnReactiveStateAttachedTo {
    override fun onReactiveStateAttachedTo(parent: Any) {
        context.preInit.trigger(this)
    }
}

@ExperimentalReactiveStateApi
public interface ReactiveViewModelContext : ReactiveStateContext {
    /** A hook to start initializing e.g. caches when the ViewModel gets constructed. */
    public val preInit: OneTimeEvent<CoroutineLauncher>
}

@ExperimentalReactiveStateApi
public fun ReactiveViewModelContext(reactiveStateContext: ReactiveStateContext): ReactiveViewModelContext =
    DefaultReactiveViewModelContext(reactiveStateContext)

private class DefaultReactiveViewModelContext(
    delegate: ReactiveStateContext,
) : ReactiveViewModelContext, ReactiveStateContext by delegate {
    override val preInit: OneTimeEvent<CoroutineLauncher> = DefaultOneTimeEvent()
}
