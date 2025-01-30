package com.lakshay.arxplorer.ui

import androidx.lifecycle.ViewModel
import com.lakshay.arxplorer.ui.paper.PaperCommentsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val commentsViewModelFactory: PaperCommentsViewModel.Factory
) : ViewModel() 