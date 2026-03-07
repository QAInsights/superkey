package io.github.naveenkumar.jmeter.superkey.pro.license;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Developer tool to generate a signed `superkey.lic` payload for a customer.
 */
public class LicenseGenerator {

    private static final String PRIVATE_KEY_BASE64 = System.getenv("SUPERKEY_PRIVATE_KEY");

    public static void main(String[] args) throws Exception {
        if (PRIVATE_KEY_BASE64 == null || PRIVATE_KEY_BASE64.isEmpty()) {
            System.err.println("ERROR: Missing SUPERKEY_PRIVATE_KEY environment variable.");
            System.exit(1);
        }

        // Example Customer Data
        String name = "Naveen Kumar";
        String email = "catch.nkn@gmail.com";
        String tier = "PRO";
        String expires = "2027-12-31"; // YYYY-MM-DD

        // The exact payload string format that the plugin will reconstruct and verify
        String payload = String.format("Name=%s\nEmail=%s\nTier=%s\nExpires=%s", name, email, tier, expires);

        // Sign the payload using the Private Key
        String signatureBase64 = sign(payload, PRIVATE_KEY_BASE64);

        String finalLicenseData = "--[SUPERKEY PRO LICENSE]--\n" +
                "Name: " + name + "\n" +
                "Email: " + email + "\n" +
                "Tier: " + tier + "\n" +
                "Expires: " + expires + "\n" +
                "Signature: " + signatureBase64 + "\n";

        Files.write(Paths.get("superkey.lic"), finalLicenseData.getBytes(StandardCharsets.UTF_8));

        System.out.println("=================================================");
        System.out.println("Success! Wrote signed license to 'superkey.lic'");
        System.out.println("Place this file in JMeter bin/ directory to unlock PRO.");
        System.out.println("=================================================");
    }

    private static String sign(String payload, String privateKeyBase64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = kf.generatePrivate(spec);

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(payload.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(signature.sign());
    }
}
