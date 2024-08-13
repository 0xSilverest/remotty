package com.silverest.remotty.common

import java.io.Serializable

enum class Signal: Serializable {
    INCREASE, DECREASE, PLAY,
    SEND_COMMAND, EXIT,
    SEND_EPISODES, MUTE, PLAY_OR_PAUSE,
    SEEK_FORWARD, SEEK_BACKWARD,
    SKIP_BACKWARD, SKIP_FORWARD, SHOWS_LIST, MODIFY_SHOWS;
}