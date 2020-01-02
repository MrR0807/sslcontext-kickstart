package nl.altindag.thunderberry.sslcontext;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.junit.Test;

@SuppressWarnings({"UnnecessaryLocalVariable", "squid:S1192", "squid:S2068"})
public class SSLContextHelperShould {

    @Test
    public void createSSLContextForTwoWayAutentication() {
        String keyStorePath = "keystores-for-unit-tests/identity.jks";
        String keyStorePassword = "secret";
        String trustStorePath = "keystores-for-unit-tests/truststore.jks";
        String trustStorePassword = "secret";

        SSLContextHelper sslContextHelper = SSLContextHelper.builder()
                                                            .withTwoWayAuthentication(keyStorePath, keyStorePassword,
                                                                                      trustStorePath, trustStorePassword)
                                                            .build();

        assertThat(sslContextHelper.isSecurityEnabled()).isTrue();
        assertThat(sslContextHelper.isOneWayAuthenticationEnabled()).isFalse();
        assertThat(sslContextHelper.isTwoWayAuthenticationEnabled()).isTrue();
        assertThat(sslContextHelper.getSslContext()).isNotNull();

        assertThat(sslContextHelper.getKeyManagerFactory()).isNotNull();
        assertThat(sslContextHelper.getKeyManagerFactory().getKeyManagers()).isNotEmpty();
        assertThat(sslContextHelper.getKeyStore()).isNotNull();

        assertThat(sslContextHelper.getX509TrustManager()).isNotNull();
        assertThat(sslContextHelper.getTrustedX509Certificate()).isNotEmpty();
        assertThat(sslContextHelper.getTrustStore()).isNotNull();
        assertThat(sslContextHelper.getX509TrustManager()).isNotNull();
        assertThat(sslContextHelper.getHostnameVerifier()).isNotNull();
    }

    @Test
    public void createSSLContextForOneWayAuthentication() {
        String trustStorePath = "keystores-for-unit-tests/truststore.jks";
        String trustStorePassword = "secret";

        SSLContextHelper sslContextHelper = SSLContextHelper.builder()
                                                            .withOneWayAuthentication(trustStorePath, trustStorePassword)
                                                            .build();

        assertThat(sslContextHelper.isSecurityEnabled()).isTrue();
        assertThat(sslContextHelper.isOneWayAuthenticationEnabled()).isTrue();
        assertThat(sslContextHelper.isTwoWayAuthenticationEnabled()).isFalse();
        assertThat(sslContextHelper.getSslContext()).isNotNull();

        assertThat(sslContextHelper.getX509TrustManager()).isNotNull();
        assertThat(sslContextHelper.getTrustedX509Certificate()).isNotEmpty();
        assertThat(sslContextHelper.getTrustStore()).isNotNull();
        assertThat(sslContextHelper.getX509TrustManager()).isNotNull();
        assertThat(sslContextHelper.getHostnameVerifier()).isNotNull();
    }

    @Test
    public void createSSLContextHelperWithHostnameVerifier() {
        String trustStorePath = "keystores-for-unit-tests/truststore.jks";
        String trustStorePassword = "secret";

        SSLContextHelper sslContextHelper = SSLContextHelper.builder()
                                                            .withOneWayAuthentication(trustStorePath, trustStorePassword)
                                                            .withHostnameVerifierEnabled(true)
                                                            .build();

        assertThat(sslContextHelper.getHostnameVerifier()).isInstanceOf(DefaultHostnameVerifier.class);
    }

    @Test
    public void createSSLContextHelperWithoutHostnameVerifier() {
        String trustStorePath = "keystores-for-unit-tests/truststore.jks";
        String trustStorePassword = "secret";

        SSLContextHelper sslContextHelper = SSLContextHelper.builder()
                                                            .withOneWayAuthentication(trustStorePath, trustStorePassword)
                                                            .withHostnameVerifierEnabled(false)
                                                            .build();

        assertThat(sslContextHelper.getHostnameVerifier()).isInstanceOf(NoopHostnameVerifier.class);
    }

    @Test
    public void createSSLContextWithTlsProtocolVersionOneDotOne() {
        String trustStorePath = "keystores-for-unit-tests/truststore.jks";
        String trustStorePassword = "secret";

        SSLContextHelper sslContextHelper = SSLContextHelper.builder()
                                                            .withOneWayAuthentication(trustStorePath, trustStorePassword)
                                                            .withProtocol("TLSv1.1")
                                                            .build();

        assertThat(sslContextHelper.getSslContext()).isNotNull();
        assertThat(sslContextHelper.getSslContext().getProtocol()).isEqualTo("TLSv1.1");
    }


