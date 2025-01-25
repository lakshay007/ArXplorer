package com.lakshay.arxplorer.ui.paper

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.lakshay.arxplorer.data.model.ArxivPaper

class PaperViewModel(application: Application) : AndroidViewModel(application) {
    
    fun downloadPdf(pdfUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(pdfUrl)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    fun sharePaper(paper: ArxivPaper) {
        val shareText = buildString {
            appendLine(paper.title)
            appendLine()
            appendLine("Authors: ${paper.authors.joinToString(", ")}")
            appendLine()
            appendLine("Abstract:")
            appendLine(paper.abstract)
            appendLine()
            appendLine("arXiv: ${paper.pdfUrl}")
            paper.doi?.let { appendLine("DOI: $it") }
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, paper.title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        val shareIntent = Intent.createChooser(intent, "Share Paper")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(shareIntent)
    }
} 