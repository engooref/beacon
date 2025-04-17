package com.example.quizpirate.Controllers.BDD.Entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "USER")
data class User(
    @PrimaryKey(autoGenerate = true)
    var usr_id: Int = 0,
    var usr_name: String = "",
    var usr_nbretry: Int = 1,
    var usr_lang: String = ""
)
