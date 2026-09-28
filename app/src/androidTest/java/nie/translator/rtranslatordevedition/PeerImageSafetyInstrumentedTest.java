package nie.translator.rtranslatordevedition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.graphics.Bitmap;
import androidx.test.runner.AndroidJUnit4;
import java.io.ByteArrayOutputStream;
import nie.translator.rtranslatordevedition.tools.Tools;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PeerImageSafetyInstrumentedTest {
    @Test
    public void validImageIsSampledWithinPixelBudget() {
        Bitmap source = Bitmap.createBitmap(512, 256, Bitmap.Config.ARGB_8888);
        byte[] encoded = encode(source);
        Bitmap decoded = Tools.decodePeerImage(encoded);

        assertNotNull(decoded);
        assertEquals(256, Math.max(decoded.getWidth(), decoded.getHeight()));
    }

    @Test
    public void oversizedMalformedAndExtremeDimensionImagesAreRejected() {
        assertNull(Tools.decodePeerImage(new byte[Tools.MAX_PEER_IMAGE_ENCODED_BYTES + 1]));
        assertNull(Tools.decodePeerImage(new byte[] {1, 2, 3, 4}));

        Bitmap extreme = Bitmap.createBitmap(4097, 1, Bitmap.Config.ALPHA_8);
        assertNull(Tools.decodePeerImage(encode(extreme)));
    }

    private static byte[] encode(Bitmap bitmap) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        return output.toByteArray();
    }
}
