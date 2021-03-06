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
package com.synopsys.integration.detect.detector.gradle;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.detect.configuration.DetectConfiguration;
import com.synopsys.integration.detect.configuration.DetectProperty;
import com.synopsys.integration.detect.configuration.PropertyAuthority;
import com.synopsys.integration.detect.detector.DetectorException;
import com.synopsys.integration.detect.exception.DetectUserFriendlyException;
import com.synopsys.integration.detect.workflow.ArtifactResolver;
import com.synopsys.integration.detect.workflow.ArtifactoryConstants;
import com.synopsys.integration.detect.workflow.file.AirGapManager;
import com.synopsys.integration.detect.workflow.file.DirectoryManager;
import com.synopsys.integration.exception.IntegrationException;

import freemarker.template.Configuration;

public class GradleInspectorManager {
    private static final String GRADLE_DIR_NAME = "gradle";
    private static final String GENERATED_GRADLE_SCRIPT_NAME = "init-detect.gradle";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DirectoryManager directoryManager;
    private final AirGapManager airGapManager;
    private final Configuration configuration;
    private final DetectConfiguration detectConfiguration;
    private final ArtifactResolver artifactResolver;

    private String generatedGradleScriptPath = null;
    private boolean hasResolvedInspector = false;

    public GradleInspectorManager(final DirectoryManager directoryManager, AirGapManager airGapManager, final Configuration configuration, final DetectConfiguration detectConfiguration,
        final ArtifactResolver artifactResolver) {
        this.directoryManager = directoryManager;
        this.airGapManager = airGapManager;
        this.configuration = configuration;
        this.detectConfiguration = detectConfiguration;
        this.artifactResolver = artifactResolver;
    }

    public String getGradleInspector() throws DetectorException {
        if (!hasResolvedInspector) {
            hasResolvedInspector = true;
            try {
                final File airGapPath = deriveGradleAirGapDir();
                final File generatedGradleScriptFile = directoryManager.getSharedFile(GRADLE_DIR_NAME, GENERATED_GRADLE_SCRIPT_NAME);
                GradleScriptCreator gradleScriptCreator = new GradleScriptCreator(detectConfiguration, configuration);
                if (airGapPath == null) {
                    Optional<String> version = findVersion();
                    if (version.isPresent()) {
                        logger.info("Resolved the gradle inspector version: " + version.get());
                        generatedGradleScriptPath = gradleScriptCreator.generateOnlineScript(generatedGradleScriptFile, version.get());
                    } else {
                        throw new DetectorException("Unable to find the gradle inspector version from artifactory.");
                    }
                } else {
                    generatedGradleScriptPath = gradleScriptCreator.generateAirGapScript(generatedGradleScriptFile, airGapPath.getCanonicalPath());
                }
            } catch (final Exception e) {
                throw new DetectorException(e);
            }
            if (generatedGradleScriptPath == null) {
                throw new DetectorException("Unable to initialize the gradle inspector.");
            } else {
                logger.trace("Derived generated gradle script path: " + generatedGradleScriptPath);
            }
        } else {
            logger.debug("Already attempted to resolve the gradle inspector script, will not attempt again.");
        }
        if (StringUtils.isBlank(generatedGradleScriptPath)) {
            throw new DetectorException("Unable to find or create the gradle inspector script.");
        }
        return generatedGradleScriptPath;
    }

    private File deriveGradleAirGapDir() {
        String gradleInspectorAirGapDirectoryPath = airGapManager.getGradleInspectorAirGapPath();

        File gradleInspectorAirGapDirectory = null;
        if (StringUtils.isNotBlank(gradleInspectorAirGapDirectoryPath)) {
            gradleInspectorAirGapDirectory = new File(gradleInspectorAirGapDirectoryPath);
            if (!gradleInspectorAirGapDirectory.exists()) {
                gradleInspectorAirGapDirectory = null;
            }
        }
        logger.trace(String.format("gradleInspectorAirGapDirectory: %s", gradleInspectorAirGapDirectory));
        return gradleInspectorAirGapDirectory;
    }

    private Optional<String> findVersion() throws IntegrationException, DetectUserFriendlyException, IOException {
        String gradleVersion = detectConfiguration.getProperty(DetectProperty.DETECT_GRADLE_INSPECTOR_VERSION, PropertyAuthority.None);
        return artifactResolver.resolveArtifactVersion(ArtifactoryConstants.ARTIFACTORY_URL, ArtifactoryConstants.GRADLE_INSPECTOR_REPO, ArtifactoryConstants.GRADLE_INSPECTOR_PROPERTY, gradleVersion);
    }

}
