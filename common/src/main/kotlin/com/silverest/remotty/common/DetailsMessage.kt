package com.silverest.remotty.common

import java.io.Serializable

data class DetailsMessage(override val signal: Signal, val details: EpisodeDetails) : Serializable, IMessage(signal)