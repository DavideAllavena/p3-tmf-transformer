package eu.fusepool.p3.transformer.tmf;

/**
 * TooManyRequests is thrown when a transformer has too many pending requests to process.
 */
public class TooManyRequests extends RuntimeException {
    public TooManyRequests(String message) {
        super(message);
    }
}
