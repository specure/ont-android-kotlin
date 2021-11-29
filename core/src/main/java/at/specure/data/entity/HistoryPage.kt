package at.specure.data.entity

data class HistoryPage(
    val historyItems: List<History>,

    val totalPages: Int?,

    val currentPage: Int?
)