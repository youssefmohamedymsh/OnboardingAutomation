package io.github.youssefmohamedymsh.kyc.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Generates the upload files scenarios need, in target/test-files, so no binaries live in the repo.
 * The browser infers each file's type from its extension, just as it would for a real user.
 */
public final class TestFiles {

    private static final Path DIR = Path.of("target", "test-files").toAbsolutePath();

    private TestFiles() {
    }

    /** Returns an upload file by name. Names starting with "empty" are 0 bytes; "large" ones are 6 MB. */
    public static Path document(String fileName) {
        byte[] content;
        if (fileName.startsWith("empty")) {
            content = new byte[0];
        } else if (fileName.startsWith("large")) {
            content = new byte[6 * 1024 * 1024];
            Arrays.fill(content, (byte) 'x');
        } else if (fileName.endsWith(".png")) {
            content = new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0};
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            content = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F'};
        } else if (fileName.endsWith(".pdf")) {
            content = "%PDF-1.4\n% Ledgerline test document\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
        } else {
            content = "Not an identity document".getBytes(StandardCharsets.UTF_8);
        }
        return write(fileName, content);
    }

    public static Path csv(String fileName, String content) {
        return write(fileName, content.getBytes(StandardCharsets.UTF_8));
    }

    private static Path write(String fileName, byte[] content) {
        try {
            Files.createDirectories(DIR);
            Path file = DIR.resolve(fileName);
            Files.write(file, content);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
