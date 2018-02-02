/*
 * Copyright 2017 TransFICC Ltd.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.  The ASF licenses this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the specific language governing permissions and limitations under the License.
 */
package com.transficc.tools.feedback.ci.jenkins;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildChangeSet;
import com.offbytwo.jenkins.model.BuildChangeSetItem;
import com.offbytwo.jenkins.model.BuildResult;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.JobWithDetails;
import com.transficc.functionality.Result;
import com.transficc.tools.feedback.ci.ContinuousIntegrationServer;
import com.transficc.tools.feedback.domain.Job;
import com.transficc.tools.feedback.domain.JobStatus;
import com.transficc.tools.feedback.domain.LatestBuildInformation;
import com.transficc.tools.feedback.domain.TestResults;
import com.transficc.tools.feedback.domain.VersionControl;
import com.transficc.tools.feedback.util.ClockService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JenkinsFacade implements ContinuousIntegrationServer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JenkinsFacade.class);
    private final JenkinsServer jenkins;
    private final ClockService clockService;
    private final VersionControl versionControl;

    public JenkinsFacade(final JenkinsServer jenkins,
                         final ClockService clockService,
                         final VersionControl versionControl)
    {
        this.jenkins = jenkins;
        this.clockService = clockService;
        this.versionControl = versionControl;
    }

    @Override
    public Result<Integer, List<Job>> getAllJobs()
    {
        try
        {
            final Map<String, com.offbytwo.jenkins.model.Job> jobs = jenkins.getJobs();
            return Result.success(jobs.values()
                                          .stream()
                                          .map(job -> new Job(job.getName(), job.getUrl(), JobStatus.DISABLED, versionControl))
                                          .collect(Collectors.toList()));
        }
        catch (final IOException e)
        {
            LOGGER.warn("Received an error trying to get jobs", e);
            return Result.error(500);
        }
    }

    @Override
    public Result<Integer, LatestBuildInformation> getLatestBuildInformation(final String jobName, final JobStatus previousJobStatus)
    {
        try
        {
            final JobWithDetails job = jenkins.getJob(jobName);
            if (job == null)
            {
                return Result.error(404);
            }
            else if (job.getLastBuild().equals(Build.BUILD_HAS_NEVER_RUN))
            {
                return Result.error(400);
            }
            else
            {
                final BuildWithDetails buildDetails = job.getLastBuild().details();
                final String revision = getRevision(buildDetails);
                final JobStatus jobStatus = !job.isBuildable() ? JobStatus.DISABLED : parse(buildDetails.getResult(), previousJobStatus);
                final double jobCompletionPercentage = (double)(clockService.currentTimeMillis() - buildDetails.getTimestamp()) / buildDetails.getEstimatedDuration() * 100;
                final BuildChangeSet changeSet = buildDetails.getChangeSet();
                final String[] comments;
                if (changeSet == null)
                {
                    comments = new String[0];
                }
                else
                {
                    final List<String> commentList = changeSet
                            .getItems()
                            .stream()
                            .map(BuildChangeSetItem::getComment)
                            .collect(Collectors.toList());
                    comments = new String[commentList.size()];
                    commentList.toArray(comments);
                }
                final TestResults testResults = getTestResults(buildDetails);
                return Result.success(new LatestBuildInformation(revision,
                                                                 jobStatus,
                                                                 buildDetails.getNumber(),
                                                                 buildDetails.getTimestamp(),
                                                                 jobCompletionPercentage,
                                                                 comments,
                                                                 buildDetails.isBuilding(),
                                                                 testResults,
                                                                 buildDetails.getDuration()));
            }
        }
        catch (final IOException e)
        {
            return Result.error(500);
        }
    }

    @SuppressWarnings("unchecked")
    private static String getRevision(final BuildWithDetails buildDetails)
    {
        for (final Object entries : buildDetails.getActions())
        {
            final Map<String, String> lastBuildRevision = ((Map<String, Map<String, String>>)entries).get("lastBuiltRevision");
            if (lastBuildRevision != null)
            {
                return lastBuildRevision.get("SHA1");
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private static TestResults getTestResults(final BuildWithDetails buildDetails)
    {
        for (final Object entries : buildDetails.getActions())
        {
            final Map<String, Object> maps = (Map<String, Object>)entries;
            if ("testReport".equals(maps.get("urlName")))
            {
                final int failCount = (int)maps.get("failCount");
                final int skipCount = (int)maps.get("skipCount");
                final int totalCount = (int)maps.get("totalCount");
                final int passCount = totalCount - failCount - skipCount;
                return new TestResults(passCount, failCount, skipCount);
            }
        }
        return null;
    }

    static JobStatus parse(final BuildResult result, final JobStatus previousStatus)
    {
        if (result == null)
        {
            return JobStatus.BUILDING;
        }

        final JobStatus output;

        switch (result)
        {
            case ABORTED:
            case FAILURE:
            case UNSTABLE:
                output = JobStatus.ERROR;
                break;
            case SUCCESS:
                output = JobStatus.SUCCESS;
                break;
            case NOT_BUILT:
                output = JobStatus.DISABLED;
                break;
            case BUILDING:
            case REBUILDING:
                output = JobStatus.BUILDING;
                break;
            default:
                output = previousStatus;
                break;
        }
        return output;
    }
}
