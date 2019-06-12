/**
 * synopsys-detect
 *
 * Copyright (c) 2019 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.detect.workflow;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.detect.exception.DetectUserFriendlyException;
import com.synopsys.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.synopsys.integration.detect.workflow.blackduck.BlackDuckPostActions;
import com.synopsys.integration.detect.workflow.blackduck.BlackDuckPostOptions;
import com.synopsys.integration.detect.workflow.blackduck.CodeLocationWaitData;
import com.synopsys.integration.detect.workflow.event.Event;
import com.synopsys.integration.detect.workflow.event.EventSystem;
import com.synopsys.integration.detect.workflow.report.util.ReportConstants;
import com.synopsys.integration.detect.workflow.status.BlackDuckBomDetectResult;
import com.synopsys.integration.detect.workflow.status.DetectResult;

public class DetectPostActions {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ProjectVersionWrapper projectVersionWrapper;
    private BlackDuckRunData blackDuckRunData;
    private BlackDuckPostActions blackDuckPostActions;
    private BlackDuckPostOptions blackDuckPostOptions;
    private CodeLocationWaitData codeLocationWaitData;
    private long timeoutInSeconds;
    private boolean hasAtLeastOneBdio;
    private boolean shouldHaveScanned;

    private final EventSystem eventSystem;

    public DetectPostActions(final EventSystem eventSystem) {
        this.eventSystem = eventSystem;
    }

    public void runPostActions() throws DetectUserFriendlyException {
        runPostBlackDuckActions();
    }

    private void runPostBlackDuckActions() throws DetectUserFriendlyException {
        logger.info(ReportConstants.RUN_SEPARATOR);
        if (null == projectVersionWrapper || null == blackDuckRunData || null == blackDuckPostActions || null == blackDuckPostOptions || null == codeLocationWaitData || 0 >= timeoutInSeconds) {
            logger.debug("Will not perform Black Duck post actions: Detect is not online.");
            return;
        }

        if (blackDuckRunData.isOnline() && blackDuckRunData.getBlackDuckServicesFactory().isPresent()) {
            logger.info("Will perform Black Duck post actions.");
            blackDuckPostActions.perform(blackDuckPostOptions, codeLocationWaitData, projectVersionWrapper, timeoutInSeconds);

            if (hasAtLeastOneBdio || shouldHaveScanned) {
                final Optional<String> componentsLink = projectVersionWrapper.getProjectVersionView().getFirstLink(ProjectVersionView.COMPONENTS_LINK);
                if (componentsLink.isPresent()) {
                    final DetectResult detectResult = new BlackDuckBomDetectResult(componentsLink.get());
                    eventSystem.publishEvent(Event.ResultProduced, detectResult);
                }
            }
            logger.info("Black Duck actions have finished.");
        }
    }

    public ProjectVersionWrapper getProjectVersionWrapper() {
        return projectVersionWrapper;
    }

    public void setProjectVersionWrapper(final ProjectVersionWrapper projectVersionWrapper) {
        this.projectVersionWrapper = projectVersionWrapper;
    }

    public BlackDuckRunData getBlackDuckRunData() {
        return blackDuckRunData;
    }

    public void setBlackDuckRunData(final BlackDuckRunData blackDuckRunData) {
        this.blackDuckRunData = blackDuckRunData;
    }

    public BlackDuckPostActions getBlackDuckPostActions() {
        return blackDuckPostActions;
    }

    public void setBlackDuckPostActions(final BlackDuckPostActions blackDuckPostActions) {
        this.blackDuckPostActions = blackDuckPostActions;
    }

    public BlackDuckPostOptions getBlackDuckPostOptions() {
        return blackDuckPostOptions;
    }

    public void setBlackDuckPostOptions(final BlackDuckPostOptions blackDuckPostOptions) {
        this.blackDuckPostOptions = blackDuckPostOptions;
    }

    public CodeLocationWaitData getCodeLocationWaitData() {
        return codeLocationWaitData;
    }

    public void setCodeLocationWaitData(final CodeLocationWaitData codeLocationWaitData) {
        this.codeLocationWaitData = codeLocationWaitData;
    }

    public long getTimeoutInSeconds() {
        return timeoutInSeconds;
    }

    public void setTimeoutInSeconds(final long timeoutInSeconds) {
        this.timeoutInSeconds = timeoutInSeconds;
    }

    public boolean isHasAtLeastOneBdio() {
        return hasAtLeastOneBdio;
    }

    public void setHasAtLeastOneBdio(final boolean hasAtLeastOneBdio) {
        this.hasAtLeastOneBdio = hasAtLeastOneBdio;
    }

    public boolean isShouldHaveScanned() {
        return shouldHaveScanned;
    }

    public void setShouldHaveScanned(final boolean shouldHaveScanned) {
        this.shouldHaveScanned = shouldHaveScanned;
    }

}