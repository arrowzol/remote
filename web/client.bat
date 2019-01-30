@ECHO OFF
IF EXIST "client._jar_" (
    IF EXIST client.jar (
        ECHO "deleting client.jar"
        DEL client.jar
    )
    ECHO "rename _jar_ to jar"
    RENAME client._jar_ client.jar
)
ECHO "running jar"
java -jar client.jar
PAUSE
