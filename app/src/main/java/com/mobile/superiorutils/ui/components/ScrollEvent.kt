package com.mobile.superiorutils.ui.components

sealed interface ScrollEvent {
    data class NewMessageInserted(val isFromMe: Boolean) : ScrollEvent
    object OlderMessagesLoaded : ScrollEvent
    object JumpToBottomRequested : ScrollEvent
}
