package com.example.quizpirate.Controllers.BDD.Entity

data class UserExportData(
    val userName: String,
    val time: String,
    val points: Int,
    val attempts: Int
)