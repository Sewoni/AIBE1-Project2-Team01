package org.sunday.projectpop.project.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.sunday.projectpop.project.model.dto.GeminiResponse;
import org.sunday.projectpop.project.model.dto.ProjectRequest;
import org.sunday.projectpop.project.model.dto.ProjectResponse;
import org.sunday.projectpop.project.model.entity.ProjectField;
import org.sunday.projectpop.project.model.entity.UserAccount;
import org.sunday.projectpop.project.model.repository.ProjectFieldRepository;
import org.sunday.projectpop.project.model.repository.SkillTagRepository;
import org.sunday.projectpop.project.model.service.*;
import org.sunday.projectpop.project.model.entity.SkillTag;

import org.sunday.projectpop.project.model.dto.ProjectSearchCondition;
import org.sunday.projectpop.project.model.entity.Project;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final SkillTagService skillTagService;
    private final UserAccountService userAccountService;
    private final ProjectApplicationService applicationService;
    private final ProjectFieldService projectFieldService;

    private final ProjectLLMService projectLLMService;
    private final GeminiLLMService geminiLLMService;

    private final ProjectFieldRepository projectFieldRepository;
    private final SkillTagRepository skillTagRepository;
    // 🖼️ 공고 작성 폼 (HTML 렌더링)
    @GetMapping("/create")
    public String showCreateForm(@RequestParam(required = false) String mode,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 Model model) {
        ProjectRequest projectRequest;

        if ("llm".equals(mode)) {
            // 1. 프롬프트 생성 및 Gemini 호출
            String userId = userDetails.getUsername(); // 또는 임시 "u01"
            // String userId = "u01";
            String prompt = projectLLMService.generatePrompt(userId);
            GeminiResponse response = geminiLLMService.getGeneratedProject(prompt);

            ProjectField field = projectFieldRepository.findByName(response.field())
                    .orElseThrow(() -> new IllegalArgumentException("해당 분야가 존재하지 않습니다."));

            // 📌 String 태그 → ID 매핑

            List<Long> requiredTagIds = skillTagRepository.findByNameIn(response.requiredTags())
                    .stream().map(SkillTag::getTagId).toList();

            List<Long> selectiveTagIds = skillTagRepository.findByNameIn(response.selectiveTags())
                    .stream().map(SkillTag::getTagId).toList();

            // 2. Gemini 응답을 ProjectRequest로 매핑
            projectRequest = ProjectRequest.builder()
                    .title(response.title())
                    .description(response.description())
                    .teamSize(response.teamSize())
                    .durationWeeks(response.durationWeeks())
                    .fieldId(field.getId())
                    .requiredTagIds(requiredTagIds) // ✅ 여기는 TagId
                    .selectiveTagIds(selectiveTagIds)
                    .build();
        } else {
            projectRequest = new ProjectRequest(); // 빈 폼
        }
        model.addAttribute("projectRequest", projectRequest);
        model.addAttribute("tags", skillTagService.getAllTags());
        model.addAttribute("fields", projectFieldService.getAllFields()); // 💡 추가

        System.out.println("✅ 등록된 태그: " + skillTagService.getAllTags());

        return "project/create"; // Thymeleaf 템플릿
    }

    // 📨 폼 제출용 공고 생성 (서버 렌더링용)
    @PostMapping("/submit")
    public String submitProject(@ModelAttribute ProjectRequest request,@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalStateException("인증되지 않은 사용자입니다. JWT 인증이 제대로 작동하지 않았습니다.");
        }
         String userId = userDetails.getUsername();
        UserAccount leader = userAccountService.getUserById(userId);
        List<Long> requiredIds = request.getRequiredTagIds() != null ? request.getRequiredTagIds() : List.of();
        List<Long> selectiveIds = request.getSelectiveTagIds() != null ? request.getSelectiveTagIds() : List.of();
        List<SkillTag> requiredTags = skillTagService.getTagsByIds(request.getRequiredTagIds());
        List<SkillTag> selectiveTags = skillTagService.getTagsByIds(request.getSelectiveTagIds());

        log.debug("📌 @AuthenticationPrincipal userId: {}", userId);
        System.out.println("✅ userId: " + userId);
        System.out.println("✅ leader: " + leader);

        System.out.println("✅ fieldId: " + request.getFieldId());
        System.out.println("✅ requiredTagIds: " + request.getRequiredTagIds());
        System.out.println("✅ selectiveTagIds: " + request.getSelectiveTagIds());
        projectService.create(request, leader, requiredTags, selectiveTags);
        return "redirect:/projects";
    }

    // 🖼️ 공고 목록 화면 보여주기 (선택)
