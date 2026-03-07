package io.github.naveenkumar.jmeter.superkey.pro.license;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

/**
 * Developer tool to generate a one-off 2048-bit RSA key pair for SuperKey
 * licensing.
 * Run this ONCE, save the Private Key securely, and hardcode the Public Key
 * into LicenseManager.
 */
public class RSAKeyGenerator {

    public static void main(String[] args) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();

        String privateKey = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());

        java.nio.file.Files.write(java.nio.file.Paths.get("private.key"),
                privateKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        java.nio.file.Files.write(java.nio.file.Paths.get("public.key"),
                publicKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        System.out.println("=================================================");
        System.out.println("====== SUPERKEY PRO LICENSE KEYS GENERATED ======");
        System.out.println("=================================================\n");

        System.out.println("Saved 'private.key' and 'public.key' to the current directory.");
        System.out.println("Keep private.key completely secret.");
        System.out.println("Copy the text from public.key into LicenseManager.java.");
        System.out.println("\n=================================================");
    }
}