    @Test
    public void throwExceptionWhenKeyStoreFileIsNotFound() {
        String trustStorePath = "keystores-for-unit-tests/not-existing-truststore.jks";
        String trustStorePassword = "secret";

        assertThatThrownBy(() -> SSLContextHelper.builder().withOneWayAuthentication(trustStorePath, trustStorePassword))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Could not find the keystore file");
    }

    @Test
    public void throwExceptionOneWayAuthenticationIsEnabledWhileTrustStorePathIsNotProvided() {
        String trustStorePath = EMPTY;
        String trustStorePassword = "secret";

        assertThatThrownBy(() -> SSLContextHelper.builder().withOneWayAuthentication(trustStorePath, trustStorePassword))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("TrustStore details are empty, which are required to be present when SSL/TLS is enabled");
    }

    @Test
    public void throwExceptionOneWayAuthenticationIsEnabledWhileTrustStorePasswordIsNotProvided() {
        String trustStorePath = "keystores-for-unit-tests/truststore.jks";
        String trustStorePassword = EMPTY;

        assertThatThrownBy(() -> SSLContextHelper.builder().withOneWayAuthentication(trustStorePath, trustStorePassword))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("TrustStore details are empty, which are required to be present when SSL/TLS is enabled");
    }

    @Test
    public void throwExceptionTwoWayAuthenticationEnabledWhileKeyStorePathIsNotProvided() {
        String keyStorePath = EMPTY;
        String keyStorePassword = "secret";
        String trustStorePath = "keystores-for-unit-tests/truststore.jks";
        String trustStorePassword = "secret";

        assertThatThrownBy(() -> SSLContextHelper.builder().withTwoWayAuthentication(keyStorePath, keyStorePassword, trustStorePath, trustStorePassword))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("TrustStore or KeyStore details are empty, which are required to be present when SSL is enabled");
    }

    @Test
    public void throwExceptionTwoWayAuthenticationEnabledWhileKeyStorePasswordIsNotProvided() {
        String keyStorePath = "keystores-for-unit-tests/identity.jks";
        String keyStorePassword = EMPTY;
        String trustStorePath = "keystores-for-unit-tests/truststore.jks";
        String trustStorePassword = "secret";

        assertThatThrownBy(() -> SSLContextHelper.builder().withTwoWayAuthentication(keyStorePath, keyStorePassword, trustStorePath, trustStorePassword))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("TrustStore or KeyStore details are empty, which are required to be present when SSL is enabled");
    }

    @Test
    public void throwExceptionTwoWayAuthenticationEnabledWhileTrustStorePathIsNotProvided() {
        String keyStorePath = "keystores-for-unit-tests/identity.jks";
        String keyStorePassword = "secret";
        String trustStorePath = EMPTY;
        String trustStorePassword = "secret";

        assertThatThrownBy(() -> SSLContextHelper.builder().withTwoWayAuthentication(keyStorePath, keyStorePassword, trustStorePath, trustStorePassword))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("TrustStore or KeyStore details are empty, which are required to be present when SSL is enabled");
    }

    @Test
    public void throwExceptionTwoWayAuthenticationEnabledWhileTrustStorePasswordIsNotProvided() {
        String keyStorePath = "keystores-for-unit-tests/identity.jks";
        String keyStorePassword = "secret";
        String trustStorePath = "keystores-for-unit-tests/truststore.jks";
        String trustStorePassword = EMPTY;

        assertThatThrownBy(() -> SSLContextHelper.builder().withTwoWayAuthentication(keyStorePath, keyStorePassword, trustStorePath, trustStorePassword))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("TrustStore or KeyStore details are empty, which are required to be present when SSL is enabled");
    }

    @Test
    public void throwExceptionWhenX509TrustManagerIsRequestWhenSecurityIsDisabled() {
        SSLContextHelper sslContextHelper = SSLContextHelper.builder()
                                                            .withoutSecurity()
                                                            .build();

        assertThatThrownBy(sslContextHelper::getX509TrustManager)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("The TrustManager could not be provided because it is not available");
    }

}
