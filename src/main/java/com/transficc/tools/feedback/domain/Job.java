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
package com.transficc.tools.feedback.domain;

import java.util.Arrays;

import com.transficc.tools.feedback.web.messaging.PublishableJob;

public class Job
{
    private static final String[] NO_COMMENTS = new String[0];
    private static final int GIT_HASH_LENGTH = 7;
    private final String name;
    private final String url;
    private final VersionControl versionControl;
    private String revision = "";
    private JobStatus jobStatus;
    private JobStatus jobStatusToDisplay;
    private int buildNumber = 0;
    private double jobCompletionPercentage;
    private String[] comments = new String[0];
    private boolean building;
    private TestResults jobsTestResults;
    private long timestamp;
    private boolean hasJustCompleted;

    public Job(final String name, final String url, final JobStatus jobStatus, final VersionControl versionControl)
    {
        this.name = name;
        this.url = url;
        this.jobStatus = jobStatus;
        this.versionControl = versionControl;
    }

    public boolean wasUpdated(final LatestBuildInformation buildInformation)
    {
        final String revision = buildInformation.getRevision();
        final JobStatus jobStatus = buildInformation.getJobStatus();
        final int buildNumber = buildInformation.getNumber();
        final long timestamp = buildInformation.getTimestamp();
        final double jobCompletionPercentage = buildInformation.getJobCompletionPercentage();
        final String[] comments = buildInformation.getComments();
        final boolean building = buildInformation.isBuilding();
        final TestResults testResults = buildInformation.getTestResults();
        final boolean needsToBeUpdated = isThereAnUpdate(revision, jobStatus, buildNumber, jobCompletionPercentage, building);

        if (needsToBeUpdated)
        {
            this.jobsTestResults = testResults;
            this.revision = "".equals(revision) ? this.revision : revision;
            this.jobStatus = jobStatus;
            this.jobStatusToDisplay = jobStatus == JobStatus.BUILDING ? jobStatusToDisplay : jobStatus;
            this.buildNumber = buildNumber;
            this.timestamp = timestamp;
            this.jobCompletionPercentage = jobCompletionPercentage;
            this.comments = comments;
            this.hasJustCompleted = this.building && !building;
            this.building = building;
        }
        return needsToBeUpdated;
    }

    public String getName()
    {
        return name;
    }

    public JobStatus getJobStatus()
    {
        return jobStatus;
    }

    public PublishableJob createPublishable(final int priority, final boolean shouldDisplayCommentsForJob)
    {
        final String revision = calculateRevision();
        final String[] comments = shouldDisplayCommentsForJob ? this.comments : NO_COMMENTS;
        return new PublishableJob(name, url, priority, revision, jobStatus, jobStatusToDisplay, buildNumber, timestamp, jobCompletionPercentage, comments, building, jobsTestResults);
    }

    private String calculateRevision()
    {
        final String calculatedRevision;
        switch (versionControl)
        {
            case GIT:
                calculatedRevision = revision.length() > GIT_HASH_LENGTH ? revision.substring(0, GIT_HASH_LENGTH) : revision;
                break;
            case SVN:
            default:
                calculatedRevision = revision;
        }
        return calculatedRevision;
    }

    private boolean isThereAnUpdate(final String revision, final JobStatus jobStatus, final int buildNumber, final double jobCompletionPercentage, final boolean building)
    {
        return !this.revision.equals(revision) || this.jobStatus != jobStatus || this.buildNumber != buildNumber ||
               (Double.compare(this.jobCompletionPercentage, jobCompletionPercentage) != 0 && !(this.jobCompletionPercentage > 100 && jobCompletionPercentage > 100)) || this.building != building;
    }

    @Override
    public String toString()
    {
        return "Job{" +
               "name='" + name + '\'' +
               ", url='" + url + '\'' +
               ", revision='" + revision + '\'' +
               ", jobStatus=" + jobStatus +
               ", buildNumber=" + buildNumber +
               ", timestamp=" + timestamp +
               ", jobCompletionPercentage=" + jobCompletionPercentage +
               ", comments=" + Arrays.toString(comments) +
               '}';
    }

    public boolean hasJustCompleted()
    {
        return hasJustCompleted;
    }
}
