// Bit32Vis — interactive Swing demo.
//
// Grug 2-Clause License
// 1. do what want
// 2. not sue grug
//
// Run from the repo root:
//   javac -d out java/bit32vis/*.java && java -cp out bit32vis.DemoApp

package bit32vis;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

public class DemoApp {

    // ── UI palette ────────────────────────────────────────────────────────────
    private static final Color APP_BG    = new Color(0xF2, 0xF2, 0xF7);
    private static final Color CARD_BG   = Color.WHITE;
    private static final Color TEXT_FG   = new Color(0x1C, 0x1C, 0x1E);
    private static final Color MUTED_FG  = new Color(0x8E, 0x8E, 0x93);
    private static final Color BORDER_FG = new Color(0xD1, 0xD1, 0xD6);

    private static JLabel statusLabel;
    private static final List<VisView>   allViews   = new ArrayList<>();
    private static final List<PixelView> pixelViews = new ArrayList<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Bit32Vis");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel root = new JPanel(new BorderLayout());
            root.setBackground(APP_BG);
            root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JScrollPane scroll = new JScrollPane(buildContent());
            scroll.setBorder(null);
            scroll.setBackground(APP_BG);
            scroll.getViewport().setBackground(APP_BG);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            root.add(scroll, BorderLayout.CENTER);

            frame.setContentPane(root);
            frame.setPreferredSize(new Dimension(820, 780));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static JPanel buildContent() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(APP_BG);

        // Input
        JPanel inputCard = makeCard(14);
        inputCard.setLayout(new BoxLayout(inputCard, BoxLayout.Y_AXIS));
        inputCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel inputLabel = makeCaptionLabel("32-BIT INPUT (8 HEX DIGITS, BIG-ENDIAN)");
        inputLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputCard.add(inputLabel);
        inputCard.add(vgap(5));

