package cl.rutaexpress.bff.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** Verifica firma, vigencia, issuer y audience con tokens firmados con una clave RSA de prueba. */
class TokenValidationTests {

    private static final SecurityProperties PROPS =
            new SecurityProperties("tenant-1", "api://rutaexpress", List.of("http://localhost:5173"));

    private static KeyPair keys;
    private static KeyPair otherKeys;
    private static NimbusJwtDecoder decoder;

    @BeforeAll
    static void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keys = generator.generateKeyPair();
        otherKeys = generator.generateKeyPair();
        decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keys.getPublic()).build();
        decoder.setJwtValidator(SecurityConfig.tokenValidator(PROPS));
    }

    private static String token(KeyPair signWith, String issuer, String audience, Instant expiresAt) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .subject("user-1")
                .issueTime(Date.from(Instant.now().minusSeconds(120)))
                .expirationTime(Date.from(expiresAt))
                .claim("roles", List.of("Admin"))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey) signWith.getPrivate()));
        return jwt.serialize();
    }

    private static String valid() throws Exception {
        return token(keys, PROPS.issuer(), PROPS.audience(), Instant.now().plusSeconds(300));
    }

    @Test
    void aceptaUnTokenValido() throws Exception {
        assertThat(decoder.decode(valid()).getClaimAsStringList("roles")).containsExactly("Admin");
    }

    @Test
    void rechazaUnaFirmaDeOtraClave() throws Exception {
        String forged = token(otherKeys, PROPS.issuer(), PROPS.audience(), Instant.now().plusSeconds(300));

        assertThatThrownBy(() -> decoder.decode(forged)).isInstanceOf(BadJwtException.class);
    }

    @Test
    void rechazaUnTokenExpirado() throws Exception {
        String expired = token(keys, PROPS.issuer(), PROPS.audience(), Instant.now().minusSeconds(90));

        assertThatThrownBy(() -> decoder.decode(expired)).isInstanceOf(BadJwtException.class);
    }

    @Test
    void rechazaUnIssuerDistinto() throws Exception {
        String wrongIssuer = token(keys, "https://login.microsoftonline.com/otro-tenant/v2.0", PROPS.audience(),
                Instant.now().plusSeconds(300));

        assertThatThrownBy(() -> decoder.decode(wrongIssuer)).isInstanceOf(BadJwtException.class);
    }

    @Test
    void rechazaUnaAudienceDistinta() throws Exception {
        String wrongAudience = token(keys, PROPS.issuer(), "api://otra-api", Instant.now().plusSeconds(300));

        assertThatThrownBy(() -> decoder.decode(wrongAudience)).isInstanceOf(BadJwtException.class);
    }
}
