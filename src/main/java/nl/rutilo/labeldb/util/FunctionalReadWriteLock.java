package nl.rutilo.labeldb.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/** Lambda support for Lock. Can be used as follows:<pre>
  *
  * lock = new FunctionalReadWriteLock();
  * lock.read( () -> { read some data } );
  * lock.write( () -> { write some data } );
  * </pre>
  *
  * @author Nicolas de Jong
  */
public class FunctionalReadWriteLock {

    private final Lock readLock;
    private final Lock writeLock;

    public FunctionalReadWriteLock() {
        this(/*fair=*/true);
    }
    public FunctionalReadWriteLock(boolean fair) {
        this(new ReentrantReadWriteLock(fair));
    }
    public FunctionalReadWriteLock(ReadWriteLock lock) {
        readLock = lock.readLock();
        writeLock = lock.writeLock();
    }

    public <T> T read(Supplier<T> block) {
        readLock.lock();
        try {
            return block.get();
        } finally {
            readLock.unlock();
        }
    }
    public void read(Runnable block) {
        readLock.lock();
        try {
            block.run();
        } finally {
            readLock.unlock();
        }
    }

    public <T> T write(Supplier<T> block) {
        writeLock.lock();
        try {
            return block.get();
        } finally {
            writeLock.unlock();
        }
    }
    public void write(Runnable block) {
        writeLock.lock();
        try {
            block.run();
        } finally {
            writeLock.unlock();
        }
    }
}
