package com.example.desafio3dsm.utils

import com.example.desafio3dsm.data.Book

object Validators {
    private val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    fun email(value: String): String? = if (emailPattern.matches(value.trim())) null else "Escribe un correo válido."
    fun password(value: String): String? = if (value.length >= 6) null else "La contraseña debe tener al menos 6 caracteres."
    fun confirm(password: String, confirmation: String): String? = if (password == confirmation) null else "Las contraseñas no coinciden."
    fun goal(value: String): Pair<Int?, String?> {
        val n = value.toIntOrNull()
        return if (n != null && n > 0) n to null else null to "La meta debe ser un número entero mayor que cero."
    }
    fun book(title: String, author: String, total: String, read: String, status: String, rating: Int?): Map<String, String> {
        val e = mutableMapOf<String, String>()
        if (title.trim().length !in 2..60) e["Título"] = "es obligatorio y debe tener entre 2 y 60 caracteres."
        if (author.trim().length !in 2..60) e["Autor"] = "es obligatorio y debe tener entre 2 y 60 caracteres."
        val t = total.toIntOrNull()
        if (t == null || t <= 0) e["Total de páginas"] = "debe ser un número entero mayor que cero."
        val p = read.toIntOrNull()
        if (p == null || p < 0 || t == null || p > t) e["Páginas leídas"] = "debe ser un entero entre 0 y el total de páginas."
        if (status == "Terminado" && rating !in 1..5) e["Calificación"] = "es obligatoria (de 1 a 5) para libros terminados."
        return e
    }
}
