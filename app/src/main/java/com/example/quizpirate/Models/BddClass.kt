package com.example.quizpirate.Models

import android.provider.ContactsContract.Intents.Insert
import org.json.JSONObject
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.Statement

class BddClass(
    ip: String,
    port: String,
    nameBdd: String,
    private var identifiant: String,
    private var mdp: String,
) {
    private var jdbcUrl : String
    private lateinit var connection : Connection



    init {
        jdbcUrl = "jdbc:mysql://${ip}:${port}/${nameBdd}"
    }

    fun LaunchTread(func : () -> Unit) {
        var detailResult: String = ""
        val exceptionThread = Thread.UncaughtExceptionHandler { _, ex -> detailResult = ex.toString() }

        val thread = Thread {
            try {
                func()
            } catch (e: Exception) {
                throw RuntimeException(e.toString())
            }  
        }

        thread.uncaughtExceptionHandler = exceptionThread
        thread.start()
        thread.join()
        if (detailResult != "") throw Exception(detailResult)
    }

    fun connect() {
        val func: () -> Unit = {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(jdbcUrl, identifiant, mdp)
            if (!connection.isValid(0)) {
                throw Exception("Connexion a la BDD non valide")
            }
        }
        LaunchTread(func)
    }

    fun select(sqlText: String, funcTrait: (rs: ResultSet) -> Unit) : Boolean {
        var result : ResultSet? = null

        val func : () -> Unit = {
            result = connection.createStatement().executeQuery(sqlText)
        }

        LaunchTread(func)
        if (result != null)
            while (result!!.next()) funcTrait(result!!)

        return (result != null)
    }

    fun insert(sqlText: String) {
        val func : () -> Unit = {
            with(connection) {
                createStatement().execute(sqlText)
            }
        }
        LaunchTread(func)
    }

    fun update(sqlText: String) {
        val func : () -> Unit = {
            with(connection) {
                createStatement().executeUpdate(sqlText)
            }
        }
        LaunchTread(func)
    }

    companion object {
        private const val bddTag = "BDD"
        const val ALL_SELECT = "*"
        object TABLE {
            object USER {
                const val TABLE_NAME = "USER"
                const val ID = "usr_id"
                const val NAME = "usr_name"
                const val INFO = "usr_info"
                const val DISCORD = "usr_disc"
                const val NBRETRY = "usr_nbretry"
                const val MAILENV = "usr_mailenv"
                const val LANG = "usr_lang"
                const val NBMAIL = "usr_nbmailEnv"
            }
            object QUESTION {
                const val TABLE_NAME = "QUESTION"
                const val ID = "que_id"
                const val NAME = "que_name"
                const val LANGUE = "que_lang"
            }
            object REPONSE {
                const val TABLE_NAME = "REPONSE"
                const val ID = "rep_id"
                const val NAME = "rep_name"
                const val QUES_ID = "rep_que_id"
                const val BON = "rep_bon"
            }
            object USER_REPONSE {
                const val TABLE_NAME = "USER_REPONSE"
                const val USER_ID = "usre_usr_id"
                const val REPONSE_ID = "usre_rep_id"
                const val USER_NBRETRY = "usre_nbretry"
            }
            object USER_TEMPS {
                const val TABLE_NAME = "USER_TEMPS"
                const val USER_ID = "ust_usr_id"
                const val RETRY = "ust_usr_retry"
                const val TEMPS = "ust_temp"
                const val POINTS = "ust_point"
            }
        }

        data class User(val id: Int, val name: String, val pseudoDiscord : String, val info : String, var nbRetry: Int)


        data class Question(var id: Int = 0, var name: String = String(), var responses: Array<Response> = arrayOf(), val lang : String) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Question

                if (id != other.id) return false
                if (name != other.name) return false
                if (!responses.contentEquals(other.responses)) return false
                if (lang != other.lang) return false

                return true
            }

            override fun hashCode(): Int {
                var result = id
                result = 31 * result + name.hashCode()
                result = 31 * result + responses.contentHashCode()
                result = 31 * result + lang.hashCode()
                return result
            }
        }

        data class Response(val id: Int, val name: String, val good: Boolean)


    }
}