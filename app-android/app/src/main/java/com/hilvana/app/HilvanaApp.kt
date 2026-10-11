package com.hilvana.app

import android.app.Application
import android.content.Context
import com.hilvana.app.data.HilvanaDatabase

/**
 * Punto de acceso único a la base de datos para el prototipo.
 * (En el MVP esto pasará a inyección de dependencias con Hilt.)
 */
class HilvanaApp : Application() {
    val database: HilvanaDatabase by lazy { HilvanaDatabase.create(this) }
}

val Context.database: HilvanaDatabase
    get() = (applicationContext as HilvanaApp).database
