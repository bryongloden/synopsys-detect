package com.blackducksoftware.integration.hub.detect.detector.clang.packagemanager.dependencyfinder;

import java.io.File;
import java.util.List;
import java.util.Optional;

import com.blackducksoftware.integration.hub.detect.detector.clang.PackageDetails;
import com.blackducksoftware.integration.hub.detect.detector.clang.packagemanager.ClangPackageManagerInfo;
import com.blackducksoftware.integration.hub.detect.util.executable.ExecutableRunner;
import com.blackducksoftware.integration.hub.detect.util.executable.ExecutableRunnerException;

public interface ClangPackageManagerResolver {
    List<PackageDetails> resolvePackages(ClangPackageManagerInfo currentPackageManager, ExecutableRunner executableRunner, File workingDirectory, String queryPackageOutput) throws ExecutableRunnerException;
}
