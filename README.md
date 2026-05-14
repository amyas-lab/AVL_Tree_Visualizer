# AVL Tree Visualizer

A Java Swing desktop application that lets you insert and delete integer keys into a self-balancing AVL tree and watch the rotations happen in real time through smooth animations.

---

## How to Run

### Prerequisites
- Java 11 or later (JDK, not just JRE)

### Compile and launch (command line)

```bash
# From the project root directory
javac -d out AVLTree.java TreePanel.java AVLVisualizer.java
java -cp out avltree.AVLVisualizer
```

### From an IDE (IntelliJ / Eclipse)
1. Open the project root as a new Java project.
2. Mark the root as a source root (the files use `package avltree`).
3. Run `AVLVisualizer.main()`.

---

## Using the Application

| Control | Action |
|---|---|
| Type a number → **Enter** or **Insert** | Insert a key into the tree |
| Type a number → **Delete** | Remove that key from the tree |
| **Clear** | Reset the tree and the canvas |
| **Random Fill** | Insert 8 random values (1–100) instantly |

The status bar at the bottom tells you what rotation was triggered (e.g., `Left-Right rotation (LR)`) and the current tree height after each operation.

Each node displays its **balance factor** (`bf=`) below its circle: green means balanced (`bf=0`), orange means tilted.

---

## Architecture — How the Three Files Coordinate

The project follows a clean **MVC** split:

```
AVLTree.java       ← Model  (pure data structure, no UI)
TreePanel.java     ← View   (Swing canvas, animation engine)
AVLVisualizer.java ← Controller (wires Model ↔ View, owns the JFrame)
```

### Typical user flow: inserting a key

```
User types "42" and presses Insert
        │
        ▼
AVLVisualizer.handleInsert()
  1. Parses the input string → int key = 42
  2. Calls tree.contains(42)  → false, so we proceed
  3. Calls tree.insert(42)    → AVLTree mutates its internal nodes,
                                 rotates if needed, records lastRotation
  4. Calls treePanel.onInsert(42)
        │
        ▼
  TreePanel.onInsert(42)
  5. recomputeTargets()  → walks the new tree via in-order traversal,
                            assigns (x, y) target coordinates to every node
  6. Adds the new node to curPos at its target position so it "pops in"
     rather than flying from (0, 0)
  7. Sets highlightKey=42, highlightAlpha=1.0 (green glow)
  8. startAnim() → starts the 16 ms Swing Timer
        │
        ▼  every 16 ms (~60 FPS)
  TreePanel.tick()
  9. For each node: curPos lerps 18 % of the way toward tgtPos →
     nodes slide smoothly to their new positions
 10. highlightAlpha decreases by 0.018 each tick → green glow fades
 11. repaint() → triggers paintComponent()
        │
        ▼
  TreePanel.paintComponent()
 12. drawEdges() first (lines behind circles)
 13. drawNodes() second (circles on top)
 14. When all nodes reach tgtPos and highlightAlpha == 0, Timer stops
        │
        ▼
AVLVisualizer updates statusLabel
 15. Reads tree.getLastRotation() and tree.getRoot().height
 16. Displays "Inserted 42 → Right rotation (LL). Tree height: 3"
```

The delete flow is identical except `treePanel.onDelete(key)` removes the key from `curPos` before recomputing targets, so the node simply disappears and the remaining nodes slide into their new positions.

For **Random Fill**, `AVLVisualizer.handleRandom()` inserts 8 keys in a tight loop and then calls `treePanel.snapToTree()` instead of `onInsert()`, skipping all animations so the final tree appears instantly.

---

## Key Functions Reference

### `AVLTree.java`

| Method | Role |
|---|---|
| `insert(int key)` | Public entry point; resets `lastRotation`, then calls `insertRec` |
| `delete(int key)` | Public entry point; resets `lastRotation`, then calls `deleteRec` |
| `insertRec(Node, int)` | Recursive BST insert; calls `balance()` on the way back up the call stack |
| `deleteRec(Node, int)` | Recursive BST delete; handles leaf / one-child / two-children cases, then calls `balance()` |
| `balance(Node)` | Reads the balance factor and applies the correct rotation (LL, RR, LR, RL); records the rotation name in `lastRotation` |
| `rotateLeft / rotateRight` | Single rotations; rewire child pointers and update heights |
| `rotateLeftRight / rotateRightLeft` | Double rotations (two single rotations chained) |
| `contains(int key)` | Delegates to `findNode`; used by the controller to guard against duplicate inserts |
| `getLastRotation()` | Returns the rotation string for the status bar |

### `TreePanel.java`

| Method | Role |
|---|---|
| `onInsert(int key)` | Called by the controller after `tree.insert()`; recomputes target positions, registers the new node, arms the highlight, starts the animation timer |
| `onDelete(int key)` | Called after `tree.delete()`; removes the deleted key from `curPos`, recomputes targets, starts the timer |
| `snapToTree()` | Recomputes targets and copies them directly to `curPos` — no animation; used after bulk inserts |
| `tick()` | Timer callback (~60 FPS); lerps every node's current position 18 % closer to its target, fades the highlight, stops the timer when everything is settled |
| `startAnim()` | Starts the Swing `Timer` if it is not already running |
| `recomputeTargets()` | In-order traversal of the live tree; assigns evenly-spaced x-coordinates and depth-based y-coordinates to `tgtPos` |
| `paintComponent(Graphics)` | Swing paint entry point; enables anti-aliasing, then calls `drawEdges()` before `drawNodes()` so edges render behind circles |
| `drawEdges() / drawNode()` | Recursive renderers that read from `curPos` (the animated position), not `tgtPos` |
| `clear()` | Stops the timer, wipes both position maps, repaints a blank canvas |

### `AVLVisualizer.java`

| Method | Role |
|---|---|
| `main(String[])` | Entry point; schedules `launch()` on the Swing Event Dispatch Thread via `SwingUtilities.invokeLater` |
| `launch()` | Builds and displays the `JFrame`: title bar, scrollable `TreePanel` in the center, and the control bar at the bottom |
| `handleInsert()` | Parses input, guards against duplicates, calls `tree.insert()` then `treePanel.onInsert()`, updates the status label |
| `handleDelete()` | Parses input, guards against missing keys, calls `tree.delete()` then `treePanel.onDelete()`, updates the status label |
| `handleClear()` | Resets both the `AVLTree` and the `TreePanel` |
| `handleRandom()` | Inserts 8 unique random values in a loop, then calls `treePanel.snapToTree()` |
| `btn() / styleButton()` | Factory helpers for creating and styling `JButton` instances |

---

## Project Structure

```
CS201_final_project_AVL/
├── AVLTree.java        # Self-balancing BST logic + Node class
├── TreePanel.java      # Animated Swing canvas
├── AVLVisualizer.java  # Main class, JFrame, event handlers
├── index.html          # (separate web demo, not part of the Swing app)
└── out/avltree/        # Compiled .class files
    ├── AVLTree.class
    ├── AVLVisualizer.class
    ├── Node.class
    └── TreePanel.class
```
