/**
 * detect-configuration
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
package com.synopsys.integration.detect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.detect.type.OperatingSystemType;

public class DetectInfo {
    private final Logger logger = LoggerFactory.getLogger(DetectInfo.class);

    private OperatingSystemType currentOs = null;
    private String detectVersion;
    private int majorVersion;

    public DetectInfo(String detectVersionText, int majorVersion, OperatingSystemType currentOs){
        this.detectVersion = detectVersionText;
        this.currentOs = currentOs;
        this.majorVersion = majorVersion;
    }

    public int getDetectMajorVersion() {
        return  majorVersion;
    }

    public String getDetectVersion() {
        return detectVersion;
    }

    public OperatingSystemType getCurrentOs() {
        return currentOs;
    }

}
