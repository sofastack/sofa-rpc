/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
