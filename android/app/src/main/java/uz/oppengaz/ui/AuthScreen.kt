package uz.oppengaz.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(vm: AppViewModel, onDone: () -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var register by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (register) "Ro'yxatdan o'tish" else "Kirish") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Orqaga") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Holat yuborish, tasdiqlash va izoh yozish uchun hisob kerak. Ma'lumotlarni ko'rish uchun kirish shart emas.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            if (register) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Ismingiz (izohlarda ko'rinadi)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text(if (register) "Parol (kamida 10 belgi)" else "Parol") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    error = when {
                        email.isBlank() || !email.contains("@") -> "To'g'ri email kiriting"
                        register && name.trim().length < 2 -> "Ismingizni kiriting"
                        register && password.length < 10 -> "Parol kamida 10 belgi bo'lsin"
                        password.isEmpty() -> "Parolni kiriting"
                        else -> null
                    }
                    if (error == null) {
                        busy = true
                        val finish: (String?) -> Unit = { err ->
                            busy = false
                            if (err == null) onDone() else error = err
                        }
                        if (register) vm.register(email, name, password, finish) else vm.login(email, password, finish)
                    }
                },
            ) { Text(if (register) "Ro'yxatdan o'tish" else "Kirish") }
            TextButton(onClick = { register = !register; error = null }) {
                Text(if (register) "Hisobim bor, kirish" else "Hisobim yo'q, ro'yxatdan o'tish")
            }
        }
    }
}
