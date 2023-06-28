package blue.lhf.filcon;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;

public class PollingFileWatcher extends Thread implements Closeable {
    private static final MessageDigest MESSAGE_DIGEST;

    static {
        final String algorithm = System.getProperty("filcon.digest", "MD5");
        try {
            MESSAGE_DIGEST = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No such algorithm: " + algorithm, e);
        }
    }

    private final Duration interval;
    private final Path path;
    private byte[] lastDigest = null;
    private boolean closed;
    private final Runnable callback;

    static class Builder {
        private final Path path;
        private Duration interval;
        
        private Builder(final Path path) {
            this.path = path;
        }
        
        public Builder every(final long amount, final TemporalUnit unit) {
            this.interval = Duration.of(amount, unit);
            return this;
        }
        
        public PollingFileWatcher ifChanged(final Runnable callback) {
            return new PollingFileWatcher(interval, path, callback);
        }
    }
    
    public static Builder check(final Path path) {
        return new Builder(path);
    }
    
    public PollingFileWatcher(final Duration interval, final Path path, final Runnable callback) {
        this.interval = interval;
        this.path = path;
        this.callback = callback;
    }
    

    @Override
    public void run() {
        try {
            while (!closed) {
                Thread.sleep(interval.toSeconds() * 1000, interval.toNanosPart());
                final byte[] digest = digest(path);
                if (Arrays.equals(digest, lastDigest)) continue;
                lastDigest = digest;
                this.callback.run();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        closed = true;
    }

    private byte[] digest(final Path path) {
        try (final InputStream in = Files.newInputStream(path)) {
            final byte[] buffer = new byte[16384];
            int read;
            while ((read = in.read(buffer)) != -1) {
                MESSAGE_DIGEST.update(buffer, 0, read);
            }

            final byte[] result = MESSAGE_DIGEST.digest();
            MESSAGE_DIGEST.reset();
            return result;
        } catch (IOException e) {
            return new byte[0];
        }
    }
}
