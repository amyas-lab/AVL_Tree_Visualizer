package avltree;

import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.util.*;

public class TreePanel extends JPanel {

    private static final int R       = 22;   // node radius (pixels)
    private static final int SPACING = 58;   // horizontal gap between in-order nodes
    private static final int V_GAP   = 78;   // vertical gap between tree levels
    private static final int TOP     = 52;   // top margin

    private static final double ZOOM_MIN  = 0.3;
    private static final double ZOOM_MAX  = 2.5;
    static final double         ZOOM_STEP = 0.15;

    private double   zoom         = 1.0;
    private Runnable onZoomChange = null;

    private final AVLTree tree;

    // Animated positions: key → [x, y].  curPos lerps toward tgtPos each tick.
    private final Map<Integer, double[]> curPos = new HashMap<>();
    private final Map<Integer, double[]> tgtPos = new HashMap<>();

    private int    highlightKey   = -1;
    private Color  highlightColor = new Color(60, 200, 60);
    private double highlightAlpha = 0.0;   // 1.0 = fully lit, fades to 0

    private final Timer animTimer = new Timer(16, e -> tick());

    public TreePanel(AVLTree tree) {
        this.tree = tree;
        setBackground(new Color(245, 245, 252));
        addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                setZoom(zoom + (e.getWheelRotation() < 0 ? ZOOM_STEP : -ZOOM_STEP));
                e.consume();
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        int n = countNodes(tree.getRoot());
        int w = (int)(Math.max(800, n * SPACING + 120) * zoom);
        int h = (int)(Math.max(500, (tree.getRoot() == null ? 1 : tree.getRoot().height) * V_GAP + TOP + 60) * zoom);
        return new Dimension(w, h);
    }

    // Called by AVLVisualizer AFTER tree.insert(key)
    public void onInsert(int key) {
        recomputeTargets();
        double[] tgt = tgtPos.get(key);
        if (tgt != null) curPos.putIfAbsent(key, tgt.clone()); // new node appears in place
        highlightKey   = key;
        highlightColor = new Color(50, 205, 50);
        highlightAlpha = 1.0;
        startAnim();
    }

    // Called by AVLVisualizer AFTER tree.delete(key)
    public void onDelete(int deletedKey) {
        curPos.remove(deletedKey);
        recomputeTargets();
        highlightKey = -1;
        startAnim();
    }

    // Snap all nodes instantly to their positions (used after bulk inserts)
    public void snapToTree() {
        recomputeTargets();
        curPos.clear();
        for (Map.Entry<Integer, double[]> e : tgtPos.entrySet())
            curPos.put(e.getKey(), e.getValue().clone());
        repaint();
    }

    public void clear() {
        animTimer.stop();
        curPos.clear();
        tgtPos.clear();
        highlightKey = -1;
        repaint();
    }

    // ── Animation ────────────────────────────────────────────────────────────
    // Instead of jumping to the target, the node moves by a fract 0.18 of the distance toward it
        // This creates a sliding effect
    private void tick() {
        boolean busy = false;

        for (Map.Entry<Integer, double[]> e : tgtPos.entrySet()) {
            double[] tgt = e.getValue();
            double[] cur = curPos.computeIfAbsent(e.getKey(), k -> tgt.clone());
            double dx = tgt[0] - cur[0], dy = tgt[1] - cur[1];
            if (Math.abs(dx) > 0.5 || Math.abs(dy) > 0.5) {
                cur[0] += dx * 0.18;
                cur[1] += dy * 0.18;
                busy = true; // Sill in motion, not stop yet
            } else { cur[0] = tgt[0]; cur[1] = tgt[1]; } // If the distance difference is < 0.5, set it to the target right away
        }
        curPos.keySet().retainAll(tgtPos.keySet());

        if (highlightAlpha > 0) { highlightAlpha -= 0.018; busy = true; }
        if (highlightAlpha < 0)   highlightAlpha = 0;

        repaint();
        if (!busy) animTimer.stop(); // Stop the timer if nothing is moving
    }

    private void startAnim() { if (!animTimer.isRunning()) animTimer.start(); }

    // ── Layout ───────────────────────────────────────────────────────────────

