package lt.skafis.bankas.service.implementations

import lt.skafis.bankas.dto.ProblemDisplayViewDto
import lt.skafis.bankas.model.Problem
import lt.skafis.bankas.repository.ProblemRepository
import lt.skafis.bankas.repository.SourceRepository
import lt.skafis.bankas.service.ProblemService
import lt.skafis.bankas.service.SortService
import lt.skafis.bankas.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SortServiceImpl: SortService {

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var problemRepository: ProblemRepository

    @Autowired
    private lateinit var sourceRepository: SourceRepository

    @Autowired
    private lateinit var problemService: ProblemService

    override fun getMySortedProblems(): List<ProblemDisplayViewDto> {
        val username = userService.getCurrentUserUsername()
        val sources = sourceRepository.getByAuthor(username)
        val problems = sources.flatMap { source ->
            problemRepository.getBySourceId(source.id)
        }
        return problems.map {
            ProblemDisplayViewDto(
                id = it.id,
                problemText = it.problemText,
                problemImageSrc = problemService.utilsGetImageSrc(it.problemImagePath),
                answerText = it.answerText,
                answerImageSrc = problemService.utilsGetImageSrc(it.answerImagePath),
            )
        }
    }

    override fun getMyUnsortedProblems(): List<ProblemDisplayViewDto> {
        val username = userService.getCurrentUserUsername()
        val sources = sourceRepository.getByAuthorSorted(username)
        val problems = sources.flatMap { source ->
            problemRepository.getBySourceId(source.id)
        }
        return problems.map {
            ProblemDisplayViewDto(
                id = it.id,
                problemText = it.problemText,
                problemImageSrc = problemService.utilsGetImageSrc(it.problemImagePath),
                answerText = it.answerText,
                answerImageSrc = problemService.utilsGetImageSrc(it.answerImagePath),
            )
        }
    }

    override fun sortMyProblem(problemId: String, categoryId: String): Problem {
        val problem = problemRepository.findById(problemId) ?: throw Exception("Problem with id $problemId not found")
        val updatedProblem = problem.copy(categoryId = categoryId)
        problemRepository.update(updatedProblem, problemId)
        return updatedProblem
    }

    override fun getNotMySortedProblems(): List<ProblemDisplayViewDto> {
        val username = userService.getCurrentUserUsername()
        val sources = sourceRepository.getByNotAuthorSorted(username)
        val problems = sources.flatMap { source ->
            problemRepository.getBySourceId(source.id)
        }
        return problems.map {
            ProblemDisplayViewDto(
                id = it.id,
                problemText = it.problemText,
                problemImageSrc = problemService.utilsGetImageSrc(it.problemImagePath),
                answerText = it.answerText,
                answerImageSrc = problemService.utilsGetImageSrc(it.answerImagePath),
            )
        }
    }

    override fun getNotMyUnsortedProblems(): List<ProblemDisplayViewDto> {
        val username = userService.getCurrentUserUsername()
        val sources = sourceRepository.getByNotAuthorUnsorted(username)
        val problems = sources.flatMap { source ->
            problemRepository.getBySourceId(source.id)
        }
        return problems.map {
            ProblemDisplayViewDto(
                id = it.id,
                problemText = it.problemText,
                problemImageSrc = problemService.utilsGetImageSrc(it.problemImagePath),
                answerText = it.answerText,
                answerImageSrc = problemService.utilsGetImageSrc(it.answerImagePath),
            )
        }
    }

    override fun sortNotMyProblem(problemId: String, categoryId: String): Problem {
        val problem = problemRepository.findById(problemId) ?: throw Exception("Problem with id $problemId not found")
        val updatedProblem = problem.copy(categoryId = categoryId)
        problemRepository.update(updatedProblem, problemId)
        return updatedProblem
    }
}