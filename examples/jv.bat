@ECHO OFF
if not exist ".jv" mkdir .jv
java -jar -Djava.util.logging.config.file=logging.properties jvaluer-cli-all-1.0.jar %*