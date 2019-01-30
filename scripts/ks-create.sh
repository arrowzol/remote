#!/bin/sh

openssl pkcs12 -export \
    -in ../certs/ca.cert.pem -inkey ../private/ca.key.pem \
    -out caroot.p12 -name caroot

openssl pkcs12 -export \
    -in ../intermediate/certs/intermediate.cert.pem -inkey ../intermediate/private/intermediate.key.pem \
    -out intermediate.p12 -name caintermediate \
    -CAfile ../certs/ca.cert.pem -caname caroot

openssl pkcs12 -export \
    -in ../intermediate/certs/zounds.mooo.com.cert.pem -inkey ../intermediate/private/zounds.mooo.com.key.pem \
    -out zounds.p12 -name zounds \
    -CAfile ../intermediate/certs/intermediate.cert.pem -caname caintermediate

keytool -importkeystore \
        -deststorepass changeit -destkeypass changeit -destkeystore server.keystore \
        -srckeystore caroot.p12 -srcstoretype PKCS12 -srcstorepass nopal92040 \
        -alias caroot

exit

keytool -importkeystore \
        -deststorepass changeit -destkeypass changeit -destkeystore server.keystore \
        -srckeystore intermediate.p12 -srcstoretype PKCS12 -srcstorepass nopal92040 \
        -alias intermediate

keytool -importkeystore \
        -deststorepass changeit -destkeypass changeit -destkeystore server.keystore \
        -srckeystore zounds.p12 -srcstoretype PKCS12 -srcstorepass nopal92040 \
        -alias zounds
