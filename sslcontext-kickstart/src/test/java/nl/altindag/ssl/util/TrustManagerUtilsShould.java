/*
 * Copyright 2019-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.altindag.ssl.util;

import nl.altindag.ssl.exception.GenericSecurityException;
import nl.altindag.ssl.exception.GenericTrustManagerException;
import nl.altindag.ssl.model.KeyStoreHolder;
import nl.altindag.ssl.trustmanager.CompositeX509ExtendedTrustManager;
import nl.altindag.ssl.trustmanager.UnsafeX509ExtendedTrustManager;
import nl.altindag.ssl.trustmanager.X509TrustManagerWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * @author Hakan Altindag
 */
@ExtendWith(MockitoExtension.class)
class TrustManagerUtilsShould {

    private static final String TRUSTSTORE_FILE_NAME = "truststore.jks";
    private static final char[] TRUSTSTORE_PASSWORD = new char[] {'s', 'e', 'c', 'r', 'e', 't'};
    private static final String KEYSTORE_LOCATION = "keystores-for-unit-tests/";
    private static final String ORIGINAL_OS_NAME = System.getProperty("os.name");

    @Test
    void combineTrustManagers() throws KeyStoreException {
        KeyStore trustStoreOne = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);
        KeyStore trustStoreTwo = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + "truststore-containing-github.jks", TRUSTSTORE_PASSWORD);
        X509ExtendedTrustManager trustManager = TrustManagerUtils
                .combine(TrustManagerUtils.createTrustManager(trustStoreOne), TrustManagerUtils.createTrustManager(trustStoreTwo));

        assertThat(trustStoreOne.size()).isEqualTo(1);
        assertThat(trustStoreTwo.size()).isEqualTo(1);
        assertThat(trustManager.getAcceptedIssuers()).hasSize(2);
    }

    @Test
    void unwrapCombinedTrustManagersAndRecombineIntoSingleBaseTrustManager() throws KeyStoreException {
        KeyStore trustStoreOne = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);
        KeyStore trustStoreTwo = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + "truststore-containing-github.jks", TRUSTSTORE_PASSWORD);

        X509ExtendedTrustManager trustManagerOne = TrustManagerUtils.createTrustManager(trustStoreOne);
        X509ExtendedTrustManager trustManagerTwo = TrustManagerUtils.createTrustManager(trustStoreTwo);

        X509ExtendedTrustManager combinedTrustManager = TrustManagerUtils.combine(trustManagerOne, trustManagerTwo);
        X509ExtendedTrustManager combinedCombinedTrustManager = TrustManagerUtils.combine(combinedTrustManager, trustManagerOne, trustManagerTwo);

        assertThat(trustStoreOne.size()).isEqualTo(1);
        assertThat(trustStoreTwo.size()).isEqualTo(1);
        assertThat(combinedTrustManager.getAcceptedIssuers()).hasSize(2);
        assertThat(combinedCombinedTrustManager.getAcceptedIssuers()).hasSize(2);

        assertThat(combinedTrustManager).isInstanceOf(CompositeX509ExtendedTrustManager.class);
        assertThat(combinedCombinedTrustManager).isInstanceOf(CompositeX509ExtendedTrustManager.class);
        assertThat(((CompositeX509ExtendedTrustManager) combinedTrustManager).size()).isEqualTo(2);
        assertThat(((CompositeX509ExtendedTrustManager) combinedCombinedTrustManager).size()).isEqualTo(4);
    }

    @Test
    void combineTrustManagersWithTrustStoreHolders() throws KeyStoreException {
        KeyStore trustStoreOne = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);
        KeyStore trustStoreTwo = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + "truststore-containing-github.jks", TRUSTSTORE_PASSWORD);

        KeyStoreHolder trustStoreHolderOne = new KeyStoreHolder(trustStoreOne, TRUSTSTORE_PASSWORD);
        KeyStoreHolder trustStoreHolderTwo = new KeyStoreHolder(trustStoreTwo, TRUSTSTORE_PASSWORD);

        X509ExtendedTrustManager trustManager = TrustManagerUtils
                .combine(TrustManagerUtils.createTrustManager(trustStoreHolderOne, trustStoreHolderTwo));

        assertThat(trustStoreOne.size()).isEqualTo(1);
        assertThat(trustStoreTwo.size()).isEqualTo(1);
        assertThat(trustManager.getAcceptedIssuers()).hasSize(2);
    }

    @Test
    void combineTrustManagersWithKeyStores() throws KeyStoreException {
        KeyStore trustStoreOne = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);
        KeyStore trustStoreTwo = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + "truststore-containing-github.jks", TRUSTSTORE_PASSWORD);

        X509ExtendedTrustManager trustManager = TrustManagerUtils
                .combine(TrustManagerUtils.createTrustManager(trustStoreOne, trustStoreTwo));

        assertThat(trustStoreOne.size()).isEqualTo(1);
        assertThat(trustStoreTwo.size()).isEqualTo(1);
        assertThat(trustManager.getAcceptedIssuers()).hasSize(2);
    }

    @Test
    void combineTrustManagersWhileFilteringDuplicateCertificates() throws KeyStoreException {
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);
        X509ExtendedTrustManager trustManager = TrustManagerUtils
                .combine(TrustManagerUtils.createTrustManager(trustStore), TrustManagerUtils.createTrustManager(trustStore));

        assertThat(trustStore.size()).isEqualTo(1);
        assertThat(trustManager.getAcceptedIssuers()).hasSize(1);
    }

    @Test
    void wrapIfNeeded() {
        X509TrustManager trustManager = mock(X509TrustManager.class);
        X509ExtendedTrustManager extendedTrustManager = TrustManagerUtils.wrapIfNeeded(trustManager);

        assertThat(extendedTrustManager).isInstanceOf(X509TrustManagerWrapper.class);
    }

    @Test
    void doNotWrapWhenInstanceIsX509ExtendedTrustManager() {
        X509ExtendedTrustManager trustManager = mock(X509ExtendedTrustManager.class);
        X509ExtendedTrustManager extendedTrustManager = TrustManagerUtils.wrapIfNeeded(trustManager);

        assertThat(extendedTrustManager)
                .isEqualTo(trustManager)
                .isNotInstanceOf(X509TrustManagerWrapper.class);
    }

    @Test
    void createTrustManagerWithCustomSecurityProviderBasedOnTheName() {
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);

        X509ExtendedTrustManager trustManager = TrustManagerUtils.createTrustManager(trustStore, TrustManagerFactory.getDefaultAlgorithm(), "SunJSSE");

        assertThat(trustManager).isNotNull();
    }

    @Test
    void createTrustManagerWithCustomSecurityProvider() {
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);
        Provider sunJSSE = Security.getProvider("SunJSSE");

        X509ExtendedTrustManager trustManager = TrustManagerUtils.createTrustManager(trustStore, TrustManagerFactory.getDefaultAlgorithm(), sunJSSE);

        assertThat(trustManager).isNotNull();
    }

    @Test
    void createTrustManagerWithJdkTrustedCertificatesWhenProvidingNullAsTrustStore() {
        X509ExtendedTrustManager trustManager = TrustManagerUtils.createTrustManager((KeyStore) null);

        assertThat(trustManager).isNotNull();
        assertThat(trustManager.getAcceptedIssuers()).hasSizeGreaterThan(10);
    }

    @Test
    void createTrustManagerWithJdkTrustedCertificatesWhenCallingCreateTrustManagerWithJdkTrustedCertificates() {
        X509ExtendedTrustManager trustManager = TrustManagerUtils.createTrustManagerWithJdkTrustedCertificates();

        assertThat(trustManager).isNotNull();
        assertThat((trustManager).getAcceptedIssuers()).hasSizeGreaterThan(10);
    }

    @Test
    void createTrustManagerWithSystemTrustedCertificate() {
        String operatingSystem = System.getProperty("os.name").toLowerCase();
        Optional<X509ExtendedTrustManager> trustManager = TrustManagerUtils.createTrustManagerWithSystemTrustedCertificates();
        if (operatingSystem.contains("mac") || operatingSystem.contains("windows")) {
            assertThat(trustManager).isPresent();
            assertThat((trustManager).get().getAcceptedIssuers()).hasSizeGreaterThan(0);
        }

        if (operatingSystem.contains("linux")) {
            assertThat(trustManager).isNotPresent();
        }
    }

    @Test
    void createTrustManagerWhenProvidingACustomTrustStore() {
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);
        X509ExtendedTrustManager trustManager = TrustManagerUtils.createTrustManager(trustStore);

        assertThat(trustManager).isNotNull();
        assertThat((trustManager).getAcceptedIssuers()).hasSize(1);
    }

    @Test
    void createUnsafeTrustManager() {
        X509ExtendedTrustManager trustManager = TrustManagerUtils.createUnsafeTrustManager();

        assertThat(trustManager)
                .isNotNull()
                .isInstanceOf(UnsafeX509ExtendedTrustManager.class)
                .isEqualTo(TrustManagerUtils.createUnsafeTrustManager());
    }

    @Test
    void createTrustManagerFromMultipleTrustManagers() {
        KeyStore trustStoreOne = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);
        KeyStore trustStoreTwo = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + "truststore-containing-github.jks", TRUSTSTORE_PASSWORD);

        X509ExtendedTrustManager trustManagerOne = TrustManagerUtils.createTrustManager(trustStoreOne);
        X509ExtendedTrustManager trustManagerTwo = TrustManagerUtils.createTrustManager(trustStoreTwo);

        X509ExtendedTrustManager trustManager = TrustManagerUtils.trustManagerBuilder()
                .withTrustManager(trustManagerOne)
                .withTrustManager(trustManagerTwo)
                .build();

        assertThat(trustManager).isNotNull();
    }

    @Test
    void createTrustManagerFromMultipleTrustManagersUsingVarArgs() {
        KeyStore trustStoreOne = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);
        KeyStore trustStoreTwo = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + "truststore-containing-github.jks", TRUSTSTORE_PASSWORD);

        X509ExtendedTrustManager trustManagerOne = TrustManagerUtils.createTrustManager(trustStoreOne);
        X509ExtendedTrustManager trustManagerTwo = TrustManagerUtils.createTrustManager(trustStoreTwo);

        X509ExtendedTrustManager trustManager = TrustManagerUtils.trustManagerBuilder()
                .withTrustManagers(trustManagerOne, trustManagerTwo)
                .build();

        assertThat(trustManager).isNotNull();
    }

    @Test
    void createTrustManagerFromMultipleTrustManagersUsingList() {
        KeyStore trustStoreOne = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);
        KeyStore trustStoreTwo = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + "truststore-containing-github.jks", TRUSTSTORE_PASSWORD);

        X509ExtendedTrustManager trustManagerOne = TrustManagerUtils.createTrustManager(trustStoreOne);
        X509ExtendedTrustManager trustManagerTwo = TrustManagerUtils.createTrustManager(trustStoreTwo);

        X509ExtendedTrustManager trustManager = TrustManagerUtils.trustManagerBuilder()
                .withTrustManagers(Arrays.asList(trustManagerOne, trustManagerTwo))
                .build();

        assertThat(trustManager).isNotNull();
    }

    @Test
    void createTrustManagerFromMultipleTrustStoresUsingVarArgs() {
        KeyStore trustStoreOne = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);
        KeyStore trustStoreTwo = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + "truststore-containing-github.jks", TRUSTSTORE_PASSWORD);

        X509ExtendedTrustManager trustManager = TrustManagerUtils.trustManagerBuilder()
                .withTrustStores(trustStoreOne, trustStoreTwo)
                .build();

        assertThat(trustManager).isNotNull();
    }

    @Test
    void createTrustManagerFromMultipleTrustStoresUsingList() {
        KeyStore trustStoreOne = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);
        KeyStore trustStoreTwo = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + "truststore-containing-github.jks", TRUSTSTORE_PASSWORD);

        X509ExtendedTrustManager trustManager = TrustManagerUtils.trustManagerBuilder()
                .withTrustStores(Arrays.asList(trustStoreOne, trustStoreTwo))
                .build();

        assertThat(trustManager).isNotNull();
    }

    @Test
    void createTrustManagerFromMultipleTrustStores() {
        KeyStore trustStoreOne = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);
        KeyStore trustStoreTwo = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + "truststore-containing-github.jks", TRUSTSTORE_PASSWORD);

        X509ExtendedTrustManager trustManager = TrustManagerUtils.trustManagerBuilder()
                .withTrustStore(trustStoreOne)
                .withTrustStore(trustStoreTwo)
                .build();

        assertThat(trustManager).isNotNull();
    }

    @Test
    void loadLinuxSystemKeyStoreReturnsOptionalOfEmpty() {
        System.setProperty("os.name", "linux");

        Optional<X509ExtendedTrustManager> trustManager = TrustManagerUtils.createTrustManagerWithSystemTrustedCertificates();
        assertThat(trustManager).isNotPresent();

        resetOsName();
    }

    @Test
    void createTrustManagerFromMultipleTrustStoresWithTrustManagerFactoryAlgorithm() {
        KeyStore trustStoreOne = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);
        KeyStore trustStoreTwo = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + "truststore-containing-github.jks", TRUSTSTORE_PASSWORD);

        X509ExtendedTrustManager trustManager = TrustManagerUtils.trustManagerBuilder()
                .withTrustStore(trustStoreOne, TrustManagerFactory.getDefaultAlgorithm())
                .withTrustStore(trustStoreTwo, TrustManagerFactory.getDefaultAlgorithm())
                .build();

        assertThat(trustManager).isNotNull();
    }

    @Test
    void throwExceptionWhenInvalidTrustManagerAlgorithmIsProvided() {
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);

        assertThatThrownBy(() -> TrustManagerUtils.createTrustManager(trustStore, "ABCD"))
                .isInstanceOf(GenericSecurityException.class)
                .hasMessage("java.security.NoSuchAlgorithmException: ABCD TrustManagerFactory not available");
    }

    @Test
    void throwExceptionWhenInvalidSecurityProviderNameIsProvided() {
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);
        String trustManagerFactoryAlgorithm = TrustManagerFactory.getDefaultAlgorithm();

        assertThatThrownBy(() -> TrustManagerUtils.createTrustManager(trustStore, trustManagerFactoryAlgorithm, "test"))
                .isInstanceOf(GenericSecurityException.class)
                .hasMessage("java.security.NoSuchProviderException: no such provider: test");
    }

    @Test
    void throwExceptionWhenInvalidSecurityProviderNameIsProvidedForTheTrustManagerFactoryAlgorithm() {
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);
        String trustManagerFactoryAlgorithm = TrustManagerFactory.getDefaultAlgorithm();

        assertThatThrownBy(() -> TrustManagerUtils.createTrustManager(trustStore, trustManagerFactoryAlgorithm, "SUN"))
                .isInstanceOf(GenericSecurityException.class)
                .hasMessage("java.security.NoSuchAlgorithmException: no such algorithm: PKIX for provider SUN");
    }

    @Test
    void throwExceptionWhenInvalidSecurityProviderIsProvidedForTheTrustManagerFactoryAlgorithm() {
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);
        String trustManagerFactoryAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        Provider sunSecurityProvider = Security.getProvider("SUN");

        assertThatThrownBy(() -> TrustManagerUtils.createTrustManager(trustStore, trustManagerFactoryAlgorithm, sunSecurityProvider))
                .isInstanceOf(GenericSecurityException.class)
                .hasMessage("java.security.NoSuchAlgorithmException: no such algorithm: PKIX for provider SUN");
    }

    @Test
    void throwGenericSecurityExceptionWhenTrustManagerFactoryCanNotInitializeWithTheProvidedTrustStore() throws KeyStoreException {
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);
        TrustManagerFactory trustManagerFactory = mock(TrustManagerFactory.class);

        doThrow(new KeyStoreException("KABOOOM!")).when(trustManagerFactory).init(any(KeyStore.class));

        assertThatThrownBy(() -> TrustManagerUtils.createTrustManager(trustStore, trustManagerFactory))
                .isInstanceOf(GenericSecurityException.class)
                .hasMessage("java.security.KeyStoreException: KABOOOM!");
    }

    @Test
    void throwGenericTrustManagerExceptionWhenProvidingEmptyListOfTrustManagersWhenCombining() {
        List<X509TrustManager> trustManagers = Collections.emptyList();
        assertThatThrownBy(() -> TrustManagerUtils.combine(trustManagers))
                .isInstanceOf(GenericTrustManagerException.class)
                .hasMessage("Input does not contain TrustManager");
    }

    private void resetOsName() {
        System.setProperty("os.name", ORIGINAL_OS_NAME);
    }

}
