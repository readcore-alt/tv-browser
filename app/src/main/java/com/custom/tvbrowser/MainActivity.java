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

    private FrameLayout rootLayout;
    private WebView webView;
    private View punteroMouse;

    // Manejo nativo de video en pantalla completa
    private View customVideoView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    private float punteroX = 960f;
    private float punteroY = 540f;
    private final float VELOCIDAD_BASE = 28f;

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

        rootLayout = new FrameLayout(this);
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
                if (url.contains("/popunder") || url.contains("/popup") || url.contains("adbanner")) {
                    return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream("".getBytes()));
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri nuevaUri = request.getUrl();
                String url = nuevaUri.toString();

                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    return true;
                }

                if (request.isForMainFrame()) {
                    String urlActual = view.getUrl();
                    if (urlActual != null) {
                        Uri uriActual = Uri.parse(urlActual);
                        String hostActual = uriActual.getHost();
                        String nuevoHost = nuevaUri.getHost();

                        if (hostActual != null && nuevoHost != null) {
                            if (hostActual.contains("google.")) {
                                return false; 
                            }

                            String raizActual = obtenerDominioRaiz(hostActual);
                            String raizNueva = obtenerDominioRaiz(nuevoHost);

                            if (!raizActual.equalsIgnoreCase(raizNueva)) {
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

        // CONEXIÓN DE PANTALLA COMPLETA NATIVA
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customVideoView != null) {
                    onHideCustomView();
                    return;
                }
                customVideoView = view;
                customViewCallback = callback;
                webView.setVisibility(View.GONE);

                rootLayout.addView(customVideoView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 
                    ViewGroup.LayoutParams.MATCH_PARENT
                ));
                // Asegura que el puntero quede por encima del video
                punteroMouse.bringToFront();
            }

            @Override
            public void onHideCustomView() {
                if (customVideoView == null) return;
                rootLayout.removeView(customVideoView);
                customVideoView = null;
                webView.setVisibility(View.VISIBLE);
                punteroMouse.bringToFront();

                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                    customViewCallback = null;
                }
            }
        });

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
                        if (customVideoView == null) webView.scrollBy(0, -150);
                    }
                    actualizarPuntero();
                    return true;

                case KeyEvent.KEYCODE_DPAD_DOWN:
                    punteroY += velocidad;
                    if (punteroY > alto - 45) {
                        punteroY = alto - 45;
                        if (customVideoView == null) webView.scrollBy(0, 150);
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
                    if (customVideoView == null) webView.scrollBy(0, -400);
                    return true;

                case KeyEvent.KEYCODE_PAGE_DOWN:
                case KeyEvent.KEYCODE_CHANNEL_DOWN:
                    if (customVideoView == null) webView.scrollBy(0, 400);
                    return true;

                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    hacerClicEnPuntero(punteroX + 13, punteroY + 13);
                    return true;

                // BOTÓN ATRÁS: Si estás en pantalla completa, sale al modo normal sin cerrar la página
                case KeyEvent.KEYCODE_BACK:
                    if (customVideoView != null) {
                        WebChromeClient client = new WebChromeClient();
                        // Salir de pantalla completa limpiamente
                        if (customViewCallback != null) {
                            customViewCallback.onCustomViewHidden();
                        }
                        if (customVideoView != null) {
                            rootLayout.removeView(customVideoView);
                            customVideoView = null;
                            webView.setVisibility(View.VISIBLE);
                            punteroMouse.bringToFront();
                        }
                        return true;
                    }

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
        // Si el video está en pantalla completa, el clic va al video; si no, al navegador
        View vistaObjetivo = (customVideoView != null) ? customVideoView : webView;

        MotionEvent eventoAbajo = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0);
        vistaObjetivo.dispatchTouchEvent(eventoAbajo);
        eventoAbajo.recycle();

        MotionEvent eventoArriba = MotionEvent.obtain(downTime, downTime + 40, MotionEvent.ACTION_UP, x, y, 0);
        vistaObjetivo.dispatchTouchEvent(eventoArriba);
        eventoArriba.recycle();
    }
}
