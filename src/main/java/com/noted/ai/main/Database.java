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
            cleanupDuplicateDocumentTitles(conn);
            stmt.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx_documents_title_unique ON documents(title)");
        }
    }

    private static void cleanupDuplicateDocumentTitles(Connection conn) throws SQLException {
        String deleteDuplicatesSql = "DELETE FROM documents WHERE id NOT IN ("
                + "SELECT id FROM ("
                + "SELECT id FROM documents d1 WHERE d1.id = ("
                + "SELECT d2.id FROM documents d2 WHERE d2.title = d1.title "
                + "ORDER BY d2.created_at DESC, d2.id DESC LIMIT 1)"
                + ")"
                + ")";

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(deleteDuplicatesSql);
        }
    }

    public static int saveDocument(String title, String content) throws SQLException {
        initialize();
        String sql = "INSERT INTO documents (title, content, created_at) VALUES (?, ?, CURRENT_TIMESTAMP) "
                + "ON CONFLICT(title) DO UPDATE SET content = excluded.content, created_at = CURRENT_TIMESTAMP";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, content);
            ps.executeUpdate();
        }

        return findLatestDocumentByTitle(title).getId();
    }

    public static void saveEmotion(int documentId, String emotion, int intensity) throws SQLException {
        initialize();
        String sql = "INSERT INTO emotions_intensity (id, emotion, intensity) VALUES (?, ?, ?) "
                + "ON CONFLICT(id) DO UPDATE SET emotion = excluded.emotion, intensity = excluded.intensity";

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
        initialize();
        String sql = "DELETE FROM documents WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public static void deleteDocumentByTitle(String title) throws SQLException {
        initialize();
        String sql = "DELETE FROM documents WHERE title = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.executeUpdate();
        }
    }

    public static List<Document> findAllDocuments() throws SQLException {
        List<Document> documents = new ArrayList<>();
        String sql = "SELECT id, title, content FROM documents d "
                + "WHERE id = (SELECT d2.id FROM documents d2 WHERE d2.title = d.title "
                + "ORDER BY d2.created_at DESC, d2.id DESC LIMIT 1) "
                + "ORDER BY created_at DESC, id DESC";

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

    public static Document findDocumentById(int id) throws SQLException {
        String sql = "SELECT id, title, content FROM documents WHERE id = ? LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Document(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("content")
                    );
                }
            }
        }

        return null;
    }

    public static Document findDocumentByTitle(String title) throws SQLException {
        String sql = "SELECT id, title, content FROM documents WHERE title = ? "
                + "ORDER BY created_at DESC, id DESC LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Document(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("content")
                    );
                }
            }
        }

        return null;
    }

    public static Document findLatestDocumentByTitle(String title) throws SQLException {
        return findDocumentByTitle(title);
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
