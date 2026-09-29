import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class GenerateJwtKeyPair {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected output directory argument");
        }

        Path output = Path.of(args[0]).toAbsolutePath().normalize();
        Files.createDirectories(output);

        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var pair = generator.generateKeyPair();

        writePem(output.resolve("private.pem"), "PRIVATE KEY", pair.getPrivate().getEncoded());
        writePem(output.resolve("public.pem"), "PUBLIC KEY", pair.getPublic().getEncoded());
        System.out.println("Generated RS256 key pair in " + output);
    }

    private static void writePem(Path path, String type, byte[] value) throws Exception {
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(value);
        String pem = "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n";
        Files.writeString(path, pem, StandardCharsets.US_ASCII);
    }
}
