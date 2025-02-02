package com.lakshay.arxplorer.ui.paper

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import com.lakshay.arxplorer.data.model.ArxivPaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperScreen(
    onBackClick: () -> Unit,
    onDownloadClick: (String) -> Unit,
    onShareClick: (ArxivPaper) -> Unit,
    viewModel: PaperViewModel = hiltViewModel()
) {
    val paper by viewModel.currentPaper.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAbstract by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(showControls) {
        if (showControls) {
            launch {
                delay(3000) // Hide controls after 3 seconds
                showControls = false
            }
        }
    }

    // Download PDF only once when paper changes
    LaunchedEffect(paper?.pdfUrl) {
        paper?.pdfUrl?.let { url ->
            if (pdfFile == null && !isDownloading) {
                isDownloading = true
                downloadProgress = 0f
                try {
                    pdfFile = downloadPdf(context, url) { progress ->
                        downloadProgress = progress
                    }
                } catch (e: Exception) {
                    error = "Failed to download PDF: ${e.message}"
                } finally {
                    isDownloading = false
                }
            }
        }
    }

    paper?.let { currentPaper ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        showControls = true
                    }
                }
        ) {
            if (showAbstract) {
                // Abstract View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = currentPaper.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentPaper.authors.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Abstract",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentPaper.abstract,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 24.sp
                    )
                }
            } else {
                // PDF Viewer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    pdfFile?.let { file ->
                        AndroidView(
                            factory = { context ->
                                PDFView(context, null).apply {
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    setOnClickListener {
                                        showControls = true
                                    }
                                    fromFile(file)
                                        .enableSwipe(true)
                                        .swipeHorizontal(false)
                                        .enableDoubletap(true)
                                        .enableAnnotationRendering(true)
                                        .spacing(0)
                                        .autoSpacing(false)
                                        .pageFitPolicy(com.github.barteksc.pdfviewer.util.FitPolicy.WIDTH)
                                        .pageSnap(true)
                                        .pageFling(true)
                                        .nightMode(false)
                                        .defaultPage(0)
                                        .onLoad {
                                            isLoading = false
                                            Log.d("PaperScreen", "PDF loaded successfully")
                                        }
                                        .onError { t ->
                                            isLoading = false
                                            error = "Failed to load PDF: ${t.message}"
                                            Log.e("PaperScreen", "Failed to load PDF", t)
                                        }
                                        .onPageError { page, t ->
                                            Log.e("PaperScreen", "Error on page $page", t)
                                        }
                                        .load()
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (isLoading || isDownloading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(100.dp)
                                ) {
                                    CircularProgressIndicator(
                                        progress = if (isDownloading) downloadProgress else 1f,
                                        modifier = Modifier.fillMaxSize(),
                                        strokeWidth = 4.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (isDownloading) {
                                        Text(
                                            text = "${(downloadProgress * 100).toInt()}%",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = "Loading PDF...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    error?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }
                }
            }

            // Floating controls
            AnimatedVisibility(
                visible = showControls,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { showAbstract = !showAbstract }) {
                                Icon(
                                    if (showAbstract) Icons.Default.Description else Icons.Default.Info,
                                    contentDescription = if (showAbstract) "Show PDF" else "Show Abstract"
                                )
                            }
                            IconButton(onClick = { onDownloadClick(currentPaper.pdfUrl) }) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Download PDF"
                                )
                            }
                            IconButton(onClick = { onShareClick(currentPaper) }) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun downloadPdf(
    context: Context, 
    url: String,
    onProgress: (Float) -> Unit = {}
): File = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder()
        .followRedirects(true)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Convert arXiv URL to PDF URL if needed
    val pdfUrl = if (url.contains("arxiv.org/abs/")) {
        url.replace("/abs/", "/pdf/").replace("http://", "https://") + ".pdf"
    } else if (!url.endsWith(".pdf")) {
        url.replace("http://", "https://") + ".pdf"
    } else {
        url.replace("http://", "https://")
    }

    Log.d("PaperScreen", "Starting PDF download from: $pdfUrl")
    
    try {
        val request = Request.Builder()
            .url(pdfUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .build()

        val response = client.newCall(request).execute()
        Log.d("PaperScreen", "Response code: ${response.code}")
        
        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            Log.e("PaperScreen", "Error response: $errorBody")
            throw Exception("Failed to download PDF: ${response.code} - ${response.message}")
        }

        val body = response.body ?: throw Exception("Empty response body")
        val contentLength = body.contentLength()
        Log.d("PaperScreen", "Starting to download PDF, expected size: $contentLength bytes")

        val file = File(context.cacheDir, "temp_${System.currentTimeMillis()}.pdf")
        var bytesWritten = 0L
        val startTime = System.currentTimeMillis()
        
        FileOutputStream(file).use { output ->
            body.byteStream().use { input ->
                val buffer = ByteArray(32768) // Increased buffer size for better performance
                var bytes = input.read(buffer)
                var lastLogTime = startTime
                
                while (bytes >= 0) {
                    output.write(buffer, 0, bytes)
                    bytesWritten += bytes
                    
                    // Log progress every second
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastLogTime >= 1000) {
                        Log.d("PaperScreen", "Download progress: $bytesWritten / $contentLength bytes (${(bytesWritten * 100f / contentLength).toInt()}%)")
                        lastLogTime = currentTime
                    }
                    
                    if (contentLength > 0) {
                        onProgress(bytesWritten.toFloat() / contentLength)
                    }
                    bytes = input.read(buffer)
                }
            }
        }
        
        val endTime = System.currentTimeMillis()
        Log.d("PaperScreen", "PDF download completed in ${(endTime - startTime)/1000} seconds")
        Log.d("PaperScreen", "Saved PDF to: ${file.absolutePath}")
        Log.d("PaperScreen", "Final file size: ${file.length()} bytes, bytes written: $bytesWritten")
        
        if (!file.exists() || file.length() == 0L) {
            throw Exception("Failed to save PDF file or file is empty")
        }

        // Verify file is readable
        try {
            file.inputStream().use { it.read(ByteArray(1024)) }
            Log.d("PaperScreen", "Successfully verified PDF file is readable")
        } catch (e: Exception) {
            Log.e("PaperScreen", "Failed to verify PDF file readability", e)
            throw Exception("Failed to verify PDF file readability: ${e.message}")
        }
        
        file
    } catch (e: Exception) {
        Log.e("PaperScreen", "Error downloading PDF", e)
        throw e
    }
} 