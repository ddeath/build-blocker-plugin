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

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Job property that stores the line feed separated list of
 * regular expressions that define the blocking jobs.
 */
public class BuildBlockerProperty extends JobProperty<Job<?, ?>> {
    /**
     * the logger
     */
    private static final Logger LOG = Logger.getLogger(BuildBlockerProperty.class.getName());

    /**
     * the enable checkbox in the job's config
     */
    public static final String USE_BUILD_BLOCKER = "useBuildBlocker";

    /**
     * blocking jobs form field name
     */
    public static final String BLOCKING_JOBS_KEY = "blockingJobs";

    /**
     * the name of dropdown element in the job's config
     */
    public static final String CHECK_TYPE_DROPDOWN = "checkType";

    /**
     * the option in the job's confing dropdown
     */
    public static final String USE_BUILD_BLOCKER_FOR_WHOLE_QUEUE = "useBuildBlockerForWholeQueue";

    /**
     * the option in the job's confing dropdown
     */
    public static final String USE_BUILD_BLOCKER_FOR_EXECUTORS = "useBuildBlockerForExecutors";

    /**
     * flag if build blocker should be used
     */
    private boolean useBuildBlocker;

    /**
     * the job names that block the build if running
     */
    private String blockingJobs;

    /**
     * information if we should check Executors or whole queue
     * USE_BUILD_BLOCKER_FOR_EXECUTORS
     * USE_BUILD_BLOCKER_FOR_WHOLE_QUEUE
     */
    private String checkType = USE_BUILD_BLOCKER_FOR_WHOLE_QUEUE;

    /**
     * Returns true if the build blocker is enabled.
     * @return true if the build blocker is enabled
     */
    @SuppressWarnings("unused")
    public boolean isUseBuildBlocker() {
        return useBuildBlocker;
    }

    /**
     * Sets the build blocker flag.
     * @param useBuildBlocker the build blocker flag
     */
    public void setUseBuildBlocker(boolean useBuildBlocker) {
        this.useBuildBlocker = useBuildBlocker;
    }

    /**
     * Returns the text of the blocking jobs field.
     * @return the text of the blocking jobs field
     */
    public String getBlockingJobs() {
        return blockingJobs;
    }

    /**
     * Sets the blocking jobs field
     * @param blockingJobs the blocking jobs entry
     */
    public void setBlockingJobs(String blockingJobs) {
        this.blockingJobs = blockingJobs;
    }

    /**
     * Sets the selected option of checkType flag.
     * @param checkType sets the selected option of checkType flag
     */
    public void setCheckType(String checkType) {
        this.checkType = checkType;
    }

    /**
     * Returns true if the build blocker is enabled for whole build queue.
     * @return true if the build blocker is enabled for whole build queue
     */
    @SuppressWarnings("unused")
    public boolean useBuildBlockerForWholeQueue() {
        return this.checkType.equals(USE_BUILD_BLOCKER_FOR_WHOLE_QUEUE);
    }

    /**
     * Descriptor
     */
    @SuppressWarnings("unused")
    @Extension
    public static final class BuildBlockerDescriptor extends JobPropertyDescriptor {

        /**
         * Constructor loading the data from the config file
         */
        public BuildBlockerDescriptor() {
            load();
        }

        /**
         * Returns the name to be shown on the website
         * @return the name to be shown on the website.
         */
        @Override
        public String getDisplayName() {
            return Messages.DisplayName();
        }

        /**
         * Returns a new instance of the build blocker property
         * when job config page is saved.
         * @param req stapler request
         * @param formData  the form data
         * @return a new instance of the build blocker property
         * @throws FormException
         */
        @Override
        public BuildBlockerProperty newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            BuildBlockerProperty buildBlockerProperty = new BuildBlockerProperty();

            if(formData.containsKey(USE_BUILD_BLOCKER)) {
                try {
                    buildBlockerProperty.setUseBuildBlocker(true);
                    buildBlockerProperty.setBlockingJobs(formData.getJSONObject(USE_BUILD_BLOCKER).getString(BLOCKING_JOBS_KEY));

                } catch(JSONException e) {
                    buildBlockerProperty.setUseBuildBlocker(false);
                    LOG.log(Level.WARNING, "could not get blocking jobs from " + formData.toString());
                }

                if(formData.getJSONObject(USE_BUILD_BLOCKER).containsKey(CHECK_TYPE_DROPDOWN)){
                    String value = formData.getJSONObject(USE_BUILD_BLOCKER).getString(CHECK_TYPE_DROPDOWN);

                    if (value.equals(USE_BUILD_BLOCKER_FOR_EXECUTORS)){
                        buildBlockerProperty.setCheckType(USE_BUILD_BLOCKER_FOR_EXECUTORS);
                    }else{
                        buildBlockerProperty.setCheckType(USE_BUILD_BLOCKER_FOR_WHOLE_QUEUE);
                    }
                }
            }

            return buildBlockerProperty;
        }

        /**
         * Returns always true a it can be used in all types of jobs.
         * @param jobType the job type to be checked if this property is applicable.
         * @return true
         */
        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return true;
        }
    }

}
