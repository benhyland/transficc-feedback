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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildChangeSet;
import com.offbytwo.jenkins.model.BuildResult;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.JobWithDetails;
import com.transficc.functionality.Result;
import com.transficc.tools.feedback.MessageBuilder;
import com.transficc.tools.feedback.domain.JobStatus;
import com.transficc.tools.feedback.domain.LatestBuildInformation;
import com.transficc.tools.feedback.domain.TestResults;
import com.transficc.tools.feedback.domain.VersionControl;
import com.transficc.tools.feedback.util.ClockService;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

public class JenkinsFacadeTest
{
    private static final String JOB_NAME = "something going on";
    private static final long TIMESTAMP = 20;
    private final JenkinsServer jenkinsServer = Mockito.mock(JenkinsServer.class);
    private final JobWithDetails jobWithDetails = Mockito.mock(JobWithDetails.class);
    private final Build lastBuild = Mockito.mock(Build.class);
    private final JenkinsFacade jenkinsFacade = new JenkinsFacade(jenkinsServer, "master job", new ClockService()
    {
        @Override
        public long currentTimeMillis()
        {
            return TIMESTAMP;
        }
    }, VersionControl.GIT);

    @Test
    public void shouldReturnWith400IfJobHasNeverBeenBuilt() throws IOException
    {
        //Given
        given(jenkinsServer.getJob(JOB_NAME)).willReturn(jobWithDetails);
        given(jobWithDetails.getLastBuild()).willReturn(Build.BUILD_HAS_NEVER_RUN);

        //When
        final Result<Integer, LatestBuildInformation> response = jenkinsFacade.getLatestBuildInformation(JOB_NAME, JobStatus.SUCCESS);

        //Then
        response.consume(error -> assertThat(error, is(400)),
                         build -> Assert.fail("This should not happen"));
    }

    @Test
    public void shouldReturnWith404IfJobCannotBeFound() throws IOException
    {
        //Given
        given(jenkinsServer.getJob(JOB_NAME)).willReturn(null);

        //When
        final Result<Integer, LatestBuildInformation> response = jenkinsFacade.getLatestBuildInformation(JOB_NAME, JobStatus.SUCCESS);

        //Then
        response.consume(error -> assertThat(error, is(404)),
                         build -> Assert.fail("This should not happen"));
    }

    @Test
    public void shouldReturnLatestBuildInformation() throws IOException
    {
        //Given
        final String revision = "34534509abfd";
        final long timestamp = 5;
        setupJobExpectations(revision, true, Optional.empty(), timestamp, 20, 1);

        //When
        final Result<Integer, LatestBuildInformation> response = jenkinsFacade.getLatestBuildInformation(JOB_NAME, JobStatus.SUCCESS);

        //Then
        response.consume(error -> Assert.fail("Should not have happened. Received: " + error),
                         information ->
                         {
                             assertThat(information.getRevision(), is(revision));
                             assertThat(information.getTimestamp(), is(timestamp));
                             assertThat(information.isBuilding(), is(true));
                             assertThat(information.getJobStatus(), is(JobStatus.SUCCESS));
                             assertThat(information.getJobCompletionPercentage(), is(75.0));
                             assertThat(information.getDuration(), is(1L));
                             assertNull(information.getTestResults());
                         });
    }

    @Test
    public void shouldReturnTestResultIfAvailable() throws IOException
    {
        //Given
        final String revision = "34534509abfd";
        final long timestamp = 5;
        setupJobExpectations(revision, true, Optional.of(new Tests(1, 2, 6)), timestamp, 20, 1);

        //When
        final Result<Integer, LatestBuildInformation> response = jenkinsFacade.getLatestBuildInformation(JOB_NAME, JobStatus.SUCCESS);

        //Then
        response.consume(error -> Assert.fail("Should not have happened. Received: " + error),
                         information ->
                         {
                             final TestResults testResults = information.getTestResults();
                             assertThat(information.getRevision(), is(revision));
                             assertThat(information.getTimestamp(), is(timestamp));
                             assertThat(information.isBuilding(), is(true));
                             assertThat(information.getJobStatus(), is(JobStatus.SUCCESS));
                             assertThat(information.getJobCompletionPercentage(), is(75.0));
                             assertThat(information.getDuration(), is(1L));
                             assertThat(testResults.getFailCount(), is(1));
                             assertThat(testResults.getSkipCount(), is(2));
                             assertThat(testResults.getPassCount(), is(3));
                         });
    }

    private void setupJobExpectations(final String revision,
                                      final boolean isBuilding,
                                      final Optional<Tests> testReport,
                                      final long timestamp,
                                      final int estimatedDuration,
                                      final int duration) throws IOException
    {
        final Map<Object, Object> revisionActions = new HashMap<>();
        final Map<Object, Object> revisions = new HashMap<>();
        revisions.put("SHA1", revision);
        revisionActions.put("lastBuiltRevision", revisions);
        final Map<Object, Object> testResults = new HashMap<>();
        testReport.ifPresent(test ->
                             {
                                 testResults.put("failCount", test.failCount);
                                 testResults.put("skipCount", test.skipCount);
                                 testResults.put("totalCount", test.totalCount);
                                 testResults.put("urlName", "testReport");
                             });
        final List<Map<Object, Object>> actions = Arrays.asList(revisionActions, testResults);
        final BuildChangeSet buildChangeSet = new BuildChangeSet();
        buildChangeSet.setItems(Collections.emptyList());

        given(jenkinsServer.getJob(JOB_NAME)).willReturn(jobWithDetails);
        given(jobWithDetails.getLastBuild()).willReturn(lastBuild);
        given(jobWithDetails.isBuildable()).willReturn(true);
        given(lastBuild.details()).willReturn(new MessageBuilder<>(BuildWithDetails.class)
                                                      .setField("actions", actions)
                                                      .setField("building", isBuilding)
                                                      .setField("changeSet", buildChangeSet)
                                                      .setField("result", BuildResult.SUCCESS)
                                                      .setField("timestamp", timestamp)
                                                      .setField("duration", duration)
                                                      .setField("estimatedDuration", estimatedDuration)
                                                      .build());
    }

    private static final class Tests
    {
        private final int failCount;
        private final int skipCount;
        private final int totalCount;

        private Tests(final int failCount, final int skipCount, final int totalCount)
        {
            this.failCount = failCount;
            this.skipCount = skipCount;
            this.totalCount = totalCount;
        }
    }
}