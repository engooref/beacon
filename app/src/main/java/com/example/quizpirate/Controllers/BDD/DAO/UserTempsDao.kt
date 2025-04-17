package com.example.quizpirate.Controllers.BDD.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.quizpirate.Controllers.BDD.Entity.UserExportData
import com.example.quizpirate.Controllers.BDD.Entity.UserTemps
@Dao
interface UserTempsDao {
    @Insert
    fun insertUserTemps(userTempsRecord: UserTemps) : Long


    @Query("""
        SELECT u.usr_name as userName, ut.ust_temp as time, ut.ust_point as points, ut.ust_usr_retry as attempts
        FROM USER_TEMPS ut
        JOIN USER u ON ut.ust_usr_id = u.usr_id
        WHERE ut.ust_point = (
            SELECT MAX(ust_point) FROM USER_TEMPS WHERE ust_usr_id = ut.ust_usr_id
        )
        GROUP BY ut.ust_usr_id
        ORDER BY ut.ust_point DESC
    """)
    fun getBestUserRecords(): List<UserExportData>
}