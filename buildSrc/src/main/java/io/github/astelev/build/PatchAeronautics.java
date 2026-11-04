package io.github.astelev.build;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** Keep the fork's overwrite compatible with access-widened ChunkMap methods. */
public abstract class PatchAeronautics extends DefaultTask {
    private static final String ENTRY =
            "qouteall/imm_ptl/core/mixin/common/chunk_sync/MixinChunkMap_C.class";
    private static final String DESCRIPTOR = "(Lnet/minecraft/world/level/chunk/LevelChunk;)V";

    @InputFile
    public abstract RegularFileProperty getInputJar();

    @OutputFile
    public abstract RegularFileProperty getOutputJar();

    @TaskAction
    public void patch() throws IOException {
        var input = getInputJar().get().getAsFile().toPath();
        var output = getOutputJar().get().getAsFile().toPath();
        Files.createDirectories(output.getParent());
        // Validate before opening the output; never modify the supplied original.
        try (var source = new ZipFile(input.toFile())) {
            var entry = source.getEntry(ENTRY);
            if (entry == null) {
                throw new GradleException("Expected Immersive Aeronautics 1.1.4 mixin was not found");
            }
            byte[] patched;
            try (var stream = source.getInputStream(entry)) {
                patched = widenOverwrite(stream.readAllBytes());
            }
            try (var destination = new ZipOutputStream(Files.newOutputStream(output))) {
                var entries = source.entries();
                while (entries.hasMoreElements()) {
                    var original = entries.nextElement();
                    var copy = new ZipEntry(original.getName());
                    copy.setTime(original.getTime());
                    destination.putNextEntry(copy);
                    if (original.getName().equals(ENTRY)) {
                        destination.write(patched);
                    } else if (!original.isDirectory()) {
                        try (var stream = source.getInputStream(original)) {
                            stream.transferTo(destination);
                        }
                    }
                    destination.closeEntry();
                }
            }
        }
    }

    private static byte[] widenOverwrite(byte[] bytes) {
        var reader = new ClassReader(bytes);
        var writer = new ClassWriter(reader, 0);
        int[] matches = {0};
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if (name.equals("onChunkReadyToSend") && descriptor.equals(DESCRIPTOR)) {
                    matches[0]++;
                    access = (access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        }, 0);
        if (matches[0] != 1) {
            throw new GradleException("Expected exactly one onChunkReadyToSend(LevelChunk) overwrite; found " + matches[0]);
        }
        return writer.toByteArray();
    }
}
