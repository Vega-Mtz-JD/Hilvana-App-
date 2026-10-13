package com.hilvana.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hilvana.app.data.Folder
import com.hilvana.app.data.Item
import com.hilvana.app.repository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado y acciones de las pantallas. Sobrevive a giros de pantalla. */
class ItemsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = application.repository

    val items: StateFlow<List<Item>> = repository.items
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val folders: StateFlow<List<Folder>> = repository.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveFolder(folder: Folder) {
        viewModelScope.launch { repository.saveFolder(folder) }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch { repository.deleteFolder(folder) }
    }

    fun save(item: Item) {
        viewModelScope.launch { repository.save(item) }
    }

    fun delete(item: Item) {
        viewModelScope.launch { repository.delete(item) }
    }

    fun toggleDone(item: Item) {
        viewModelScope.launch { repository.save(item.copy(done = !item.done)) }
    }

    /** Sacar de la Bandeja: ya se revisó/ordenó. */
    fun markReviewed(item: Item) {
        viewModelScope.launch { repository.save(item.copy(inInbox = false)) }
    }
}
