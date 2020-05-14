@ECHO OFF
IF EXIST "client.B._jar_" (
    IF EXIST client.B.jar (
        ECHO "deleting client.B.jar"
        DEL client.B.jar
    )
    ECHO "rename _jar_ to jar"
    RENAME client.B._jar_ client.B.jar
)
ECHO "running jar"
java -jar client.B.jar
PAUSE
