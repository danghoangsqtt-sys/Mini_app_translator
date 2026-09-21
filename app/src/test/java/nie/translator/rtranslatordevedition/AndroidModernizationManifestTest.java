/*
 * Copyright 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package nie.translator.rtranslatordevedition;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class AndroidModernizationManifestTest {
    private static final File MANIFEST = new File("src/main/AndroidManifest.xml");
    private static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";

    @Test
    public void nearbyPermissions_supportLegacyAndModernApiPaths() throws Exception {
        Document document = readManifest();

        assertEquals("30", permission(document, "android.permission.BLUETOOTH")
                .getAttributeNS(ANDROID_NAMESPACE, "maxSdkVersion"));
        assertEquals("30", permission(document, "android.permission.ACCESS_FINE_LOCATION")
                .getAttributeNS(ANDROID_NAMESPACE, "maxSdkVersion"));
        assertEquals("30", permission(document, "android.permission.ACCESS_COARSE_LOCATION")
                .getAttributeNS(ANDROID_NAMESPACE, "maxSdkVersion"));
        assertEquals("neverForLocation", permission(document, "android.permission.BLUETOOTH_SCAN")
                .getAttributeNS(ANDROID_NAMESPACE, "usesPermissionFlags"));
        assertNotNull(permission(document, "android.permission.BLUETOOTH_CONNECT"));
        assertNotNull(permission(document, "android.permission.BLUETOOTH_ADVERTISE"));
        assertNotNull(permission(document, "android.permission.NEARBY_WIFI_DEVICES"));
    }

    @Test
    public void launcherIsTheOnlyExportedApplicationComponent() throws Exception {
        Document document = readManifest();

        assertEquals("true", component(document, "activity",
                "nie.translator.rtranslatordevedition.LoadingActivity")
                .getAttributeNS(ANDROID_NAMESPACE, "exported"));
        assertEquals("false", component(document, "activity",
                "nie.translator.rtranslatordevedition.voice_translation.VoiceTranslationActivity")
                .getAttributeNS(ANDROID_NAMESPACE, "exported"));
        assertFalse(hasComponent(document, "service",
                "nie.translator.rtranslatordevedition.GeneralService"));
    }

    @Test
    public void foregroundVoiceServicesDeclareOnlyRequiredTypesAndPermissions() throws Exception {
        Document document = readManifest();

        assertNotNull(permission(document, "android.permission.FOREGROUND_SERVICE_MICROPHONE"));
        assertNotNull(permission(document, "android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE"));
        assertEquals("microphone|connectedDevice", component(document, "service",
                "nie.translator.rtranslatordevedition.voice_translation._conversation_mode._conversation.ConversationService")
                .getAttributeNS(ANDROID_NAMESPACE, "foregroundServiceType"));
        assertEquals("microphone|connectedDevice", component(document, "service",
                "nie.translator.rtranslatordevedition.voice_translation._walkie_talkie_mode._walkie_talkie.WalkieTalkieService")
                .getAttributeNS(ANDROID_NAMESPACE, "foregroundServiceType"));
        assertEquals("", component(document, "service",
                "nie.translator.rtranslatordevedition.voice_translation._walkie_talkie_mode.recognizer_services.FirstLanguageRecognizerService")
                .getAttributeNS(ANDROID_NAMESPACE, "foregroundServiceType"));
    }

    private static Document readManifest() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(MANIFEST);
    }

    private static Element permission(Document document, String permissionName) {
        NodeList permissions = document.getElementsByTagName("uses-permission");
        for (int index = 0; index < permissions.getLength(); index++) {
            Element permission = (Element) permissions.item(index);
            if (permissionName.equals(permission.getAttributeNS(ANDROID_NAMESPACE, "name"))) {
                return permission;
            }
        }
        return null;
    }

    private static Element component(Document document, String componentType, String componentName) {
        NodeList components = document.getElementsByTagName(componentType);
        for (int index = 0; index < components.getLength(); index++) {
            Element component = (Element) components.item(index);
            if (componentName.equals(component.getAttributeNS(ANDROID_NAMESPACE, "name"))) {
                return component;
            }
        }
        return null;
    }

    private static boolean hasComponent(Document document, String componentType, String componentName) {
        return component(document, componentType, componentName) != null;
    }
}
