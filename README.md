# Troubleshooting guide
1. Build jar file
2. Remove `META-INF/*.DSA, META-INF/*.SF` files from jar file
3. Run `java -jar <jar-file-name> <Ethereum private key> <SORA did> <Comma separated list of SORA transactions hashes>` Arguments should be specified without <>
