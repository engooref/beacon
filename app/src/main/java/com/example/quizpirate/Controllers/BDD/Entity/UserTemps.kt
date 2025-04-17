package com.example.quizpirate.Controllers.BDD.Entity

import androidx.room.Entity

@Entity(tableName = "USER_TEMPS", primaryKeys = ["ust_usr_id", "ust_usr_retry"])
data class UserTemps(
    val ust_usr_id: Int,
    val ust_usr_retry: Int,
    val ust_temp: String,
    val ust_point: Int
)
