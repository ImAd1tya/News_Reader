import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

// =====================================================================
//  NEWS READER — Single File Version (Java OOP + Swing GUI)
//  HOW TO RUN IN INTELLIJ:
//  1. File → New → Project → Name it "NewsReader" → Create
//  2. Right-click "src" folder → New → Java Class → name it "NewsReader"
//  3. DELETE all existing code in the file
//  4. Paste THIS entire file
//  5. Click the green ▶ Run button
// =====================================================================

public class NewsReader {

    // ── MAIN ENTRY POINT ─────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }

    // ═════════════════════════════════════════════════════════════════
    //  MODEL  (OOP: Encapsulation)
    // ═════════════════════════════════════════════════════════════════
    static class Article {
        private String title, description, url, source, date, category;

        Article(String title, String description, String url, String source, String date, String category) {
            this.title       = title;
            this.description = description;
            this.url         = url;
            this.source      = source;
            this.date        = date;
            this.category    = category;
        }

        String getTitle()       { return title; }
        String getDescription() { return description; }
        String getUrl()         { return url; }
        String getSource()      { return source; }
        String getDate()        { return date; }
        String getCategory()    { return category; }
    }

    // ═════════════════════════════════════════════════════════════════
    //  SERVICE INTERFACE  (OOP: Abstraction)
    // ═════════════════════════════════════════════════════════════════
    interface NewsService {
        List<Article> fetchHeadlines(String category) throws Exception;
    }

    // ═════════════════════════════════════════════════════════════════
    //  SERVICE IMPLEMENTATION  (OOP: Polymorphism / implements)
    //  Uses GNews.io free public API — no sign-up needed for demo key
    // ═════════════════════════════════════════════════════════════════
    static class GNewsService implements NewsService {

        @Override
        public List<Article> fetchHeadlines(String category) throws Exception {
            String topic = mapCategory(category);
            String urlStr = "https://gnews.io/api/v4/top-headlines?lang=en&max=10"
                    + (topic.isEmpty() ? "" : "&topic=" + topic)
                    + "&token=bb0c6a8cef32afaa3fd40d695dc618d8";
            try {
                String json = httpGet(urlStr);
                List<Article> result = parseJson(json, category);
                return result.isEmpty() ? mockData(category) : result;
            } catch (Exception e) {
                return mockData(category);   // offline fallback
            }
        }

        private String mapCategory(String cat) {
            switch (cat.toLowerCase()) {
                case "technology": return "technology";
                case "sports":     return "sports";
                case "science":    return "science";
                case "health":     return "health";
                case "business":   return "business";
                case "entertainment": return "entertainment";
                default:           return "";
            }
        }

        private String httpGet(String urlStr) throws Exception {
            HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            c.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
            BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            c.disconnect();
            return sb.toString();
        }

        // Simple manual JSON parser — no external library needed
        private List<Article> parseJson(String json, String category) {
            List<Article> list = new ArrayList<>();
            for (String part : json.split("\\{")) {
                String title  = field(part, "title");
                String desc   = field(part, "description");
                String url    = field(part, "url");
                String date   = field(part, "publishedAt");
                String source = nested(part, "source", "name");
                if (title != null && !title.isEmpty() && !title.equals("null")) {
                    if (date   != null && date.length() > 10) date = date.substring(0, 10);
                    if (desc   == null || desc.equals("null"))   desc   = "No description.";
                    if (source == null || source.equals("null")) source = "Unknown";
                    list.add(new Article(title, desc, url, source, date, category));
                }
            }
            return list;
        }

        private String field(String json, String key) {
            String k = "\"" + key + "\":\"";
            int s = json.indexOf(k);
            if (s < 0) return null;
            s += k.length();
            int e = json.indexOf("\"", s);
            return e < 0 ? null : json.substring(s, e).replace("\\u0026","&").replace("\\\"","\"");
        }

        private String nested(String json, String parent, String child) {
            String pk = "\"" + parent + "\":{";
            int s = json.indexOf(pk);
            return s < 0 ? null : field(json.substring(s + pk.length()), child);
        }

