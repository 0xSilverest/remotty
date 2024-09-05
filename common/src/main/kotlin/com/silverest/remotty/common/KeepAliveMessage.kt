package com.silverest.remotty.common

data class KeepAliveMessage (override val signal: Signal= Signal.KEEP_ALIVE,
                             val message: String,
                             val timestamp: Long = System.currentTimeMillis()): IMessage(signal)