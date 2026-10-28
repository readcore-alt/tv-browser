package com.custom.tvbrowser;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {

    private WebView webView;
    private View punteroMouse;

    private float punteroX = 960f;
    private float punteroY = 540f;
    private final float VELOCIDAD_BASE = 28f;

    // Servidores de publicidad conocidos
    private static final Set<String> LISTA_NEGRA_ADS = new HashSet<>(Arrays.asList(
        "adsterra", "propellerads", "popads", "popcash", "exoclick", "juicyads", 
        "monetag", "yadro", "adkeeper", "bidvertiser", "trafficjunky", "doubleclick",
        "googleads", "adnxs", "onclick", "bet365", "1xbet", "parimatch", "clksite",
        "adcolony", "vungle", "outbrain", "taboola", "mgid", "imasdk.googleapis.com",
        "histats", "whos.amung.us", "scorecardresearch", "zedo", "infolinks"
    ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.BLACK);

        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Bloqueo total de ventanas nuevas
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);

        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString().toLowerCase();
                for (String adHost : LISTA_NEGRA_ADS) {
                    if (url.contains(adHost)) {
                        return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream("".getBytes()));
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            // EL ANCLA DE DOMINIO: Destruye el secuestro de pestañas y las redirecciones de video
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri nuevaUri = request.getUrl();
                String url = nuevaUri.toString();

                // 1. Bloquear esquemas raros de apps externas
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    return true;
                }

                // 2. Si la orden intenta sacar la pestaña completa de la pantalla:
                if (request.isForMainFrame()) {
                    String urlActual = view.getUrl();
                    if (urlActual != null) {
                        Uri uriActual = Uri.parse(urlActual);
                        String hostActual = uriActual.getHost();
                        String nuevoHost = nuevaUri.getHost();

                        if (hostActual != null && nuevoHost != null) {
                            // Si estamos en Google, permite ir a cualquier enlace que elijas
                            if (hostActual.contains("google.")) {
                                return false; 
                            }

                            // Extraer dominio base (ej. animeonline.ninja)
                            String raizActual = obtenerDominioRaiz(hostActual);
                            String raizNueva = obtenerDominioRaiz(nuevoHost);

                            // Si una orden intenta secuestrar la pestaña hacia un sitio externo:
                            if (!raizActual.equalsIgnoreCase(raizNueva)) {
                                // ¡SECUESTRO DETECTADO Y DESTRUIDO!
                                return true; 
                            }
                        }
                    }
                }

                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                CookieManager.getInstance().flush();

                // Destruye window.open y limpia banners
                String script = "javascript:(function() {" +
                    "window.open = function() { return null; };" +
                    "var css = '[id*=\"ads\"], [class*=\"ads\"], iframe[src*=\"ad\"], .banner-ad { display: none !important; }';" +
                    "var style = document.createElement('style');" +
                    "style.type = 'text/css';" +
                    "style.innerHTML = css;" +
                    "document.head.appendChild(style);" +
                    "})()";
                view.evaluateJavascript(script, null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl("https://www.google.com");

        rootLayout.addView(webView);

        punteroMouse = new View(this);
        int tamano = 26;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(tamano, tamano);
        punteroMouse.setLayoutParams(params);

        GradientDrawable estilo = new GradientDrawable();
        estilo.setShape(GradientDrawable.OVAL);
        estilo.setColor(Color.parseColor("#FF1744"));
        estilo.setStroke(3, Color.WHITE);
        punteroMouse.setBackground(estilo);

        rootLayout.addView(punteroMouse);
        actualizarPuntero();

        setContentView(rootLayout);
    }

    // Compara dominios base para no confundir subdominios
    private String obtenerDominioRaiz(String host) {
        String[] partes = host.split("\\.");
        if (partes.length >= 2) {
            return partes[partes.length - 2] + "." + partes[partes.length - 1];
        }
        return host;
    }

    private void actualizarPuntero() {
        if (punteroMouse != null) {
            punteroMouse.setX(punteroX);
            punteroMouse.setY(punteroY);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int ancho = getWindow().getDecorView().getWidth();
            int alto = getWindow().getDecorView().getHeight();
            if (ancho == 0) ancho = 1920;
            if (alto == 0) alto = 1080;

            float velocidad = VELOCIDAD_BASE + Math.min(event.getRepeatCount() * 8f, 50f);

            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    punteroY -= velocidad;
                    if (punteroY < 20) {
                        punteroY = 20;
                        webView.scrollBy(0, -150);
                    }
                    actualizarPuntero();
                    return true;

                case KeyEvent.KEYCODE_DPAD_DOWN:
                    punteroY += velocidad;
                    if (punteroY > alto - 45) {
                        punteroY = alto - 45;
                        webView.scrollBy(0, 150);
                    }
                    actualizarPuntero();
                    return true;

                case KeyEvent.KEYCODE_DPAD_LEFT:
                    punteroX -= velocidad;
                    if (punteroX < 10) punteroX = 10;
                    actualizarPuntero();
                    return true;

                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    punteroX += velocidad;
                    if (punteroX > ancho - 35) punteroX = ancho - 35;
                    actualizarPuntero();
                    return true;

                case KeyEvent.KEYCODE_PAGE_UP:
                case KeyEvent.KEYCODE_CHANNEL_UP:
                    webView.scrollBy(0, -400);
                    return true;

                case KeyEvent.KEYCODE_PAGE_DOWN:
                case KeyEvent.KEYCODE_CHANNEL_DOWN:
                    webView.scrollBy(0, 400);
                    return true;

                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    hacerClicEnPuntero(punteroX + 13, punteroY + 13);
                    return true;

                case KeyEvent.KEYCODE_BACK:
                    if (webView.canGoBack()) {
                        webView.goBack();
                        return true;
                    }
                    break;
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    private void hacerClicEnPuntero(float x, float y) {
        long downTime = SystemClock.uptimeMillis();

        MotionEvent eventoAbajo = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0);
        webView.dispatchTouchEvent(eventoAbajo);
        eventoAbajo.recycle();

        MotionEvent eventoArriba = MotionEvent.obtain(downTime, downTime + 40, MotionEvent.ACTION_UP, x, y, 0);
        webView.dispatchTouchEvent(eventoArriba);
        eventoArriba.recycle();
    }
}
