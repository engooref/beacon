package com.example.quizpirate.Controllers.BDD.Entity

import androidx.room.Embedded
import androidx.room.Relation

data class QuestionWithResponses(
    @Embedded val question: Question,
    @Relation(
        parentColumn = "que_id",
        entityColumn = "rep_que_id"
    )
    val responses: List<Response>
)
