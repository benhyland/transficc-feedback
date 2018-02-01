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

public final class LatestBuildInformation
{
    private final String revision;
    private final JobStatus jobStatus;
    private final int number;
    private final double jobCompletionPercentage;
    private final String[] comments;
    private final boolean building;
    private final TestResults testResults;
    private final long duration;
    private final long timestamp;

    public LatestBuildInformation(final String revision,
                                  final JobStatus jobStatus,
                                  final int number,
                                  final long timestamp,
                                  final double jobCompletionPercentage,
                                  final String[] comments,
                                  final boolean building,
                                  final TestResults testResults,
                                  final long duration)
    {
        this.revision = revision;
        this.jobStatus = jobStatus;
        this.number = number;
        this.timestamp = timestamp;
        this.jobCompletionPercentage = jobCompletionPercentage;
        this.comments = comments;
        this.building = building;
        this.testResults = testResults;
        this.duration = duration;
    }

    public String getRevision()
    {
        return revision;
    }

    public JobStatus getJobStatus()
    {
        return jobStatus;
    }

    public int getNumber()
    {
        return number;
    }

    public double getJobCompletionPercentage()
    {
        return jobCompletionPercentage;
    }

    public String[] getComments()
    {
        return comments;
    }

    public boolean isBuilding()
    {
        return building;
    }

    public TestResults getTestResults()
    {
        return testResults;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public long getDuration()
    {
        return duration;
    }
}
