JAVAFX_LIB="/Users/jorgini/Desktop/javafx-sdk-22.0.1_arch/lib/"

java -Dprism.order=sw -Djava.awt.headless=true --module-path $JAVAFX_LIB:jars/server_gui-1.0-SNAPSHOT.jar:jars/producer-1.0-SNAPSHOT.jar -m ru.hse.server_gui/ru.hse.server_gui.ConfigApplication