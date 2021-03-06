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
package com.synopsys.integration.detect.tool.docker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.detect.util.executable.Executable;
import com.synopsys.integration.detect.util.executable.ExecutableRunner;
import com.synopsys.integration.detect.util.executable.ExecutableRunnerException;
import com.synopsys.integration.detect.workflow.codelocation.DetectCodeLocation;
import com.synopsys.integration.detect.workflow.codelocation.DetectCodeLocationType;
import com.synopsys.integration.detect.workflow.extraction.Extraction;
import com.synopsys.integration.detect.workflow.file.DetectFileFinder;
import com.google.gson.Gson;
import com.synopsys.integration.bdio.BdioReader;
import com.synopsys.integration.bdio.BdioTransformer;
import com.synopsys.integration.bdio.graph.DependencyGraph;
import com.synopsys.integration.bdio.model.BdioId;
import com.synopsys.integration.bdio.model.Forge;
import com.synopsys.integration.bdio.model.SimpleBdioDocument;
import com.synopsys.integration.bdio.model.externalid.ExternalId;
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;

public class DockerExtractor {
    public static final String DOCKER_TAR_META_DATA_KEY = "dockerTar";

    public static final String TAR_FILENAME_PATTERN = "*.tar.gz";
    public static final String DEPENDENCIES_PATTERN = "*bdio.jsonld";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DetectFileFinder detectFileFinder;
    private final DockerProperties dockerProperties;
    private final ExecutableRunner executableRunner;
    private final BdioTransformer bdioTransformer;
    private final ExternalIdFactory externalIdFactory;
    private final Gson gson;

    public DockerExtractor(final DetectFileFinder detectFileFinder, final DockerProperties dockerProperties,
        final ExecutableRunner executableRunner, final BdioTransformer bdioTransformer, final ExternalIdFactory externalIdFactory, final Gson gson) {
        this.detectFileFinder = detectFileFinder;
        this.dockerProperties = dockerProperties;
        this.executableRunner = executableRunner;
        this.bdioTransformer = bdioTransformer;
        this.externalIdFactory = externalIdFactory;
        this.gson = gson;
    }

    public Extraction extract(final File directory, final File outputDirectory, final File bashExe, final File javaExe, final String image, final String tar,
        final DockerInspectorInfo dockerInspectorInfo) {
        try {
            String imageArgument = null;
            String imagePiece = null;
            if (StringUtils.isNotBlank(tar)) {
                final File dockerTarFile = new File(tar);
                imageArgument = String.format("--docker.tar=%s", dockerTarFile.getCanonicalPath());
                imagePiece = detectFileFinder.extractFinalPieceFromPath(dockerTarFile.getCanonicalPath());
            } else if (StringUtils.isNotBlank(image)) {
                imagePiece = image;
                imageArgument = String.format("--docker.image=%s", image);
            }

            if (StringUtils.isBlank(imageArgument) || StringUtils.isBlank(imagePiece)) {
                return new Extraction.Builder().failure("No docker image found.").build();
            } else {
                return executeDocker(outputDirectory, imageArgument, imagePiece, tar, directory, javaExe, bashExe, dockerInspectorInfo);
            }
        } catch (final Exception e) {
            return new Extraction.Builder().exception(e).build();
        }
    }

    private void importTars(final File inspectorJar, final List<File> importTars, final File directory, final Map<String, String> environmentVariables, final File bashExe) {
        try {
            for (final File imageToImport : importTars) {
                // The -c is a bash option, the following String is the command we want to run
                final List<String> dockerImportArguments = Arrays.asList(
                    "-c",
                    "docker load -i \"" + imageToImport.getCanonicalPath() + "\"");

                final Executable dockerImportImageExecutable = new Executable(directory, environmentVariables, bashExe.toString(), dockerImportArguments);
                executableRunner.execute(dockerImportImageExecutable);
            }
        } catch (final Exception e) {
            logger.debug("Exception encountered when resolving paths for docker air gap, running in online mode instead");
            logger.debug(e.getMessage());
        }
    }

