package com.example.quizpirate.Controllers.BDD.DAO

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.quizpirate.Controllers.BDD.Entity.User
import com.example.quizpirate.Controllers.BDD.Entity.UserResponse

@Dao
interface UserResponseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUserReponse(userReponse: UserResponse)

    @Update
    fun updateUserReponse(UserResponses: UserResponse)

    @Delete
    fun deleteUserResponse(UserResponses: UserResponse)
    @Query("SELECT * FROM user_reponse WHERE usre_usr_id = :id")
    fun getResponsesForUser(id: Int): Array<UserResponse>
}
