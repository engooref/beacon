package com.example.quizpirate.Controllers.BDD.Entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "RESPONSE")
data class Response(
    @PrimaryKey(autoGenerate = true) val rep_id: Int = 0,
    val rep_name: String,
    val rep_que_id: Int,
    val rep_bon: Boolean
)
