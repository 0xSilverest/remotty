package com.silverest.remotty.common

import java.io.Serializable

data class Message(override val signal: Signal, val content: String) : Serializable, IMessage(signal)
