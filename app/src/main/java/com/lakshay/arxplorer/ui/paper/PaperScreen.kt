package com.lakshay.arxplorer.ui.paper

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import com.lakshay.arxplorer.data.model.ArxivPaper
import kotlinx.coroutines.Dispatchers
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { }, // Empty title to save space
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp), // Reduced spacing between icons
                        modifier = Modifier.padding(end = 4.dp) // Reduced right padding
                    ) {
                        IconButton(
                            onClick = { showAbstract = !showAbstract },
                            modifier = Modifier.size(36.dp) // Smaller icon button
                        ) {
                            Icon(
                                if (showAbstract) Icons.Default.Description else Icons.Default.Info,
                                contentDescription = if (showAbstract) "Show PDF" else "Show Abstract",
                                modifier = Modifier.size(20.dp) // Smaller icon
                            )
                        }
                        IconButton(
                            onClick = { onDownloadClick(paper.pdfUrl) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = "Download PDF",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { onShareClick(paper) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                modifier = Modifier.height(48.dp) // Reduced height of top bar
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                // PDF Viewer with maximized space
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
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

                    AndroidView(
                        factory = { context ->
                            PDFView(context, null).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
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
                                            .spacing(0) // Reduced spacing
                                            .autoSpacing(false) // Disabled auto spacing
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
                }
            }
        }
    }
}

private suspend fun downloadPdf(context: Context, url: String): File = withContext(Dispatchers.IO) {
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
        val contentType = response.header("Content-Type")
        Log.d("PaperScreen", "Content-Type: $contentType")
        
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