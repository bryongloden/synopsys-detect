package com.synopsys.integration.detectable.detectable.executable.resolver;

import java.io.File;

import com.synopsys.integration.detectable.DetectableEnvironment;

public interface MavenResolver {
    File resolveMaven(final DetectableEnvironment environment);
}
