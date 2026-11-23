package com.mhyddnn.mubarokappmobile.feature.visit.domain

data class Visit(
    val id: Int,
    val storeName: String,
    val visitDate: String,
    val marketAreaId: Int,
    val marketAreaName: String?,
    val note: String?,
    val status: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class VisitPage(
    val items: List<Visit>,
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int,
)

data class VisitMarketArea(
    val id: Int,
    val name: String,
    val code: String? = null,
)

data class NewVisit(
    val storeName: String,
    val visitDate: String,
    val marketAreaId: Int,
    val note: String?,
)