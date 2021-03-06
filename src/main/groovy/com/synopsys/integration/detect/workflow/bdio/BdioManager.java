/**
 * synopsys-detect
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
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
package com.synopsys.integration.detect.workflow.bdio;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.detect.DetectInfo;
import com.synopsys.integration.detect.configuration.DetectConfiguration;
import com.synopsys.integration.detect.exception.DetectUserFriendlyException;
import com.synopsys.integration.detect.workflow.codelocation.BdioCodeLocationCreator;
import com.synopsys.integration.detect.workflow.codelocation.BdioCodeLocationResult;
import com.synopsys.integration.detect.workflow.codelocation.CodeLocationNameManager;
import com.synopsys.integration.detect.workflow.codelocation.DetectCodeLocation;
import com.synopsys.integration.detect.workflow.event.Event;
import com.synopsys.integration.detect.workflow.event.EventSystem;
import com.synopsys.integration.detect.workflow.file.DirectoryManager;
import com.synopsys.integration.detect.workflow.status.DetectorStatus;
import com.synopsys.integration.detect.workflow.status.StatusType;
import com.synopsys.integration.bdio.SimpleBdioFactory;
import com.synopsys.integration.blackduck.codelocation.bdioupload.UploadTarget;
import com.synopsys.integration.util.IntegrationEscapeUtil;
import com.synopsys.integration.util.NameVersion;

public class BdioManager {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DetectInfo detectInfo;
    private final SimpleBdioFactory simpleBdioFactory;
    private final BdioCodeLocationCreator bdioCodeLocationCreator;
    private final DirectoryManager directoryManager;
    private final IntegrationEscapeUtil integrationEscapeUtil;
    private final CodeLocationNameManager codeLocationNameManager;
    private final DetectConfiguration detectConfiguration;
    private final EventSystem eventSystem;

    public BdioManager(final DetectInfo detectInfo, final SimpleBdioFactory simpleBdioFactory, final IntegrationEscapeUtil integrationEscapeUtil, final CodeLocationNameManager codeLocationNameManager,
            final DetectConfiguration detectConfiguration, final BdioCodeLocationCreator codeLocationManager, final DirectoryManager directoryManager, final EventSystem eventSystem) {
        this.detectInfo = detectInfo;
        this.simpleBdioFactory = simpleBdioFactory;
        this.integrationEscapeUtil = integrationEscapeUtil;
        this.codeLocationNameManager = codeLocationNameManager;
        this.detectConfiguration = detectConfiguration;
        this.bdioCodeLocationCreator = codeLocationManager;
        this.directoryManager = directoryManager;
        this.eventSystem = eventSystem;
    }

    public BdioResult createBdioFiles(String aggregateName, NameVersion projectNameVersion, List<DetectCodeLocation> codeLocations) throws DetectUserFriendlyException {
        DetectBdioWriter detectBdioWriter = new DetectBdioWriter(simpleBdioFactory, detectInfo);

        if (StringUtils.isBlank(aggregateName)) {
            logger.info("Creating BDIO code locations.");
            final BdioCodeLocationResult codeLocationResult = bdioCodeLocationCreator.createFromDetectCodeLocations(codeLocations, projectNameVersion);
            codeLocationResult.getFailedBomToolGroupTypes().forEach(it -> eventSystem.publishEvent(Event.StatusSummary, new DetectorStatus(it, StatusType.FAILURE)));

            logger.info("Creating BDIO files from code locations.");
            CodeLocationBdioCreator codeLocationBdioCreator = new CodeLocationBdioCreator(detectBdioWriter, simpleBdioFactory);
            final List<UploadTarget> uploadTargets = codeLocationBdioCreator.createBdioFiles(directoryManager.getBdioOutputDirectory(), codeLocationResult.getBdioCodeLocations(), projectNameVersion);

            return new BdioResult(uploadTargets);
        } else {
            logger.info("Creating aggregate BDIO file.");
            AggregateBdioCreator aggregateBdioCreator = new AggregateBdioCreator(simpleBdioFactory, integrationEscapeUtil, codeLocationNameManager, detectConfiguration, detectBdioWriter);
            final Optional<UploadTarget> uploadTarget = aggregateBdioCreator.createAggregateBdioFile(directoryManager.getSourceDirectory(), directoryManager.getBdioOutputDirectory(), codeLocations, projectNameVersion);
            if (uploadTarget.isPresent()) {
                return new BdioResult(Arrays.asList(uploadTarget.get()));
            } else {
                return new BdioResult(Collections.emptyList());
            }
        }
    }

}
