#!/bin/bash

echo "📦 Downloading AuthBridge Dependencies..."
mkdir -p libs
cd libs

echo "⬇️  Downloading Velocity API..."
curl -L -o velocity-api-3.1.0.jar https://repo.papermc.io/repository/maven-public/com/velocitypowered/velocity-api/3.1.0/velocity-api-3.1.0.jar 2>/dev/null || wget -q https://repo.papermc.io/repository/maven-public/com/velocitypowered/velocity-api/3.1.0/velocity-api-3.1.0.jar

echo "⬇️  Downloading Configurate..."
curl -L -o configurate-core-4.1.2.jar https://repo.codemc.io/repository/maven-public/org/spongepowered/configurate-core/4.1.2/configurate-core-4.1.2.jar 2>/dev/null || wget -q https://repo.codemc.io/repository/maven-public/org/spongepowered/configurate-core/4.1.2/configurate-core-4.1.2.jar
curl -L -o configurate-yaml-4.1.2.jar https://repo.codemc.io/repository/maven-public/org/spongepowered/configurate-yaml/4.1.2/configurate-yaml-4.1.2.jar 2>/dev/null || wget -q https://repo.codemc.io/repository/maven-public/org/spongepowered/configurate-yaml/4.1.2/configurate-yaml-4.1.2.jar

echo "⬇️  Downloading SLF4J..."
curl -L -o slf4j-api-1.7.36.jar https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar 2>/dev/null || wget -q https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar

echo "⬇️  Downloading Adventure..."
curl -L -o adventure-api-4.12.0.jar https://repo1.maven.org/maven2/net/kyori/adventure-api/4.12.0/adventure-api-4.12.0.jar 2>/dev/null || wget -q https://repo1.maven.org/maven2/net/kyori/adventure-api/4.12.0/adventure-api-4.12.0.jar
curl -L -o adventure-text-serializer-legacy-4.12.0.jar https://repo1.maven.org/maven2/net/kyori/adventure-text-serializer-legacy/4.12.0/adventure-text-serializer-legacy-4.12.0.jar 2>/dev/null || wget -q https://repo1.maven.org/maven2/net/kyori/adventure-text-serializer-legacy/4.12.0/adventure-text-serializer-legacy-4.12.0.jar

echo "⬇️  Downloading Additional Dependencies..."
curl -L -o snakeyaml-2.0.jar https://repo1.maven.org/maven2/org/yaml/snakeyaml/2.0/snakeyaml-2.0.jar 2>/dev/null || wget -q https://repo1.maven.org/maven2/org/yaml/snakeyaml/2.0/snakeyaml-2.0.jar
curl -L -o javax.inject-1.jar https://repo1.maven.org/maven2/javax/inject/javax.inject/1/javax.inject-1.jar 2>/dev/null || wget -q https://repo1.maven.org/maven2/javax/inject/javax.inject/1/javax.inject-1.jar

cd ..
echo "✅ Dependencies downloaded!"
ls -lh libs/
