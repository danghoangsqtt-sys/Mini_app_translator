package nie.translator.rtranslatordevedition;

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import nie.translator.rtranslatordevedition.tools.CustomLocale;
import org.junit.Test;

public class GlobalOnDeviceLanguagesTest {
    @Test public void languageCatalogResultsAreAlwaysDefensiveCopies() {
        ArrayList<CustomLocale> cached = new ArrayList<>(); cached.add(new CustomLocale("en", "US"));
        ArrayList<CustomLocale> result = Global.defensiveLanguageCopy(cached);
        result.clear();
        assertEquals(1, cached.size());
    }
}
