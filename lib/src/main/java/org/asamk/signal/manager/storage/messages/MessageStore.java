package org.asamk.signal.manager.storage.messages;

import org.asamk.signal.manager.storage.Database;
import org.asamk.signal.manager.storage.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MessageStore {

    private static final Logger logger = LoggerFactory.getLogger(MessageStore.class);
    private static final String TABLE_MESSAGE_STORE = "message_store";

    private final Database database;

    public MessageStore(Database database) {
        this.database = database;
    }

    public static void createSql(Connection connection) throws SQLException {
        try (final var statement = connection.createStatement()) {
            statement.executeUpdate("""
                                    CREATE TABLE message_store (
                                      _id INTEGER PRIMARY KEY AUTOINCREMENT,
                                      thread_id TEXT NOT NULL,
                                      sender_address TEXT NOT NULL,
                                      recipient_address TEXT,
                                      timestamp INTEGER NOT NULL,
                                      body TEXT,
                                      message_type INTEGER NOT NULL CHECK (message_type IN (0, 1)),
                                      group_id TEXT,
                                      envelope_metadata TEXT NOT NULL DEFAULT '{}',
                                      created_at INTEGER NOT NULL
                                    ) STRICT;
                                    """);
            statement.executeUpdate("CREATE INDEX idx_message_timestamp ON message_store(timestamp)");
            statement.executeUpdate("CREATE INDEX idx_message_thread ON message_store(thread_id)");
            statement.executeUpdate("CREATE INDEX idx_message_sender ON message_store(sender_address)");
        }
    }

    public void storeMessage(
            String threadId,
            String senderAddress,
            String recipientAddress,
            long timestamp,
            String body,
            int messageType,
            String groupId,
            String envelopeMetadata
    ) throws SQLException {
        final var sql = """
                          INSERT INTO %s (thread_id, sender_address, recipient_address, timestamp, body, message_type, group_id, envelope_metadata, created_at)
                          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                          """.formatted(TABLE_MESSAGE_STORE);
        try (final var connection = database.getConnection()) {
            try (final var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, threadId);
                statement.setString(2, senderAddress);
                statement.setString(3, recipientAddress);
                statement.setLong(4, timestamp);
                statement.setString(5, body);
                statement.setInt(6, messageType);
                statement.setString(7, groupId);
                statement.setString(8, envelopeMetadata != null ? envelopeMetadata : "{}");
                statement.setLong(9, System.currentTimeMillis());
                statement.executeUpdate();
            }
        }
    }

    public List<StoredMessage> getMessages(String threadId, int limit, int offset) throws SQLException {
        final var sql = """
                          SELECT _id, thread_id, sender_address, recipient_address, timestamp, body, message_type, group_id, envelope_metadata, created_at
                          FROM %s
                          WHERE thread_id = ?
                          ORDER BY timestamp DESC
                          LIMIT ? OFFSET ?
                          """.formatted(TABLE_MESSAGE_STORE);
        final List<StoredMessage> messages = new ArrayList<>();
        try (final var connection = database.getConnection()) {
            try (final var statement = connection.prepareStatement(sql)) {
                statement.setString(1, threadId);
                statement.setInt(2, limit);
                statement.setInt(3, offset);
                try (var resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        messages.add(mapResultSetToStoredMessage(resultSet));
                    }
                }
            }
        }
        return messages;
    }

    public Optional<StoredMessage> getMessageById(long id) throws SQLException {
        final var sql = """
                          SELECT _id, thread_id, sender_address, recipient_address, timestamp, body, message_type, group_id, envelope_metadata, created_at
                          FROM %s
                          WHERE _id = ?
                          """.formatted(TABLE_MESSAGE_STORE);
        try (final var connection = database.getConnection()) {
            try (final var statement = connection.prepareStatement(sql)) {
                statement.setLong(1, id);
                return Utils.executeQueryForOptional(statement, this::mapResultSetToStoredMessage);
            }
        }
    }

    public void deleteMessage(long id) throws SQLException {
        final var sql = "DELETE FROM %s WHERE _id = ?".formatted(TABLE_MESSAGE_STORE);
        try (final var connection = database.getConnection()) {
            try (final var statement = connection.prepareStatement(sql)) {
                statement.setLong(1, id);
                statement.executeUpdate();
            }
        }
    }

    private StoredMessage mapResultSetToStoredMessage(ResultSet resultSet) throws SQLException {
        return new StoredMessage(
                resultSet.getLong("_id"),
                resultSet.getString("thread_id"),
                resultSet.getString("sender_address"),
                resultSet.getString("recipient_address"),
                resultSet.getLong("timestamp"),
                resultSet.getString("body"),
                resultSet.getInt("message_type"),
                resultSet.getString("group_id"),
                resultSet.getString("envelope_metadata"),
                resultSet.getLong("created_at")
        );
    }
}
