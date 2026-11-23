package com.gabriel.estante.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * O banco de dados do app (arquivo "estante.db").
 * Ao mudar a entidade Livro no futuro, é preciso aumentar a "version".
 */
@Database(entities = [Livro::class], version = 2, exportSchema = false)
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
                )
                    // O app ainda não foi lançado, então não há dados de usuários a preservar.
                    // Em vez de escrever uma migração, o Room apaga e recria o banco quando a
                    // versão muda. Depois do lançamento, troque isto por migrações de verdade.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { instancia = it }
            }
    }
}
