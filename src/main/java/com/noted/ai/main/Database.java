package com.noted.ai.main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private static final String URL = "jdbc:sqlite:noted_ai.db";
    private static Connection connection;

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL);
        }
        return connection;
    }

    public static void initialize() throws SQLException {
        String documentSql = "CREATE TABLE IF NOT EXISTS documents ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "title TEXT NOT NULL,"
                + "content TEXT NOT NULL,"
                + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP"
                + ")";

        String emotionSql = "CREATE TABLE IF NOT EXISTS emotions_intensity ("
                + "id INTEGER PRIMARY KEY,"
                + "emotion TEXT NOT NULL,"
                + "intensity INTEGER NOT NULL CHECK (intensity BETWEEN 1 AND 10),"
                + "FOREIGN KEY (id) REFERENCES documents(id) ON DELETE CASCADE"
                + ")";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(documentSql);
            stmt.executeUpdate(emotionSql);
        }
    }

    public static int saveDocument(String title, String content) throws SQLException {
        initialize();
        String sql = "INSERT INTO documents (title, content) VALUES (?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, content);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        throw new SQLException("Failed to generate a document id.");
    }

    public static void saveEmotion(int documentId, String emotion, int intensity) throws SQLException {
        initialize();
        String sql = "INSERT INTO emotions_intensity (id, emotion, intensity) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, documentId);
            ps.setString(2, emotion);
            ps.setInt(3, intensity);
            ps.executeUpdate();
        }
    }

    public static void updateDocument(int id, String title, String content) throws SQLException {
        String sql = "UPDATE documents SET title = ?, content = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, content);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public static void deleteDocument(int id) throws SQLException {
        String sql = "DELETE FROM documents WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public static List<Document> findAllDocuments() throws SQLException {
        List<Document> documents = new ArrayList<>();
        String sql = "SELECT id, title, content FROM documents ORDER BY id DESC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                documents.add(new Document(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("content")
                ));
            }
        }

        return documents;
    }

    public static void main(String[] args) {
        try {
            Database.initialize();
            int documentId = Database.saveDocument("Example Note", "Hello from SQLite JDBC!");
            Database.saveEmotion(documentId, "Happy", 9);

            for (Document doc : Database.findAllDocuments()) {
                System.out.println(doc.getId() + " | " + doc.getTitle());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static final class Document {
        private final int id;
        private final String title;
        private final String content;

        public Document(int id, String title, String content) {
            this.id = id;
            this.title = title;
            this.content = content;
        }

        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }
    }
}
