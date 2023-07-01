package com.ensody.reactivestate

public interface RevisionedValue<T> {
    public val revisionedValue: Pair<T, ULong>
}
