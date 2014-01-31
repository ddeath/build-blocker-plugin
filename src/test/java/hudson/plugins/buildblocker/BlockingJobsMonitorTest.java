/*
 * The MIT License
 *
 * Copyright (c) 2004-2011, Sun Microsystems, Inc., Frederik Fromm
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.buildblocker;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.AbstractProject;
import hudson.model.labels.LabelAtom;
import hudson.slaves.DumbSlave;
import hudson.slaves.SlaveComputer;
import hudson.tasks.Shell;
import jenkins.model.Jenkins;
import org.jvnet.hudson.test.HudsonTestCase;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests
 */
public class BlockingJobsMonitorTest extends HudsonTestCase {

    /**
     * One test for all for faster execution
     * @throws Exception
     */
    public void testConstructor() throws Exception {
        // clear queue from preceding tests
        Jenkins.getInstance().getQueue().clear();

        // init slave
        LabelAtom label = new LabelAtom("label");
        DumbSlave slave = this.createSlave(label);
        SlaveComputer c = slave.getComputer();
        c.connect(false).get(); // wait until it's connected
        if(c.isOffline()) {
            fail("Slave failed to go online: "+c.getLog());
        }

        String blockingJobName = "blockingJob";
        String queueBlockingJobName = "queueBlockingJob";

        FreeStyleProject blockingProjectInQueue = this.createFreeStyleProject(queueBlockingJobName);

        FreeStyleProject blockingProject = this.createFreeStyleProject(blockingJobName);

        blockingProjectInQueue.getBuildTriggerUpstreamProjects().add((AbstractProject)blockingProject);

        blockingProject.setAssignedLabel(label);
        blockingProjectInQueue.setAssignedLabel(label);
        blockingProjectInQueue.setBlockBuildWhenUpstreamBuilding(true);


        Shell shell = new Shell("sleep 5");
        blockingProject.getBuildersList().add(shell);
        blockingProjectInQueue.getBuildersList().add(shell);

        Future<FreeStyleBuild> future = blockingProject.scheduleBuild2(0);

        // wait until blocking job started
        while(! slave.getComputer().getExecutors().get(0).isBusy()) {
            TimeUnit.SECONDS.sleep(1);
        }

        Future<FreeStyleBuild> futureInQueue = blockingProjectInQueue.scheduleBuild2(0);

        //tests with useBuildBlockerForExecutors checkType
        BlockingJobsMonitor blockingJobsMonitorUsingNull = new BlockingJobsMonitor(null, false);
        assertNull(blockingJobsMonitorUsingNull.getBlockingJob(null));

        BlockingJobsMonitor blockingJobsMonitorNotMatching = new BlockingJobsMonitor("xxx", false);
        assertNull(blockingJobsMonitorNotMatching.getBlockingJob(null));

        BlockingJobsMonitor blockingJobsMonitorUsingFullName = new BlockingJobsMonitor(blockingJobName, false);
        assertEquals(blockingJobName, blockingJobsMonitorUsingFullName.getBlockingJob(null).getDisplayName());

        BlockingJobsMonitor blockingJobsMonitorUsingRegex = new BlockingJobsMonitor("block.*", false);
        assertEquals(blockingJobName, blockingJobsMonitorUsingRegex.getBlockingJob(null).getDisplayName());

        BlockingJobsMonitor blockingJobsMonitorUsingMoreLines = new BlockingJobsMonitor("xxx\nblock.*\nyyy", false);
        assertEquals(blockingJobName, blockingJobsMonitorUsingMoreLines.getBlockingJob(null).getDisplayName());

        BlockingJobsMonitor blockingJobsMonitorUsingRegexMatchingQueue = new BlockingJobsMonitor("queue.*", false);
        assertNull(blockingJobsMonitorUsingRegexMatchingQueue.getBlockingJob(null));

        BlockingJobsMonitor blockingJobsMonitorUsingFullNameMatchingQueue = new BlockingJobsMonitor(queueBlockingJobName, false);
        assertNull(blockingJobsMonitorUsingFullNameMatchingQueue.getBlockingJob(null));


        //tests with useBuildBlockerForWholeQueue checkType
        BlockingJobsMonitor blockingJobsMonitorUsingNullOnWholeQueue = new BlockingJobsMonitor(null, true);
        assertNull(blockingJobsMonitorUsingNullOnWholeQueue.getBlockingJob(null));

        BlockingJobsMonitor blockingJobsMonitorNotMatchingOnWholeQueue = new BlockingJobsMonitor("xxx", true);
        assertNull(blockingJobsMonitorNotMatchingOnWholeQueue.getBlockingJob(null));

        BlockingJobsMonitor blockingJobsMonitorUsingFullNameOnWholeQueue = new BlockingJobsMonitor(blockingJobName, true);
        assertEquals(blockingJobName, blockingJobsMonitorUsingFullNameOnWholeQueue.getBlockingJob(null).getDisplayName());

        BlockingJobsMonitor blockingJobsMonitorUsingRegexOnWholeQueue = new BlockingJobsMonitor("block.*", true);
        assertEquals(blockingJobName, blockingJobsMonitorUsingRegexOnWholeQueue.getBlockingJob(null).getDisplayName());

        BlockingJobsMonitor blockingJobsMonitorUsingMoreLinesOnWholeQueue = new BlockingJobsMonitor("xxx\nblock.*\nyyy", true);
        assertEquals(blockingJobName, blockingJobsMonitorUsingMoreLinesOnWholeQueue.getBlockingJob(null).getDisplayName());

        BlockingJobsMonitor blockingJobsMonitorUsingRegexMatchingQueueOnWholeQueue = new BlockingJobsMonitor("queue.*", true);
        assertEquals(queueBlockingJobName, blockingJobsMonitorUsingRegexMatchingQueueOnWholeQueue.getBlockingJob(null).getDisplayName());

        BlockingJobsMonitor blockingJobsMonitorUsingFullNameMatchingQueueOnWholeQueue = new BlockingJobsMonitor(queueBlockingJobName, true);
        assertEquals(queueBlockingJobName, blockingJobsMonitorUsingFullNameMatchingQueueOnWholeQueue.getBlockingJob(null).getDisplayName());

        // wait until blocking job stopped
        while (! future.isDone() || ! futureInQueue.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }

        assertNull(blockingJobsMonitorUsingFullName.getBlockingJob(null));
    }
}
