package com.lakshay.arxplorer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lakshay.arxplorer.data.model.ArxivPaper
import com.lakshay.arxplorer.service.GeminiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class Message(
    val content: String,
    val isUser: Boolean,
    val isError: Boolean = false,
    val isThinking: Boolean = false
)

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false
)

class ChatViewModel(private val geminiService: GeminiService) : ViewModel() {
    private val _chatState = MutableStateFlow(ChatState())
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    fun sendMessage(paper: ArxivPaper, userInput: String) {
        if (userInput.isBlank()) return

        viewModelScope.launch {
            // Add user message
            _chatState.value = _chatState.value.copy(
                messages = _chatState.value.messages + Message(userInput, isUser = true),
                isLoading = true
            )

            // Get response from Gemini
            var lastThinkingMessage: Message? = null
            geminiService.chat(paper, userInput).collect { response ->
                if (response.startsWith("ArXAI is thinking")) {
                    // Add thinking message
                    lastThinkingMessage = Message(response, isUser = false, isThinking = true)
                    _chatState.value = _chatState.value.copy(
                        messages = _chatState.value.messages + lastThinkingMessage!!
                    )
                } else {
                    // Remove thinking message if it exists
                    val messages = if (lastThinkingMessage != null) {
                        _chatState.value.messages.filterNot { it == lastThinkingMessage }
                    } else {
                        _chatState.value.messages
                    }
                    
                    // Add actual response
                    _chatState.value = _chatState.value.copy(
                        messages = messages + Message(
                            content = response,
                            isUser = false,
                            isError = response.startsWith("Error:")
                        ),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun summarizePaper(paper: ArxivPaper) {
        viewModelScope.launch {
            _chatState.value = _chatState.value.copy(isLoading = true)

            var lastThinkingMessage: Message? = null
            geminiService.summarize(paper).collect { response ->
                if (response.startsWith("ArXAI is analyzing")) {
                    // Add thinking message
                    lastThinkingMessage = Message(response, isUser = false, isThinking = true)
                    _chatState.value = _chatState.value.copy(
                        messages = _chatState.value.messages + lastThinkingMessage!!
                    )
                } else {
                    // Remove thinking message if it exists
                    val messages = if (lastThinkingMessage != null) {
                        _chatState.value.messages.filterNot { it == lastThinkingMessage }
                    } else {
                        _chatState.value.messages
                    }
                    
                    // Add actual response
                    _chatState.value = _chatState.value.copy(
                        messages = messages + Message(
                            content = response,
                            isUser = false,
                            isError = response.startsWith("Error:")
                        ),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun clearChat() {
        _chatState.value = ChatState()
    }
} 