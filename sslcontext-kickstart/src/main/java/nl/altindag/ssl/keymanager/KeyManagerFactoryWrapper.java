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

package nl.altindag.ssl.keymanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyManagerFactorySpi;
import javax.net.ssl.ManagerFactoryParameters;
import java.security.KeyStore;
import java.security.Provider;
import java.util.Objects;

/**
 * <strong>NOTE:</strong>
 * Please don't use this class directly as it is part of the internal API. Class name and methods can be changed any time.
 * Instead use the {@link nl.altindag.ssl.util.KeyManagerUtils KeyManagerUtils} which provides the same functionality
 * while it has a stable API because it is part of the public API.
 *
 * @author Hakan Altindag
 */
public class KeyManagerFactoryWrapper extends KeyManagerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyManagerFactoryWrapper.class);
    private static final String KEY_MANAGER_FACTORY_ALGORITHM = "no-algorithm";
    private static final Provider PROVIDER = new Provider("", 1.0, "") {};

    public KeyManagerFactoryWrapper(KeyManager keyManager) {
        super(new KeyManagerFactorySpiWrapper(keyManager), PROVIDER, KEY_MANAGER_FACTORY_ALGORITHM);
    }

    private static class KeyManagerFactorySpiWrapper extends KeyManagerFactorySpi {

        private final KeyManager[] keyManagers;

        public KeyManagerFactorySpiWrapper(KeyManager keyManager) {
            Objects.requireNonNull(keyManager);
            this.keyManagers = new KeyManager[]{keyManager};
        }

        @Override
        protected void engineInit(KeyStore keyStore, char[] keyStorePassword) {
            LOGGER.info("Ignoring provided KeyStore");
        }

        @Override
        protected void engineInit(ManagerFactoryParameters managerFactoryParameters) {
            LOGGER.info("Ignoring provided ManagerFactoryParameters");
        }

        @Override
        protected KeyManager[] engineGetKeyManagers() {
            return keyManagers;
        }

    }

}
