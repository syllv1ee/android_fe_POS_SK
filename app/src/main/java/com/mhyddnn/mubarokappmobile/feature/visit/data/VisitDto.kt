package com.mhyddnn.mubarokappmobile.feature.visit.data

import com.google.gson.annotations.SerializedName
import com.mhyddnn.mubarokappmobile.feature.visit.domain.NewVisit
import com.mhyddnn.mubarokappmobile.feature.visit.domain.Visit
import com.mhyddnn.mubarokappmobile.feature.visit.domain.VisitMarketArea
import com.mhyddnn.mubarokappmobile.feature.visit.domain.VisitPage

data class VisitDto(
    val id: Int,

    @SerializedName("store_name")
    val storeName: String,

    @SerializedName("visit_date")
    val visitDate: String,

    @SerializedName("market_area_id")
    val marketAreaId: Int,

    val marketArea: VisitMarketAreaDto? = null,

    val note: String? = null,

    val status: String,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null,
)

data class VisitMarketAreaDto(
    val id: Int,
    val name: String,
    val code: String? = null,
)

data class VisitListDataDto(
    val items: List<VisitDto> = emptyList(),
    val meta: VisitMetaDto? = null,
)

data class VisitMetaDto(
    val page: Int = 1,
    val limit: Int = 20,
    val total: Int = 0,

    @SerializedName("totalPages")
    val totalPages: Int = 0,
)

data class CreateVisitRequest(
    @SerializedName("store_name")
    val storeName: String,

    @SerializedName("visit_date")
    val visitDate: String,

    @SerializedName("market_area_id")
    val marketAreaId: Int,

    val note: String? = null,
)

data class UpdateVisitStatusRequest(
    val status: String,
)

fun VisitDto.toDomain(): Visit =
    Visit(
        id = id,
        storeName = storeName,
        visitDate = visitDate,
        marketAreaId = marketAreaId,
        marketAreaName = marketArea?.name,
        note = note,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun VisitMarketAreaDto.toDomain(): VisitMarketArea =
    VisitMarketArea(
        id = id,
        name = name,
        code = code,
    )

fun VisitListDataDto.toDomain(): VisitPage =
    VisitPage(
        items = items.map { it.toDomain() },
        page = meta?.page ?: 1,
        limit = meta?.limit ?: 20,
        total = meta?.total ?: items.size,
        totalPages = meta?.totalPages ?: 1,
    )

fun NewVisit.toRequest(): CreateVisitRequest =
    CreateVisitRequest(
        storeName = storeName.trim(),
        visitDate = visitDate,
        marketAreaId = marketAreaId,
        note = note?.trim()?.takeIf { it.isNotBlank() },
    )