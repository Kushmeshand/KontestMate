package com.example.kontestmate.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.kontestmate.models.BookmarkEntity

@Database(entities = [BookmarkEntity::class], version = 2)
abstract class BookmarkDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: BookmarkDatabase? = null

        fun getDatabase(context: Context): BookmarkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BookmarkDatabase::class.java,
                    "bookmark_db"
                )
                    .fallbackToDestructiveMigration() // 💥 auto-wipes old schema & recreates DB
                    .build()

                INSTANCE = instance
                instance
            }
        }

    }
}
