package com.custom.tvbrowser;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

    private WebView webView;
    private View punteroMouse;

    // Coordenadas iniciales del puntero
    private float punteroX = 960f;
    private float punteroY = 540f;
    private final float VELOCIDAD_BASE = 28f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.BLACK);

        // 1. Motor de Navegación
        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl("https://www.google.com");

        rootLayout.addView(webView);

        // 2. El Puntero (Círculo rojo visible)
        punteroMouse = new View(this);
        int tamano = 26;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(tamano, tamano);
        punteroMouse.setLayoutParams(params);

        GradientDrawable estilo = new GradientDrawable();
        estilo.setShape(GradientDrawable.OVAL);
        estilo.setColor(Color.parseColor("#FF1744")); // Rojo
        estilo.setStroke(3, Color.WHITE); // Borde blanco
        punteroMouse.setBackground(estilo);

        rootLayout.addView(punteroMouse);
        actualizarPuntero();

        setContentView(rootLayout);
    }

    private void actualizarPuntero() {
        if (punteroMouse != null) {
            punteroMouse.setX(punteroX);
            punteroMouse.setY(punteroY);
        }
    }

    // ADUANA TOTAL DE ENTRADA: Le quita las flechas a Chrome antes de que las secuestre
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();

        // Control del puntero al presionar las teclas
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int ancho = getWindow().getDecorView().getWidth();
            int alto = getWindow().getDecorView().getHeight();
            if (ancho == 0) ancho = 1920;
            if (alto == 0) alto = 1080;

            // Aceleración si dejas el botón presionado
            float velocidad = VELOCIDAD_BASE + Math.min(event.getRepeatCount() * 8f, 50f);

            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    punteroY -= velocidad;
                    if (punteroY < 20) {
                        punteroY = 20;
                        webView.scrollBy(0, -150); // Scroll arriba
                    }
                    actualizarPuntero();
                    return true; // <-- Consumido, Chrome no lo toca

                case KeyEvent.KEYCODE_DPAD_DOWN:
                    punteroY += velocidad;
                    if (punteroY > alto - 45) {
                        punteroY = alto - 45;
                        webView.scrollBy(0, 150); // Scroll abajo
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

                // Scroll rápido con botones de canal (CH+ / CH-) si los usas
                case KeyEvent.KEYCODE_PAGE_UP:
                case KeyEvent.KEYCODE_CHANNEL_UP:
                    webView.scrollBy(0, -400);
                    return true;

                case KeyEvent.KEYCODE_PAGE_DOWN:
                case KeyEvent.KEYCODE_CHANNEL_DOWN:
                    webView.scrollBy(0, 400);
                    return true;

                // Clic virtual en la posición del círculo rojo
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    hacerClicEnPuntero(punteroX + 13, punteroY + 13);
                    return true;

                // Volver atrás en el historial
                case KeyEvent.KEYCODE_BACK:
                    if (webView.canGoBack()) {
                        webView.goBack();
                        return true;
                    }
                    break;
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            // Destruye la soltada para que Chrome no procese nada al soltar la tecla
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
