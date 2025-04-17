package com.example.quizpirate.Controllers.BDD.Entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "QUESTION")
data class Question(
    @PrimaryKey(autoGenerate = true) val que_id: Int = 0,
    val que_name: String,
    val que_lang: String
)
