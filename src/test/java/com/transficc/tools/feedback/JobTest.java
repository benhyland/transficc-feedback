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
package com.transficc.tools.feedback;

import com.transficc.tools.feedback.domain.Job;
import com.transficc.tools.feedback.domain.JobStatus;
import com.transficc.tools.feedback.domain.LatestBuildInformation;
import com.transficc.tools.feedback.domain.VersionControl;
import com.transficc.tools.feedback.web.messaging.PublishableJob;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class JobTest
{
    private final Job job = new Job("tom", "url", JobStatus.SUCCESS, false, VersionControl.GIT);

    @Before
    public void setup()
    {
        Assert.assertTrue(job.wasUpdated(new LatestBuildInformation("revision1", JobStatus.SUCCESS, 1, 1468934838586L, 110, new String[0], false, null, 0)));
    }

    @Test
    public void shouldNotPublishIfJobHasAlreadyCompleted()
    {
        Assert.assertFalse(job.wasUpdated(new LatestBuildInformation("revision1", JobStatus.SUCCESS, 1, 1468934838586L, 110, new String[0], false, null, 0)));
    }

    @Test
    public void shouldPublishIfANewRunHasStarted()
    {
        Assert.assertTrue(job.wasUpdated(new LatestBuildInformation("revision2", JobStatus.SUCCESS, 2, 1468934838586L, 0, new String[0], false, null, 0)));
    }

    @Test
    public void shouldPublishIfAnUpdateForARunIsReceived()
    {
        Assert.assertTrue(job.wasUpdated(new LatestBuildInformation("revision2", JobStatus.SUCCESS, 2, 1468934838586L, 10, new String[0], false, null, 0)));
        Assert.assertTrue(job.wasUpdated(new LatestBuildInformation("revision2", JobStatus.SUCCESS, 2, 1468934838586L, 20, new String[0], false, null, 0)));
    }

    @Test
    public void shouldPublishIfJobCompletes()
    {
        Assert.assertTrue(job.wasUpdated(new LatestBuildInformation("revision2", JobStatus.SUCCESS, 2, 1468934838586L, 10, new String[0], false, null, 0)));
        Assert.assertTrue(job.wasUpdated(new LatestBuildInformation("revision2", JobStatus.SUCCESS, 2, 1468934838586L, 110, new String[0], false, null, 0)));
    }

    @Test
    public void shouldTruncateGitHashes()
    {
        Assert.assertTrue(job.wasUpdated(new LatestBuildInformation("revision21", JobStatus.SUCCESS, 2, 1468934838586L, 0, new String[0], false, null, 0)));

        final PublishableJob publishable = job.createPublishable(1);

        assertThat(publishable.getRevision(), is("revisio"));
    }

    @Test
    public void shouldNotTruncateRevisionIfVersionControlIsSvn()
    {
        final Job job = new Job("tom", "url", JobStatus.SUCCESS, false, VersionControl.SVN);
        Assert.assertTrue(job.wasUpdated(new LatestBuildInformation("revision21", JobStatus.SUCCESS, 2, 1468934838586L, 0, new String[0], false, null, 0)));

        final PublishableJob publishable = job.createPublishable(1);

        assertThat(publishable.getRevision(), is("revision21"));
    }

    @Test
    public void shouldShouldReturnTrueIfJobWasPreviouslyBuilding()
    {
        Assert.assertTrue(job.wasUpdated(new LatestBuildInformation("revision2", JobStatus.SUCCESS, 2, 1468934838586L, 10, new String[0], true, null, 0)));
        Assert.assertTrue(job.wasUpdated(new LatestBuildInformation("revision2", JobStatus.SUCCESS, 2, 1468934838586L, 10, new String[0], false, null, 0)));

        final boolean isComplete = job.hasJustCompleted();

        assertThat(isComplete, is(true));
    }

    @Test
    public void shouldReturnFalseIfJobIsStillBuilding()
    {
        Assert.assertTrue(job.wasUpdated(new LatestBuildInformation("revision2", JobStatus.SUCCESS, 2, 1468934838586L, 10, new String[0], true, null, 0)));
        Assert.assertTrue(job.wasUpdated(new LatestBuildInformation("revision2", JobStatus.SUCCESS, 2, 1468934838586L, 30, new String[0], true, null, 0)));

        final boolean isComplete = job.hasJustCompleted();

        assertThat(isComplete, is(false));
    }

    @Test
    public void shouldReturnFalseIfTheJobHasJustStartedToBeBuilt()
    {
        Assert.assertTrue(job.wasUpdated(new LatestBuildInformation("revision2", JobStatus.SUCCESS, 2, 1468934838586L, 10, new String[0], false, null, 0)));
        Assert.assertTrue(job.wasUpdated(new LatestBuildInformation("revision2", JobStatus.SUCCESS, 2, 1468934838586L, 10, new String[0], true, null, 0)));

        final boolean isComplete = job.hasJustCompleted();

        assertThat(isComplete, is(false));
    }
}
