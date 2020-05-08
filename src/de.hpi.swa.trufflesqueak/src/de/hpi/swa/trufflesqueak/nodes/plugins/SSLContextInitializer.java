/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.plugins;

import static java.util.Arrays.asList;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

/**
 * Create SSL contexts with two trust managers: one containing the custom certificate used in test,
 * and a second one containing the system's certificates.
 *
 * <p>
 * Because the current primary use-case for custom certificates is during Squeak unit tests, the
 * implementation is minimal, and limited to reading certificates in PEM format.
 * </p>
 */
public final class SSLContextInitializer {

    private static final String CERTIFICATE = "CERTIFICATE";
    private static final String CERTIFICATE_ALIAS = "testcert";
    private static final String PRIVATE_KEY = "RSA PRIVATE KEY";
    private static final String PRIVATE_KEY_ALIAS = "testkey";
    private static final String KEYSTORE_PASSWORD = "changeit";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private SSLContextInitializer() {
    }

    public static SSLContext createSSLContext(final Path path) throws GeneralSecurityException, IOException {
        final CertificateInfo info = readPem(path.toFile());
        final KeyStore keyStore = prepareKeyStore(info);
        final KeyManager[] keyManagers = collectKeyManagers(keyStore);
        final TrustManager[] trustManagers = collectTrustManagers(keyStore);

        final SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagers, trustManagers, null);
        return context;
    }

    private static CertificateInfo readPem(final File file)
                    throws IOException, GeneralSecurityException {

        Certificate certificate = null;
        PrivateKey key = null;

        try (PemReader reader = new PemReader(new FileReader(file))) {

            while (true) {
                final PemObject read = reader.readPemObject();
                if (read == null) {
                    break;
                } else if (read.getType().equals(CERTIFICATE)) {
                    certificate = readCertificate(read.getContent());
                } else if (read.getType().equals(PRIVATE_KEY)) {
                    key = readPrivateKey(read.getContent());
                }
            }

            return new CertificateInfo(certificate, key);
        }
    }

    private static PrivateKey readPrivateKey(final byte[] keyBytes) throws GeneralSecurityException {
        final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        final KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePrivate(spec);
    }

    private static Certificate readCertificate(final byte[] cert) throws GeneralSecurityException {
        final CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return factory.generateCertificate(new ByteArrayInputStream(cert));
    }

    private static KeyStore prepareKeyStore(final CertificateInfo info)
                    throws GeneralSecurityException, IOException {
        final KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
        store.load(null);
        store.setCertificateEntry(CERTIFICATE_ALIAS, info.certificate);
        store.setKeyEntry(PRIVATE_KEY_ALIAS, info.privateKey, KEYSTORE_PASSWORD.toCharArray(),
                        new Certificate[]{info.certificate});
        return store;
    }

    private static KeyManager[] collectKeyManagers(final KeyStore store)
                    throws GeneralSecurityException {
        final KeyManagerFactory factory = prepareKeyManagerFactory(store);
        return factory.getKeyManagers();
    }

    private static KeyManagerFactory prepareKeyManagerFactory(final KeyStore store)
                    throws GeneralSecurityException {
        final KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(store, KEYSTORE_PASSWORD.toCharArray());
        return factory;
    }

    private static TrustManager[] collectTrustManagers(final KeyStore store)
                    throws GeneralSecurityException {
        final TrustManagerFactory customFactory = prepareTrustManagerFactory(store);
        final TrustManagerFactory systemFactory = prepareSystemTrustMangerFactory();

        final List<TrustManager> trustManagers = new ArrayList<>(
                        asList(customFactory.getTrustManagers()));
        trustManagers.addAll(asList(systemFactory.getTrustManagers()));
        return new TrustManager[]{new CompositeTrustManager(trustManagers)};
    }

    private static TrustManagerFactory prepareTrustManagerFactory(final KeyStore store)
                    throws GeneralSecurityException {
        final TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(store);
        return factory;
    }

    private static TrustManagerFactory prepareSystemTrustMangerFactory()
                    throws GeneralSecurityException {
        final TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init((KeyStore) null);
        return factory;
    }

    private static final class CertificateInfo {

        private final Certificate certificate;
        private final PrivateKey privateKey;

        private CertificateInfo(final Certificate certificate, final PrivateKey privateKey) {
            this.certificate = Objects.requireNonNull(certificate, "No certificate in PEM");
            this.privateKey = Objects.requireNonNull(privateKey, "No private key in PEM");
        }
    }

    /**
     * Build a trust manager that can query subordinate trust managers.
     *
     * <p>
     * While {@link SSLContext#init(KeyManager[], TrustManager[], SecureRandom)} <i>looks</i> like
     * it could cope with several trust managers, only the first trust manager of the array will be
     * used, ever, according to the Javadoc.
     * </p>
     *
     * <p>
     * Second, having an extra trust manager is inherent to validating a custom certificate at
     * runtime. We cannot rely on the fact that the custom certificate is already installed on the
     * system and therefore available via the system's trust manager. Also, it appears to be
     * impossible to obtain an instance of the system's trust manager and add custom certificates
     * after its creation.
     * </p>
     */
    private static final class CompositeTrustManager implements X509TrustManager {

        private final List<X509TrustManager> managers;

        private CompositeTrustManager(final List<TrustManager> managers) {
            assert !managers.isEmpty();
            this.managers = new ArrayList<>();
            for (final TrustManager manager : managers) {
                assert manager instanceof X509TrustManager;
                this.managers.add((X509TrustManager) manager);
            }
        }

        @Override
        public void checkClientTrusted(final X509Certificate[] chain, final String authType)
                        throws CertificateException {

            CertificateException lastError = null;
            for (final X509TrustManager manager : managers) {
                try {
                    manager.checkClientTrusted(chain, authType);
                    return;
                } catch (final CertificateException e) {
                    lastError = e;
                }
            }

            if (lastError != null) {
                throw lastError;
            }
        }

        @Override
        public void checkServerTrusted(final X509Certificate[] chain, final String authType)
                        throws CertificateException {

            CertificateException lastError = null;
            for (final X509TrustManager manager : managers) {
                try {
                    manager.checkServerTrusted(chain, authType);
                    return;
                } catch (final CertificateException e) {
                    lastError = e;
                }
            }

            if (lastError != null) {
                throw lastError;
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            final List<X509Certificate> certificates = new ArrayList<>();
            for (final X509TrustManager manager : managers) {
                certificates.addAll(asList(manager.getAcceptedIssuers()));
            }
            return certificates.toArray(new X509Certificate[0]);
        }
    }
}
