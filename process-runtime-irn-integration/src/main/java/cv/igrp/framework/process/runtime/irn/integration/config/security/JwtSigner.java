package cv.igrp.framework.process.runtime.irn.integration.config.security;

import io.jsonwebtoken.Jwts;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.Map;

/**
 * JwtSigner for PKCS#1 RSA private keys (-----BEGIN RSA PRIVATE KEY-----)
 * Generates RS256 signed JWT tokens.
 */
public class JwtSigner {

    private final Resource privateKeyResource;

    private final PrivateKey privateKey;

    public JwtSigner(@Value("${igrp.authorization.jwt.private-key:default}") Resource privateKeyResource) {
        this.privateKeyResource = privateKeyResource;
        this.privateKey = loadPrivateKey(); // load once during instantiation
    }

    /**
     * Loads a PKCS#1 RSA private key from PEM using BouncyCastle
     */
    private PrivateKey loadPrivateKey() {
        try (PemReader reader = new PemReader(new InputStreamReader(privateKeyResource.getInputStream()))) {

            byte[] content = reader.readPemObject().getContent();

            RSAPrivateKey bcKey = RSAPrivateKey.getInstance(content);

            RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(
                    bcKey.getModulus(),
                    bcKey.getPublicExponent(),
                    bcKey.getPrivateExponent(),
                    bcKey.getPrime1(),
                    bcKey.getPrime2(),
                    bcKey.getExponent1(),
                    bcKey.getExponent2(),
                    bcKey.getCoefficient()
            );

            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load PKCS#1 RSA private key", e);
        }
    }

    /**
     * Generate RS256 signed JWT token with the given payload
     */
    public String generateRS256Token(Map<String, Object> payload) {
        return Jwts.builder()
                .claims(payload)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }
}
