import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
        this.bucketLock = new ReentrantLock(true);
    }

    /**
     * removes tokens from the bucket every second
     * @param tokens
     */
    void take(long tokens) {
        boolean done = false;
        while(!done) {
            bucketLock.lock();
            try {
                // checks if there are enough tokens in the bucket
                if (numOfTokens - tokens >= 0) {
                    numOfTokens -= tokens;
                    done = true;
                }
            } finally {
                bucketLock.unlock();
            }

            if(!done){
                try {
                    Thread.sleep(200);
                }
                catch (InterruptedException e){

                }
            }
        }
    }

    /**
     * adds tokens to the bucket
     * @param tokens
     */
    void add(long tokens) {
        bucketLock.lock();
        try {
            long totalTokens = tokens + numOfTokens;
            this.numOfTokens = totalTokens > bucketSize ? bucketSize : totalTokens;
        } finally {
            bucketLock.unlock();
        }
    }

    /**
     * terminates TokenBucket
     */
    void terminate() {
        this.terminated = true;
    }

    /**
     * Checks if TokenBucket is terminated
     * @return answer
     */
    boolean terminated() {
        return terminated;
    }

    /**
     * Sets the TokenBucket to a given number of tokens
     * @param tokens
     */
    void set(long tokens) {
        bucketLock.lock();
        try {
            this.numOfTokens = tokens;
        } finally {
            bucketLock.unlock();
        }
    }
}
