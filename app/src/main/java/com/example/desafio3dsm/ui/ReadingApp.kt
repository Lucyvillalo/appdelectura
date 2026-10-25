package com.example.desafio3dsm.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.desafio3dsm.data.*
import com.example.desafio3dsm.utils.Validators
import com.example.desafio3dsm.viewmodel.ReadingViewModel
import java.util.Calendar

@Composable
fun ReadingApp(vm: ReadingViewModel) {
    val nav = rememberNavController()
    val user by vm.user.collectAsState()
    val books by vm.books.collectAsState()
    val goal by vm.goal.collectAsState()
    val busy by vm.busy.collectAsState()
    val message by vm.message.collectAsState()
    val year = Calendar.getInstance().get(Calendar.YEAR)
    LaunchedEffect(user) {
        val current = nav.currentDestination?.route
        if (user != null && (current == "login" || current == "register")) nav.navigate("home") { popUpTo("login") { inclusive = true }; launchSingleTop = true }
        if (user == null && current != null && current !in listOf("login", "register")) nav.navigate("login") { popUpTo(nav.graph.startDestinationId) { inclusive = true }; launchSingleTop = true }
    }
    NavHost(navController = nav, startDestination = if (user == null) "login" else "home") {
        composable("login") { AuthScreen(false, busy, message, vm::clearMessage, { nav.navigate("register") }) { e, p -> vm.signIn(e, p) {} } }
        composable("register") { AuthScreen(true, busy, message, vm::clearMessage, { nav.popBackStack() }) { e, p -> vm.signUp(e, p) {} } }
        composable("home") { HomeScreen(user.orEmpty(), books, goal, message, vm::clearMessage,
            { nav.navigate("editor/new") }, { nav.navigate("library") }, { nav.navigate("stats") }, { nav.navigate("goal") }, vm::signOut,
            { nav.navigate("editor/${it.id}") }) }
        composable("library") { Page("Mi biblioteca", nav) {
            if (books.isEmpty()) EmptyState("Aún no tienes libros en tu biblioteca.", "Agregar el primero") { nav.navigate("editor/new") }
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(books, key = { it.id }) { BookCard(it) { nav.navigate("editor/${it.id}") } } }
            Spacer(Modifier.height(12.dp)); Button(onClick = { nav.navigate("editor/new") }, modifier = Modifier.fillMaxWidth()) { Text("Agregar libro") }
        } }
        composable("stats") { StatsScreen(books, year, nav) }
        composable("goal") { GoalScreen(goal.target, nav) { vm.setGoal(it); nav.popBackStack() } }
        composable("editor/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id").orEmpty(); val existing = books.firstOrNull { it.id == id }
            BookEditor(existing, busy, message, vm::clearMessage, { nav.popBackStack() }, vm::save, { vm.delete(it); nav.popBackStack() })
        }
    }
}

