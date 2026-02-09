package com.example.quizpirate.Utils

import android.util.Log
import com.example.quizpirate.Controllers.BDD.DAO.QuestionDao
import com.example.quizpirate.Controllers.BDD.DAO.ResponseDao
import com.example.quizpirate.Controllers.BDD.Entity.Question
import com.example.quizpirate.Controllers.BDD.Entity.Response
import com.example.quizpirate.Controllers.BDD.Entity.UserExportData
import com.opencsv.CSVReader
import java.io.File
import java.io.FileReader
import java.io.IOException



object CsvUtils {

    // En-tête attendu dans le fichier CSV
    private const val HEADER_IMPORT = "Question,Lang,Reponse 1,Reponse 2,Reponse 3,Reponse 4,IdBonneRep"
    private const val HEADER_EXPORT = "UserName,Time,Points,Attempts"

    /**
     * Exporte la liste des données utilisateurs dans le fichier CSV fourni.
     */
    fun exportUsersToCsv(file: File, data: List<UserExportData>) {
        try {
            file.printWriter().use { out ->
                out.println(HEADER_EXPORT)
                data.forEach { record ->
                    // On entoure le nom et le temps de guillemets au cas où ils contiendraient des virgules.
                    out.println("\"${record.userName}\",\"${record.time}\",${record.points},${record.attempts}")
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun importQuestionsFromCsv(
        csvFilePath: File,
        questionDao: QuestionDao,
        responseDao: ResponseDao
    ) {
        try {
            CSVReader(FileReader(csvFilePath)).use { reader ->
                var line: Array<String>? = reader.readNext()
                while (line != null) {
                    // On vérifie qu'on a exactement 7 colonnes
                    if (line.size == 7) {
                        val questionText = line[1].trim()
                        val lang = line[0].trim()

                        // Insertion de la question
                        val question = Question(que_name = questionText, que_lang = lang)
                        val questionId = questionDao.insertQuestion(question).toInt()

                        // Conversion en index 0-based ; si invalide, -1 (aucune bonne réponse)
                        val goodIndex = line[6].trim().toIntOrNull()?.minus(1) ?: -1

                        // Parcours des 4 réponses (colonnes 2,3,4,5)
                        for (i in 0 until 4) {

                            val responseText = line[2 + i].trim()
                            val isGood = (i == goodIndex)
                            val response = Response(
                                rep_name   = responseText,
                                rep_que_id = questionId,
                                rep_bon    = isGood
                            )
                            responseDao.insertResponse(response)
                        }
                    } else {
                        // Optionnel : loguer ou gérer les lignes malformées
                        Log.w("CSV_IMPORT", "Ligne ignorée, format inattendu : ${line.joinToString()}")
                    }
                    line = reader.readNext()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}
