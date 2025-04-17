package com.example.quizpirate.Controllers.BDD.DAO

import androidx.room.*
import com.example.quizpirate.Controllers.BDD.Entity.User

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(user: User): Long

    @Update
    fun updateUser(user: User)

    @Query("SELECT * FROM USER WHERE usr_id = :userId")
    fun getUserById(userId: Long): User?

    @Query("SELECT * FROM USER WHERE usr_name = :name LIMIT 1")
    fun getUserByName(name: String): User?

    @Delete
    fun deleteUser(tempUser: User)
}