    private Extraction executeDocker(File outputDirectory, final String imageArgument, final String imagePiece, final String dockerTarFilePath, final File directory, final File javaExe,
        final File bashExe,
        final DockerInspectorInfo dockerInspectorInfo)
        throws IOException, ExecutableRunnerException {

        final File dockerPropertiesFile = new File(outputDirectory, "application.properties");
        dockerProperties.populatePropertiesFile(dockerPropertiesFile, outputDirectory);
        final Map<String, String> environmentVariables = new HashMap<>(0);
        final List<String> dockerArguments = new ArrayList<>();
        dockerArguments.add("-jar");
        dockerArguments.add(dockerInspectorInfo.getDockerInspectorJar().getAbsolutePath());
        dockerArguments.add("--spring.config.location");
        dockerArguments.add("file:" + dockerPropertiesFile.getCanonicalPath());
        dockerArguments.add(imageArgument);
        if (dockerInspectorInfo.hasAirGapImageFiles()) {
            importTars(dockerInspectorInfo.getDockerInspectorJar(), dockerInspectorInfo.getAirGapInspectorImageTarfiles(), outputDirectory, environmentVariables, bashExe);
        }
        final Executable dockerExecutable = new Executable(outputDirectory, environmentVariables, javaExe.getAbsolutePath(), dockerArguments);
        executableRunner.execute(dockerExecutable);

        final File producedTarFile = detectFileFinder.findFile(outputDirectory, TAR_FILENAME_PATTERN);
        File scanFile = null;
        if (null != producedTarFile && producedTarFile.isFile()) {
            scanFile = producedTarFile;
        } else {
            logger.debug(String.format("No files found matching pattern [%s]. Expected docker-inspector to produce file in %s", TAR_FILENAME_PATTERN, outputDirectory.getCanonicalPath()));
            if (StringUtils.isNotBlank(dockerTarFilePath)) {
                final File dockerTarFile = new File(dockerTarFilePath);
                if (dockerTarFile.isFile()) {
                    logger.debug(String.format("Will scan the provided Docker tar file %s", dockerTarFile.getCanonicalPath()));
                    scanFile = dockerTarFile;
                }
            }
        }

        Extraction.Builder extractionBuilder = findCodeLocations(outputDirectory, directory, imagePiece);
        extractionBuilder.metaData(DOCKER_TAR_META_DATA_KEY, scanFile);
        return extractionBuilder.build();
    }

    private Extraction.Builder findCodeLocations(final File directoryToSearch, final File directory, final String imageName) {
        final File bdioFile = detectFileFinder.findFile(directoryToSearch, DEPENDENCIES_PATTERN);
        if (bdioFile != null) {
            SimpleBdioDocument simpleBdioDocument = null;

            try (final InputStream dockerOutputInputStream = new FileInputStream(bdioFile); BdioReader bdioReader = new BdioReader(gson, dockerOutputInputStream)) {
                simpleBdioDocument = bdioReader.readSimpleBdioDocument();
            } catch (final Exception e) {
                return new Extraction.Builder().exception(e);
            }

            if (simpleBdioDocument != null) {
                final DependencyGraph dependencyGraph = bdioTransformer.transformToDependencyGraph(simpleBdioDocument.project, simpleBdioDocument.components);

                final String projectName = simpleBdioDocument.project.name;
                final String projectVersionName = simpleBdioDocument.project.version;

                // TODO ejk - update this when project external id is not req'd anymore
                final Forge dockerForge = new Forge(BdioId.BDIO_ID_SEPARATOR, BdioId.BDIO_ID_SEPARATOR, simpleBdioDocument.project.bdioExternalIdentifier.forge);
                final String externalIdPath = simpleBdioDocument.project.bdioExternalIdentifier.externalId;
                final ExternalId projectExternalId = externalIdFactory.createPathExternalId(dockerForge, externalIdPath);

                final DetectCodeLocation detectCodeLocation = new DetectCodeLocation.Builder(DetectCodeLocationType.DOCKER, directory.toString(), projectExternalId, dependencyGraph).dockerImage(imageName).build();

                return new Extraction.Builder().success(detectCodeLocation).projectName(projectName).projectVersion(projectVersionName);
            }
        }

        return new Extraction.Builder().failure("No files found matching pattern [" + DEPENDENCIES_PATTERN + "]. Expected docker-inspector to produce file in " + directory.toString());
    }

}
