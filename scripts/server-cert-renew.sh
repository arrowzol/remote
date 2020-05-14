#!/bin/sh

NAME=zounds.mooo.com
VERSION=2


# generate new server key

echo "GENERATE SERVER CERT"

openssl genrsa \
    -aes256 \
    -out intermediate/private/${NAME}.${VERSION}.key.pem \
    2048

chmod 400 intermediate/private/${NAME}.${VERSION}.key.pem


# generate new server CSR, connected to the new server key

echo "GENERATE CSR"

openssl req -config intermediate/openssl.cnf \
    -key intermediate/private/${NAME}.${VERSION}.key.pem \
    -new -sha256 -out intermediate/csr/${NAME}.${VERSION}.csr.pem


# CA intermediate cert signs new server cert, expires in 8 years (I tried 1, but a year later I regreted that)

echo "INTERMEDIATE SIGNS CSR"

openssl ca -config intermediate/openssl.cnf \
    -extensions server_cert -days 2920 -notext -md sha256 \
    -in intermediate/csr/${NAME}.${VERSION}.csr.pem \
    -out intermediate/certs/${NAME}.${VERSION}.cert.pem

chmod 444 intermediate/certs/${NAME}.${VERSION}.cert.pem


# verify

echo "VERIFY"

openssl x509 -noout -text \
    -in intermediate/certs/${NAME}.${VERSION}.cert.pem

openssl x509 -noout -text \
    -in intermediate/certs/${NAME}.${VERSION}.cert.pem

