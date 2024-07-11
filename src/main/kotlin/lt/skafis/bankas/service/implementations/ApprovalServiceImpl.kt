package lt.skafis.bankas.service.implementations

import lt.skafis.bankas.dto.*
import lt.skafis.bankas.model.Problem
import lt.skafis.bankas.model.ReviewStatus
import lt.skafis.bankas.model.Role
import lt.skafis.bankas.model.Source
import lt.skafis.bankas.repository.ProblemRepository
import lt.skafis.bankas.repository.SourceRepository
import lt.skafis.bankas.repository.StorageRepository
import lt.skafis.bankas.service.ApprovalService
import lt.skafis.bankas.service.ProblemService
import lt.skafis.bankas.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.threeten.bp.Instant
import org.webjars.NotFoundException
import java.util.*

@Service
class ApprovalServiceImpl: ApprovalService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var sourceRepository: SourceRepository

    @Autowired
    private lateinit var problemRepository: ProblemRepository

    @Autowired
    private lateinit var problemService: ProblemService

    @Autowired
    private lateinit var storageRepository: StorageRepository

    override fun submitSourceData(sourceData: SourceSubmitDto): String {
        val username = userService.getCurrentUserUsername()

        log.info("Creating source in firestore by user: $username")
        val createdSource = sourceRepository.create(
            Source(
                name = sourceData.name,
                description = sourceData.description,
                author = username
            )
        )
        return createdSource.id
    }

    override fun submitProblem(
        sourceId: String,
        problem: ProblemSubmitDto,
        problemImageFile: MultipartFile?,
        answerImageFile: MultipartFile?
    ): String {
        val username = userService.getCurrentUserUsername()
        val imagesUUID = UUID.randomUUID()

        log.info("Creating problem in firestore by user: $username")
        val createdProblem = problemRepository.create(
            Problem(
                problemText = problem.problemText,
                problemImagePath = problemService.utilsGetNewPath(problem.problemImageUrl, if (problemImageFile == null) "" else "problems/${imagesUUID}.${
                    problemImageFile.originalFilename?.split(".")?.last() ?: ""
                }"),
                answerText = problem.answerText,
                answerImagePath = problemService.utilsGetNewPath(problem.answerImageUrl, if (answerImageFile == null) "" else "answers/${imagesUUID}.${
                    answerImageFile.originalFilename?.split(".")?.last() ?: ""
                }"),
                sourceId = sourceId
            )
        )

        log.info("Uploading images to storage by user: $username")
        problemImageFile?.let {
            storageRepository.uploadImage(problemImageFile, "problems/${imagesUUID}.${it.originalFilename?.split(".")?.last()}")
        }
        answerImageFile?.let {
            storageRepository.uploadImage(answerImageFile, "answers/${imagesUUID}.${it.originalFilename?.split(".")?.last()}")
        }

        return createdProblem.id
    }

    override fun getMySources(): List<Source> {
        val username = userService.getCurrentUserUsername()
        log.info("Getting sources by user: $username")
        return sourceRepository.getByAuthor(username)
    }

    override fun getProblemsBySource(sourceId: String): List<ProblemDisplayViewDto> {
        val username = userService.getCurrentUserUsername()
        val source = sourceRepository.findById(sourceId) ?: throw NotFoundException("Source not found")
        if (source.author != username  && source.reviewStatus != ReviewStatus.APPROVED) {
            userService.grantRoleAtLeast(Role.ADMIN)
        }

        log.info("Getting problems by source: $sourceId")
        return problemRepository.getBySourceId(sourceId)
            .map {
                ProblemDisplayViewDto(
                    id = it.id,
                    problemText = it.problemText,
                    problemImageSrc = problemService.utilsGetImageSrc(it.problemImagePath),
                    answerText = it.answerText,
                    answerImageSrc = problemService.utilsGetImageSrc(it.answerImagePath),
                )
            }
    }

    override fun approve(sourceId: String, reviewMessage: String): Source {
        val username = userService.getCurrentUserUsername()
        log.info("Approving source: $sourceId with message: $reviewMessage by user: $username")
        val source = sourceRepository.findById(sourceId) ?: throw NotFoundException("Source not found")
        val updatedSource = source.copy(
            reviewStatus = ReviewStatus.APPROVED,
            reviewMessage = reviewMessage,
            reviewedBy = username,
            reviewedOn = Instant.now().toString()
        )
        sourceRepository.update(updatedSource, sourceId)
        return updatedSource
    }

    override fun reject(sourceId: String, reviewMessage: String): Source {
        val username = userService.getCurrentUserUsername()
        log.info("Rejecting source: $sourceId with message: $reviewMessage by user: $username")
        val source = sourceRepository.findById(sourceId) ?: throw NotFoundException("Source not found")
        val updatedSource = source.copy(
            reviewStatus = ReviewStatus.REJECTED,
            reviewMessage = reviewMessage,
            reviewedBy = username,
            reviewedOn = Instant.now().toString()
        )
        sourceRepository.update(updatedSource, sourceId)
        return updatedSource
    }

    override fun deleteSource(sourceId: String) {
        val username = userService.getCurrentUserUsername()
        log.info("Deleting source: $sourceId by user: $username")
        val source = sourceRepository.findById(sourceId) ?: throw NotFoundException("Source not found")
        if (source.author != username) {
            throw IllegalAccessException("User $username does not own source $sourceId")
        }
        val problems = problemRepository.getBySourceId(sourceId)
        if (problems.isNotEmpty()) {
            throw IllegalAccessException("Source $sourceId has problems, delete them first")
        }
        sourceRepository.delete(sourceId)
    }

    override fun deleteProblem(problemId: String) {
        val username = userService.getCurrentUserUsername()
        log.info("Deleting problem: $problemId by user: $username")
        val problem = problemRepository.findById(problemId) ?: throw NotFoundException("Problem not found")
        val source = sourceRepository.findById(problem.sourceId) ?: throw NotFoundException("Source not found")
        if (source.author != username) {
            throw IllegalAccessException("User $username does not own source ${problem.sourceId}")
        }
        problemRepository.delete(problemId)
        if (problem.problemImagePath.startsWith("problems/")) {
            storageRepository.deleteImage("problems/${problem.problemImagePath}")
        }
        if (problem.answerImagePath.startsWith("answers/")) {
            storageRepository.deleteImage("answers/${problem.answerImagePath}")
        }
        val modifiedSource = source.copy(
            lastModifiedOn = Instant.now().toString(),
            reviewStatus = ReviewStatus.PENDING
        )
        sourceRepository.update(modifiedSource, problem.sourceId)
    }

    override fun updateSource(sourceId: String, sourceData: SourceSubmitDto): Source {
        val username = userService.getCurrentUserUsername()
        log.info("Updating source: $sourceId by user: $username")
        val source = sourceRepository.findById(sourceId) ?: throw NotFoundException("Source not found")
        if (source.author != username) {
            throw IllegalAccessException("User $username does not own source $sourceId")
        }
        val updatedSource = source.copy(
            name = sourceData.name,
            description = sourceData.description,
            lastModifiedOn = Instant.now().toString(),
            reviewStatus = ReviewStatus.PENDING
        )
        sourceRepository.update(updatedSource, sourceId)
        return updatedSource
    }

    override fun updateProblem(
        problemId: String,
        problem: ProblemSubmitDto,
        problemImageFile: MultipartFile?,
        answerImageFile: MultipartFile?
    ): Problem {
        val username = userService.getCurrentUserUsername()
        val imagesUUID = UUID.randomUUID()

        log.info("Updating problem: $problemId by user: $username")
        val problemToUpdate = problemRepository.findById(problemId) ?: throw NotFoundException("Problem not found")
        val updatedProblem = problemToUpdate.copy(
            problemText = problem.problemText,
            problemImagePath = problemService.utilsGetNewPath(problem.problemImageUrl, if (problemImageFile == null) "" else "problems/${imagesUUID}.${
                problemImageFile.originalFilename?.split(".")?.last() ?: ""
            }"),
            answerText = problem.answerText,
            answerImagePath = problemService.utilsGetNewPath(problem.answerImageUrl, if (answerImageFile == null) "" else "answers/${imagesUUID}.${
                answerImageFile.originalFilename?.split(".")?.last() ?: ""
            }"),
        )
        problemRepository.update(updatedProblem, problemId)

        problemImageFile?.let {
            storageRepository.uploadImage(problemImageFile, "problems/${imagesUUID}.${it.originalFilename?.split(".")?.last()}")
        }
        answerImageFile?.let {
            storageRepository.uploadImage(answerImageFile, "answers/${imagesUUID}.${it.originalFilename?.split(".")?.last()}")
        }
        problemToUpdate.problemImagePath.takeIf { it.startsWith("problems/") }?.let {
            storageRepository.deleteImage(it)
        }
        problemToUpdate.answerImagePath.takeIf { it.startsWith("answers/") }?.let {
            storageRepository.deleteImage(it)
        }

        return updatedProblem
    }
}