    private void recomputeTargets() {
        tgtPos.clear();
        int n = countNodes(tree.getRoot());
        if (n == 0) { revalidate(); return; }
        int logW   = (int) Math.max(Math.max(getWidth() / zoom, 800), (n + 1) * SPACING);
        int startX = (logW - (n - 1) * SPACING) / 2;
        int[] idx  = {0};
        placeNodes(tree.getRoot(), 0, idx, startX);
        revalidate();
    }

    // In-Order Traversal and incrementing index idx[0]++ during inorder walk to ensure the smallest keys
    // are assigned the leftmost X-coordinates, and the largest keys the rightmost
    private void placeNodes(Node node, int depth, int[] idx, int startX) {
        if (node == null) return;
        placeNodes(node.left,  depth + 1, idx, startX);
        tgtPos.put(node.key, new double[]{ startX + idx[0]++ * SPACING, TOP + depth * V_GAP });
        placeNodes(node.right, depth + 1, idx, startX);
    }

    private int countNodes(Node n) {
        return n == null ? 0 : 1 + countNodes(n.left) + countNodes(n.right);
    }

    // ── Painting ─────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.scale(zoom, zoom);
        drawEdges(g2, tree.getRoot());
        drawNodes(g2, tree.getRoot());
    }

    private void drawEdges(Graphics2D g2, Node node) {
        if (node == null) return;
        double[] pos = curPos.get(node.key);
        if (pos == null) return;
        g2.setColor(new Color(160, 160, 185));
        g2.setStroke(new BasicStroke(1.8f));
        drawEdge(g2, pos, node.left);
        drawEdge(g2, pos, node.right);
        drawEdges(g2, node.left);
        drawEdges(g2, node.right);
    }

    private void drawEdge(Graphics2D g2, double[] from, Node child) {
        if (child == null) return;
        double[] to = curPos.get(child.key);
        if (to != null) g2.drawLine((int) from[0], (int) from[1], (int) to[0], (int) to[1]);
    }

    private void drawNodes(Graphics2D g2, Node node) {
        if (node == null) return;
        drawNodes(g2, node.left);
        drawNodes(g2, node.right);
        drawNode(g2, node);
    }

    private void drawNode(Graphics2D g2, Node node) {
        double[] pos = curPos.get(node.key);
        if (pos == null) return;
        int cx = (int) pos[0], cy = (int) pos[1];

        // Fill: highlighted (fading) or plain light-blue
        Color base = new Color(173, 216, 230);
        Color fill = (node.key == highlightKey && highlightAlpha > 0)
                     ? blend(highlightColor, base, (float) Math.min(1.0, highlightAlpha))
                     : base;

        g2.setColor(fill);
        g2.fillOval(cx - R, cy - R, 2*R, 2*R);
        g2.setColor(new Color(70, 130, 180));
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(cx - R, cy - R, 2*R, 2*R);

        // Node value (centered)
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        String keyStr = String.valueOf(node.key);
        g2.drawString(keyStr, cx - fm.stringWidth(keyStr)/2, cy + fm.getAscent()/2 - 1);

        // Balance factor label (small, below the circle)
        int bf = (node.left  == null ? 0 : node.left.height)
               - (node.right == null ? 0 : node.right.height);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2.setColor(bf == 0 ? new Color(0, 140, 0) : new Color(210, 80, 0));
        String bfStr = "bf=" + bf;
        FontMetrics sfm = g2.getFontMetrics();
        g2.drawString(bfStr, cx - sfm.stringWidth(bfStr)/2, cy + R + 13);
    }

    // ── Zoom ─────────────────────────────────────────────────────────────────

    public void setZoom(double z) {
        zoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, z));
        recomputeTargets();
        revalidate();
        repaint();
        if (onZoomChange != null) onZoomChange.run();
    }

    public double getZoom() { return zoom; }

    public void setOnZoomChange(Runnable r) { onZoomChange = r; }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Color blend(Color a, Color b, float t) {
        return new Color(
            clamp((int)(a.getRed()   * t + b.getRed()   * (1 - t))),
            clamp((int)(a.getGreen() * t + b.getGreen() * (1 - t))),
            clamp((int)(a.getBlue()  * t + b.getBlue()  * (1 - t)))
        );
    }

    private int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
