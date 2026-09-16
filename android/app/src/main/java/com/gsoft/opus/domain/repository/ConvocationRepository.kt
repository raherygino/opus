package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Convocation
import com.gsoft.opus.domain.model.ConvocationAttachment
import com.gsoft.opus.domain.repository.UploadFile

/** Input data for creating/updating a convocation. */
data class ConvocationFormData(
    val type: String,
    val dateConvocation: String,
    val numero: String? = null,
    val nom: String,
    val adresse: String?,
    val infraction: String?,
    val personneAccuseRecu: String?,
    val numeroDossier: String?,
    val observation: String?
)

interface ConvocationRepository {
    suspend fun getConvocationList(
        type: String? = null,
        search: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null
    ): Resource<List<Convocation>>

    suspend fun getConvocation(id: Int): Resource<Convocation>
    suspend fun createConvocation(data: ConvocationFormData): Resource<Convocation>
    suspend fun updateConvocation(id: Int, data: ConvocationFormData): Resource<Convocation>
    suspend fun deleteConvocation(id: Int): Resource<Unit>
    suspend fun peekConvocationNumber(type: String): Resource<String>

    suspend fun getConvocationAttachments(convocationId: Int): Resource<List<ConvocationAttachment>>
    suspend fun addConvocationAttachment(convocationId: Int, title: String, file: UploadFile): Resource<ConvocationAttachment>
    suspend fun updateConvocationAttachmentTitle(convocationId: Int, attachId: Int, title: String): Resource<ConvocationAttachment>
    suspend fun deleteConvocationAttachment(convocationId: Int, attachId: Int): Resource<Unit>
}
