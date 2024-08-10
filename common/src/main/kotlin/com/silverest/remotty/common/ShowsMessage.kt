package com.silverest.remotty.common

import java.io.Serializable

data class ShowsMessage(override val signal: Signal, val shows: List<ShowDescriptor>): Serializable, IMessage(signal)
