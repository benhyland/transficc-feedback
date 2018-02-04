package com.transficc.tools.feedback.ci.gitlab;

import com.transficc.functionality.Result;
import com.transficc.tools.feedback.ci.ContinuousIntegrationServer;
import com.transficc.tools.feedback.domain.Job;
import com.transficc.tools.feedback.domain.JobStatus;
import com.transficc.tools.feedback.domain.*;
import org.gitlab4j.api.Constants;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.Pager;
import org.gitlab4j.api.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GitlabFacade implements ContinuousIntegrationServer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(GitlabFacade.class);
    private final GitLabApi gitLabApi;

    public GitlabFacade(final GitLabApi gitLabApi)
    {
        this.gitLabApi = gitLabApi;
    }

    @Override
    public Result<Integer, List<Job>> getAllJobs()
    {
        try
        {
            final Pager<Project> projectPager = gitLabApi.getProjectApi().getProjects(100);
            final List<Job> jobs = new ArrayList<>(projectPager.getTotalItems());
            while (projectPager.hasNext())
            {
                final List<Project> projects = projectPager.next();
                for (final Project project : projects)
                {
                    final List<Pipeline> pipelines = getLatestPipeline(project);
                    if (!project.getArchived() && project.getVisibility() != Visibility.PRIVATE && pipelines.size() == 1)
                    {
                        final Job job = new Job(project.getPathWithNamespace(), project.getWebUrl() + "/pipelines/", JobStatus.DISABLED, VersionControl.GIT);
                        jobs.add(job);
                    }
                }
            }
            return Result.success(jobs);
        }
        catch (final GitLabApiException e)
        {
            LOGGER.warn("Received an error trying to get jobs", e);
            return Result.error(e.getHttpStatus());
        }
    }

    @Override
    public Result<Integer, LatestBuildInformation> getLatestBuildInformation(final String jobName, final JobStatus previousJobStatus)
    {
        final int namespaceEndIndex = jobName.indexOf("/");
        final String namespace = jobName.substring(0, namespaceEndIndex);
        final String path = jobName.substring(namespaceEndIndex + 1);
        try
        {
            final Project project = gitLabApi.getProjectApi().getProject(namespace, path);

            if (project.getDefaultBranch() == null)
            {
                return Result.error(400);
            }

            final Commit commit = gitLabApi.getRepositoryApi().getBranch(project.getId(), project.getDefaultBranch()).getCommit();

            final List<Pipeline> pipelines = getLatestPipeline(project);

            if (pipelines.size() != 1)
            {
                return Result.error(400);
            }

            final Pipeline pipeline = pipelines.get(0);
            final String revision = commit.getId();
            final JobStatus jobStatus = convertJobStatus(pipeline.getStatus());
            final int buildNumber = pipeline.getId();
            final long buildTimestamp = pipeline.getStarted_at() == null ? 0L : pipeline.getStarted_at().getTime();
            final double jobCompletionPercentage = jobStatus == JobStatus.BUILDING ? 0.0 : 100.0;
            final String[] comments = new String[]{commit.getMessage()};
            final boolean building = jobStatus == JobStatus.BUILDING;
            final TestResults testResults = new TestResults(0, 0, 0);
            final long buildDuration = pipeline.getDuration() == null ? 0 : pipeline.getDuration().longValue();
            return Result.success(new LatestBuildInformation(revision, jobStatus, buildNumber, buildTimestamp, jobCompletionPercentage, comments, building, testResults, buildDuration));

        }
        catch (final GitLabApiException e)
        {
            LOGGER.warn("Received an error trying to get latest build information", e);
            return Result.error(e.getHttpStatus());
        }
    }

    private List<Pipeline> getLatestPipeline(Project project) throws GitLabApiException
    {
        return gitLabApi.getPipelineApi().getPipelines(project.getId(), null, null,
                project.getDefaultBranch(), false, null, null, Constants.PipelineOrderBy.ID, Constants.SortOrder.DESC, 1, 1);
    }

    private JobStatus convertJobStatus(final PipelineStatus status)
    {
        if (status == null)
        {
            return JobStatus.DISABLED;
        }

        switch (status)
        {
            case FAILED:
                return JobStatus.ERROR;
            case CANCELED:
            case SKIPPED:
                return JobStatus.DISABLED;
            case PENDING:
            case RUNNING:
                return JobStatus.BUILDING;
            case SUCCESS:
                return JobStatus.SUCCESS;
        }
        return JobStatus.ERROR;
    }
}
