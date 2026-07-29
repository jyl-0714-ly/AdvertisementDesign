package com.advertisementdesign.back.project.service;

import com.advertisementdesign.back.project.model.ProjectModels;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Kept as the project module's public facade while callers migrate to explicit query contracts.
 */
@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectQueryService queryService;

    public List<ProjectModels.ProjectSummaryView> list(String status, String keyword) {
        return queryService.listAuthorizedSummaries(status, keyword);
    }

    public ProjectModels.ProjectFullDetailView detail(Long projectId) {
        return queryService.requireFullDetail(projectId);
    }
}
