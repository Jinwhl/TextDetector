package com.example.textdetector

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.textdetector.ui.theme.TextDetectorTheme
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TextDetectorTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var recognizedText by remember { mutableStateOf("") }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        uri?.let {
            processImage(context = context, imgUri = it) { text ->
                recognizedText = text
            }
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
        //horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { imagePickerLauncher.launch("image/*") }) {
            Text(text = "Select Image")
        }
        Spacer(modifier = Modifier.height(16.dp))
        selectedImageUri?.let { uri ->
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = recognizedText,
            onValueChange = { newText -> recognizedText = newText },
            modifier = Modifier
                .fillMaxWidth()
                .size(height = 200.dp, width = 400.dp),
            textStyle = TextStyle(fontSize = 16.sp),
            placeholder = {
                Text(text = "Enter your text here", fontSize = 16.sp)
            }
        )

        var targetlanguage by remember { mutableStateOf("Select Target Language") }
        Column (
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ){
            Spacer(modifier = Modifier.height(16.dp))
            SelectTargetLanguage(targetlanguage) { language -> targetlanguage = language }
            Spacer(modifier = Modifier.height(16.dp))
            TranslateAtoB(inputText = recognizedText, targetLanguage = targetlanguage)
        }
    }
}

@Composable
fun SelectTargetLanguage(selectedLanguage: String, onLanguageSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf("English", "Chinese", "Japanese", "Latvian")

    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .border(0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
            .clickable { expanded = true }
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    ) {
        Text(text = selectedLanguage, fontSize = 16.sp)
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            languages.forEach { language ->
                DropdownMenuItem(
                    onClick = {
                        onLanguageSelected(language)
                        expanded = false
                    },
                    text = { Text(text = language) }
                )
            }
        }
    }
}
@Composable
fun TranslateAtoB(inputText: String, targetLanguage: String) {
    var isDownloaded by remember { mutableStateOf(false) }
    val targetLanguageCode = when (targetLanguage) {
        "English" -> TranslateLanguage.ENGLISH
        "Chinese" -> TranslateLanguage.CHINESE
        "Japanese" -> TranslateLanguage.JAPANESE
        "Latvian" -> TranslateLanguage.LATVIAN
        else -> ""
    }

    if (targetLanguageCode.isNotEmpty()) {
        val KoLtTranslator = remember(targetLanguageCode) {
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.KOREAN)
                    .setTargetLanguage(targetLanguageCode)
                    .build()
            )
        }
        DownloadModel(KoLtTranslator, onSuccess = {
            isDownloaded = true
        })

        var outputText by remember { mutableStateOf("") }

        KoLtTranslator.translate(inputText)
            .addOnSuccessListener { translatedText ->
                outputText = translatedText
            }

        Text(outputText)
    } else {
        Text("Please select a target language.")
    }
}

@Composable
fun DownloadModel( myTranslator: Translator, onSuccess: () -> Unit, ) {
    LaunchedEffect(key1 = myTranslator) {
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
        myTranslator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                // when download is success, change isDownloaded to true
                onSuccess()
            }
    }
}
private fun processImage(context: Context, imgUri: Uri, onTextRecognized: (String) -> Unit) {
    try {
        val image = InputImage.fromFilePath(context, imgUri)
        val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
        val result: Task<com.google.mlkit.vision.text.Text> = recognizer.process(image)

        result.addOnSuccessListener { visionText ->
            onTextRecognized(visionText.text)
        }.addOnFailureListener { e ->
            onTextRecognized("Failed to recognize text: ${e.message}")
        }
    } catch (e: IOException) {
        e.printStackTrace()
        onTextRecognized("Failed to recognize text: ${e.message}")
    }
}