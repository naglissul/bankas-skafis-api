package lt.skafis.bankas.model

data class Problem (
    override var id: String = "",
    val skfCode: String = "",
    val problemText: String = "",
    val problemImagePath: String = "",
    val answerText: String = "",
    val answerImagePath: String = "",
    val categoryId: String = "",
    val sourceId: String = "",
    val isApproved: Boolean = false,
): Identifiable