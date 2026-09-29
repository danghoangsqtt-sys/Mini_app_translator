package nie.translator.rtranslatordevedition.voice_translation;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WalkieTalkieIconContractTest {
    @Test
    public void pairingActionUsesSuppliedWalkieIconAndLabel() throws Exception {
        String layout = new String(Files.readAllBytes(
                new File("src/main/res/layout/fragment_pairing.xml").toPath()), StandardCharsets.UTF_8);
        assertTrue(layout.contains("app:srcCompat=\"@drawable/call_icon\""));
        assertTrue(layout.contains("app:tint=\"?attr/colorPrimary\""));
        assertTrue(layout.contains("android:contentDescription=\"@string/cd_start_walkie_talkie\""));
        assertFalse(layout.contains("@drawable/walkie_talkie_white_icon"));
    }

    @Test
    public void packagedIconExactlyMatchesTransparentNonBlankSource() throws Exception {
        File source = new File("../images/call_icon.png");
        File packaged = new File("src/main/res/drawable-nodpi/call_icon.png");
        assertTrue(source.isFile());
        assertTrue(packaged.isFile());
        assertArrayEquals(Files.readAllBytes(source.toPath()), Files.readAllBytes(packaged.toPath()));

        byte[] png = Files.readAllBytes(packaged.toPath());
        assertTrue("detailed supplied artwork must not collapse to a blank placeholder", png.length > 100_000);
        assertTrue("PNG signature", png.length > 26
                && (png[0] & 0xff) == 0x89 && png[1] == 'P' && png[2] == 'N' && png[3] == 'G');
        assertTrue("supplied icon width must be positive", readBigEndianInt(png, 16) > 0);
        assertTrue("supplied icon height must be positive", readBigEndianInt(png, 20) > 0);
        assertTrue("PNG must retain an alpha channel", (png[25] & 0xff) == 4 || (png[25] & 0xff) == 6);
    }

    private static int readBigEndianInt(byte[] value, int offset) {
        return ((value[offset] & 0xff) << 24)
                | ((value[offset + 1] & 0xff) << 16)
                | ((value[offset + 2] & 0xff) << 8)
                | (value[offset + 3] & 0xff);
    }
}