        // ── OFFLINE / DEMO DATA (shown when there is no internet) ────
        private List<Article> mockData(String category) {
            List<Article> list = new ArrayList<>();
            if (category.equalsIgnoreCase("sports")) {
                list.add(new Article("Champions League Quarter-Finals Begin",
                        "Eight clubs battle for a semi-final spot this week in Europe.",
                        "https://bbc.com/sport", "BBC Sport", "2026-04-13", category));
                list.add(new Article("NBA Playoffs: Full Schedule Released",
                        "The NBA has confirmed dates and matchups for the first round.",
                        "https://espn.com", "ESPN", "2026-04-13", category));
                list.add(new Article("Bangladesh vs India Test Series Preview",
                        "Bangladesh prepare for a tough home series against India.",
                        "https://espncricinfo.com", "Cricinfo", "2026-04-12", category));
            } else if (category.equalsIgnoreCase("technology")) {
                list.add(new Article("AI Models Hit New Benchmarks in 2026",
                        "The latest generation of large language models surpasses expert-level tests.",
                        "https://techcrunch.com", "TechCrunch", "2026-04-13", category));
                list.add(new Article("Apple Announces Next iPhone Line-Up",
                        "Advanced health sensors and an on-device AI chip headline the new release.",
                        "https://theverge.com", "The Verge", "2026-04-13", category));
                list.add(new Article("Quantum Computing Reaches Commercial Use",
                        "IBM demonstrates first business-ready quantum processor.",
                        "https://wired.com", "Wired", "2026-04-12", category));
            } else {
                list.add(new Article("Global Leaders Gather for Climate Summit",
                        "World leaders discuss binding carbon-reduction targets for 2035.",
                        "https://reuters.com", "Reuters", "2026-04-13", category));
                list.add(new Article("Stock Markets Hit All-Time High",
                        "S&P 500 and Nasdaq reach record levels on strong earnings season.",
                        "https://bloomberg.com", "Bloomberg", "2026-04-13", category));
                list.add(new Article("Scientists Announce Cancer Therapy Breakthrough",
                        "A new immunotherapy approach shows 90% remission in early trials.",
                        "https://nature.com", "Nature", "2026-04-12", category));
            }
            return list;
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  ARTICLE CARD PANEL  (OOP: Inheritance — extends JPanel)
    // ═════════════════════════════════════════════════════════════════
    static class ArticleCard extends JPanel {

        ArticleCard(Article a) {
            setLayout(new BorderLayout(8, 4));
            setBackground(Color.WHITE);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 235)),
                    new EmptyBorder(12, 14, 12, 14)));

