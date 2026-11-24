package com.vault.zerocloud

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vault.zerocloud.crypto.KeystoreManager
import com.vault.zerocloud.data.VaultDbHelper
import com.vault.zerocloud.data.VaultItem
import com.vault.zerocloud.databinding.ActivityMainBinding
import com.vault.zerocloud.databinding.DialogAddItemBinding

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "ZeroVaultMainActivity"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: VaultDbHelper
    private lateinit var adapter: VaultAdapter

    private var isVaultUnlocked = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val totpRefreshRunnable = object : Runnable {
        override fun run() {
            if (isVaultUnlocked) {
                adapter.notifyDataSetChanged()
                mainHandler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = VaultDbHelper(this)
        setupRecycler()
        setupListeners()

        // Prompt biometric unlock on launch
        promptBiometricUnlock()
    }

    private fun setupRecycler() {
        adapter = VaultAdapter(emptyList()) { item ->
            confirmDeleteItem(item)
        }
        binding.rvVault.layoutManager = LinearLayoutManager(this)
        binding.rvVault.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnUnlock.setOnClickListener {
            promptBiometricUnlock()
        }

        binding.tvLockVaultBtn.setOnClickListener {
            lockVault()
        }

        binding.fabAdd.setOnClickListener {
            showAddItemDialog()
        }
    }

    private fun promptBiometricUnlock() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.i(TAG, "Biometric authentication confirmed: Vault Unlocked")
                unlockVault()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.w(TAG, "Biometric unlock error $errorCode: $errString")
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    Toast.makeText(this@MainActivity, "Auth Error: $errString", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(this@MainActivity, "Biometrics not recognized", Toast.LENGTH_SHORT).show()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock ZeroCloud Vault")
            .setSubtitle("Confirm biometrics or device PIN to access encrypted vault")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun unlockVault() {
        isVaultUnlocked = true
        binding.layoutLockedOverlay.visibility = View.GONE
        binding.layoutVaultContent.visibility = View.VISIBLE
        binding.fabAdd.visibility = View.VISIBLE
        loadVaultItems()

        mainHandler.removeCallbacks(totpRefreshRunnable)
        mainHandler.post(totpRefreshRunnable)
    }

    private fun lockVault() {
        isVaultUnlocked = false
        mainHandler.removeCallbacks(totpRefreshRunnable)
        binding.layoutLockedOverlay.visibility = View.VISIBLE
        binding.layoutVaultContent.visibility = View.GONE
        binding.fabAdd.visibility = View.GONE
        adapter.updateList(emptyList())
        Toast.makeText(this, "Vault Locked", Toast.LENGTH_SHORT).show()
    }

    private fun loadVaultItems() {
        val items = dbHelper.getAllItems()
        adapter.updateList(items)

        binding.tvItemCount.text = "${items.size} Encrypted Items"
        binding.layoutEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showAddItemDialog() {
        val dialogBinding = DialogAddItemBinding.inflate(LayoutInflater.from(this))
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val title = dialogBinding.etTitle.text.toString().trim()
            val meta = dialogBinding.etMeta.text.toString().trim()
            val secret = dialogBinding.etSecret.text.toString().trim()

            if (title.isEmpty() || secret.isEmpty()) {
                Toast.makeText(this, "Title and Secret are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val category = when {
                dialogBinding.rbTotp.isChecked -> "TOTP_2FA"
                dialogBinding.rbNote.isChecked -> "SECRET_NOTE"
                else -> "PASSWORD"
            }

            try {
                // Hardware AES-256-GCM encryption
                val cryptoResult = KeystoreManager.encrypt(secret)

                val item = VaultItem(
                    title = title,
                    category = category,
                    encryptedBlob = cryptoResult.cipherTextBase64,
                    iv = cryptoResult.ivBase64,
                    extraMeta = meta
                )

                dbHelper.insertItem(item)
                dialog.dismiss()
                loadVaultItems()
                Toast.makeText(this, "Item safely encrypted and stored!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Encryption failed: ${e.message}", e)
                Toast.makeText(this, "Encryption failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        dialog.show()
    }

    private fun confirmDeleteItem(item: VaultItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to permanently delete \"${item.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                dbHelper.deleteItem(item.id)
                loadVaultItems()
                Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onStop() {
        super.onStop()
        // Auto-lock vault when user switches apps or turns off screen
        if (isVaultUnlocked) {
            lockVault()
        }
    }
}
