package com.hilvana.app

import android.app.Application
import android.content.Context
import com.hilvana.app.data.HilvanaDatabase
import com.hilvana.app.data.ItemRepository

/**
 * Punto de acceso único a los datos para el prototipo.
 * (En el MVP esto pasará a inyección de dependencias con Hilt.)
 */
class HilvanaApp : Application() {
    val database: HilvanaDatabase by lazy { HilvanaDatabase.create(this) }
    val repository: ItemRepository by lazy { ItemRepository(this, database.itemDao()) }
}

val Context.database: HilvanaDatabase
    get() = (applicationContext as HilvanaApp).database

val Context.repository: ItemRepository
    get() = (applicationContext as HilvanaApp).repository
