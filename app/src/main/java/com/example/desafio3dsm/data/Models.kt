package com.example.desafio3dsm.data

data class Book(
    var id: String = "", var title: String = "", var author: String = "",
    var genre: String = GENRES.first(), var totalPages: Int = 0, var pagesRead: Int = 0,
    var status: String = STATUSES.first(), var rating: Int? = null,
    var finishedDate: Long? = null, var createdAt: Long = System.currentTimeMillis()
) {
    val progress: Float get() = if (totalPages > 0) (pagesRead.toFloat() / totalPages).coerceIn(0f, 1f) else 0f
}
val GENRES = listOf("Novela", "Ciencia ficción", "Historia", "Desarrollo personal")
val STATUSES = listOf("Por leer", "Leyendo", "Terminado")
data class AnnualGoal(var year: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR), var target: Int = 12)
data class ReadingStats(val genreCounts: Map<String, Int>, val topGenres: List<String>, val pages: Int, val bestBook: Book?, val finishedCount: Int)