            // Title (clickable link)
            JLabel titleLbl = new JLabel("<html><b>" + esc(a.getTitle()) + "</b></html>");
            titleLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
            titleLbl.setForeground(new Color(25, 80, 170));
            titleLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            titleLbl.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e)  { openUrl(a.getUrl()); }
                public void mouseEntered(MouseEvent e)  { titleLbl.setForeground(new Color(190, 40, 40)); }
                public void mouseExited(MouseEvent e)   { titleLbl.setForeground(new Color(25, 80, 170)); }
            });

            // Description
            String d = a.getDescription();
            if (d.length() > 160) d = d.substring(0, 157) + "…";
            JLabel descLbl = new JLabel("<html><p style='width:480px'>" + esc(d) + "</p></html>");
            descLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
            descLbl.setForeground(new Color(80, 80, 80));

            // Source + date
            JLabel metaLbl = new JLabel("  " + a.getSource() + "   " + a.getDate());
            metaLbl.setFont(new Font("SansSerif", Font.ITALIC, 10));
            metaLbl.setForeground(new Color(140, 140, 150));

            JPanel center = new JPanel();
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.setBackground(Color.WHITE);
            center.add(titleLbl);
            center.add(Box.createVerticalStrut(3));
            center.add(descLbl);
            center.add(Box.createVerticalStrut(3));
            center.add(metaLbl);
            add(center, BorderLayout.CENTER);

            // Category badge (right side)
            JLabel badge = new JLabel(" " + a.getCategory().toUpperCase() + " ");
            badge.setFont(new Font("SansSerif", Font.BOLD, 9));
            badge.setForeground(Color.WHITE);
            badge.setOpaque(true);
            badge.setBackground(badgeColor(a.getCategory()));
            badge.setBorder(new EmptyBorder(3, 6, 3, 6));
            JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            bp.setBackground(Color.WHITE);
            bp.add(badge);
            add(bp, BorderLayout.EAST);
        }

        private Color badgeColor(String cat) {
            switch (cat.toLowerCase()) {
                case "sports":      return new Color(30, 130, 50);
                case "technology":  return new Color(80, 40, 160);
                case "business":    return new Color(180, 120, 10);
                case "health":      return new Color(190, 40, 80);
                case "science":     return new Color(20, 120, 170);
                default:            return new Color(25, 80, 170);
            }
        }

        private void openUrl(String url) {
            try { Desktop.getDesktop().browse(new URI(url)); }
            catch (Exception ex) { JOptionPane.showMessageDialog(this, "Cannot open:\n" + url); }
        }

        private String esc(String s) {
            return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  MAIN WINDOW  (OOP: Inheritance — extends JFrame)
    // ═════════════════════════════════════════════════════════════════
    static class MainFrame extends JFrame {

        // OOP: Composition — MainFrame HAS-A NewsService
        private final NewsService service = new GNewsService();

        private final JComboBox<String> categoryBox;
        private final JButton          fetchBtn;
        private final JPanel           listPanel;
        private final JScrollPane      scroll;
        private final JLabel           statusLbl;

        MainFrame() {
            setTitle("News Reader");
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setSize(740, 620);
            setLocationRelativeTo(null);
            setLayout(new BorderLayout());

            // ── HEADER ──────────────────────────────────────────────
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(new Color(20, 50, 110));
            header.setBorder(new EmptyBorder(14, 20, 14, 20));
            JLabel title = new JLabel("  News Reader");
            title.setFont(new Font("SansSerif", Font.BOLD, 22));
            title.setForeground(Color.WHITE);
            JLabel sub = new JLabel("  Latest headlines from around the world");
            sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
            sub.setForeground(new Color(170, 195, 240));
            JPanel tp = new JPanel();
            tp.setLayout(new BoxLayout(tp, BoxLayout.Y_AXIS));
            tp.setBackground(new Color(20, 50, 110));
            tp.add(title);
            tp.add(sub);
            header.add(tp, BorderLayout.WEST);

            // ── FILTER BAR ──────────────────────────────────────────
            JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            bar.setBackground(new Color(238, 242, 252));
            bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 210, 228)));

            JLabel lbl = new JLabel("Category:");
            lbl.setFont(new Font("SansSerif", Font.BOLD, 12));

            String[] cats = {"General","Technology","Sports","Business","Science","Health","Entertainment"};
            categoryBox = new JComboBox<>(cats);
            categoryBox.setFont(new Font("SansSerif", Font.PLAIN, 12));
            categoryBox.setPreferredSize(new Dimension(165, 28));

            fetchBtn = new JButton("Get News");
            fetchBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
            fetchBtn.setBackground(new Color(25, 95, 200));
            fetchBtn.setForeground(Color.WHITE);
            fetchBtn.setFocusPainted(false);
            fetchBtn.addActionListener(e -> loadNews());

            bar.add(lbl);
            bar.add(categoryBox);
            bar.add(fetchBtn);

            // ── ARTICLE LIST ─────────────────────────────────────────
            listPanel = new JPanel();
            listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
            listPanel.setBackground(Color.WHITE);

            scroll = new JScrollPane(listPanel);
            scroll.setBorder(null);
            scroll.getVerticalScrollBar().setUnitIncrement(16);

            // ── STATUS BAR ──────────────────────────────────────────
            JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 4));
            statusBar.setBackground(new Color(245, 246, 249));
            statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(215, 218, 228)));
            statusLbl = new JLabel("Ready — select a category and click Get News");
            statusLbl.setFont(new Font("SansSerif", Font.ITALIC, 11));
            statusLbl.setForeground(new Color(110, 110, 130));
            statusBar.add(statusLbl);

            // ── TOP AREA ────────────────────────────────────────────
            JPanel top = new JPanel(new BorderLayout());
            top.add(header, BorderLayout.NORTH);
            top.add(bar,    BorderLayout.SOUTH);

            add(top,       BorderLayout.NORTH);
            add(scroll,    BorderLayout.CENTER);
            add(statusBar, BorderLayout.SOUTH);

            // Load general news on startup
            loadNews();
        }

        private void loadNews() {
            String cat = (String) categoryBox.getSelectedItem();
            fetchBtn.setEnabled(false);
            statusLbl.setText("Fetching " + cat + " news…");
            listPanel.removeAll();
            JLabel wait = new JLabel("Loading…", SwingConstants.CENTER);
            wait.setFont(new Font("SansSerif", Font.ITALIC, 14));
            wait.setForeground(new Color(130, 130, 160));
            listPanel.add(Box.createVerticalStrut(50));
            listPanel.add(wait);
            listPanel.revalidate();
            listPanel.repaint();

            // Background thread so GUI doesn't freeze
            new SwingWorker<List<Article>, Void>() {
                @Override protected List<Article> doInBackground() throws Exception {
                    return service.fetchHeadlines(cat);
                }
                @Override protected void done() {
                    try {
                        showArticles(get(), cat);
                    } catch (Exception ex) {
                        statusLbl.setText("Error: " + ex.getMessage());
                    } finally {
                        fetchBtn.setEnabled(true);
                    }
                }
            }.execute();
        }

        private void showArticles(List<Article> articles, String cat) {
            listPanel.removeAll();
            if (articles.isEmpty()) {
                JLabel none = new JLabel("No articles found.", SwingConstants.CENTER);
                none.setForeground(Color.GRAY);
                listPanel.add(Box.createVerticalStrut(60));
                listPanel.add(none);
            } else {
                for (Article a : articles) {
                    listPanel.add(new ArticleCard(a));   // OOP: polymorphism via ArticleCard
                }
                listPanel.add(Box.createVerticalGlue());
            }
            statusLbl.setText(articles.size() + " articles  |  " + cat
                    + "  |  Click a headline to open it in your browser");
            listPanel.revalidate();
            listPanel.repaint();
            SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
        }
    }
}