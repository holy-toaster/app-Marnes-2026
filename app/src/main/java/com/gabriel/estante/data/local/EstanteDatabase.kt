package com.gabriel.estante.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * O banco de dados do app (arquivo "estante.db").
 * Ao mudar a entidade Livro no futuro, é preciso aumentar a "version".
 */
@Database(entities = [Livro::class], version = 1, exportSchema = false)
abstract class EstanteDatabase : RoomDatabase() {

    abstract fun livroDao(): LivroDao

    companion object {
        // @Volatile: todas as threads enxergam o valor mais recente
        @Volatile
        private var instancia: EstanteDatabase? = null

        /** Devolve sempre o mesmo banco (abrir vários seria desperdício). */
        fun getInstance(context: Context): EstanteDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    EstanteDatabase::class.java,
                    "estante.db"
                ).build().also { instancia = it }
            }
    }
}
