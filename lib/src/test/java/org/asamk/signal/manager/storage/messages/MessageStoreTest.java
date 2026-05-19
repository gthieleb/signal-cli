package org.asamk.signal.manager.storage.messages;

import org.asamk.signal.manager.storage.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageStoreTest {

    private Connection connection;
    private MessageStore messageStore;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
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
            stmt.executeUpdate("CREATE INDEX idx_message_timestamp ON message_store(timestamp)");
            stmt.executeUpdate("CREATE INDEX idx_message_thread ON message_store(thread_id)");
            stmt.executeUpdate("CREATE INDEX idx_message_sender ON message_store(sender_address)");
        }

        Database database = new Database(org.slf4j.LoggerFactory.getLogger(MessageStoreTest.class), 1, null) {
            @Override
            public Connection getConnection() {
                return connection;
            }

            @Override
            public void close() {
                try {
                    if (connection != null && !connection.isClosed()) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to close connection", e);
                }
            }

            @Override
            protected void createDatabase(Connection connection) {}

            @Override
            protected void upgradeDatabase(Connection connection, long oldVersion) {}
        };

        messageStore = new MessageStore(database);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    void testStoreAndRetrieveMessage() throws SQLException {
        messageStore.storeMessage(
                "thread-1",
                "+1234567890",
                "+0987654321",
                1234567890L,
                "Hello, World!",
                0,
                null,
                "{}"
        );

        List<StoredMessage> messages = messageStore.getMessages("thread-1", 10, 0);
        assertEquals(1, messages.size());

        StoredMessage msg = messages.get(0);
        assertEquals("thread-1", msg.getThreadId());
        assertEquals("+1234567890", msg.getSenderAddress());
        assertEquals("+0987654321", msg.getRecipientAddress());
        assertEquals(1234567890L, msg.getTimestamp());
        assertEquals("Hello, World!", msg.getBody());
        assertEquals(0, msg.getMessageType());
        assertEquals("{}", msg.getEnvelopeMetadata());
        assertTrue(msg.getId() > 0);
        assertTrue(msg.getCreatedAt() > 0);
    }

    @Test
    void testGetMessageById() throws SQLException {
        messageStore.storeMessage(
                "thread-1",
                "+1234567890",
                null,
                1234567890L,
                "Test message",
                1,
                null,
                "{\"key\":\"value\"}"
        );

        List<StoredMessage> messages = messageStore.getMessages("thread-1", 10, 0);
        assertEquals(1, messages.size());
        long id = messages.get(0).getId();

        Optional<StoredMessage> found = messageStore.getMessageById(id);
        assertTrue(found.isPresent());
        assertEquals("Test message", found.get().getBody());
        assertEquals("{\"key\":\"value\"}", found.get().getEnvelopeMetadata());
    }

    @Test
    void testGetMessageById_NotFound() throws SQLException {
        Optional<StoredMessage> found = messageStore.getMessageById(99999L);
        assertFalse(found.isPresent());
    }

    @Test
    void testPagination() throws SQLException {
        for (int i = 0; i < 5; i++) {
            messageStore.storeMessage(
                    "thread-paginate",
                    "+1111111111",
                    "+2222222222",
                    1000L + i,
                    "Message " + i,
                    0,
                    null,
                    "{}"
            );
        }

        List<StoredMessage> page1 = messageStore.getMessages("thread-paginate", 2, 0);
        assertEquals(2, page1.size());
        assertEquals("Message 4", page1.get(0).getBody());
        assertEquals("Message 3", page1.get(1).getBody());

        List<StoredMessage> page2 = messageStore.getMessages("thread-paginate", 2, 2);
        assertEquals(2, page2.size());
        assertEquals("Message 2", page2.get(0).getBody());
        assertEquals("Message 1", page2.get(1).getBody());

        List<StoredMessage> page3 = messageStore.getMessages("thread-paginate", 2, 4);
        assertEquals(1, page3.size());
        assertEquals("Message 0", page3.get(0).getBody());
    }

    @Test
    void testDeleteMessage() throws SQLException {
        messageStore.storeMessage(
                "thread-delete",
                "+1234567890",
                "+0987654321",
                1234567890L,
                "To be deleted",
                0,
                null,
                "{}"
        );

        List<StoredMessage> messages = messageStore.getMessages("thread-delete", 10, 0);
        assertEquals(1, messages.size());
        long id = messages.get(0).getId();

        messageStore.deleteMessage(id);

        List<StoredMessage> afterDelete = messageStore.getMessages("thread-delete", 10, 0);
        assertEquals(0, afterDelete.size());

        Optional<StoredMessage> found = messageStore.getMessageById(id);
        assertFalse(found.isPresent());
    }

    @Test
    void testStoreMessageWithGroupId() throws SQLException {
        messageStore.storeMessage(
                "thread-group",
                "+1234567890",
                null,
                1234567890L,
                "Group message",
                1,
                "group-id-123",
                "{}"
        );

        List<StoredMessage> messages = messageStore.getMessages("thread-group", 10, 0);
        assertEquals(1, messages.size());
        assertEquals("group-id-123", messages.get(0).getGroupId());
    }

    @Test
    void testGetMessagesFiltersByThreadId() throws SQLException {
        messageStore.storeMessage("thread-a", "+1", null, 1000L, "Message A", 0, null, "{}");
        messageStore.storeMessage("thread-b", "+2", null, 1001L, "Message B", 0, null, "{}");
        messageStore.storeMessage("thread-a", "+1", null, 1002L, "Message A2", 0, null, "{}");

        List<StoredMessage> threadA = messageStore.getMessages("thread-a", 10, 0);
        assertEquals(2, threadA.size());

        List<StoredMessage> threadB = messageStore.getMessages("thread-b", 10, 0);
        assertEquals(1, threadB.size());
        assertEquals("Message B", threadB.get(0).getBody());
    }
}
