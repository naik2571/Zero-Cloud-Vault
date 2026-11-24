package com.vault.zerocloud

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.vault.zerocloud.crypto.KeystoreManager
import com.vault.zerocloud.crypto.TotpGenerator
import com.vault.zerocloud.data.VaultItem
import com.vault.zerocloud.databinding.ItemVaultBinding

class VaultAdapter(
    private var items: List<VaultItem>,
    private val onDeleteClick: (VaultItem) -> Unit
) : RecyclerView.Adapter<VaultAdapter.VaultViewHolder>() {

    private val revealedStates = mutableMapOf<Long, Boolean>()

    inner class VaultViewHolder(val binding: ItemVaultBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VaultViewHolder {
        val binding = ItemVaultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VaultViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<VaultItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: VaultViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.binding.tvItemTitle.text = item.title
        holder.binding.tvItemMeta.text = item.extraMeta.ifEmpty { "Air-gapped secure entry" }

        when (item.category) {
            "TOTP_2FA" -> {
                holder.binding.tvCategoryIcon.text = "🔢"
                holder.binding.tvCountdown.visibility = View.VISIBLE

                try {
                    val rawSecret = KeystoreManager.decrypt(item.encryptedBlob, item.iv)
                    val otp = TotpGenerator.generateTotp(rawSecret)
                    val remaining = TotpGenerator.getRemainingSeconds()

                    // Format as "123 456" for readability
                    val formattedOtp = if (otp.length == 6) "${otp.substring(0, 3)} ${otp.substring(3)}" else otp
                    holder.binding.tvSecretContent.text = formattedOtp
                    holder.binding.tvCountdown.text = "${remaining}s"
                } catch (e: Exception) {
                    holder.binding.tvSecretContent.text = "ERR: DECRYPT"
                    holder.binding.tvCountdown.visibility = View.GONE
                }

                holder.binding.btnRevealCopy.text = "Copy Code"
                holder.binding.btnRevealCopy.setOnClickListener {
                    try {
                        val rawSecret = KeystoreManager.decrypt(item.encryptedBlob, item.iv)
                        val otp = TotpGenerator.generateTotp(rawSecret)
                        copyToClipboard(context, "TOTP Code", otp)
                        Toast.makeText(context, "2FA code copied!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Decryption error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            "PASSWORD" -> {
                holder.binding.tvCategoryIcon.text = "🔑"
                holder.binding.tvCountdown.visibility = View.GONE

                val isRevealed = revealedStates[item.id] ?: false
                if (isRevealed) {
                    try {
                        val plain = KeystoreManager.decrypt(item.encryptedBlob, item.iv)
                        holder.binding.tvSecretContent.text = plain
                        holder.binding.btnRevealCopy.text = "Copy"
                    } catch (e: Exception) {
                        holder.binding.tvSecretContent.text = "ERR: DECRYPT"
                        holder.binding.btnRevealCopy.text = "Reveal"
                    }
                } else {
                    holder.binding.tvSecretContent.text = "••••••••••••"
                    holder.binding.btnRevealCopy.text = "Reveal"
                }

                holder.binding.btnRevealCopy.setOnClickListener {
                    if (revealedStates[item.id] == true) {
                        // Already revealed, clicking copies password to clipboard
                        try {
                            val plain = KeystoreManager.decrypt(item.encryptedBlob, item.iv)
                            copyToClipboard(context, "Password", plain)
                            Toast.makeText(context, "Password copied to clipboard!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Decryption error", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        revealedStates[item.id] = true
                        notifyItemChanged(holder.adapterPosition)
                    }
                }
            }
            else -> { // SECRET_NOTE
                holder.binding.tvCategoryIcon.text = "📝"
                holder.binding.tvCountdown.visibility = View.GONE

                val isRevealed = revealedStates[item.id] ?: false
                if (isRevealed) {
                    try {
                        val plain = KeystoreManager.decrypt(item.encryptedBlob, item.iv)
                        holder.binding.tvSecretContent.text = plain
                        holder.binding.btnRevealCopy.text = "Hide"
                    } catch (e: Exception) {
                        holder.binding.tvSecretContent.text = "ERR: DECRYPT"
                        holder.binding.btnRevealCopy.text = "Reveal"
                    }
                } else {
                    holder.binding.tvSecretContent.text = "••••••••••••"
                    holder.binding.btnRevealCopy.text = "Reveal"
                }

                holder.binding.btnRevealCopy.setOnClickListener {
                    revealedStates[item.id] = !(revealedStates[item.id] ?: false)
                    notifyItemChanged(holder.adapterPosition)
                }
            }
        }

        holder.binding.btnDelete.setOnClickListener {
            onDeleteClick(item)
        }
    }

    private fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
}
