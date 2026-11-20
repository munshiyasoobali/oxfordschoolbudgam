package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "oxford_session_prefs")

data class SchoolPaymentAccount(
    val accountHolderName: String = "OXFORD SCHOOL EDUCATION BUDGAM",
    val upiId: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val ifscCode: String = ""
) {
    val hasUpiConfigured: Boolean
        get() = upiId.isNotBlank() && upiId.contains("@")

    val hasBankConfigured: Boolean
        get() = bankName.isNotBlank() && accountNumber.isNotBlank() && ifscCode.isNotBlank()

    val isConfigured: Boolean
        get() = hasUpiConfigured || hasBankConfigured
}

class SessionPreferences(private val context: Context) {
    private val keyLoggedInUserId = longPreferencesKey("logged_in_user_id")
    private val keyPayeeName = stringPreferencesKey("school_payee_name")
    private val keyUpiId = stringPreferencesKey("school_upi_id")
    private val keyBankName = stringPreferencesKey("school_bank_name")
    private val keyAccountNumber = stringPreferencesKey("school_account_number")
    private val keyIfscCode = stringPreferencesKey("school_ifsc_code")

    val loggedInUserId: Flow<Long?> = context.sessionDataStore.data.map { prefs ->
        val value = prefs[keyLoggedInUserId] ?: -1L
        if (value > 0L) value else null
    }

    val paymentAccount: Flow<SchoolPaymentAccount> = context.sessionDataStore.data.map { prefs ->
        SchoolPaymentAccount(
            accountHolderName = prefs[keyPayeeName]?.takeIf { it.isNotBlank() }
                ?: "OXFORD SCHOOL EDUCATION BUDGAM",
            upiId = prefs[keyUpiId] ?: "",
            bankName = prefs[keyBankName] ?: "",
            accountNumber = prefs[keyAccountNumber] ?: "",
            ifscCode = prefs[keyIfscCode] ?: ""
        )
    }

    suspend fun setLoggedInUserId(userId: Long?) {
        context.sessionDataStore.edit { prefs ->
            if (userId != null && userId > 0L) {
                prefs[keyLoggedInUserId] = userId
            } else {
                prefs.remove(keyLoggedInUserId)
            }
        }
    }

    suspend fun savePaymentAccount(account: SchoolPaymentAccount) {
        context.sessionDataStore.edit { prefs ->
            prefs[keyPayeeName] = account.accountHolderName.trim()
                .ifBlank { "OXFORD SCHOOL EDUCATION BUDGAM" }
            prefs[keyUpiId] = account.upiId.trim()
            prefs[keyBankName] = account.bankName.trim()
            prefs[keyAccountNumber] = account.accountNumber.trim()
            prefs[keyIfscCode] = account.ifscCode.trim().uppercase()
        }
    }
}
