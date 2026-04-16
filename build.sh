#!/bin/bash

javac --module-path javafx/lib/ --add-modules javafx.controls -Xlint:unchecked Main.java
java --module-path javafx/lib --add-modules javafx.controls Main
