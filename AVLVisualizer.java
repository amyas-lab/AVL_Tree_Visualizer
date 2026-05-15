package avltree;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Random;

public class AVLVisualizer {

    private final AVLTree tree = new AVLTree(); // Implements AVL tree logic
    private TreePanel  treePanel; // Custom JPanel to draw the tree
    private JLabel     statusLabel; // To notify user of what is going on
    private JTextField inputField; // To input numbers

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new AVLVisualizer()::launch);
    }

    private void launch() { // Create the main frame and add all components to it
        JFrame frame = new JFrame("AVL Tree Visualizer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 680);

        treePanel = new TreePanel(tree);

        // ── Input field ──────────────────────────────────────────────────────
        inputField = new JTextField(6);
        inputField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        inputField.addActionListener(e -> handleInsert()); // Enter key = Insert

        // ── Buttons ──────────────────────────────────────────────────────────
        JButton btnInsert = btn("Insert",      e -> handleInsert());
        JButton btnDelete = btn("Delete",      e -> handleDelete());
        JButton btnClear  = btn("Clear",       e -> handleClear());
        JButton btnRandom = btn("Random Fill", e -> handleRandom());

        styleButton(btnInsert, new Color(60, 160, 60),  Color.WHITE);
        styleButton(btnDelete, new Color(200, 70, 60),  Color.WHITE);
        styleButton(btnClear,  new Color(120, 120, 140), Color.WHITE);
        styleButton(btnRandom, new Color(70, 130, 180), Color.WHITE);

        // ── Status label ─────────────────────────────────────────────────────
        statusLabel = new JLabel("Enter a number and press Insert.");
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 13));
        statusLabel.setForeground(new Color(50, 50, 120));

        // ── Control bar ──────────────────────────────────────────────────────
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        south.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        south.setBackground(new Color(250, 250, 255));
        south.add(new JLabel("Value:"));
        south.add(inputField);
        south.add(btnInsert);
        south.add(btnDelete);
        south.add(btnClear);
        south.add(btnRandom);
        south.add(Box.createHorizontalStrut(16));
        south.add(statusLabel);

        // ── Title bar ────────────────────────────────────────────────────────
        JLabel title = new JLabel("  AVL Tree Visualizer", JLabel.LEFT);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(new Color(40, 80, 160));
        title.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 0));
        title.setBackground(new Color(230, 240, 255));
        title.setOpaque(true);

        frame.setLayout(new BorderLayout());
        frame.add(title, BorderLayout.NORTH);
        frame.add(new JScrollPane(treePanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
        frame.add(south, BorderLayout.SOUTH);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ── Event handlers ───────────────────────────────────────────────────────

    private void handleInsert() {
        String text = inputField.getText().trim();
        try {
            int key = Integer.parseInt(text);
            if (tree.contains(key)) { status("Key " + key + " already exists in the tree."); return; }
            tree.insert(key);
            treePanel.onInsert(key); // From the TreePanel class, tells panel to animate the new node
            String rot = tree.getLastRotation(); // Gets the last rotation type from the AVLTree class
            status("Inserted " + key + (rot.equals("none") ? "." : "  →  " + rot + ".")
                   + "   Tree height: " + treeHeight());
            inputField.selectAll(); 
        } catch (NumberFormatException ex) {
            status("Invalid input — please enter an integer.");
        }
    }

    private void handleDelete() {
        String text = inputField.getText().trim();
        try {
            int key = Integer.parseInt(text);
            if (!tree.contains(key)) { status("Key " + key + " is not in the tree."); return; }
            tree.delete(key);
            treePanel.onDelete(key);
            String rot = tree.getLastRotation();
            status("Deleted " + key + (rot.equals("none") ? "." : "  →  " + rot + ".")
                   + "   Tree height: " + treeHeight());
            inputField.selectAll();
        } catch (NumberFormatException ex) {
            status("Invalid input — please enter an integer.");
        }
    }

    private void handleClear() {
        tree.clear();
        treePanel.clear();
        status("Tree cleared.");
    }

    private void handleRandom() {
        Random rng = new Random();
        int added = 0;
        while (added < 8) {
            int k = rng.nextInt(99) + 1;
            if (!tree.contains(k)) { tree.insert(k); added++; }
        }
        treePanel.snapToTree();
        status("Inserted 8 random values.   Tree height: " + treeHeight());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void status(String msg) { statusLabel.setText(msg); }

    private int treeHeight() {
        Node r = tree.getRoot();
        return r == null ? 0 : r.height;
    }

    private JButton btn(String label, ActionListener al) {
        JButton b = new JButton(label);
        b.addActionListener(al);
        return b;
    }

    private void styleButton(JButton b, Color bg, Color fg) {
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));
        b.setOpaque(true);
    }
}
