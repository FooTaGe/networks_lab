/**
 * A token bucket based rate-limiter.
 *
 * This class should implement a "soft" rate limiter by adding maxBytesPerSecond tokens to the bucket every second,
 * or a "hard" rate limiter by resetting the bucket to maxBytesPerSecond tokens every second.
 */
public class RateLimiter implements Runnable {
    private final TokenBucket tokenBucket;
    private final Long maxBps;

    RateLimiter(TokenBucket tokenBucket, Long maxBps) {
        this.tokenBucket = tokenBucket;
        this.maxBps = maxBps;
    }

    @Override
    public void run() {
        if (maxBps == null){ //*no limitation*: using hard rate limiter to refill bucket size every second
            while(tokenBucket.terminated == false){
                try {
                    Thread.sleep(1000); //wait one second
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    //TODO print something
                }
                tokenBucket.set(tokenBucket.bucketSize);
            }

        } else { //*limitation*: using soft rate limiter to add maxBps every second
            while(tokenBucket.terminated == false){
                try {
                    Thread.sleep(1000); //wait one second
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                tokenBucket.add(maxBps);
            }
        }
    }
}
