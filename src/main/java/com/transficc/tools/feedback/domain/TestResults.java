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
