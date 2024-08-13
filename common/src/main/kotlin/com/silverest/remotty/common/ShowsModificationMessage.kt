package com.silverest.remotty.common

import java.io.Serializable

data class ShowsModificationMessage(
    override val signal: Signal, val showsToAdd: Set<ShowDescriptor>,
    val showsToRemove: Set<ShowDescriptor>
) : Serializable, IMessage(signal)
