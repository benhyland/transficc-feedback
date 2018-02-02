package com.transficc.tools.feedback.ci;

import java.util.List;

import com.transficc.functionality.Result;
import com.transficc.tools.feedback.domain.Job;
import com.transficc.tools.feedback.domain.JobStatus;
import com.transficc.tools.feedback.domain.LatestBuildInformation;

public interface ContinuousIntegrationServer
{
    Result<Integer, List<Job>> getAllJobs();

    Result<Integer, LatestBuildInformation> getLatestBuildInformation(String jobName, JobStatus previousJobStatus);
}
