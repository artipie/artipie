# JKS:
keytool -genkey -alias test -keyalg RSA -keystore keystore.jks -keysize 2048 -validity 7200 -dname CN=localhost -keypass secret -storepass secret

#####################################################################
# PEM:
#   1. RSA private key generation:
openssl genrsa -out private-key.pem 3072
#   2. Create RSA public key:
openssl rsa -in private-key.pem -pubout -out public-key.pem
#   3. Creating an RSA Self-Signed Certificate
#        Note: Common Name should be 'localhost'
openssl req -new -x509 -key private-key.pem -out cert.pem -days 7200

#Example:
# openssl req -new -x509 -key private-key.pem -out cert.pem -days 7200
# Country Name (2 letter code) [AU]:
# State or Province Name (full name) [Some-State]:
# Locality Name (eg, city) []:
# Organization Name (eg, company) [Internet Widgits Pty Ltd]:
# Organizational Unit Name (eg, section) []:
# Common Name (e.g. server FQDN or YOUR name) []:localhost
# Email Address []:

#   4. Concat private and public pem files to one file:
cat private-key.pem  public-key.pem > keys.pem

#####################################################################
# PFX:
openssl pkcs12 -export -inkey private-key.pem -in cert.pem -out cert.pfx

Example:
openssl pkcs12 -export -inkey private-key.pem -in cert.pem -out cert.pfx
Enter Export Password:secret
Verifying - Enter Export Password:secret