package lt.skafis.bankas.service.implementations

import lt.skafis.bankas.dto.ProblemPostDto
import lt.skafis.bankas.model.Problem
import lt.skafis.bankas.repository.ProblemRepository
import lt.skafis.bankas.repository.StorageRepository
import lt.skafis.bankas.service.ProblemService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.webjars.NotFoundException

@Service
class ProblemServiceImpl: ProblemService {

    @Autowired
    private lateinit var problemRepository: ProblemRepository

    @Autowired
    private lateinit var storageRepository: StorageRepository

    override fun getProblems(): List<Problem> =
        problemRepository.findAll()


    override fun getProblemById(id: String): Problem =
        problemRepository.findById(id) ?: throw NotFoundException("Problem with id $id not found")


    override fun createProblem(problemPostDto: ProblemPostDto): Problem =
         problemRepository.create(
            Problem(
                skfCode = problemPostDto.skfCode,
                problemText = problemPostDto.problemText,
                problemImagePath = problemPostDto.problemImagePath,
                answerText = problemPostDto.answerText,
                answerImagePath = problemPostDto.answerImagePath,
                categories = problemPostDto.categories,
                sourceId = problemPostDto.sourceId
            )
        )


    override fun updateProblem(id: String, problemPostDto: ProblemPostDto): Problem {
        var problemToUpdate = problemRepository.findById(id) ?: throw NotFoundException("Problem with id $id not found")
        problemToUpdate = problemToUpdate.copy(
            skfCode = problemPostDto.skfCode,
            problemText = problemPostDto.problemText,
            problemImagePath = problemPostDto.problemImagePath,
            answerText = problemPostDto.answerText,
            answerImagePath = problemPostDto.answerImagePath,
            categories = problemPostDto.categories,
            sourceId = problemPostDto.sourceId
        )
        val success = problemRepository.update(problemToUpdate, id)
        return if (success) problemToUpdate
        else throw Exception("Failed to update problem with id $id")
    }

    override fun deleteProblem(id: String) {
        val success = problemRepository.delete(id)
        if (!success) throw Exception("Failed to delete problem with id $id")
    }

    override fun utilsGetImageSrc(imagePath: String): String {
        return imagePath.let {
            if (
                it.startsWith("problems/") ||
                it.startsWith("answers/")
            ) {
                storageRepository.getImageUrl(it)
            } else if (it.isEmpty()) {
                ""
            } else {
                throw IllegalArgumentException("Invalid image path: $it")
            }
        }
    }
}