package com.example.quizpirate.Controllers.BDD.DAO

import androidx.room.Dao
import androidx.room.Insert
import com.example.quizpirate.Controllers.BDD.Entity.Response


@Dao
interface ResponseDao {

    @Insert
    fun insertResponse(response: Response): Long
}