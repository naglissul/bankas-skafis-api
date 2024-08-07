package lt.skafis.bankas.service.implementations

import lt.skafis.bankas.dto.ProblemDisplayViewDto
import lt.skafis.bankas.dto.SourceDisplayDto
import lt.skafis.bankas.model.*
import lt.skafis.bankas.repository.CategoryRepository
import lt.skafis.bankas.repository.ProblemRepository
import lt.skafis.bankas.repository.SourceRepository
import lt.skafis.bankas.service.ProblemService
import lt.skafis.bankas.service.PublicService
import lt.skafis.bankas.service.SourceService
import lt.skafis.bankas.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PublicServiceImpl: PublicService {

    @Autowired
    private lateinit var sourceRepository: SourceRepository

    @Autowired
    private lateinit var problemService: ProblemService

    @Autowired
    private lateinit var problemRepository: ProblemRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var sourceService: SourceService

    @Autowired
    private lateinit var userService: UserService

    override fun getProblemsCount(): Long {
        return problemRepository.countApproved()
    }

    override fun getCategoriesCount(): Long {
        return categoryRepository.countDocuments()
    }

    override fun getCategoryProblemCount(categoryId: String): Long {
        return problemRepository.countApprovedByCategoryId(categoryId)
    }

    override fun getProblemsByCategoryShuffle(categoryId: String): List<ProblemDisplayViewDto> {
        return problemRepository.getByCategoryId(categoryId)
            .filter {
                it.isApproved
            }
            .map {
                ProblemDisplayViewDto(
                    id = it.id,
                    sourceListNr = it.sourceListNr,
                    skfCode = it.skfCode,
                    problemText = it.problemText,
                    problemImageSrc = problemService.utilsGetImageSrc(it.problemImagePath),
                    answerText = it.answerText,
                    answerImageSrc = problemService.utilsGetImageSrc(it.answerImagePath),
                    categories = it.categories,
                    sourceId = it.sourceId,
                )
            }.shuffled()
    }

    override fun getCategoryById(categoryId: String): Category {
        return categoryRepository.findById(categoryId) ?: throw Exception("Category with id $categoryId not found")
    }

    override fun getCategories(): List<Category> {
        return categoryRepository.findAll()
            .sortedBy {
                it.name
            }
    }

    override fun getProblemBySkfCode(skfCode: String): ProblemDisplayViewDto {
        val problem = problemRepository.getBySkfCode(skfCode)
        if (!problem.isApproved) throw Exception("Problem with skfCode $skfCode is not approved")

        return ProblemDisplayViewDto(
            id = problem.id,
            sourceListNr = problem.sourceListNr,
            skfCode = problem.skfCode,
            problemText = problem.problemText,
            problemImageSrc = problemService.utilsGetImageSrc(problem.problemImagePath),
            answerText = problem.answerText,
            answerImageSrc = problemService.utilsGetImageSrc(problem.answerImagePath),
            categories = problem.categories,
            sourceId = problem.sourceId,
        )
    }

    override fun getSourceById(sourceId: String): SourceDisplayDto {
        val source = sourceService.getSourceById(sourceId)
        if (source.reviewStatus != ReviewStatus.APPROVED && source.authorId != userService.getCurrentUserId())
        {
            userService.grantRoleAtLeast(Role.ADMIN)
        }
        val authorUsername = userService.getUsernameById(source.authorId)
        return source.toDisplayDto(authorUsername)
    }

    override fun getSourcesByAuthor(authorUsername: String): List<Source> {
        return sourceRepository.getByAuthor(authorUsername).filter {
            it.reviewStatus == ReviewStatus.APPROVED
        }
    }

    override fun getUnsortedProblems(): List<ProblemDisplayViewDto> {
        return problemRepository.getUnsortedApprovedProblems()
            .map {
                ProblemDisplayViewDto(
                    id = it.id,
                    sourceListNr = it.sourceListNr,
                    skfCode = it.skfCode,
                    problemText = it.problemText,
                    problemImageSrc = problemService.utilsGetImageSrc(it.problemImagePath),
                    answerText = it.answerText,
                    answerImageSrc = problemService.utilsGetImageSrc(it.answerImagePath),
                    categories = it.categories,
                    sourceId = it.sourceId,
                )
            }
            .shuffled()
    }

    override fun getUnsortedProblemsCount(): Long {
        return problemRepository.countUnsortedApproved()
    }

}