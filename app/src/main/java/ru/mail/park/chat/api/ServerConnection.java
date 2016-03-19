package ru.mail.park.chat.api;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import info.guardianproject.netcipher.NetCipher;
import info.guardianproject.netcipher.proxy.OrbotHelper;

/**
 * Created by 1запуск BeCompact on 29.02.2016.
 */

// FIXME: don't trust everyone!
// TODO: check security
public class ServerConnection {
    HttpsURLConnection httpsURLConnection;
    Context context;
    String parameters = null;

    public ServerConnection(Context context, String url) throws IOException {
        this(context, new URL(url));
    }

    public ServerConnection(Context context, URL url) throws IOException {
        this.context = context;
        setUrl(url);
    }

    protected TrustManager getTrustManager() {
        TrustManager tm = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
        return tm;
    }

    public void setUrl(String url) throws IOException {
        setUrl(new URL(url));
    }

    public void setUrl(URL url) throws IOException {
        boolean torStart = OrbotHelper.requestStartTor(context);

        try {
            if (torStart) {
                NetCipher.setProxy(NetCipher.ORBOT_HTTP_PROXY);
                httpsURLConnection = NetCipher.getHttpsURLConnection(url);
            } else {
                httpsURLConnection = (HttpsURLConnection) url.openConnection();
            }

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{getTrustManager()}, null);

            httpsURLConnection.setSSLSocketFactory(sslContext.getSocketFactory());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public void setRequestMethod(String method) throws ProtocolException {
        httpsURLConnection.setRequestMethod(method);
    }

    public String getResponse() {
        StringBuilder responseBuilder = new StringBuilder();

        try {
            // AFAIK everything except GET sends parameters the same way
            if (!httpsURLConnection.getRequestMethod().equals("GET")) {
                byte[] postData = parameters.getBytes(Charset.forName("UTF-8"));

                httpsURLConnection.setDoOutput(true);
                httpsURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                httpsURLConnection.setRequestProperty("charset", "utf-8");
                httpsURLConnection.setRequestProperty("Content-Length", Integer.toString(postData.length));

                httpsURLConnection.getOutputStream().write(postData);
            }

            BufferedReader rd = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                responseBuilder.append(line);
            }
            rd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return responseBuilder.toString();
    }
}
