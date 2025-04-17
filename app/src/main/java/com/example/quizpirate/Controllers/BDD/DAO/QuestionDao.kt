package com.example.quizpirate.Controllers.BDD.DAO

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.quizpirate.Controllers.BDD.Entity.Question
import com.example.quizpirate.Controllers.BDD.Entity.QuestionWithResponses

@Dao
interface QuestionDao {
    @Query("SELECT COUNT(*) FROM QUESTION WHERE que_lang = :lang")
    fun countQuestionsByLanguage(lang: String): Long

    @Transaction
    @Query(
        """
        SELECT * FROM QUESTION 
        WHERE que_lang = :lang 
        ORDER BY RANDOM() 
        LIMIT :nbLimit
        """
    )
    fun getRandomQuestions(lang: String, nbLimit: Int): List<QuestionWithResponses>

    @Transaction
    @Query(
        """
        SELECT * FROM QUESTION 
        WHERE que_lang = :lang AND que_id NOT IN (:excludedIds)
        ORDER BY RANDOM() 
        LIMIT :nbLimit
        """
    )
    fun getRandomQuestionsExcluding(lang: String, excludedIds: List<Int>, nbLimit: Int): List<QuestionWithResponses>

    @Insert
    fun insertQuestion(question: Question): Long

}
