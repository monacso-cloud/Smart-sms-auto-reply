import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.util.HexFormat;

public class SelectSigningAlias {
    public static void main(String[] args) throws Exception {
        char[] password = System.getenv("STORE_PASSWORD").toCharArray();
        KeyStore store = KeyStore.getInstance(Path.of(args[0]).toFile(), password);
        java.util.Arrays.fill(password, '\0');
        String expected = "b75a689cf031e3954fa9d8353366b8adf045a01bdaf4f6e5cf3ae9133a1af26b";
        String selected = null;
        var aliases = store.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (!store.isKeyEntry(alias) || store.getCertificate(alias) == null) continue;
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(store.getCertificate(alias).getEncoded()));
            if (expected.equals(digest)) {
                if (selected != null) throw new IllegalStateException("Multiple keys match the expected certificate.");
                selected = alias;
            }
        }
        if (selected == null) {
            throw new IllegalStateException("The stored keystore does not contain the expected release certificate. Confirm the original signing keystore.");
        }
        if (selected.contains("\n") || selected.contains("\r")) throw new IllegalStateException("Invalid alias.");
        Files.writeString(Path.of(System.getenv("GITHUB_ENV")), "SMART_SMS_RELEASE_ALIAS=" + selected + "\n",
                StandardOpenOption.APPEND);
        System.out.println("Selected the existing key matching the expected release certificate.");
    }
}
