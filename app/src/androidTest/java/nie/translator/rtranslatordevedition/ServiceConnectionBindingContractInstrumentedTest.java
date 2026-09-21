package nie.translator.rtranslatordevedition;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class ServiceConnectionBindingContractInstrumentedTest {
    @Test
    public void repeatedBindWithSameConnectionRejectsTheSecondUnbindOnApi36() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        ConnectedConnection connection = new ConnectedConnection();
        Intent intent = new Intent(context, BindingProbeService.class);

        assertTrue(context.bindService(intent, connection, Context.BIND_AUTO_CREATE));
        assertTrue("the probe must be connected before the repeated bind", connection.awaitConnected());
        assertTrue(context.bindService(intent, connection, Context.BIND_AUTO_CREATE));

        context.unbindService(connection);

        try {
            context.unbindService(connection);
            fail("The second unbind for the same connection must be rejected");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test
    public void falseBindResultStillCreatesAConnectionThatMustBeUnbound() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        ServiceConnection connection = new EmptyConnection();
        Intent missingService = new Intent("nie.translator.rtranslatordevedition.MISSING_SERVICE");
        missingService.setPackage(context.getPackageName());

        assertFalse(context.bindService(missingService, connection, Context.BIND_AUTO_CREATE));
        context.unbindService(connection);
    }

    private static final class EmptyConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }

    private static final class ConnectedConnection implements ServiceConnection {
        private final CountDownLatch connected = new CountDownLatch(1);

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            connected.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        boolean awaitConnected() {
            try {
                return connected.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }
}
