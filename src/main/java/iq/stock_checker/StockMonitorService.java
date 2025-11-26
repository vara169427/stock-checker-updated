package iq.stock_checker;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class StockMonitorService {

    private static final String BOT_TOKEN = "8268301332:AAF6LlMrEVBCkR9FaZ87nr8CIP0cNfynCqM";
    private static final String CHAT_ID = "1456153642";

    private volatile String buyNowXpath = "div.btn-buynow.btn";
    private volatile String notifyMeXpath = "div.btn-notify.btn";

    // 2 log lists
    private final List<String> liveLogs = new CopyOnWriteArrayList<>();
    private final List<String> buyNowLogs = new CopyOnWriteArrayList<>();

    public List<String> getLiveLogs() { return liveLogs; }
    public List<String> getBuyNowLogs() { return buyNowLogs; }

    public void clearLiveLogs() { liveLogs.clear(); }
    public void clearBuyNowLogs() { buyNowLogs.clear(); }

    private volatile boolean running = false;
    private ExecutorService pool;


    // ----------------------- START ----------------------------
    public synchronized void start(List<String> urls) {
        if (running) {
            liveLogs.add("⚠️ Already running");
            return;
        }

        liveLogs.clear();
        buyNowLogs.clear();
        liveLogs.add("✅ Started monitoring");

        running = true;
        pool = Executors.newFixedThreadPool(urls.size());

        for (String url : urls) {
            if (!url.trim().isEmpty()) {
                pool.submit(() -> monitor(url.trim()));
            }
        }
    }

    // ----------------------- STOP ----------------------------
    public synchronized void stop() {
        running = false;

        if (pool != null) {
            pool.shutdownNow();
            pool = null;
        }

        liveLogs.add("🛑 Monitoring stopped");
    }


    // ----------------------- MONITOR URL ----------------------------
    private void monitor(String url) {
        liveLogs.add("▶️ Checking started: " + url);

        while (running) {
            try {
                Thread.sleep(3000); // wait 3 seconds for page to load

                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0")
                        .timeout(5000)
                        .get();

                Element buyNowBtn = doc.selectFirst(buyNowXpath);     // Buy Now
                Element notifyBtn = doc.selectFirst(notifyMeXpath);   // Notify Me

                String status;

                if (buyNowBtn != null && buyNowBtn.text().toLowerCase().contains("buy")) {
                    status = "BUY NOW";

                    // -------- Add to BuyNow list --------
                    buyNowLogs.add("🎉 IN STOCK: " + url);
                    sendTelegramMessage("IN STOCK: " + url);

                    break;
                }
                else if (notifyBtn != null) {
                    status = "OUT OF STOCK";
                }
                else {
                    status = "OUT OF STOCK";
                }

                liveLogs.add("[" + url + "] ➜ " + status);

                Thread.sleep(2000);

            } catch (Exception e) {
                liveLogs.add("❌ ERROR: " + url);
                liveLogs.add("Reason: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                try { Thread.sleep(2000); } catch (Exception ignored) {}
            }
        }
    }


    // ----------------------- TELEGRAM SEND ----------------------------
    private void sendTelegramMessage(String message) {
        try {
            String text = URLEncoder.encode(message, "UTF-8");
            String urlString = "https://api.telegram.org/bot" + BOT_TOKEN +
                    "/sendMessage?chat_id=" + CHAT_ID + "&text=" + text;

            HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
            conn.setRequestMethod("GET");
            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception ignored) {}
    }


    // ---------------- XPATH UPDATE METHODS --------------------
    public void setBuyNowXpath(String xpath) {
        if (xpath != null && !xpath.trim().isEmpty()) {
            this.buyNowXpath = xpath.trim();
            liveLogs.add("🔧 BuyNow CSS updated: " + this.buyNowXpath);
        }
    }

    public void setNotifyMeXpath(String xpath) {
        if (xpath != null && !xpath.trim().isEmpty()) {
            this.notifyMeXpath = xpath.trim();
            liveLogs.add("🔧 NotifyMe CSS updated: " + this.notifyMeXpath);
        }
    }

    public String getBuyNowXpath() { return buyNowXpath; }
    public String getNotifyMeXpath() { return notifyMeXpath; }
}
