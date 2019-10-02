package com.alipay.sofa.rpc.transport.http;

import java.io.File;
import java.security.cert.CertificateException;

/**
 * Set path for certificate and privateKey separately
 * 
 * @author <a href="mailto:466178395@qq.com">LiHao</a>
 */
public final class SelfSignedCer {

    private final File certificate;
    private final File privateKey;

    /**
     * initiate a new class.
     *
     * @param certificatePath
     *            path of certificate
     * @param privateKeyPath
     *            path of privateKey
     */
    public SelfSignedCer(String certificatePath, String privateKeyPath) throws CertificateException {

        certificate = new File(certificatePath);
        privateKey = new File(privateKeyPath);
    }

    /**
     * Returns the generated X.509 certificate file in PEM format.
     */
    public File certificate() {
        return certificate;
    }

    /**
     * Returns the generated RSA private key file in PEM format.
     */
    public File privateKey() {
        return privateKey;
    }

}