        JTextField field = new JTextField("89abcdef");
        field.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_FG),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        int fieldH = field.getPreferredSize().height * 2;
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, fieldH));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, fieldH));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputCard.add(field);
        inputCard.add(vgap(8));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        statusLabel.setForeground(MUTED_FG);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputCard.add(statusLabel);

        content.add(inputCard);
        content.add(vgap(20));

        // Styles
        content.add(sectionLabel("Styles"));
        content.add(vgap(8));
        JPanel stylesRow = hrow(
            variantCard(Bit32Vis.Style.STANDARD,      200, "Standard"),
            variantCard(Bit32Vis.Style.HIGH_CONTRAST, 200, "High Contrast"),
            variantCard(Bit32Vis.Style.MONOCHROME,    200, "Monochrome"));
        content.add(stylesRow);
        content.add(vgap(20));

        // Sizes (Standard)
        content.add(sectionLabel("Sizes  (Standard)"));
        content.add(vgap(8));
        JPanel sizesCard = makeCard(12);
        sizesCard.setLayout(new BoxLayout(sizesCard, BoxLayout.X_AXIS));
        sizesCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        boolean first = true;
        for (int sz : new int[]{160, 120, 80, 56, 40, 28}) {
            if (!first) sizesCard.add(hgap(14));
            first = false;
            JPanel sub = vstack(CARD_BG);
            VisView v = new VisView(Bit32Vis.Style.STANDARD, sz);
            allViews.add(v);
            v.setAlignmentX(Component.CENTER_ALIGNMENT);
            sub.add(v);
            sub.add(vgap(4));
            JLabel lbl = makeCaptionLabel(sz + " px");
            lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            sub.add(lbl);
            sizesCard.add(sub);
        }
        sizesCard.add(Box.createHorizontalGlue());
        content.add(sizesCard);
        content.add(vgap(20));

        // Pixel grid ×6
        content.add(sectionLabel("Pixel Grid  (16 \u00d7 22,  \u00d76 zoom)"));
        content.add(vgap(8));
        content.add(pixelRow(6));
        content.add(vgap(20));

        // Pixel grid ×1
        content.add(sectionLabel("Pixel Grid  (16 \u00d7 22,  actual size)"));
        content.add(vgap(8));
        content.add(pixelRow(1));
        content.add(vgap(24));

        // Live update
        updateAll(field.getText());
        Timer debounce = new Timer(80, e -> updateAll(field.getText()));
        debounce.setRepeats(false);
        field.getDocument().addDocumentListener(new DocumentListener() {
            void kick() { debounce.restart(); }
            public void insertUpdate(DocumentEvent e)  { kick(); }
            public void removeUpdate(DocumentEvent e)  { kick(); }
            public void changedUpdate(DocumentEvent e) { kick(); }
        });

        return content;
    }

    private static JPanel pixelRow(int cellPx) {
        JPanel card = makeCard(12);
        card.setLayout(new BoxLayout(card, BoxLayout.X_AXIS));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        boolean first = true;
        for (Bit32Vis.Style style : Bit32Vis.Style.values()) {
            if (!first) card.add(hgap(20));
            first = false;
            JPanel sub = vstack(CARD_BG);
            PixelView pv = new PixelView(style, cellPx);
            pixelViews.add(pv);
            pv.setAlignmentX(Component.CENTER_ALIGNMENT);
            sub.add(pv);
            sub.add(vgap(4));
            JLabel lbl = makeCaptionLabel(styleName(style));
            lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            sub.add(lbl);
            card.add(sub);
        }
        card.add(Box.createHorizontalGlue());
        return card;
    }

    private static void updateAll(String input) {
        try {
            int bits = parseBits(input);
            Bit32Vis.VisSpec visual = Bit32Vis.spec(bits);
            String fallback = visual.fallback() ? " → A| fallback" : "";
            statusLabel.setText(String.format(
                    "unsigned: %d  ·  mixed: %08x  ·  mode: %s%s",
                    Integer.toUnsignedLong(bits), visual.mixed(),
                    visual.preferredMode().label(), fallback));
            allViews.forEach(v -> v.setInput(bits));
            pixelViews.forEach(v -> v.setInput(bits));
        } catch (IllegalArgumentException e) {
            statusLabel.setText(e.getMessage());
        }
    }

    static int parseBits(String input) {
        String hex = input.trim();
        if (hex.startsWith("0x") || hex.startsWith("0X")) hex = hex.substring(2);
        if (!hex.matches("[0-9a-fA-F]{8}"))
            throw new IllegalArgumentException("Enter exactly 8 hexadecimal digits.");
        return (int) Long.parseLong(hex, 16);
    }

    // =========================================================================
    // Views
    // =========================================================================

    static class VisView extends JComponent {
        private final Bit32Vis.Style style;
        private Bit32Vis.VisSpec spec;

        VisView(Bit32Vis.Style style, int w) {
            this.style = style;
            int h = (int) Math.round(w * 22.0 / 16.0);
            Dimension d = new Dimension(w, h);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        void setInput(int bits) {
            spec = Bit32Vis.spec(bits, style);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (spec == null) return;
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                paintVis(g2, spec, getWidth(), getHeight());
            } finally {
                g2.dispose();
            }
        }
    }

    static class PixelView extends JComponent {
        private final Bit32Vis.Style style;
        private final int cellPx;
        private Bit32Vis.PixelGrid grid;

        PixelView(Bit32Vis.Style style, int cellPx) {
            this.style = style;
            this.cellPx = cellPx;
            Dimension d = new Dimension(16 * cellPx, 22 * cellPx);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        void setInput(int bits) {
            grid = Bit32Vis.pixels(bits, style);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (grid == null) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            try {
                byte[] px = grid.pixels();
                int w = grid.width();
                int h = grid.height();
                Color bg = parseHex(grid.background().hex());
                Color fg = parseHex(grid.foreground().hex());
                for (int row = 0; row < h; row++) {
                    for (int col = 0; col < w; col++) {
                        int v = px[row * w + col] & 0xFF;
                        g2.setColor(v == 0 ? bg : fg);
                        g2.fillRect(col * cellPx, row * cellPx, cellPx, cellPx);
                    }
                }
            } finally {
                g2.dispose();
            }
        }
    }

    // =========================================================================
    // Paint logic — continuous 16×22 viewBox matching the exact pixel topology.
    // =========================================================================

    static void paintVis(Graphics2D g2, Bit32Vis.VisSpec spec, int width, int height) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,      RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        double scale = Math.min(width / 16.0, height / 22.0);
        double w  = 16 * scale, h = 22 * scale;
        double ox = (width  - w) / 2.0, oy = (height - h) / 2.0;

        // Background tile
        g2.setColor(parseHex(spec.background().hex()));
        g2.fill(new RoundRectangle2D.Double(ox, oy, w, h,
                2.0 * scale, 2.0 * scale));

        // Build one union before painting. Rendering overlapping antialiased
        // primitives separately can leave hairline background seams.
        Area foreground = new Area();
        for (int row = 0; row < Bit32Vis.ROWS; row++) {
            for (int column = 0; column < Bit32Vis.COLUMNS; column++) {
                if (spec.cells()[row][column] == 0) continue;
                double x = ox + (1 + column * 3) * scale;
                double y = oy + (1 + row * 3) * scale;
                // The corner radius is half the cell width. Consequently, a
                // terminal cell forms a true semicircular end cap.
                foreground.add(new Area(new RoundRectangle2D.Double(
                        x, y, 2 * scale, 2 * scale, 2 * scale, 2 * scale)));
            }
        }

        Bit32Vis.Edge[] edges = Bit32Vis.edges();
        for (int i = 0; i < edges.length; i++) {
            if (spec.connections()[i] == 0) continue;
            Bit32Vis.Edge edge = edges[i];
            double x = ox + (1 + edge.startColumn() * 3) * scale;
            double y = oy + (1 + edge.startRow() * 3) * scale;
            if (edge.startRow() == edge.endRow()) {
                foreground.add(new Area(new Rectangle2D.Double(
                        x + scale, y, 3 * scale, 2 * scale)));
            } else {
                foreground.add(new Area(new Rectangle2D.Double(
                        x, y + scale, 2 * scale, 3 * scale)));
            }
        }

        // A four-cell cycle closes its one-unit central junction.
        for (int row = 0; row < Bit32Vis.ROWS - 1; row++) {
            for (int column = 0; column < Bit32Vis.COLUMNS - 1; column++) {
                if (edgeValue(spec, row, column, row, column + 1) == 1
                        && edgeValue(spec, row + 1, column, row + 1, column + 1) == 1
                        && edgeValue(spec, row, column, row + 1, column) == 1
                        && edgeValue(spec, row, column + 1, row + 1, column + 1) == 1) {
                    foreground.add(new Area(new Rectangle2D.Double(
                            ox + (3 + column * 3) * scale,
                            oy + (3 + row * 3) * scale,
                            scale, scale)));
                }
            }
        }

        g2.setColor(parseHex(spec.foreground().hex()));
        g2.fill(foreground);
    }

    private static int edgeValue(
            Bit32Vis.VisSpec spec, int startRow, int startColumn,
            int endRow, int endColumn) {
        Bit32Vis.Edge[] edges = Bit32Vis.edges();
        for (int i = 0; i < edges.length; i++) {
            Bit32Vis.Edge edge = edges[i];
            if (edge.startRow() == startRow && edge.startColumn() == startColumn
                    && edge.endRow() == endRow && edge.endColumn() == endColumn) {
                return spec.connections()[i];
            }
        }
        return 0;
    }

    // =========================================================================
    // UI helpers
    // =========================================================================

    private static JPanel variantCard(Bit32Vis.Style style, int size, String label) {
        JPanel card = makeCard(12);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        VisView v = new VisView(style, size);
        allViews.add(v);
        v.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(v);
        card.add(vgap(6));
        JLabel lbl = makeCaptionLabel(label);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lbl);
        return card;
    }

    private static JPanel hrow(JPanel a, JPanel b, JPanel c) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setBackground(APP_BG);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(a);
        row.add(hgap(12));
        row.add(b);
        row.add(hgap(12));
        row.add(c);
        Dimension pref = row.getPreferredSize();
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
        return row;
    }

    static Color parseHex(String hex) {
        return new Color(
            Integer.parseInt(hex.substring(1, 3), 16),
            Integer.parseInt(hex.substring(3, 5), 16),
            Integer.parseInt(hex.substring(5, 7), 16));
    }

    private static JPanel makeCard(int pad) {
        JPanel p = new JPanel();
        p.setBackground(CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_FG),
            BorderFactory.createEmptyBorder(pad, pad, pad, pad)));
        return p;
    }

    private static JPanel vstack(Color bg) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(bg);
        return p;
    }

    private static JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        l.setForeground(TEXT_FG);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel makeCaptionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        l.setForeground(MUTED_FG);
        return l;
    }

    private static Component vgap(int h) { return Box.createVerticalStrut(h); }
    private static Component hgap(int w) { return Box.createHorizontalStrut(w); }

    private static String styleName(Bit32Vis.Style style) {
        return switch (style) {
            case STANDARD      -> "Standard";
            case HIGH_CONTRAST -> "High Contrast";
            case MONOCHROME    -> "Monochrome";
        };
    }
}
