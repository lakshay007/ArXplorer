package com.lakshay.arxplorer.data.model

import java.time.ZonedDateTime

data class ArxivPaper(
    val id: String,
    val title: String,
    val authors: List<String>,
    val summary: String,
    val publishedDate: ZonedDateTime,
    val updatedDate: ZonedDateTime,
    val pdfUrl: String,
    val categories: List<String>,
    val doi: String? = null,
    val primaryCategory: String,
    val abstract: String,
    val commentaries: String? = null,
    val journalRef: String? = null
) 