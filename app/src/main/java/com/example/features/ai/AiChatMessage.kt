package com.example.features.ai

import java.util.UUID

/**
 * نموذج رسالة المحادثة مع المساعد الذكي
 */
data class AiChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val executedActionBadge: String? = null,
    val isError: Boolean = false
)
