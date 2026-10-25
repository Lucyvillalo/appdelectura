package com.example.desafio3dsm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.desafio3dsm.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class ReadingViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val repo = ReadingRepository(auth)
    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books = _books.asStateFlow()
    private val _goal = MutableStateFlow(AnnualGoal())
    val goal = _goal.asStateFlow()
    private val _user = MutableStateFlow(auth.currentUser?.email)
    val user = _user.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private var booksJob: Job? = null
    private var goalJob: Job? = null
    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        observeUserData(firebaseAuth.currentUser)
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    private fun observeUserData(user: FirebaseUser?) {
        _user.value = user?.email
        booksJob?.cancel()
        goalJob?.cancel()
        if (user == null) {
            _books.value = emptyList()
            _goal.value = AnnualGoal()
            return
        }
        booksJob = viewModelScope.launch {
            repo.observeBooks()
                .catch {
                    _books.value = emptyList()
                    _message.value = "No se pudieron cargar los libros: ${it.localizedMessage ?: "revisa tu conexión."}"
                }
                .collect { _books.value = it }
        }
        goalJob = viewModelScope.launch {
            repo.observeGoal()
                .catch { _message.value = "No se pudo cargar la meta: ${it.localizedMessage ?: "revisa tu conexión."}" }
                .collect { _goal.value = it }
        }
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        booksJob?.cancel()
        goalJob?.cancel()
        super.onCleared()
    }

    fun clearMessage() { _message.value = null }

    private fun spanish(t: Throwable): String {
        val code = (t as? com.google.firebase.auth.FirebaseAuthException)?.errorCode.orEmpty().uppercase()
        val message = t.message.orEmpty().lowercase()
        return when {
            code.contains("EMAIL_ALREADY_IN_USE") || "email-already-in-use" in message || "email address is already" in message ->
                "Ese correo ya está registrado."
            code.contains("INVALID_EMAIL") || "invalid-email" in message || "badly formatted" in message ->
                "El correo no tiene un formato válido."
            code.contains("WEAK_PASSWORD") || "weak-password" in message ->
                "La contraseña es demasiado débil. Usa al menos 6 caracteres."
            code.contains("INVALID_CREDENTIAL") || code.contains("WRONG_PASSWORD") || code.contains("USER_NOT_FOUND") ||
                "invalid-credential" in message || "wrong-password" in message || "no user record" in message ->
                "Correo o contraseña incorrectos."
            code.contains("USER_DISABLED") ->
                "Esta cuenta está deshabilitada. Contacta al administrador."
            code.contains("TOO_MANY_REQUESTS") ->
                "Hubo demasiados intentos. Espera unos minutos e inténtalo de nuevo."
            code.contains("OPERATION_NOT_ALLOWED") ->
                "El acceso con correo y contraseña no está habilitado en Firebase."
            code.contains("NETWORK_REQUEST_FAILED") || "network" in message || "disconnected" in message ->
                "Sin conexión. Revisa tu internet e inténtalo de nuevo."
            "permission_denied" in message || "permission denied" in message ->
                "No tienes permiso para acceder a estos datos."
            else -> "Ocurrió un error. Revisa tu conexión e inténtalo de nuevo."
        }
    }
    fun signIn(email: String, password: String, done: () -> Unit) = authAction(done) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    fun signUp(email: String, password: String, done: () -> Unit) = authAction(done) {
        auth.createUserWithEmailAndPassword(email.trim(), password).await()
    }

    private fun authAction(done: () -> Unit, action: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                action()
                done()
            } catch (e: Exception) {
                _message.value = spanish(e)
            } finally {
                _busy.value = false
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun save(book: Book) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val old = _books.value.firstOrNull { it.id == book.id }
                val finished = book.status == "Terminado"
                repo.saveBook(
                    book.copy(
                        rating = if (finished) book.rating else null,
                        finishedDate = if (!finished) null
                        else old?.finishedDate ?: book.finishedDate ?: System.currentTimeMillis()
                    )
                )
                _message.value = "Libro guardado."
            } catch (e: Exception) {
                _message.value = spanish(e)
            } finally {
                _busy.value = false
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            _busy.value = true
            try {
                repo.deleteBook(id)
                _message.value = "Libro eliminado."
            } catch (e: Exception) {
                _message.value = spanish(e)
            } finally {
                _busy.value = false
            }
        }
    }

    fun setGoal(target: Int) {
        viewModelScope.launch {
            _busy.value = true
            try {
                repo.saveGoal(AnnualGoal(Calendar.getInstance().get(Calendar.YEAR), target))
                _message.value = "Meta anual guardada."
            } catch (e: Exception) {
                _message.value = spanish(e)
            } finally {
                _busy.value = false
            }
        }
    }
}