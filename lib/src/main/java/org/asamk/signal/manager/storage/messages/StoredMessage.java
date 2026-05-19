package org.asamk.signal.manager.storage.messages;

import java.util.Objects;

/**
 * Immutable data class representing a stored message in the message_store table.
 */
public class StoredMessage {

    private final long id;
    private final String threadId;
    private final String senderAddress;
    private final String recipientAddress;
    private final long timestamp;
    private final String body;
    private final int messageType;
    private final String groupId;
    private final String envelopeMetadata;
    private final long createdAt;

    public StoredMessage(
            long id,
            String threadId,
            String senderAddress,
            String recipientAddress,
            long timestamp,
            String body,
            int messageType,
            String groupId,
            String envelopeMetadata,
            long createdAt
    ) {
        this.id = id;
        this.threadId = threadId;
        this.senderAddress = senderAddress;
        this.recipientAddress = recipientAddress;
        this.timestamp = timestamp;
        this.body = body;
        this.messageType = messageType;
        this.groupId = groupId;
        this.envelopeMetadata = envelopeMetadata;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getThreadId() {
        return threadId;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public String getRecipientAddress() {
        return recipientAddress;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getBody() {
        return body;
    }

    public int getMessageType() {
        return messageType;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getEnvelopeMetadata() {
        return envelopeMetadata;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoredMessage that = (StoredMessage) o;
        return id == that.id
                && timestamp == that.timestamp
                && messageType == that.messageType
                && createdAt == that.createdAt
                && Objects.equals(threadId, that.threadId)
                && Objects.equals(senderAddress, that.senderAddress)
                && Objects.equals(recipientAddress, that.recipientAddress)
                && Objects.equals(body, that.body)
                && Objects.equals(groupId, that.groupId)
                && Objects.equals(envelopeMetadata, that.envelopeMetadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id,
                threadId,
                senderAddress,
                recipientAddress,
                timestamp,
                body,
                messageType,
                groupId,
                envelopeMetadata,
                createdAt);
    }

    @Override
    public String toString() {
        return "StoredMessage{"
                + "id=" + id
                + ", threadId='" + threadId + '\''
                + ", senderAddress='" + senderAddress + '\''
                + ", recipientAddress='" + recipientAddress + '\''
                + ", timestamp=" + timestamp
                + ", body='" + body + '\''
                + ", messageType=" + messageType
                + ", groupId='" + groupId + '\''
                + ", envelopeMetadata='" + envelopeMetadata + '\''
                + ", createdAt=" + createdAt
                + '}';
    }
}
