package org.javamodularity.moduleplugin.internal;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.javamodularity.moduleplugin.tasks.PatchModuleExtension;

import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public final class PatchModuleResolver {

    private static final Logger LOGGER = Logging.getLogger(PatchModuleResolver.class);

    private final PatchModuleExtension patchModuleExtension;
    /**
     * Takes a JAR name and resolves it to a full JAR path. If returned JAR patch is empty, the JAR is skipped.
     */
    private final UnaryOperator<String> jarNameResolver;

    public PatchModuleResolver(PatchModuleExtension patchModuleExtension, UnaryOperator<String> jarNameResolver) {
        this.patchModuleExtension = patchModuleExtension;
        this.jarNameResolver = jarNameResolver;
    }

    public Stream<String> toArgumentStream() {
        return toValueStream().flatMap(value -> Stream.of("--patch-module", value));
    }

    public Stream<String> toValueStream() {
        return patchModuleExtension.getConfig().stream()
                .map(patch -> patch.split("="))
                .map(this::resolvePatchModuleValue)
                .filter(Objects::nonNull);
    }

    private String resolvePatchModuleValue(String[] parts) {
        String moduleName = parts[0];
        String jarName = parts[1];

        String jarPath = jarNameResolver.apply(jarName);
        if (jarPath.isEmpty()) {
            LOGGER.warn("Skipped patching {} into {}", jarName, moduleName);
            return null;
        }
        return moduleName + "=" + jarPath;
    }
}
