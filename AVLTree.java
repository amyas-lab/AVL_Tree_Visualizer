package avltree;

// A single node in the AVL tree
class Node {
    int key;      // value stored in this node
    int height;   // height of the subtree rooted here
    Node left;
    Node right;

    Node(int key) {
        this.key    = key;
        this.height = 1;   // a new node is always a leaf
        this.left   = null;
        this.right  = null;
    }
}

// Self-balancing binary search tree (AVL)
// Automatically rotates after every insert / delete to stay balanced
public class AVLTree {

    private Node   root;
    private String lastRotation = "none";   // set by balance(), reset on each public op

    // Public API

    public Node   getRoot()          { return root; }
    public String getLastRotation()  { return lastRotation; }

    public void insert(int key) { lastRotation = "none"; root = insertRec(root, key); }

    public void delete(int key) { lastRotation = "none"; root = deleteRec(root, key); }

    public void clear() { root = null; }

    // Returns true if the tree contains the given key
    public boolean contains(int key) { return findNode(root, key) != null; }

    // Height & Balance Factor 

    // Returns 0 for null nodes to avoid null checks everywhere
    private int height(Node n) { return (n == null) ? 0 : n.height; }

    private void updateHeight(Node n) {
        n.height = 1 + Math.max(height(n.left), height(n.right));
    }

    // balance factor = height(left) - height(right)
    // > 1  → left-heavy  (need rotation)
    // < -1 → right-heavy (need rotation)
    private int balanceFactor(Node n) {
        return (n == null) ? 0 : height(n.left) - height(n.right);
    }

    // Rotations
    // Left rotation — fixes RR imbalance

    private Node rotateLeft(Node z) {
        Node y  = z.right;
        Node T2 = y.left;

        y.left  = z;
        z.right = T2;

        updateHeight(z);  // update z first because it is now lower than y
        updateHeight(y);

        return y;  // y is the new root of this subtree
    }

    // Right rotation — fixes LL imbalance
  
    private Node rotateRight(Node z) {
        Node y  = z.left;
        Node T3 = y.right;

        y.right = z;
        z.left  = T3;

        updateHeight(z);
        updateHeight(y);

        return y;
    }

    // Left-Right rotation — fixes LR imbalance
    // Step 1: rotate left  on the left child  → becomes LL
    // Step 2: rotate right on the current node
    private Node rotateLeftRight(Node z) {
        z.left = rotateLeft(z.left);
        return rotateRight(z);
    }

    // Right-Left rotation — fixes RL imbalance
    // Step 1: rotate right on the right child → becomes RR
    // Step 2: rotate left  on the current node
    private Node rotateRightLeft(Node z) {
        z.right = rotateRight(z.right);
        return rotateLeft(z);
    }

    // Balance 

    // Called on every node on the way back up after insert / delete.
    // Checks the balance factor and applies the correct rotation if needed.
    private Node balance(Node node) {
        updateHeight(node);
        int bf = balanceFactor(node);

        if (bf > 1) {
            if (balanceFactor(node.left) >= 0) {
                lastRotation = "Right rotation (LL)";
                return rotateRight(node);
            }
            lastRotation = "Left-Right rotation (LR)";
            return rotateLeftRight(node);
        }
        if (bf < -1) {
            if (balanceFactor(node.right) <= 0) {
                lastRotation = "Left rotation (RR)";
                return rotateLeft(node);
            }
            lastRotation = "Right-Left rotation (RL)";
            return rotateRightLeft(node);
        }

        return node;  // already balanced
    }

    // Insert

    private Node insertRec(Node node, int key) {
        if (node == null) return new Node(key);  // found the correct spot

        if      (key < node.key) node.left  = insertRec(node.left,  key);
        else if (key > node.key) node.right = insertRec(node.right, key);
        else    return node;  // duplicate key — do nothing

        return balance(node);  // re-balance on the way back up
    }

    // Delete 

    private Node deleteRec(Node node, int key) {
        if (node == null) return null;  // key not found

        if      (key < node.key) node.left  = deleteRec(node.left,  key);
        else if (key > node.key) node.right = deleteRec(node.right, key);
        else {
            // Found the node to delete
            if (node.left == null || node.right == null) {
                // Case 1 & 2: leaf or one child — replace with the child
                node = (node.left != null) ? node.left : node.right;
            } else {
                // Case 3: two children
                // Replace value with in-order successor (smallest in right subtree)
                // then delete the successor from the right subtree
                Node successor = findMin(node.right);
                node.key   = successor.key;
                node.right = deleteRec(node.right, successor.key);
            }
        }

        if (node == null) return null;  // deleted a leaf
        return balance(node);
    }

    // Utilities 

    // Finds the leftmost (smallest) node in a subtree
    private Node findMin(Node node) {
        while (node.left != null) node = node.left;
        return node;
    }

    private Node findNode(Node node, int key) {
        if (node == null)    return null;
        if (key == node.key) return node;
        return (key < node.key)
            ? findNode(node.left,  key)
            : findNode(node.right, key);
    }

    // Debug 

    // Prints every node in ascending order — useful for quick sanity checks
    public void printInOrder() {
        System.out.print("In-order: ");
        printInOrderRec(root);
        System.out.println();
    }

    private void printInOrderRec(Node node) {
        if (node == null) return;
        printInOrderRec(node.left);
        System.out.print(node.key + "(h=" + node.height + ", bf=" + balanceFactor(node) + ") ");
        printInOrderRec(node.right);
    }

    // Returns true if every node in the tree satisfies the AVL property
    public boolean isValidAVL() { return checkAVL(root); }

    private boolean checkAVL(Node node) {
        if (node == null) return true;
        int bf = balanceFactor(node);
        if (bf > 1 || bf < -1) {
            System.err.println("AVL violation at node " + node.key + ", BF=" + bf);
            return false;
        }
        return checkAVL(node.left) && checkAVL(node.right);
    }
    
}