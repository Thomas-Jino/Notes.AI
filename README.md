# Noted.AI Swing Sample

Minimal Java Swing sample using FlatLaf for theming and SQLite (sqlite-jdbc) for a tiny embedded DB.

Quick commands:

```bash
# build
mvn -v
mvn clean compile

# run
mvn exec:java -Dexec.mainClass="com.noted.ai.main.Main"
```

Files:
- `src/main/java/com/noted/ai/main/Main.java` - sample Swing app
- `pom.xml` - Maven project file with dependencies
