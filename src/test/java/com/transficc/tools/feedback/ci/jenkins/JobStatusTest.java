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

import com.offbytwo.jenkins.model.BuildResult;
import com.transficc.tools.feedback.domain.JobStatus;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class JobStatusTest
{
    @Test
    public void shouldReturnBuildingIfNoBuildResultForJob()
    {
        assertThat(JenkinsFacade.parse(null, null), is(JobStatus.BUILDING));
    }

    @Test
    public void shouldReturnErrorIfJobWasAborted()
    {
        assertThat(JenkinsFacade.parse(BuildResult.ABORTED, null), is(JobStatus.ERROR));
    }

    @Test
    public void shouldReturnErrorIfJobFailed()
    {
        assertThat(JenkinsFacade.parse(BuildResult.FAILURE, null), is(JobStatus.ERROR));
    }

    @Test
    public void shouldReturnErrorIfJobUnstable()
    {
        assertThat(JenkinsFacade.parse(BuildResult.UNSTABLE, null), is(JobStatus.ERROR));
    }

    @Test
    public void shouldReturnSuccessIfJobSuccessful()
    {
        assertThat(JenkinsFacade.parse(BuildResult.SUCCESS, null), is(JobStatus.SUCCESS));
    }

    @Test
    public void shouldReturnDisabledIfJobNotBuilt()
    {
        assertThat(JenkinsFacade.parse(BuildResult.NOT_BUILT, null), is(JobStatus.DISABLED));
    }

    @Test
    public void shouldReturnBuildingIfJobCurrentlyBeingBuilt()
    {
        assertThat(JenkinsFacade.parse(BuildResult.BUILDING, null), is(JobStatus.BUILDING));
    }

    @Test
    public void shouldReturnBuildingIfJobCurrentlyBeingReBuilt()
    {
        assertThat(JenkinsFacade.parse(BuildResult.REBUILDING, null), is(JobStatus.BUILDING));
    }

    @Test
    public void shouldDefaultToPreviousStatusForAllOtherCases()
    {
        assertNull(JenkinsFacade.parse(BuildResult.CANCELLED, null));
        assertNull(JenkinsFacade.parse(BuildResult.UNKNOWN, null));
    }
}
