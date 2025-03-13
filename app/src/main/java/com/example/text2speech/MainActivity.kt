package com.example.text2speech

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var textToSpeech: TextToSpeech
    private var isSpeaking = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.US
            }
        }

        textToSpeech.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking.value = true
            }
            override fun onDone(utteranceId: String?) {
                isSpeaking.value = false
            }
            override fun onError(utteranceId: String?) {
                isSpeaking.value = false
            }
        })

        setContent {
            TextToSpeechApp(textToSpeech, isSpeaking)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}

@Composable
fun TextToSpeechApp(textToSpeech: TextToSpeech, isSpeaking: MutableState<Boolean>) {
    var text by remember { mutableStateOf(TextFieldValue("")) }
    val languageOptions = listOf(
        "English (US)" to Locale.US,
        "Tamil" to Locale("ta", "IN"),
        "French" to Locale.FRANCE,
        "German" to Locale.GERMANY
    )
    var selectedLocale by remember { mutableStateOf(Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color(0xFFE3F2FD)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Text to Speech", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
        Spacer(modifier = Modifier.height(16.dp))
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            textStyle = TextStyle(fontSize = 18.sp, color = Color.Black),
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(8.dp))
                .padding(12.dp)
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        DropdownMenuComponent(languageOptions, selectedLocale) { selectedLocale = it }

        Spacer(modifier = Modifier.height(16.dp))
        if (!isSpeaking.value) {
            Button(
                onClick = {
                    textToSpeech.language = selectedLocale
                    textToSpeech.speak(text.text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
                    isSpeaking.value = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
            ) {
                Text("Speak", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (isSpeaking.value) {
            VoiceIndicator()
        }
    }
}

@Composable
fun DropdownMenuComponent(options: List<Pair<String, Locale>>, selected: Locale, onSelectionChange: (Locale) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf(options.find { it.second == selected }?.first ?: "Select Language") }

    Box(modifier = Modifier.fillMaxWidth()) {
        Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedText)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (label, locale) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        selectedText = label
                        onSelectionChange(locale)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun VoiceIndicator() {
    val infiniteTransition = rememberInfiniteTransition()
    val waveHeight by infiniteTransition.animateFloat(
        initialValue = 5f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    Canvas(modifier = Modifier.size(100.dp)) {
        drawCircle(
            color = Color.Blue,
            radius = waveHeight,
            style = Stroke(width = 5f)
        )
    }
}
