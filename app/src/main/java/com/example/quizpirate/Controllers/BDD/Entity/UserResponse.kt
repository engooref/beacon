package com.example.quizpirate.Controllers.BDD.Entity

import androidx.room.Entity

@Entity(tableName = "USER_REPONSE", primaryKeys = ["usre_usr_id", "usre_rep_id"])
data class UserResponse(
    val usre_usr_id: Int,
    val usre_rep_id: Int,
    val usre_nbretry: Int
)
