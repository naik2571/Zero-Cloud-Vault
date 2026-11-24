package com.vault.zerocloud.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class VaultDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "zerocloud_vault.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_ITEMS = "vault_items"
        const val COL_ID = "id"
        const val COL_TITLE = "title"
        const val COL_CATEGORY = "category"
        const val COL_ENCRYPTED_BLOB = "encrypted_blob"
        const val COL_IV = "iv"
        const val COL_EXTRA_META = "extra_meta"
        const val COL_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createSql = """
            CREATE TABLE $TABLE_ITEMS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_ENCRYPTED_BLOB TEXT NOT NULL,
                $COL_IV TEXT NOT NULL,
                $COL_EXTRA_META TEXT,
                $COL_TIMESTAMP INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createSql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ITEMS")
        onCreate(db)
    }

    fun insertItem(item: VaultItem): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, item.title)
            put(COL_CATEGORY, item.category)
            put(COL_ENCRYPTED_BLOB, item.encryptedBlob)
            put(COL_IV, item.iv)
            put(COL_EXTRA_META, item.extraMeta)
            put(COL_TIMESTAMP, item.timestamp)
        }
        return db.insert(TABLE_ITEMS, null, values)
    }

    fun getAllItems(): List<VaultItem> {
        val list = mutableListOf<VaultItem>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_ITEMS,
            null,
            null,
            null,
            null,
            null,
            "$COL_TIMESTAMP DESC"
        )
        cursor.use {
            val idIndex = it.getColumnIndexOrThrow(COL_ID)
            val titleIndex = it.getColumnIndexOrThrow(COL_TITLE)
            val catIndex = it.getColumnIndexOrThrow(COL_CATEGORY)
            val blobIndex = it.getColumnIndexOrThrow(COL_ENCRYPTED_BLOB)
            val ivIndex = it.getColumnIndexOrThrow(COL_IV)
            val metaIndex = it.getColumnIndexOrThrow(COL_EXTRA_META)
            val timeIndex = it.getColumnIndexOrThrow(COL_TIMESTAMP)

            while (it.moveToNext()) {
                list.add(
                    VaultItem(
                        id = it.getLong(idIndex),
                        title = it.getString(titleIndex),
                        category = it.getString(catIndex),
                        encryptedBlob = it.getString(blobIndex),
                        iv = it.getString(ivIndex),
                        extraMeta = it.getString(metaIndex) ?: "",
                        timestamp = it.getLong(timeIndex)
                    )
                )
            }
        }
        return list
    }

    fun deleteItem(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_ITEMS, "$COL_ID = ?", arrayOf(id.toString()))
    }
}