@Composable private fun AuthScreen(register: Boolean, busy: Boolean, message: String?, clear: () -> Unit, switch: () -> Unit, submit: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }; var passError by remember { mutableStateOf<String?>(null) }; var confirmError by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("📚", style = MaterialTheme.typography.displayMedium)
        Text(if (register) "Crear cuenta" else "Biblioteca lectora", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(if (register) "Organiza tus lecturas y alcanza tu meta." else "Inicia sesión para continuar leyendo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(email, { email = it; emailError = null }, label = { Text("Correo electrónico") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), isError = emailError != null, supportingText = { emailError?.let { Text(it) } }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it; passError = null }, label = { Text("Contraseña") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), isError = passError != null, supportingText = { passError?.let { Text(it) } }, modifier = Modifier.fillMaxWidth())
        if (register) OutlinedTextField(confirm, { confirm = it; confirmError = null }, label = { Text("Confirmar contraseña") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), isError = confirmError != null, supportingText = { confirmError?.let { Text(it) } }, modifier = Modifier.fillMaxWidth())
        if (message != null) { Text(message, color = MaterialTheme.colorScheme.error); LaunchedEffect(message) { clear() } }
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            emailError = Validators.email(email); passError = Validators.password(password); confirmError = if (register) Validators.confirm(password, confirm) else null
            if (emailError == null && passError == null && confirmError == null) submit(email, password)
        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(if (register) "Registrarme" else "Iniciar sesión") }
        TextButton(onClick = switch, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(if (register) "Ya tengo una cuenta · Iniciar sesión" else "Crear una cuenta") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun HomeScreen(email: String, books: List<Book>, goal: AnnualGoal, message: String?, clear: () -> Unit, add: () -> Unit, library: () -> Unit, stats: () -> Unit, editGoal: () -> Unit, signOut: () -> Unit, edit: (Book) -> Unit) {
    val year = Calendar.getInstance().get(Calendar.YEAR)
    val finished = books.count { it.status == "Terminado" && it.finishedDate?.let { d -> Calendar.getInstance().apply { timeInMillis = d }.get(Calendar.YEAR) } == year }
    val reading = books.filter { it.status == "Leyendo" }
    Scaffold(topBar = { TopAppBar(title = { Text("Hola, ${email.substringBefore('@')}") }, actions = { IconButton(onClick = signOut) { Icon(Icons.Default.Logout, "Cerrar sesión") } }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (message != null) { AssistChip(onClick = clear, label = { Text(message) }) }
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Tu reto de lectura · $year", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("$finished de ${goal.target} libros terminados", style = MaterialTheme.typography.bodyLarge)
                    LinearProgressIndicator(progress = { (finished.toFloat() / goal.target.coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
                    Text(if (finished >= goal.target) "¡Felicidades! Cumpliste tu meta anual 🎉" else "Te faltan ${(goal.target - finished).coerceAtLeast(0)} libros para tu meta.")
                    TextButton(onClick = editGoal) { Text("Modificar meta") }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Leyendo ahora", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text("${reading.size} libros") }
            if (reading.isEmpty()) Text("No tienes libros en lectura. ¡Agrega uno para comenzar!", color = MaterialTheme.colorScheme.onSurfaceVariant)
            reading.forEach { BookCard(it) { edit(it) } }
            Button(onClick = add, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Agregar libro") }
            OutlinedButton(onClick = library, modifier = Modifier.fillMaxWidth()) { Text("Ver biblioteca") }
            OutlinedButton(onClick = stats, modifier = Modifier.fillMaxWidth()) { Text("Ver estadísticas") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun Page(title: String, nav: androidx.navigation.NavHostController, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Volver") } }) }) { pad -> Column(Modifier.fillMaxSize().padding(pad).padding(16.dp), content = content) }
}

@Composable private fun BookCard(book: Book, click: () -> Unit) {
    Card(onClick = click, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text(book.status, color = MaterialTheme.colorScheme.primary) }
            Text("${book.author} · ${book.genre}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(progress = { book.progress }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            Text("${(book.progress * 100).toInt()}% · ${book.pagesRead}/${book.totalPages} páginas")
            if (book.status == "Terminado") Text("★".repeat(book.rating ?: 0) + "☆".repeat(5 - (book.rating ?: 0)), color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable private fun EmptyState(text: String, action: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 50.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("📖", style = MaterialTheme.typography.displaySmall); Text(text); Spacer(Modifier.height(12.dp)); Button(onClick = onClick) { Text(action) } }
}

@Composable private fun GoalScreen(current: Int, nav: androidx.navigation.NavHostController, save: (Int) -> Unit) {
    var value by remember { mutableStateOf(current.toString()) }; var error by remember { mutableStateOf<String?>(null) }
    Page("Meta anual", nav) {
        Text("¿Cuántos libros quieres terminar este año?")
        OutlinedTextField(value, { value = it; error = null }, label = { Text("Número de libros") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = error != null, supportingText = { error?.let { Text(it) } }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { val (n, e) = Validators.goal(value); error = e; if (n != null) save(n) }, modifier = Modifier.fillMaxWidth()) { Text("Guardar meta") }
    }
}

@Composable private fun StatsScreen(books: List<Book>, year: Int, nav: androidx.navigation.NavHostController) {
    val finished = books.filter { it.status == "Terminado" && it.finishedDate?.let { d -> Calendar.getInstance().apply { timeInMillis = d }.get(Calendar.YEAR) } == year }
    val counts = GENRES.associateWith { g -> finished.count { it.genre == g } }
    val max = counts.values.maxOrNull() ?: 0
    val top = if (max == 0) emptyList() else counts.filterValues { it == max }.keys.toList()
    val best = finished.sortedWith(compareByDescending<Book> { it.rating ?: 0 }.thenBy { it.finishedDate ?: Long.MAX_VALUE }).firstOrNull()
    Page("Estadísticas · $year", nav) {
        if (finished.isEmpty()) Text("Aún no has terminado libros este año. ¡Cada lectura cuenta!")
        Text("Libros terminados por género", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        GENRES.forEach { g -> Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text(g, Modifier.weight(1f)); Text("${counts[g] ?: 0}") } } }
        Spacer(Modifier.height(8.dp))
        Text("Género más leído", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(if (top.isEmpty()) "Sin datos" else if (top.size > 1) "Empate: ${top.joinToString(", ")}" else top.first())
        Spacer(Modifier.height(8.dp)); Text("Páginas leídas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("${finished.sumOf { it.totalPages }} páginas")
        Spacer(Modifier.height(8.dp)); Text("Mejor calificado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(best?.let { "${it.title} · ${"★".repeat(it.rating ?: 0)}" } ?: "Sin datos")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun BookEditor(book: Book?, busy: Boolean, message: String?, clear: () -> Unit, back: () -> Unit, save: (Book) -> Unit, delete: (String) -> Unit) {
    var title by remember(book) { mutableStateOf(book?.title.orEmpty()) }; var author by remember(book) { mutableStateOf(book?.author.orEmpty()) }
    var genre by remember(book) { mutableStateOf(book?.genre ?: GENRES.first()) }; var status by remember(book) { mutableStateOf(book?.status ?: STATUSES.first()) }
    var total by remember(book) { mutableStateOf(book?.totalPages?.takeIf { it > 0 }?.toString().orEmpty()) }; var read by remember(book) { mutableStateOf(book?.pagesRead?.toString() ?: "0") }
    var rating by remember(book) { mutableStateOf(book?.rating) }; var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }; var confirmDelete by remember { mutableStateOf(false) }
    var genreMenu by remember { mutableStateOf(false) }; var statusMenu by remember { mutableStateOf(false) }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Eliminar libro") }, text = { Text("¿Seguro que quieres eliminar «${book?.title}»?") }, confirmButton = { TextButton(onClick = { book?.id?.let(delete) }) { Text("Eliminar") } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } })
    Scaffold(topBar = { TopAppBar(title = { Text(if (book == null) "Agregar libro" else "Editar libro") }, navigationIcon = { IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Volver") } }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Field(title, { title = it }, "Título", errors["Título"])
            Field(author, { author = it }, "Autor", errors["Autor"])
            ExposedDropdownMenuBox(expanded = genreMenu, onExpandedChange = { genreMenu = !genreMenu }) {
                OutlinedTextField(genre, {}, Modifier.menuAnchor().fillMaxWidth(), readOnly = true, label = { Text("Género") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(genreMenu) })
                ExposedDropdownMenu(expanded = genreMenu, onDismissRequest = { genreMenu = false }) { GENRES.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { genre = it; genreMenu = false }) } }
            }
            Field(total, { total = it.filter(Char::isDigit) }, "Total de páginas", errors["Total de páginas"], true)
            Field(read, { read = it.filter(Char::isDigit) }, "Páginas leídas", errors["Páginas leídas"], true)
            ExposedDropdownMenuBox(expanded = statusMenu, onExpandedChange = { statusMenu = !statusMenu }) {
                OutlinedTextField(status, {}, Modifier.menuAnchor().fillMaxWidth(), readOnly = true, label = { Text("Estado") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(statusMenu) })
                ExposedDropdownMenu(expanded = statusMenu, onDismissRequest = { statusMenu = false }) { STATUSES.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { status = it; if (it != "Terminado") rating = null; statusMenu = false }) } }
            }
            if (status == "Terminado") {
                Text("Calificación · ${if (rating == null) "obligatoria" else "$rating de 5"}")
                Row { (1..5).forEach { n -> IconButton(onClick = { rating = n }) { Icon(if ((rating ?: 0) >= n) Icons.Default.Star else Icons.Default.StarBorder, "${n} estrellas", tint = MaterialTheme.colorScheme.secondary) } } }
                errors["Calificación"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            if (message != null) { Text(message, color = MaterialTheme.colorScheme.error); LaunchedEffect(message) { clear() } }
            Button(onClick = {
                errors = Validators.book(title, author, total, read, status, rating)
                val t = total.toIntOrNull(); val p = read.toIntOrNull()
                if (errors.isEmpty() && t != null && p != null) save(Book(book?.id.orEmpty(), title.trim(), author.trim(), genre, t, p, status, rating, book?.finishedDate, book?.createdAt ?: System.currentTimeMillis()))
            }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Guardando…" else "Guardar libro") }
            if (book != null) OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("Eliminar libro") }
        }
    }
}

@Composable private fun Field(value: String, onChange: (String) -> Unit, label: String, error: String?, number: Boolean = false) {
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Number else KeyboardType.Text), isError = error != null, supportingText = { error?.let { Text(it) } }, modifier = Modifier.fillMaxWidth())
}





