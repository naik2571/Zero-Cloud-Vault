package com.vault.zerocloud.data

data class VaultItem(
    val id: Long = 0,
    val title: String,
    val category: String, // PASSWORD, TOTP_2FA, SECRET_NOTE
    val encryptedBlob: String,
    val iv: String,
    val extraMeta: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
