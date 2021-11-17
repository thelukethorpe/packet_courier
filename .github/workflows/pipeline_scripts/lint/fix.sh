# Install Maven dependencies.
echo "Resolving Maven plugins..."
mvn dependency:resolve-plugins -T1C > /dev/null

# Run Maven style format.
echo "Fixing style..."
mvn com.coveo:fmt-maven-plugin:format -q
