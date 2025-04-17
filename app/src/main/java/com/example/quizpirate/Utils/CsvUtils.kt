package com.example.quizpirate.Utils

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

    /**
     * Importe les questions et leurs réponses depuis un fichier CSV à l'aide d'OpenCSV.
     *
     * Chaque ligne du CSV doit être au format :
     * Question,Lang,Reponse 1,Reponse 2,Reponse 3,Reponse 4,IdBonneRep
     *
     * IdBonneRep est un nombre (de 1 à 4) indiquant la position (1-based) de la bonne réponse.
     *
     * @param csvFilePath Le chemin du fichier CSV.
     * @param questionDao Le DAO pour les questions.
     * @param responseDao Le DAO pour les réponses.
     */
    fun importQuestionsFromCsv(
        csvFilePath: File,
        questionDao: QuestionDao,
        responseDao: ResponseDao
    ) {
        try {
            CSVReader(FileReader(csvFilePath)).use { reader ->
                // Lit la première ligne pour vérifier l'en-tête
                var line = reader.readNext()
                if (line != null && line.joinToString(",").trim() == HEADER_IMPORT) {
                    // Passe l'en-tête
                    line = reader.readNext()
                }
                // Parcourt chaque ligne du fichier CSV
                while (line != null) {
                    if (line.size >= 7) {
                        val questionText = line[0].trim()
                        val lang = line[1].trim()
                        // Crée et insère la question dans la base
                        val question = Question(que_name = questionText, que_lang = lang)
                        val questionId = questionDao.insertQuestion(question).toInt()

                        // IdBonneRep est 1-based, on convertit en index 0-based
                        val goodIndex = line[6].trim().toIntOrNull()?.minus(1) ?: -1

                        // Insère les 4 réponses (de l'index 2 à 5)
                        for (i in 2 until 6) {
                            val responseText = line[i].trim()
                            val isGood = (i - 2 == goodIndex)
                            val response = Response(rep_name = responseText, rep_que_id = questionId, rep_bon = isGood)
                            responseDao.insertResponse(response)
                        }
                    }
                    line = reader.readNext()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}
