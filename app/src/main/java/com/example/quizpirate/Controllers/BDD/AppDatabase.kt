package com.example.quizpirate.Controllers.BDD

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.example.quizpirate.Controllers.BDD.Entity.UserResponse
import com.example.quizpirate.Controllers.BDD.Entity.User
import com.example.quizpirate.Controllers.BDD.Entity.Question
import com.example.quizpirate.Controllers.BDD.Entity.Response
import com.example.quizpirate.Controllers.BDD.Entity.UserTemps

import com.example.quizpirate.Controllers.BDD.DAO.UserDao
import com.example.quizpirate.Controllers.BDD.DAO.QuestionDao
import com.example.quizpirate.Controllers.BDD.DAO.ResponseDao
import com.example.quizpirate.Controllers.BDD.DAO.UserResponseDao
import com.example.quizpirate.Controllers.BDD.DAO.UserTempsDao

@Database(entities = [User::class, Question::class, Response::class, UserResponse::class, UserTemps::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun questionDao(): QuestionDao
    abstract fun userReponseDao(): UserResponseDao
    abstract fun userTempsDao(): UserTempsDao

    abstract fun responseDao() : ResponseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
