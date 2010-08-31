package contrib.Testing.NBQ_Tester;
import junit.framework.TestCase;
import java.util.Queue;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Prashant Deva.
 * Date: Nov 1, 2008
 */
public class NBQ_Tester extends TestCase {
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("contrib.Testing.NBQ_Tester.NBQ_Tester");
  }

    private Queue<Integer> queue;

    public void setUp()
    {
        queue = new LinkedList<Integer>();
        checkIfEmpty();
    }

    protected void tearDown() {
        checkIfEmpty();
        queue=null;
    }

    public void testPoll()
    {

        queue.offer(1);
        assertEquals(1, (int) queue.poll());

    }

    public void testPeek()
    {

        queue.offer(2);
        assertEquals(2, (int) queue.peek());

        assertEquals(2, (int) queue.poll());

    }


    public void testRemove()
    {
        queue.offer(2);
        queue.offer(4);

        assertEquals(2, (int) queue.remove());
        assertEquals(4, (int) queue.remove());

        try{
            queue.remove();
        }catch(NoSuchElementException e)
        {
            return;
        }

        fail();
    }


    public void testOffer()
    {
        queue.offer(1);
        queue.offer(2);
        queue.offer(3);
        assertEquals(1,(int)queue.poll());

        queue.offer(4);

        assertEquals(2,(int)queue.remove());
        assertEquals(3,(int)queue.remove());
        assertEquals(4,(int)queue.remove());
    }


    public void testElement()
    {
        queue.offer(2);
        queue.offer(4);

        assertEquals(2, (int) queue.element());
        assertEquals(2, (int) queue.remove());

        assertEquals(4, (int) queue.element());
        assertEquals(4, (int) queue.remove());

        try{
            queue.element();
        }catch(NoSuchElementException e)
        {
            return;
        }

        fail();
    }

    private void checkIfEmpty() {
        assertNull(queue.peek());
    }
}
