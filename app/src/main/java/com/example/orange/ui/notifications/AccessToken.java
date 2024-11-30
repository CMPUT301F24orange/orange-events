package com.example.orange.ui.notifications;

import android.util.Log;

import java.util.Arrays;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class AccessToken {

    private static final String firebaseMessagingScope = "https://www.googleapis.com/auth/firebase.messaging";

    public String getAccessToken(){
        try {
            String jsonString = "{\n" +
                    "  \"type\": \"service_account\",\n" +
                    "  \"project_id\": \"event-lottery-system---orange\",\n" +
                    "  \"private_key_id\": \"bc5e29341b9e7778bca5a6d007145739d469d574\",\n" +
                    "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCw0c2FEUauczja\\ne2BnD8MUIuG98PIBVZbZfan3FFmQljL/ZKYv3dMnX/Hc+eCHz2RpqsdMYwKKZfer\\nb28VRzIP0I+u4WjCjjX47sVL1xpc+mvm3n21UeVE9lQXT5lvewI0ZWzvqGXdU7p9\\n8UcX6ex+qkmVuhXdyGf2Ox5l5fpWFNzSTEbZnM7F8+m6++CVOTVgkGZSWAztGUqI\\nF0VarNtfRVYr/okkvYOMZ1DpPUW41AmwEiNpfx7EIoc5YfVme34N0zBH5iRcd1oz\\n/CCnxtaaT47qixCon0brdtqpe/BXwlspzLt2NfYu4EIOnoAZ8lyLrG2YTG6tYMVg\\nupMi94eDAgMBAAECggEANFGH1bFEcDbQojjyT9xmlO0zTU/fU4y/K1BW+7BORg1j\\nPpVJ5QzmPxbRClj3Wkf/xpJNESmUpV3BeDzrKekcg3hrI0w4AqUwjB0eTK/zcYJ5\\nbeD0YjO+unaGcDVRyZ4ki1tdbdYuedwd6Mj9B7LkEqCRUFe0w6BaPo3Ek1MGjPR2\\nR+atO5LRuLKKkAw8qFNOUAgFLYhcNAEG7DTAL2nOjmGtyc2FHkQflr1xyvxDY3iZ\\n2OZGLJmVXXXki8xQHZBTXPAZirvhNLeziV//ysKoREJpzLXJYpZj713wP4/tbkNl\\nGRHmfuunbmj/vrIJNVvLo3wyKqBtvt0O6KuVNg34WQKBgQDfsRgWs9RE5xyHRV4H\\naXPSjHUDRVrbUBrqzs+02HK6c85+Y+rtXqp8EAT9MiMmBVy/tl2BAupMFIBVRU0R\\nbO0MwE7mFrTpGBhWtDaIZ2mfmWQBRbi13K58olS3jGtdIitfsuNzn/v5Lbfx4kcM\\nosDBtrTIqy+hJ3fu52eJ5xTdKwKBgQDKW6DcxOpdKa9H+BkM1AiFcgi3jcpgo61g\\nSCA2NroUWWyiwd/VJqXcKpsgcOAEtQfPWF3TbT6prl6RzTRe+6E+IuObJ04oOp95\\nWW1aRdY9oBVlKnljLm2NW0s9L9JtpelsAKTqOekH9Fbuy5HZj+F9liZB3rHuMy8U\\nidwLYkzDCQKBgQCXX2MasdSaT/8JNxLbVywOtgfVD9DdCoc8kHkUO0jgDdAVzY3D\\n1cTusXR+1rfcWdKa1VDPJebhVxpFGeF+QkTj1RtPLtx7xFdsDQW+JqTeYNHA3qqR\\nPuZA5yUHFJnu61mIqCitLP05cQvCPsqvrU9dh0MtbFKN7oSFgciaFGsVAwKBgDgq\\nQnXEOgBtp3bkR5+l4k+XnQ/FTRyRkXedzUL+4ZrwTxTFlujEd1iqgq/4ZFUqIebM\\n8g3SzoeuHBV/zT9nvBNQ/7d6q1jDHKxDw/RAWfx0yDtyFIsQwdtwMWvkMBW67RTL\\nAPr549IPYWmaGvs10jJXUHsL4nrovfKHaQnH2OJxAoGBALWqXKCEddp1zSoTa539\\navMqONBqWLLaJ/VgUSuwl3IwZhWFn9Clf2BCLyfbUHObipkcs95vhK+7nrU+zRv2\\n13fp14uGGF3jSM6omLyoUaVfO4aj6kX+zcHND47igaQ1Yy6J2gSoCyAxxol0mtXr\\nzYw3xDTnRT6wRS2g8dkNLC8X\\n-----END PRIVATE KEY-----\\n\",\n" +
                    "  \"client_email\": \"firebase-adminsdk-qeq11@event-lottery-system---orange.iam.gserviceaccount.com\",\n" +
                    "  \"client_id\": \"109961595161917357399\",\n" +
                    "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
                    "  \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" +
                    "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" +
                    "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-qeq11%40event-lottery-system---orange.iam.gserviceaccount.com\",\n" +
                    "  \"universe_domain\": \"googleapis.com\"\n" +
                    "}\n";

            InputStream stream = new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8));

            GoogleCredentials googleCredentials = GoogleCredentials.fromStream(stream).createScoped(Arrays.asList(firebaseMessagingScope));
            googleCredentials.refresh();
            return googleCredentials.getAccessToken().getTokenValue();
        } catch (IOException e){
            Log.e("error", "" + e.getMessage());
            return null;
        }
    }
}
