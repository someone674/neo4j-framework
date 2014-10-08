package com.graphaware.writer;

import com.graphaware.common.util.IterableUtils;
import com.graphaware.common.util.PropertyContainerUtils;
import com.graphaware.test.integration.DatabaseIntegrationTest;
import org.junit.Test;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Test for {@link com.graphaware.writer.TxPerTaskWriter}.
 */
public class TxPerTaskWriterTest extends DatabaseIntegrationTest {

    private DatabaseWriter writer;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        writer = new TxPerTaskWriter(getDatabase());
        writer.start();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        writer.stop();
    }

    @Test
    public void shouldExecuteRunnable() {
        writer.write(new Runnable() {
            @Override
            public void run() {
                getDatabase().createNode();
            }
        });

        waitABit();

        try (Transaction tx = getDatabase().beginTx()) {
            assertEquals(1, IterableUtils.countNodes(getDatabase()));
            tx.success();
        }
    }

    @Test
    public void shouldNotWaitForResult() {
        Boolean result = writer.write(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Thread.sleep(50);
                getDatabase().createNode();
                return true;
            }
        }, "test", 0);

        assertNull(result);
        try (Transaction tx = getDatabase().beginTx()) {
            assertEquals(0, IterableUtils.countNodes(getDatabase()));
            tx.success();
        }

        waitABit();

        try (Transaction tx = getDatabase().beginTx()) {
            assertEquals(1, IterableUtils.countNodes(getDatabase()));
            tx.success();
        }
    }

    @Test
    public void shouldWaitForResult() {
        Boolean result = writer.write(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                getDatabase().createNode();
                return true;
            }
        }, "test", 50);

        assertTrue(result);
        try (Transaction tx = getDatabase().beginTx()) {
            assertEquals(1, IterableUtils.countNodes(getDatabase()));
            tx.success();
        }
    }

    @Test
    public void shouldNotWaitForLongTakingResult() {
        Boolean result = writer.write(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Thread.sleep(20);
                getDatabase().createNode();
                return true;
            }
        }, "test", 10);

        assertNull(result);
        try (Transaction tx = getDatabase().beginTx()) {
            assertEquals(0, IterableUtils.countNodes(getDatabase()));
            tx.success();
        }

        waitABit();

        try (Transaction tx = getDatabase().beginTx()) {
            assertEquals(1, IterableUtils.countNodes(getDatabase()));
            tx.success();
        }
    }

    @Test
    public void whenQueueIsFullTasksGetDropped() {
        writer = new TxPerTaskWriter(getDatabase(), 2);
        writer.start();

        Runnable task = new Runnable() {
            @Override
            public void run() {
                getDatabase().createNode();
            }
        };

        for (int i = 0; i < 10; i++) {
            writer.write(task);
        }

        waitABit();

        try (Transaction tx = getDatabase().beginTx()) {
            assertTrue(10 > IterableUtils.countNodes(getDatabase()));
            tx.success();
        }
    }

    @Test(expected = RuntimeException.class)
    public void runtimeExceptionFromTaskGetsPropagatedIfWaiting() {
        writer.write(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                throw new RuntimeException("Deliberate Testing Exception");
            }
        }, "test", 50);
    }

    @Test(expected = RuntimeException.class)
    public void checkedExceptionFromTaskGetsTranslatedIfWaiting() {
        writer.write(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                throw new IOException("Deliberate Testing Exception");
            }
        }, "test", 10);
    }

    @Test
    public void runtimeExceptionFromTaskGetsIgnoredIfNotWaiting() {
        writer.write(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                throw new RuntimeException("Deliberate Testing Exception");
            }
        }, "test", 0);

        writer.write(new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException("Deliberate Testing Exception");
            }
        });
    }

    @Test
    public void multipleThreadsCanSubmitTasks() {
        writer = new TxPerTaskWriter(getDatabase());
        writer.start();

        final Runnable task = new Runnable() {
            @Override
            public void run() {
                getDatabase().createNode();
            }
        };

        ExecutorService executorService = Executors.newFixedThreadPool(20);
        for (int i = 0; i < 100; i++) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    writer.write(task);
                }
            });

        }

        waitABit();
        waitABit();
        waitABit();

        try (Transaction tx = getDatabase().beginTx()) {
            assertEquals(100, IterableUtils.countNodes(getDatabase()));
            tx.success();
        }
    }

    @Test
    public void tasksAreFinishedBeforeShutdown() throws InterruptedException {
        writer = new TxPerTaskWriter(getDatabase());
        writer.start();

        final Runnable task = new Runnable() {
            @Override
            public void run() {
                getDatabase().createNode();
            }
        };

        ExecutorService executorService = Executors.newFixedThreadPool(20);
        for (int i = 0; i < 100; i++) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    writer.write(task);
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);

        writer.stop();

        try (Transaction tx = getDatabase().beginTx()) {
            assertEquals(100, IterableUtils.countNodes(getDatabase()));
            tx.success();
        }
    }

    @Test
    public void callablesWork() throws InterruptedException {
        writer = new TxPerTaskWriter(getDatabase());
        writer.start();

        final Callable<Long> task = new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                for (Node node : getDatabase().findNodesByLabelAndProperty(DynamicLabel.label("test"), "test", "test")) {
                    System.out.println(PropertyContainerUtils.nodeToString(node));
                }
                Node test = getDatabase().createNode(DynamicLabel.label("test"));
                test.setProperty("test","test");
                return test.getId();
            }
        };

        ExecutorService executorService = Executors.newFixedThreadPool(20);
        for (int i = 0; i < 100; i++) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    writer.write(task, "test", 0);
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);

        writer.stop();

        try (Transaction tx = getDatabase().beginTx()) {
            assertEquals(100, IterableUtils.countNodes(getDatabase()));
            tx.success();
        }
    }

    private void waitABit() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
