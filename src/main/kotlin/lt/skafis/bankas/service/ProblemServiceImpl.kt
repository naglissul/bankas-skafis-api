package lt.skafis.bankas.service

import lt.skafis.bankas.dto.CountDto
import lt.skafis.bankas.dto.ProblemDisplayViewDto
import lt.skafis.bankas.dto.ProblemPostDto
import lt.skafis.bankas.dto.ProblemViewDto
import lt.skafis.bankas.repository.FirestoreProblemRepository
import lt.skafis.bankas.repository.ProblemStorageRepository
import org.apache.logging.log4j.util.InternalException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.webjars.NotFoundException
import java.net.URI
import java.net.URL

@Service
class ProblemServiceImpl (
    val firestoreProblemRepository: FirestoreProblemRepository,
    val problemStorageRepository: ProblemStorageRepository,
    val answerStorageRepository: ProblemStorageRepository
) : ProblemService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getProblemById(id: String): ProblemDisplayViewDto {
        log.info("Fetching problem by id: $id")
        val problem = firestoreProblemRepository.getProblemById(id) ?: throw NotFoundException("Problem not found")
        return getImageSources(problem)
    }

    override fun getProblemsByCategoryId(categoryId: String): List<ProblemDisplayViewDto> {
        log.info("Fetching problems by category id: $categoryId")
        val problems = firestoreProblemRepository.getProblemsByCategoryId(categoryId)
        val problemDisplayViewDtos = problems.map { problem ->
            getImageSources(problem)
        }
        log.info("Problems fetched successfully")
        return problemDisplayViewDtos
    }

    override fun createProblem(problem: ProblemPostDto, userId: String, problemImageFile: MultipartFile?, answerImageFile: MultipartFile?): ProblemViewDto {
        TODO("Not yet implemented")
    }

    override fun updateProblem(id: String, problem: ProblemPostDto, userId: String, problemImageFile: MultipartFile?, answerImageFile: MultipartFile?): ProblemViewDto {
        TODO("Not yet implemented")
    }

    override fun deleteProblem(id: String, userId: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun getProblemCount(): CountDto {
        log.info("Fetching problems count")
        val count = firestoreProblemRepository.countDocuments()
        if (count == 0L) throw InternalException("Failed to count problems")
        log.info("Problems count fetched successfully")
        return CountDto(count)
    }

    private fun getImageSources(problem: ProblemViewDto): ProblemDisplayViewDto {
        var problemImageSrc: String? = null
        var answerImageSrc: String? = null

        if (!problem.problemImage.isNullOrEmpty()) {
            problemImageSrc = getImageSrc(problem.problemImage)
        }
        if (!problem.answerImage.isNullOrEmpty()) {
            answerImageSrc = getImageSrc(problem.answerImage)
        }
        return ProblemDisplayViewDto(
            id = problem.id,
            problemImageSrc = problemImageSrc,
            answerImageSrc = answerImageSrc,
            problemText = problem.problemText,
            answerText = problem.answerText,
            categoryId = problem.categoryId,
            createdOn = problem.createdOn
        )
    }

    private fun getImageSrc(imagePath: String?): String? {
        return imagePath?.let {
            if (isValidUrl(it)) {
                // Validate the URL to prevent injection attacks
                URI(it)// This will throw an exception if the URL is not valid
                it
            } else if (it.startsWith("problems/") || it.startsWith("answers/")) {
                // Assume that if the path starts with "problems/" or "answers/", it's a storage path
                if (it.startsWith("problems/")) {
                    problemStorageRepository.getImageUrl(it.removePrefix("problems/"))
                } else {
                    answerStorageRepository.getImageUrl(it.removePrefix("answers/"))
                }
            } else {
                throw IllegalArgumentException("Invalid image path: $it")
            }
        }
    }

    private fun isValidUrl(url: String): Boolean {
        val regex = Regex("https?://[\\w-]+(\\.[\\w-]+)+(/[\\w- ./?%&=]*)?")
        return regex.matches(url)
    }
}