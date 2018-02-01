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

public class TestResults
{
    private final int passCount;
    private final int failCount;
    private final int skipCount;

    public TestResults(final int passCount, final int failCount, final int skipCount)
    {
        this.passCount = passCount;
        this.failCount = failCount;
        this.skipCount = skipCount;
    }

    public int getPassCount()
    {

        return passCount;
    }

    public int getFailCount()
    {
        return failCount;
    }

    public int getSkipCount()
    {
        return skipCount;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        final TestResults that = (TestResults)o;

        if (passCount != that.passCount)
        {
            return false;
        }
        if (failCount != that.failCount)
        {
            return false;
        }
        return skipCount == that.skipCount;

    }

    @Override
    public int hashCode()
    {
        int result = passCount;
        result = 31 * result + failCount;
        result = 31 * result + skipCount;
        return result;
    }
}
