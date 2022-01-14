package me.lean.tech.dev;

import nl.altindag.ssl.util.CertificateUtils;
import nl.altindag.ssl.util.PemUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
//import org.springframework.core.io.ClassPathResource;

import javax.naming.ConfigurationException;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Properties;

public class Mtls {

    private static final String KEY_PASSWORD = "myAppKeystorePassword";

    /**
     * How to:
     * - Create a p12 file from the certificates
     * - Go to Lean's dev portal
     * - Select "Integration" from the left side
     * - Download the Certification Authority give it a name, ex: ca
     * - Download the public/private keys zip file by clicking on "Download New Certificate"
     * - Extract the zip file, rename the private key to key.pem, rename the public key to cert.crt
     * - Copy all the certificates in a new folder, name it /certs
     * - Run the following command, you will be asked to put on a password, please remember it
     * ```bash
     * openssl pkcs12 -export -in cert.crt  -inkey key.pem  -certfile ca.pem -out yourp12filename.p12
     * ```
     * - A new file will be generated called yourp12filename.p12, move it to the resources folder
     * - Replace the "p12.filename", "p12.password", "app.token" dummy properties with your own.
     * - Run the main class, you should receive "HTTP/1.1 200" and a valid response in the console.
     *
     */

    public static void main(String[] args) throws IOException, GeneralSecurityException, ConfigurationException {
        Properties props = new Properties();
        props.load(Mtls.class.getClassLoader().getResourceAsStream("application.properties"));

        // creating SSL context and passing in the p12 cert store and password
//        SSLContext sslContext =
//                SSLContextBuilder.create().loadTrustMaterial(TrustAllStrategy.INSTANCE)
//                        .loadKeyMaterial(new ClassPathResource(props.getProperty("p12.filename")).getFile(),
//                                props.getProperty("p12.passowrd").toCharArray(),
//                                props.getProperty("p12.passowrd").toCharArray())
//                        .build();

        String certificatePath = props.getProperty("lean.certificates.path");
        String crtCertificateName = props.getProperty("lean.certificate.name");
        String privateKeyName = props.getProperty("lean.private.key.name");
        String publicKeyName = props.getProperty("lean.public.key.name");
        Path crtCertificatePath = Paths.get(certificatePath, crtCertificateName);
        Path privateKeyPath = Paths.get(certificatePath, privateKeyName);
        Path publicKeyPath = Paths.get(certificatePath, publicKeyName);

        KeyStore keyStore = createKeyStoreUsingHelperLib(crtCertificatePath, privateKeyPath, publicKeyPath);
        SSLContext sslContext = buildSSLContext(keyStore);
        callUsingApacheClient(sslContext, props);

        callUsingRestTemplate(sslContext, props);
    }

    private static void callUsingRestTemplate(SSLContext sslContext, Properties props) {

    }

//    private static KeyStore createKeyStoreUsingOpenSSL(Path certificatePath, Path clientCrtCertificate, Path privateKeyPem, Path publicKeyPem) throws ConfigurationException, GeneralSecurityException, IOException {
//        Runtime rt = Runtime.getRuntime();
//        Path outputPkcsFile = Paths.get(certificatePath.toString(), "new-keyStore-"+ UUID.randomUUID()+".p12");
//        String createKeyStoreCommand = String.format("openssl pkcs12 -export -in %s -inkey %s -certfile %s -out %s", clientCrtCertificate, privateKeyPem, publicKeyPem, outputPkcsFile.toAbsolutePath());
//        Process pr = rt.exec(createKeyStoreCommand);
//
//        KeyStore keyStore = KeyStore.getInstance("pkcs12");
//        keyStore.load(new FileInputStream(outputPkcsFile.toString()), KEY_PASSWORD.toCharArray());
//
//        return keyStore;
//    }

    private static KeyStore createKeyStoreUsingHelperLib(Path clientCrtCertificate, Path privateKeyPem, Path publicKeyPem) throws ConfigurationException, GeneralSecurityException, IOException {
        List<Certificate> certificates = CertificateUtils.loadCertificate(clientCrtCertificate, publicKeyPem);
        PrivateKey privateKey = PemUtils.loadPrivateKey(privateKeyPem);

        KeyStore keyStore = KeyStore.getInstance("pkcs12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("client-key", privateKey, KEY_PASSWORD.toCharArray(), certificates.toArray(new Certificate[3]));

        return keyStore;
    }

    private static void callUsingApacheClient( SSLContext sslContext, Properties props) throws IOException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create().setSSLContext(sslContext);

        try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
            String baseUrl = props.getProperty("lean.base.url");
            String uri = baseUrl + "/banks/v1/";
            HttpGet request = new HttpGet(uri);
            // add request headers
            request.addHeader("lean-app-token", props.getProperty(props.getProperty("p12.filename")));
            request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                // Get HttpResponse Status
                System.out.println(response.getStatusLine().toString()); // HTTP/1.1 200 OK
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    // return it as a String
                    String result = EntityUtils.toString(entity);
                    System.out.println(result);
                } else {
                    System.out.println("Unexpected error: unable to retrieve response");
                }
            }
        }
    }

    private static SSLContext buildSSLContext(KeyStore keyStore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        return SSLContextBuilder.create()
                        .loadKeyMaterial(keyStore, KEY_PASSWORD.toCharArray())
                        .loadTrustMaterial(new TrustAllStrategy())
                        .setProtocol("TLSv1.3")
                        .build();
    }
}
