package iq.stock_checker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class WebController {

    @Autowired
    private StockMonitorService service;

    // ---------------- START MONITORING ----------------
    @PostMapping("/start")
    public void start(@RequestBody(required = false) String body) {

        if (body == null || body.trim().isEmpty()) {
            service.getLiveLogs().add("⚠️ Start clicked but no URLs provided");
            return;
        }

        List<String> urls = List.of(body.split("\\R")); // supports Windows/Linux newline
        service.start(urls);
    }

    // ---------------- STOP ----------------
    @PostMapping("/stop")
    public void stop() {
        service.stop();
    }

    // ---------------- LIVE LOGS ----------------
    @GetMapping("/logs/live")
    public List<String> liveLogs() {
        return service.getLiveLogs();
    }

    // ---------------- BUY NOW LOGS ----------------
    @GetMapping("/logs/buynow")
    public List<String> buyNowLogs() {
        return service.getBuyNowLogs();
    }

    // ---------------- CLEAR LOGS ----------------
    @PostMapping("/logs/live/clear")
    public void clearLive() {
        service.clearLiveLogs();
    }

    @PostMapping("/logs/buynow/clear")
    public void clearBuyNow() {
        service.clearBuyNowLogs();
    }

    // ---------------- XPATHS ----------------
    @GetMapping("/xpaths")
    public Map<String, String> getXpaths() {
        return Map.of(
                "buyNow", service.getBuyNowXpath(),
                "notifyMe", service.getNotifyMeXpath()
        );
    }

    @PostMapping("/xpaths")
    public void updateXpaths(@RequestBody Map<String, String> body) {
        service.setBuyNowXpath(body.get("buyNow"));
        service.setNotifyMeXpath(body.get("notifyMe"));
    }
}
