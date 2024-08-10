package com.silverest.remotty.common

import java.io.Serializable

data class EpisodeMessage(override val signal: Signal, val shows: List<EpisodeDescriptor>): Serializable, IMessage(signal)
