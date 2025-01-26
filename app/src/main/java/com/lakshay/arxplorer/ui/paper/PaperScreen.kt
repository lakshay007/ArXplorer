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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperScreen(
    paper: ArxivPaper,
    onBackClick: () -> Unit,
    onDownloadClick: (String) -> Unit,
    onShareClick: (ArxivPaper) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAbstract by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

    LaunchedEffect(showControls) {
        if (showControls) {
            launch {
                delay(3000) // Hide controls after 3 seconds
                showControls = false
            }
        }
    }

    Box(
        modifier = modifier
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
                    text = paper.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = paper.authors.joinToString(", "),
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
                    text = paper.abstract,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 24.sp
                )
                if (paper.doi != null || paper.journalRef != null || paper.commentaries != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Additional Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    paper.doi?.let {
                        Text(
                            text = "DOI: $it",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    paper.journalRef?.let {
                        Text(
                            text = "Journal Reference: $it",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    paper.commentaries?.let {
                        Text(
                            text = "Comments: $it",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            // PDF Viewer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
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
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { pdfView ->
                        coroutineScope.launch {
                            try {
                                val file = downloadPdf(context, paper.pdfUrl)
                                withContext(Dispatchers.Main) {
                                    pdfView.fromFile(file)
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
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    error = "Failed to download PDF: ${e.message}"
                                }
                            }
                        }
                    }
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
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
                        IconButton(onClick = { onDownloadClick(paper.pdfUrl) }) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = "Download PDF"
                            )
                        }
                        IconButton(onClick = { onShareClick(paper) }) {
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

private suspend fun downloadPdf(
    context: Context, 
    url: String,
    onProgress: (Float) -> Unit = {}  // Make progress callback optional with empty default
): File = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()

    // Convert arXiv URL to PDF URL if needed
    val pdfUrl = if (url.contains("arxiv.org/abs/")) {
        url.replace("/abs/", "/pdf/").replace("http://", "https://") + ".pdf"
    } else if (!url.endsWith(".pdf")) {
        url.replace("http://", "https://") + ".pdf"
    } else {
        url.replace("http://", "https://")
    }

    Log.d("PaperScreen", "Downloading PDF from: $pdfUrl")
    
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
        Log.d("PaperScreen", "Downloaded PDF size: $contentLength bytes")

        val file = File(context.cacheDir, "temp_${System.currentTimeMillis()}.pdf")
        var bytesWritten = 0L
        
        FileOutputStream(file).use { output ->
            body.byteStream().use { input ->
                val buffer = ByteArray(8192)
                var bytes = input.read(buffer)
                while (bytes >= 0) {
                    output.write(buffer, 0, bytes)
                    bytesWritten += bytes
                    if (contentLength > 0) {
                        onProgress(bytesWritten.toFloat() / contentLength)
                    }
                    bytes = input.read(buffer)
                }
            }
        }
        
        Log.d("PaperScreen", "Saved PDF to: ${file.absolutePath}")
        Log.d("PaperScreen", "File size: ${file.length()} bytes written: $bytesWritten")
        
        if (!file.exists() || file.length() == 0L) {
            throw Exception("Failed to save PDF file or file is empty")
        }
        
        file
    } catch (e: Exception) {
        Log.e("PaperScreen", "Error downloading PDF", e)
        throw e
    }
} 