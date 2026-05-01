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

//
//  OOP CONCEPTS:
//  1. Encapsulation         → Article (private fields + getters/setters)
//  2. Method Overloading    → Article.display() — 3 versions
//  3. Method Overriding     → GNewsService overrides fetchNews(), buildUrl()
//  4. Abstraction           → NewsService interface
//  5. Inheritance           → ArticleCard extends JPanel
//                             MainFrame extends JFrame
//                             GNewsService extends BaseNewsService
//  6. Custom Exceptions     → NewsFeedException, NetworkException,
//                             ParseException, InvalidCategoryException
//

// =====================================================================

public class NewsReader {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }

    // =================================================================
    //  CUSTOM EXCEPTIONS
    // =================================================================

    /** Base custom exception for all news errors. */
    static class NewsFeedException extends Exception {
        private final int errorCode;
        NewsFeedException(String message, int errorCode) {
            super(message);
            this.errorCode = errorCode;
        }
        NewsFeedException(String message) { this(message, -1); }
        int getErrorCode() { return errorCode; }
    }

    /** Thrown when HTTP request fails or network is unreachable. */
    static class NetworkException extends NewsFeedException {
        private final String url;
        NetworkException(String message, String url) {
            super(message, 503);
            this.url = url;
        }
        String getAttemptedUrl() { return url; }
    }

    /** Thrown when the JSON response cannot be parsed. */
    static class ParseException extends NewsFeedException {
        ParseException(String message) { super(message, 422); }
    }

    /** Thrown when an unsupported category name is supplied. */
    static class InvalidCategoryException extends NewsFeedException {
        InvalidCategoryException(String category) {
            super("Unsupported category: '" + category + "'", 400);
        }
    }

    // =================================================================
    //  MODEL — Encapsulation + Method Overloading
    // =================================================================

    static class Article {

        // ── Private fields (Encapsulation) ───────────────────────────
        private String title;
        private String description;
        private String url;
        private String source;
        private String date;
        private String category;

        Article(String title, String description, String url,
                String source, String date, String category) {
            this.title       = title;
            this.description = description;
            this.url         = url;
            this.source      = source;
            this.date        = date;
            this.category    = category;
        }

        // ── Getters & Setters ─────────────────────────────────────────
        String getTitle()       { return title; }
        String getDescription() { return description; }
        String getUrl()         { return url; }
        String getSource()      { return source; }
        String getDate()        { return date; }
        String getCategory()    { return category; }

        void setTitle(String title)             { this.title = title; }
        void setDescription(String description) { this.description = description; }
        void setUrl(String url)                 { this.url = url; }
        void setSource(String source)           { this.source = source; }
        void setDate(String date)               { this.date = date; }
        void setCategory(String category)       { this.category = category; }

        // ── Method Overloading: display() in 3 signatures ─────────────

        /** Overload 1 — title + category only. */
        String display() {
            return "[" + category.toUpperCase() + "] " + title;
        }

        /** Overload 2 — optionally include description. */
        String display(boolean showDescription) {
            String base = display();
            if (showDescription && description != null)
                base += "\n  " + description;
            return base;
        }

        /** Overload 3 — include description, trimmed to maxLen chars. */
        String display(boolean showDescription, int maxLen) {
            String base = display();
            if (showDescription && description != null) {
                String d = description.length() > maxLen
                        ? description.substring(0, maxLen) + "…"
                        : description;
                base += "\n  " + d;
            }
            return base;
        }
    }

    // =================================================================
    //  ABSTRACTION — Interface
    // =================================================================

    interface NewsService {
        List<Article> fetchNews(String category) throws NewsFeedException;
        String getProviderName();
    }

    // =================================================================
    //  ABSTRACT BASE CLASS — Template Methods (Overriding targets)
    // =================================================================

    static abstract class BaseNewsService implements NewsService {

        protected static final String[] VALID_CATEGORIES = {
                "general", "technology", "sports",
                "business", "science", "health", "entertainment"
        };

        /** Validate category; throw InvalidCategoryException if bad. */
        protected void validateCategory(String category)
                throws InvalidCategoryException {
            if (category == null || category.isBlank())
                throw new InvalidCategoryException("(empty)");
            for (String c : VALID_CATEGORIES)
                if (c.equalsIgnoreCase(category)) return;
            throw new InvalidCategoryException(category);
        }

        /** Build the API request URL — subclasses must override. */
        protected abstract String buildUrl(String category);

        /** Parse raw JSON into Articles — subclasses must override. */
        protected abstract List<Article> parseResponse(String json, String category)
                throws ParseException;

        /** Offline fallback data — subclasses must override. */
        protected abstract List<Article> offlineData(String category);

        // ── Method Overriding: fetchNews() ────────────────────────────
        // Subclasses override this; BaseNewsService provides the
        // validate → fetch → parse → fallback skeleton.
        @Override
        public List<Article> fetchNews(String category) throws NewsFeedException {
            validateCategory(category);          // may throw InvalidCategoryException
            try {
                String url  = buildUrl(category);
                String json = httpGet(url);      // may throw NetworkException
                List<Article> list = parseResponse(json, category); // may throw ParseException
                return list.isEmpty() ? offlineData(category) : list;
            } catch (NetworkException | ParseException e) {
                System.err.println("Falling back to offline data: " + e.getMessage());
                return offlineData(category);
            }
        }

        /** Performs HTTP GET; throws NetworkException on failure. */
        private String httpGet(String urlStr) throws NetworkException {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8_000);
                conn.setReadTimeout(8_000);
                conn.setRequestProperty("User-Agent", "NewsReader/2.0");
                int status = conn.getResponseCode();
                if (status != 200)
                    throw new NetworkException("HTTP " + status, urlStr);
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                return sb.toString();
            } catch (NetworkException ne) {
                throw ne;
            } catch (Exception ex) {
                throw new NetworkException("Connection failed: " + ex.getMessage(), urlStr);
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
    }

    // =================================================================
    //  CONCRETE SERVICE — Overrides buildUrl, parseResponse, offlineData
    // =================================================================

    static class GNewsService extends BaseNewsService {

        private static final String TOKEN = "bb0c6a8cef32afaa3fd40d695dc618d8";

        // ── Method Overriding ─────────────────────────────────────────
        @Override
        public String getProviderName() { return "GNews.io"; }

        @Override
        protected String buildUrl(String category) {
            String topic = mapTopic(category);
            return "https://gnews.io/api/v4/top-headlines?lang=en&max=10"
                    + (topic.isEmpty() ? "" : "&topic=" + topic)
                    + "&token=" + TOKEN;
        }

        @Override
        protected List<Article> parseResponse(String json, String category)
                throws ParseException {
            if (json == null || json.isBlank())
                throw new ParseException("Empty API response");
            List<Article> list = new ArrayList<>();
            try {
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
            } catch (Exception e) {
                throw new ParseException("JSON parse error: " + e.getMessage());
            }
            return list;
        }

        @Override
        protected List<Article> offlineData(String category) {
            List<Article> list = new ArrayList<>();
            switch (category.toLowerCase()) {
                case "sports":
                    list.add(new Article("Champions League Quarter-Finals Begin",
                            "Eight clubs battle for a semi-final spot this week in Europe.",
                            "https://bbc.com/sport","BBC Sport","2026-04-13",category));
                    list.add(new Article("NBA Playoffs: Full Schedule Released",
                            "The NBA confirmed dates and matchups for the first round.",
                            "https://espn.com","ESPN","2026-04-13",category));
                    list.add(new Article("Bangladesh vs India Test Series Preview",
                            "Bangladesh prepare for a tough home series against India.",
                            "https://espncricinfo.com","Cricinfo","2026-04-12",category));
                    break;
                case "technology":
                    list.add(new Article("AI Models Hit New Benchmarks in 2026",
                            "The latest LLMs surpass expert-level tests across domains.",
                            "https://techcrunch.com","TechCrunch","2026-04-13",category));
                    list.add(new Article("Apple Announces Next iPhone Line-Up",
                            "Advanced health sensors and on-device AI chip headline the release.",
                            "https://theverge.com","The Verge","2026-04-13",category));
                    list.add(new Article("Quantum Computing Reaches Commercial Use",
                            "IBM demonstrates first business-ready quantum processor.",
                            "https://wired.com","Wired","2026-04-12",category));
                    break;
                case "health":
                    list.add(new Article("Cancer Therapy Breakthrough Announced",
                            "A new immunotherapy approach shows 90% remission in early trials.",
                            "https://nature.com","Nature","2026-04-12",category));
                    list.add(new Article("WHO Issues New Sleep Guidelines for Adults",
                            "Experts recommend 7-9 hours; irregular sleep linked to heart risk.",
                            "https://who.int","WHO","2026-04-11",category));
                    break;
                case "science":
                    list.add(new Article("James Webb Captures Distant Galaxy",
                            "Image reveals star formation 400 million years post-Big Bang.",
                            "https://nasa.gov","NASA","2026-04-13",category));
                    list.add(new Article("CRISPR Cures Rare Blood Disorder",
                            "Clinical trial shows gene editing eliminates sickle-cell symptoms.",
                            "https://nature.com","Nature","2026-04-12",category));
                    break;
                case "business":
                    list.add(new Article("Stock Markets Hit All-Time High",
                            "S&P 500 and Nasdaq reach record levels on strong earnings.",
                            "https://bloomberg.com","Bloomberg","2026-04-13",category));
                    list.add(new Article("Tech Giants Report Record Q1 Profits",
                            "Apple, Microsoft and Alphabet all beat analyst expectations.",
                            "https://reuters.com","Reuters","2026-04-12",category));
                    break;
                case "entertainment":
                    list.add(new Article("Cannes 2026 Lineup Announced",
                            "Directors from 40 countries compete for the Palme d'Or.",
                            "https://variety.com","Variety","2026-04-13",category));
                    list.add(new Article("Streaming Wars: New Players Enter Market",
                            "Three new platforms launch with exclusive original content.",
                            "https://hollywoodreporter.com","Hollywood Reporter","2026-04-12",category));
                    break;
                default:
                    list.add(new Article("Global Leaders Gather for Climate Summit",
                            "World leaders discuss binding carbon-reduction targets for 2035.",
                            "https://reuters.com","Reuters","2026-04-13",category));
                    list.add(new Article("Scientists Announce Cancer Therapy Breakthrough",
                            "A new immunotherapy shows 90% remission in early trials.",
                            "https://nature.com","Nature","2026-04-12",category));
                    list.add(new Article("Stock Markets Hit All-Time High",
                            "S&P 500 and Nasdaq reach record levels on strong earnings.",
                            "https://bloomberg.com","Bloomberg","2026-04-13",category));
            }
            return list;
        }

        // ── Helpers ───────────────────────────────────────────────────
        private String mapTopic(String cat) {
            switch (cat.toLowerCase()) {
                case "technology":    return "technology";
                case "sports":        return "sports";
                case "science":       return "science";
                case "health":        return "health";
                case "business":      return "business";
                case "entertainment": return "entertainment";
                default:              return "";
            }
        }

        private String field(String json, String key) {
            String k = "\"" + key + "\":\"";
            int s = json.indexOf(k);
            if (s < 0) return null;
            s += k.length();
            int e = json.indexOf("\"", s);
            return e < 0 ? null : json.substring(s, e)
                    .replace("\\u0026","&").replace("\\\"","\"");
        }

        private String nested(String json, String parent, String child) {
            String pk = "\"" + parent + "\":{";
            int s = json.indexOf(pk);
            return s < 0 ? null : field(json.substring(s + pk.length()), child);
        }
    }

    // =================================================================
    //  UI — ArticleCard (Inheritance: extends JPanel)
    // =================================================================

    static class ArticleCard extends JPanel {

        private final Article article;   // Encapsulation: private field
        private JLabel titleLabel;

        ArticleCard(Article article) {
            this.article = article;
            buildUI();
        }

        Article getArticle() { return article; }   // encapsulated getter

        private void buildUI() {
            setLayout(new BorderLayout(8, 4));
            setBackground(Color.WHITE);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(225, 228, 238)),
                    new EmptyBorder(14, 16, 14, 16)));

            // Title link
            titleLabel = new JLabel("<html><b>" + esc(article.getTitle()) + "</b></html>");
            titleLabel.setFont(new Font("Georgia", Font.BOLD, 13));
            titleLabel.setForeground(new Color(18, 68, 154));
            titleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            titleLabel.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e)  { openUrl(article.getUrl()); }
                public void mouseEntered(MouseEvent e)  { titleLabel.setForeground(new Color(185, 28, 28)); }
                public void mouseExited(MouseEvent e)   { titleLabel.setForeground(new Color(18, 68, 154)); }
            });

            // Description — uses overloaded display(boolean, int)
            String desc = article.display(true, 160);
            desc = desc.contains("\n") ? desc.split("\n")[1].trim() : article.getDescription();
            JLabel descLabel = new JLabel("<html><p style='width:460px;color:#505060'>"
                    + esc(desc) + "</p></html>");
            descLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));

            // Meta
            JLabel metaLabel = new JLabel("  " + article.getSource() + "   " + article.getDate());
            metaLabel.setFont(new Font("SansSerif", Font.ITALIC, 10));
            metaLabel.setForeground(new Color(130, 130, 150));

            JPanel center = new JPanel();
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.setBackground(Color.WHITE);
            center.add(titleLabel);
            center.add(Box.createVerticalStrut(4));
            center.add(descLabel);
            center.add(Box.createVerticalStrut(4));
            center.add(metaLabel);

            // Category badge
            JLabel badge = new JLabel(" " + article.getCategory().toUpperCase() + " ");
            badge.setFont(new Font("SansSerif", Font.BOLD, 9));
            badge.setForeground(Color.WHITE);
            badge.setOpaque(true);
            badge.setBackground(badgeColor(article.getCategory()));
            badge.setBorder(new EmptyBorder(4, 7, 4, 7));
            JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            bp.setBackground(Color.WHITE);
            bp.add(badge);

            add(center, BorderLayout.CENTER);
            add(bp,     BorderLayout.EAST);
        }

        private Color badgeColor(String cat) {
            switch (cat.toLowerCase()) {
                case "sports":        return new Color(21, 128, 61);
                case "technology":    return new Color(76, 29, 149);
                case "business":      return new Color(161, 98, 7);
                case "health":        return new Color(185, 28, 28);
                case "science":       return new Color(7, 89, 133);
                case "entertainment": return new Color(190, 24, 93);
                default:              return new Color(25, 80, 170);
            }
        }

        private void openUrl(String url) {
            try {
                if (url == null || url.isBlank()) throw new Exception("No URL");
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Could not open:\n" + url, "Error", JOptionPane.WARNING_MESSAGE);
            }
        }

        private String esc(String s) {
            if (s == null) return "";
            return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
        }
    }

    // =================================================================
    //  UI — MainFrame (Inheritance: extends JFrame + Composition)
    // =================================================================

    static class MainFrame extends JFrame {

        // Composition: MainFrame HAS-A NewsService (interface reference)
        private final NewsService service = new GNewsService();

        private final JComboBox<String> categoryBox;
        private final JButton           fetchBtn;
        private final JPanel            listPanel;
        private final JScrollPane       scroll;
        private final JLabel            statusLabel;

        private static final Color NAVY = new Color(15, 40, 95);

        MainFrame() {
            setTitle("News Reader");
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setSize(760, 640);
            setMinimumSize(new Dimension(600, 480));
            setLocationRelativeTo(null);
            setLayout(new BorderLayout());

            // Header
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(NAVY);
            header.setBorder(new EmptyBorder(16, 22, 16, 22));
            JLabel titleLbl = new JLabel("  📰  News Reader");
            titleLbl.setFont(new Font("Georgia", Font.BOLD, 24));
            titleLbl.setForeground(Color.WHITE);
            JLabel subLbl = new JLabel("  Latest headlines from around the world");
            subLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
            subLbl.setForeground(new Color(160, 190, 240));
            JPanel tp = new JPanel();
            tp.setLayout(new BoxLayout(tp, BoxLayout.Y_AXIS));
            tp.setBackground(NAVY);
            tp.add(titleLbl);
            tp.add(Box.createVerticalStrut(4));
            tp.add(subLbl);
            header.add(tp, BorderLayout.WEST);

            // Filter bar
            String[] cats = {"General","Technology","Sports","Business","Science","Health","Entertainment"};
            categoryBox = new JComboBox<>(cats);
            categoryBox.setFont(new Font("SansSerif", Font.PLAIN, 12));
            categoryBox.setPreferredSize(new Dimension(170, 28));

            fetchBtn = new JButton("Get News ▶");
            fetchBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
            fetchBtn.setBackground(new Color(22, 58, 138));
            fetchBtn.setForeground(Color.WHITE);
            fetchBtn.setFocusPainted(false);
            fetchBtn.addActionListener(e -> loadNews());

            JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            bar.setBackground(new Color(237, 241, 252));
            bar.setBorder(BorderFactory.createMatteBorder(0,0,1,0, new Color(200,210,228)));
            JLabel catLbl = new JLabel("Category:");
            catLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            bar.add(catLbl);
            bar.add(categoryBox);
            bar.add(fetchBtn);

            // Article list
            listPanel = new JPanel();
            listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
            listPanel.setBackground(Color.WHITE);
            scroll = new JScrollPane(listPanel);
            scroll.setBorder(null);
            scroll.getVerticalScrollBar().setUnitIncrement(16);

            // Status bar
            statusLabel = new JLabel("Ready — select a category and click Get News");
            statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
            statusLabel.setForeground(new Color(100, 105, 130));
            JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 4));
            statusBar.setBackground(new Color(245, 246, 250));
            statusBar.setBorder(BorderFactory.createMatteBorder(1,0,0,0, new Color(210,215,230)));
            statusBar.add(statusLabel);

            JPanel top = new JPanel(new BorderLayout());
            top.add(header, BorderLayout.NORTH);
            top.add(bar,    BorderLayout.SOUTH);
            add(top,       BorderLayout.NORTH);
            add(scroll,    BorderLayout.CENTER);
            add(statusBar, BorderLayout.SOUTH);

            loadNews();
        }

        private void loadNews() {
            String cat = ((String) categoryBox.getSelectedItem()).toLowerCase();
            fetchBtn.setEnabled(false);
            statusLabel.setText("Fetching " + cat + " news…");
            listPanel.removeAll();
            JLabel wait = new JLabel("Loading…", SwingConstants.CENTER);
            wait.setFont(new Font("Georgia", Font.ITALIC, 15));
            wait.setForeground(new Color(130, 140, 175));
            listPanel.add(Box.createVerticalStrut(60));
            listPanel.add(wait);
            listPanel.revalidate();
            listPanel.repaint();

            new SwingWorker<List<Article>, Void>() {
                @Override protected List<Article> doInBackground() throws Exception {
                    return service.fetchNews(cat);   // Polymorphism via interface
                }
                @Override protected void done() {
                    try {
                        showArticles(get(), cat);
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause();
                        // Handle custom exception hierarchy
                        String msg = (cause instanceof NewsFeedException)
                                ? cause.getMessage() : ex.getMessage();
                        statusLabel.setText("⚠  Error: " + msg);
                        listPanel.removeAll();
                        JLabel err = new JLabel("⚠  " + msg, SwingConstants.CENTER);
                        err.setForeground(new Color(185, 28, 28));
                        listPanel.add(Box.createVerticalStrut(80));
                        listPanel.add(err);
                        listPanel.revalidate();
                        listPanel.repaint();
                    } finally {
                        fetchBtn.setEnabled(true);
                    }
                }
            }.execute();
        }

        private void showArticles(List<Article> articles, String cat) {
            listPanel.removeAll();
            if (articles == null || articles.isEmpty()) {
                JLabel none = new JLabel("No articles found.", SwingConstants.CENTER);
                none.setForeground(Color.GRAY);
                listPanel.add(Box.createVerticalStrut(80));
                listPanel.add(none);
            } else {
                for (Article a : articles)
                    listPanel.add(new ArticleCard(a));   // Polymorphism: ArticleCard IS-A JPanel
                listPanel.add(Box.createVerticalGlue());
            }
            int count = articles == null ? 0 : articles.size();
            statusLabel.setText(count + " articles  |  " + cat
                    + "  |  Click a headline to open in browser");
            listPanel.revalidate();
            listPanel.repaint();
            SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
        }
    }
}
