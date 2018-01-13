import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.Lock;

/**
 * A Token Bucket (https://en.wikipedia.org/wiki/Token_bucket)
 *
 * This thread-safe bucket should support the following methods:
 *
 * - take(n): remove n tokens from the bucket (blocks until n tokens are available and taken)
 * - set(n): set the bucket to contain n tokens (to allow "hard" rate limiting)
 * - add(n): add n tokens to the bucket (to allow "soft" rate limiting)
 * - terminate(): mark the bucket as terminated (used to communicate between threads)
 * - terminated(): return true if the bucket is terminated, false otherwise
 *
 */
class TokenBucket {
    long bucketSize;
    long numOfTokens;
    Lock bucketLock;
    boolean terminated;

    TokenBucket(long bucketSize) {
        this.bucketSize = bucketSize;
        this.numOfTokens = 0;
        this.terminated = false;
    }

    void take(long tokens) {
        bucketLock.lock();
        try {
            this.numOfTokens = tokens > numOfTokens ? numOfTokens : tokens;
        } finally {
            bucketLock.unlock();
        }
    }


    //TODO add
    void add(long tokens) {
        bucketLock.lock();
        try {
            long totalTokens = tokens + numOfTokens;
            this.numOfTokens = totalTokens > bucketSize ? bucketSize : totalTokens;
        } finally {
            bucketLock.unlock();
        }
    }

    //TODO should these be locked as well?
    void terminate() {
        this.terminated = true;
    }

    boolean terminated() {
        return terminated;
    }

    void set(long tokens) {
        bucketLock.lock();
        try {
            this.numOfTokens = tokens;
        } finally {
            bucketLock.unlock();
        }
    }
}
