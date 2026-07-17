package com.mobile.superiorchat.ui.components

sealed interface ScrollEvent {
    data class NewMessageInserted(val isFromMe: Boolean) : ScrollEvent
    object OlderMessagesLoaded : ScrollEvent
    object JumpToBottomRequested : ScrollEvent
}
