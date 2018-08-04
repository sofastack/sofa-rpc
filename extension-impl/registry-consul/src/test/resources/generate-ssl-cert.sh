#!/usr/bin/env bash

source $1

#CA Options
DNAME_CA="CN=test.org,O=TestOrgCA,L=London,C=UK"
CA_KEY_PASSWORD="change_me"
CA_KEY_STORE_PASSWORD="change_me"
CA_KEY_STORE="ca.keyStore.jks"
CA_TRUST_STORE="ca.trustStore.jks"
CA_TRUST_STORE_PASSWORD="change_me"
CA_CERT="ca.cert"
CA_KEY="custom-ca"

# Client Options
DNAME="CN=localBox,OU=OU,O=TestOrg,L=London,ST=Unknown,C=UK"
SAN=dns:localhost,ip:127.0.0.1,ip:::1
KEY_STORE="keyStore.jks"
TRUST_STORE="trustStore.jks"
KEY_ALIAS="key"
KEY_PASSWORD="change_me"
KEY_STORE_PASSWORD="change_me"
TRUST_STORE_PASSWORD="change_me"
CLIENT_CERT="client.p12"

function generate_certificate {
    # Make Dir if not exist
    mkdir -p ssl

    cd ssl

    rm $KEY_STORE $CA_CERT $TRUST_STORE $CA_KEY_STORE $CA_TRUST_STORE $CLIENT_CERT

    # generate cert authority
    echo "Generating CA Cert"
    keytool -genkey -alias $CA_KEY -ext BC=ca:true -dname "$DNAME_CA" -keyalg RSA -keysize 4096 -sigalg SHA512withRSA -keypass "$CA_KEY_PASSWORD" -validity 3650 -keystore "$CA_KEY_STORE" -storepass "$CA_KEY_STORE_PASSWORD"
    keytool -export -alias $CA_KEY -file "$CA_CERT"  -dname "$DNAME_CA" -rfc -keystore "$CA_KEY_STORE" -storepass "$CA_KEY_STORE_PASSWORD"
    keytool -import -trustcacerts -noprompt -alias $CA_KEY -file "$CA_CERT" -keystore $CA_TRUST_STORE -storepass "$CA_TRUST_STORE_PASSWORD"

    # generate host keystore

    echo "Generate Client Key"
    keytool -genkey -alias "$KEY_ALIAS" -dname "$DNAME" -keyalg RSA -keysize 4096 -sigalg SHA512withRSA -keypass "$KEY_PASSWORD" -validity 3650 -keystore "$KEY_STORE" -storepass "$KEY_STORE_PASSWORD"
    keytool -certreq -alias "$KEY_ALIAS" -ext BC=ca:true -ext "SAN=$SAN" -keyalg RSA -keysize 4096 -sigalg SHA512withRSA -keypass "$KEY_PASSWORD" -validity 3650 -keystore "$KEY_STORE" -storepass "$KEY_STORE_PASSWORD" -file request.csr
    keytool -gencert -alias $CA_KEY -validity 3650 -sigalg SHA512withRSA -ext "SAN=$SAN" -infile request.csr -outfile response.crt -rfc -keypass "$CA_KEY_PASSWORD" -keystore "$CA_KEY_STORE" -storepass "$CA_KEY_STORE_PASSWORD"
    keytool -import -trustcacerts -noprompt -alias $CA_KEY -file "$CA_CERT" -keystore "$KEY_STORE" -storepass "$KEY_STORE_PASSWORD"
    keytool -import -trustcacerts -alias "$KEY_ALIAS" -file response.crt -keypass "$KEY_PASSWORD" -keystore "$KEY_STORE" -storepass "$KEY_STORE_PASSWORD"
    keytool -import -trustcacerts -noprompt -alias $CA_KEY -file "$CA_CERT" -keystore "$TRUST_STORE" -storepass "$TRUST_STORE_PASSWORD"
    rm request.csr response.crt

    echo "Generate Client Cert"
    keytool -importkeystore -srckeystore "$KEY_STORE" -srcalias "$KEY_ALIAS" -srcstorepass "$KEY_PASSWORD" -destkeystore "$CLIENT_CERT" -deststoretype PKCS12 -deststorepass "$KEY_PASSWORD"

    openssl pkcs12 -in "$CLIENT_CERT" -nokeys -out key.crt -passin pass:"$KEY_PASSWORD"
    openssl pkcs12 -in "$CLIENT_CERT" -nodes -nocerts -out key.key -passin pass:"$KEY_PASSWORD"
}


echo "Checking if certificate are already generated or not?"

if [ ! -f ssl/keyStore.jks ];  then
    generate_certificate
else
    echo "No need to create certificate"
fi