//    @GetMapping
//    public String listProjects(@ModelAttribute ProjectSearchCondition condition, Model model) {
//        Page<Project> page = projectService.searchProjects(condition); // 페이징된 결과
//
//        model.addAttribute("projects", page.getContent());      // 실제 공고 목록
//        model.addAttribute("page", page);                       // 전체 페이지 정보
//        model.addAttribute("condition", condition);             // 필터 상태 유지용
//
//        return "project/list";
//    }
    @GetMapping
    public String listProjects(
            @ModelAttribute ProjectSearchCondition condition,
            @PageableDefault(size = 6) Pageable pageable,
            Model model
    ) {
        Page<Project> projects = projectService.searchProjects(condition, pageable);

        List<ProjectResponse> responseList = projects.getContent().stream()
                .map(project -> ProjectResponse.from(
                        project,
                        projectService.getRequiredTagNames(project.getProjectId()),
                        projectService.getSelectiveTagNames(project.getProjectId())
                ))
                .toList();

        model.addAttribute("projects", responseList);
        model.addAttribute("condition", condition);
        model.addAttribute("tags", skillTagService.getAllTags());
        model.addAttribute("fields", projectFieldService.getAllFields());

        return "project/list_new";
    }

//    @GetMapping("/filter")
//    public String filterProjectsAjax(@ModelAttribute ProjectSearchCondition condition,
//                                     @PageableDefault(size = 6) Pageable pageable,
//                                     Model model) {
//        Page<Project> projects = projectService.searchProjects(condition, pageable);
//        model.addAttribute("projects", projects.getContent());
//        return "project/list :: projectList"; // fragment만 반환
//    }
@GetMapping("/filter")
public String filterProjectsAjax(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String sortBy,
        @PageableDefault(size = 6) Pageable pageable,
        Model model
) {
    ProjectSearchCondition condition = new ProjectSearchCondition();
    if (status != null) condition.setStatus(List.of(status));
    if (sortBy != null) condition.setSortBy(sortBy);

    Page<Project> projects = projectService.searchProjects(condition, pageable);

    List<ProjectResponse> responseList = projects.getContent().stream()
            .map(project -> ProjectResponse.from(
                    project,
                    projectService.getRequiredTagNames(project.getProjectId()),
                    projectService.getSelectiveTagNames(project.getProjectId())
            ))
            .toList();

    model.addAttribute("projects", responseList); // ✅ 필터도 DTO로
    return "project/list :: projectList";
}


//    @GetMapping("/{projectId}")
//    public String viewProjectDetail(@PathVariable String projectId, Model model) {
//        ProjectResponse response = projectService.getProjectDetailWithTags(projectId);
//        List<String> requiredTags = projectService.getRequiredTagNames(projectId);
//        List<String> selectiveTags = projectService.getSelectiveTagNames(projectId);
//
//        model.addAttribute("project", response);
//        model.addAttribute("requiredTags", requiredTags);
//        model.addAttribute("selectiveTags", selectiveTags);
//        return "project/details"; // Thymeleaf 템플릿
//    }
@GetMapping("/detail")
public String viewProjectDetail(@RequestParam("projectId") String projectId, Model model) {


    ProjectResponse response = projectService.getProjectDetailWithTags(projectId);
    // 🔍 여기에 로그 추가
    System.out.println("===== 공고 설명 원문 =====");
    System.out.println(response.description());
    System.out.println("========================");

    List<String> requiredTags = projectService.getRequiredTagNames(projectId);
    List<String> selectiveTags = projectService.getSelectiveTagNames(projectId);





    model.addAttribute("project", response);
    model.addAttribute("requiredTags", requiredTags);
    model.addAttribute("selectiveTags", selectiveTags);

    return "project/details";
}

   @PostMapping("/apply")
    public String apply(@RequestParam("projectId") String projectId,
                        @AuthenticationPrincipal UserDetails userDetails,
                        RedirectAttributes redirectAttributes) {
        try {
            applicationService.applyToProject(projectId, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("success", "지원이 완료되었습니다!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

       return "redirect:/projects/detail?projectId=" + projectId;

   }


}

