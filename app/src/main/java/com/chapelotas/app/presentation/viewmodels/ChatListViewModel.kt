package com.chapelotas.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapelotas.app.data.database.ChapelotasDatabase
import com.chapelotas.app.data.database.entities.ChatThread
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val database: ChapelotasDatabase
) : ViewModel() {

    val activeThreads: StateFlow<List<ChatThread>> = database.chatThreadDao()
        .observeActiveThreads()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalUnreadCount: StateFlow<Int> = database.chatThreadDao()
        .observeUnreadCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
}