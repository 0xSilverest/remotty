package com.silverest.remotty.common

import java.io.Serializable

enum class Signal: Serializable {
    INCREASE, DECREASE, PLAY,
    SEND_COMMAND, EXIT,
    SEND_EPISODES, MUTE, PLAY_OR_PAUSE,
    SEEK_FORWARD, SEEK_BACKWARD,
    SHOWS_LIST, MODIFY_SHOWS, SEND_DETAILS,
    SKIP_CHAPTER, PUT_SUBS, CLOSE, PLAY_MOVIE
}