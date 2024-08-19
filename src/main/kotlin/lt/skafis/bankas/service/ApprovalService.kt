package lt.skafis.bankas.service

import lt.skafis.bankas.dto.*
import org.springframework.web.multipart.MultipartFile

interface ApprovalService {
    fun submitSourceData(sourceData: SourceSubmitDto): String

    fun submitProblem(
        sourceId: String,
        problem: ProblemSubmitDto,
        problemImageFile: MultipartFile?,
        answerImageFile: MultipartFile?,
    ): String

    fun getMySources(): List<SourceDisplayDto>

    fun getProblemsBySource(
        sourceId: String,
        page: Int,
        size: Int,
    ): List<ProblemDisplayViewDto>

    fun approve(
        sourceId: String,
        reviewMessage: String,
    ): SourceDisplayDto

    fun reject(
        sourceId: String,
        reviewMessage: String,
    ): SourceDisplayDto

    fun deleteSource(sourceId: String)

    fun deleteProblem(problemId: String)

    fun updateSource(
        sourceId: String,
        sourceData: SourceSubmitDto,
    ): SourceDisplayDto

    fun getSources(): List<SourceDisplayDto>

    fun updateProblemTexts(
        problemId: String,
        problemTextsDto: ProblemTextsDto,
    )

    fun deleteProblemImage(problemId: String)

    fun deleteAnswerImage(problemId: String)

    fun uploadProblemImage(
        problemId: String,
        image: MultipartFile,
    ): String

    fun uploadAnswerImage(
        problemId: String,
        image: MultipartFile,
    ): String
}
