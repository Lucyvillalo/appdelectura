package com.example.desafio3dsm.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ReadingRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    private val root = FirebaseDatabase.getInstance().reference
    private fun userRef() = root.child("users").child(auth.currentUser?.uid ?: error("Sesión no iniciada"))
    fun observeBooks() = callbackFlow {
        val ref = userRef().child("books")
        val listener = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { trySend(s.children.mapNotNull { it.getValue(Book::class.java)?.copy(id = it.key ?: "") }) }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener); awaitClose { ref.removeEventListener(listener) }
    }
    fun observeGoal() = callbackFlow {
        val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val ref = userRef().child("goals").child(year.toString())
        val listener = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { trySend(s.getValue(AnnualGoal::class.java) ?: AnnualGoal(year)) }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener); awaitClose { ref.removeEventListener(listener) }
    }
    suspend fun saveBook(book: Book) {
        val ref = userRef().child("books")
        val id = book.id.ifBlank { ref.push().key ?: error("No se pudo crear el libro") }
        ref.child(id).setValue(book.copy(id = id)).await()
    }
    suspend fun deleteBook(id: String) { userRef().child("books").child(id).removeValue().await() }
    suspend fun saveGoal(goal: AnnualGoal) { userRef().child("goals").child(goal.year.toString()).setValue(goal).await() }
}
