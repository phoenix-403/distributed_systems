package milestone_1;

import app_kvServer.Persist;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static app_kvServer.Persist.init;

public class AdditionalTest extends TestCase {

    // TODO add your test cases, at least 3

    @Test
    public void testStub() {
        Assert.assertTrue(true);
    }


    @Test
    public void testKeyFileLookUp() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        // testing private method
        assert (init());

        Method method = Persist.class.getDeclaredMethod("getFileKeyStoredIn", String.class);
        method.setAccessible(true);

        Assert.assertEquals("97.db", ((File) method.invoke(null, "ABC")).getName());
        Assert.assertEquals("97.db", ((File) method.invoke(null, "abc")).getName());
        Assert.assertEquals("98.db", ((File) method.invoke(null, "b")).getName());
        Assert.assertEquals("99.db", ((File) method.invoke(null, "c")).getName());
        Assert.assertEquals("123.db", ((File) method.invoke(null, "~")).getName());

    }

}
