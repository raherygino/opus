package com.gsoft.opus.domain.model

/** A system role as returned by GET /api/roles. */
data class Role(
    val id: Int,
    val code: String,
    val name: String,
    val description: String?,
    val permissions: List<Permission> = emptyList()
)
